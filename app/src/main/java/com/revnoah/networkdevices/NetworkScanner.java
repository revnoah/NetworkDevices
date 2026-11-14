package com.revnoah.networkdevices;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class NetworkScanner {
    interface Listener {
        void onScanStarted(String networkKey, String networkLabel, int totalHosts);

        void onDeviceFound(DeviceInfo device);

        void onProgress(int scannedHosts, int totalHosts);

        void onScanFinished(int deviceCount);

        void onScanFailed(String message);
    }

    private static final int HOST_COUNT = 254;
    private static final int PING_TIMEOUT_MS = 1000;
    private static final int TCP_TIMEOUT_MS = 450;
    private static final int HTTP_TIMEOUT_MS = 1200;
    private static final int[] COMMON_PORTS = {
            22, 23, 53, 80, 139, 443, 445, 548, 631, 8000, 8080, 8443, 9000, 9090
    };
    private final Context context;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    NetworkScanner(Context context) {
        this.context = context.getApplicationContext();
    }

    void cancel() {
        cancelled.set(true);
    }

    void scan(Listener listener) {
        cancelled.set(false);
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            listener.onScanFailed("Wi-Fi service is unavailable.");
            return;
        }

        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo == null || dhcpInfo.ipAddress == 0) {
            listener.onScanFailed("Connect to Wi-Fi to scan the local network.");
            return;
        }

        NetworkIdentity networkIdentity = getCurrentNetworkIdentity();
        if (networkIdentity == null) {
            listener.onScanFailed("Connect to Wi-Fi to scan the local network.");
            return;
        }

        int localIp = dhcpInfo.ipAddress;
        int subnetBase = localIp & 0x00FFFFFF;
        listener.onScanStarted(networkIdentity.key, networkIdentity.label, HOST_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(32);
        ExecutorCompletionService<DeviceInfo> completionService = new ExecutorCompletionService<>(executor);
        List<Future<DeviceInfo>> futures = new ArrayList<>();

        for (int host = 1; host <= HOST_COUNT; host++) {
            final int hostAddress = subnetBase | (host << 24);
            futures.add(completionService.submit(() -> probeHost(hostAddress)));
        }

        int scanned = 0;
        int found = 0;
        try {
            while (scanned < futures.size() && !cancelled.get()) {
                Future<DeviceInfo> future = completionService.poll(250, TimeUnit.MILLISECONDS);
                if (future == null) {
                    continue;
                }
                scanned++;
                DeviceInfo device = future.get();
                if (device != null) {
                    found++;
                    listener.onDeviceFound(device);
                }
                listener.onProgress(scanned, futures.size());
            }
        } catch (Exception exception) {
            listener.onScanFailed("Scan failed: " + exception.getMessage());
        } finally {
            for (Future<DeviceInfo> future : futures) {
                future.cancel(true);
            }
            executor.shutdownNow();
        }

        listener.onScanFinished(found);
    }

    private DeviceInfo probeHost(int hostAddress) {
        if (cancelled.get()) {
            return null;
        }

        String ipAddress = Formatter.formatIpAddress(hostAddress);
        PingResult pingResult = ping(ipAddress);
        List<ServiceProbe> openServices = scanCommonPorts(ipAddress);
        if (!pingResult.reachable && openServices.isEmpty()) {
            return null;
        }

        String macAddress = readMacAddress(ipAddress);
        String hostname = resolveHostname(hostAddress, ipAddress);
        String netBiosName = resolveNetBiosName(ipAddress);
        String names = summarizeNames(hostname, netBiosName);
        String manufacturer = ManufacturerLookup.lookup(macAddress);
        String services = summarizeServices(openServices);
        WebSummary webSummary = summarizeWeb(ipAddress, openServices);
        return new DeviceInfo(ipAddress, macAddress, hostname, manufacturer, pingResult.label, names, services, webSummary.label, webSummary.urls, "");
    }

    NetworkIdentity getCurrentNetworkIdentity() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return null;
        }

        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo == null || dhcpInfo.ipAddress == 0) {
            return null;
        }

        int localIp = dhcpInfo.ipAddress;
        int subnetBase = localIp & 0x00FFFFFF;
        String localIpLabel = Formatter.formatIpAddress(localIp);
        String networkKey = Formatter.formatIpAddress(subnetBase) + "/24";
        String networkLabel = networkKey + " from " + localIpLabel;
        return new NetworkIdentity(networkKey, networkLabel);
    }

    private PingResult ping(String ipAddress) {
        PingResult shellResult = pingWithCommand(ipAddress);
        if (shellResult.reachable) {
            return shellResult;
        }

        long start = System.nanoTime();
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            boolean reachable = address.isReachable(PING_TIMEOUT_MS);
            if (reachable) {
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                return new PingResult(true, elapsedMs + " ms");
            }
        } catch (IOException ignored) {
        }
        return new PingResult(false, "No response");
    }

    private PingResult pingWithCommand(String ipAddress) {
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder("/system/bin/ping", "-c", "1", "-W", "1", ipAddress);
            builder.redirectErrorStream(true);
            process = builder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                return new PingResult(false, "No response");
            }

            String latency = parseLatency(output.toString());
            return new PingResult(true, latency == null ? "Responded" : latency + " ms");
        } catch (Exception ignored) {
            return new PingResult(false, "No response");
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private String parseLatency(String pingOutput) {
        String marker = "time=";
        int start = pingOutput.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = pingOutput.indexOf(" ms", start);
        if (end < 0) {
            return null;
        }
        return pingOutput.substring(start, end).trim();
    }

    private String resolveHostname(int hostAddress, String ipAddress) {
        try {
            byte[] bytes = new byte[]{
                    (byte) (hostAddress & 0xff),
                    (byte) ((hostAddress >> 8) & 0xff),
                    (byte) ((hostAddress >> 16) & 0xff),
                    (byte) ((hostAddress >> 24) & 0xff)
            };
            InetAddress address = InetAddress.getByAddress(bytes);
            String hostName = address.getCanonicalHostName();
            return hostName == null || hostName.equals(ipAddress) ? "Unknown" : hostName;
        } catch (UnknownHostException exception) {
            return "Unknown";
        }
    }

    private String resolveNetBiosName(String ipAddress) {
        byte[] query = new byte[]{
                0x12, 0x34, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x20, 0x43, 0x4B, 0x41,
                0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x41, 0x41, 0x00, 0x00, 0x21,
                0x00, 0x01
        };

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(700);
            InetAddress address = InetAddress.getByName(ipAddress);
            socket.send(new DatagramPacket(query, query.length, address, 137));

            byte[] buffer = new byte[576];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            return parseNetBiosName(Arrays.copyOf(buffer, response.getLength()));
        } catch (IOException ignored) {
            return "Unknown";
        }
    }

    private String parseNetBiosName(byte[] response) {
        for (int i = 57; i + 18 <= response.length; i += 18) {
            String name = new String(response, i, 15).trim();
            int suffix = response[i + 15] & 0xff;
            if (!name.isEmpty() && suffix == 0x00) {
                return name;
            }
        }
        return "Unknown";
    }

    private List<ServiceProbe> scanCommonPorts(String ipAddress) {
        List<ServiceProbe> openServices = new ArrayList<>();
        for (int port : COMMON_PORTS) {
            if (cancelled.get()) {
                break;
            }
            if (isTcpPortOpen(ipAddress, port)) {
                openServices.add(new ServiceProbe(port, serviceName(port)));
            }
        }
        return openServices;
    }

    private boolean isTcpPortOpen(String ipAddress, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, port), TCP_TIMEOUT_MS);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private String serviceName(int port) {
        switch (port) {
            case 22:
                return "SSH";
            case 23:
                return "Telnet";
            case 53:
                return "DNS";
            case 80:
                return "HTTP";
            case 139:
                return "NetBIOS";
            case 443:
                return "HTTPS";
            case 445:
                return "SMB";
            case 548:
                return "AFP";
            case 631:
                return "IPP";
            case 8000:
            case 8080:
            case 8443:
            case 9000:
            case 9090:
                return "Web/Admin";
            default:
                return "TCP";
        }
    }

    private String summarizeNames(String hostname, String netBiosName) {
        List<String> names = new ArrayList<>();
        if (!"Unknown".equals(hostname)) {
            names.add("DNS " + hostname);
        }
        if (!"Unknown".equals(netBiosName) && !netBiosName.equalsIgnoreCase(hostname)) {
            names.add("NetBIOS " + netBiosName);
        }
        return names.isEmpty() ? "Unknown" : join(names, ", ");
    }

    private String summarizeServices(List<ServiceProbe> openServices) {
        if (openServices.isEmpty()) {
            return "None detected";
        }

        List<String> labels = new ArrayList<>();
        for (ServiceProbe service : openServices) {
            labels.add(service.port + "/" + service.name);
        }
        return join(labels, ", ");
    }

    private WebSummary summarizeWeb(String ipAddress, List<ServiceProbe> openServices) {
        List<String> webSummaries = new ArrayList<>();
        List<String> webUrls = new ArrayList<>();
        for (ServiceProbe service : openServices) {
            if (service.port == 80 || service.port == 8000 || service.port == 8080 || service.port == 9000 || service.port == 9090) {
                webUrls.add("http://" + ipAddress + ":" + service.port + "/");
                webSummaries.add(fetchWebInfo("http", ipAddress, service.port));
            } else if (service.port == 443 || service.port == 8443) {
                webUrls.add("https://" + ipAddress + ":" + service.port + "/");
                webSummaries.add(fetchWebInfo("https", ipAddress, service.port));
            }
        }
        if (webSummaries.isEmpty()) {
            return new WebSummary("No public web server detected", "");
        }
        return new WebSummary(join(webSummaries, " | "), join(webUrls, "|"));
    }

    private String fetchWebInfo(String scheme, String ipAddress, int port) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(scheme + "://" + ipAddress + ":" + port + "/");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "NetworkDevicesScanner/1.0");

            int status = connection.getResponseCode();
            String server = connection.getHeaderField("Server");
            String location = connection.getHeaderField("Location");
            String title = readTitle(connection);
            List<String> parts = new ArrayList<>();
            parts.add(scheme.toUpperCase(Locale.US) + ":" + port + " " + status);
            if (server != null && !server.trim().isEmpty()) {
                parts.add("Server " + compact(server));
            }
            if (title != null && !title.isEmpty()) {
                parts.add("Title " + compact(title));
            }
            if (location != null && !location.trim().isEmpty()) {
                parts.add("Redirect " + compact(location));
            }
            return join(parts, ", ");
        } catch (Exception exception) {
            return scheme.toUpperCase(Locale.US) + ":" + port + " open";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readTitle(HttpURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            char[] chars = new char[4096];
            int length = reader.read(chars);
            if (length <= 0) {
                return null;
            }
            String body = new String(chars, 0, length);
            String lowerBody = body.toLowerCase(Locale.US);
            int start = lowerBody.indexOf("<title");
            if (start < 0) {
                return null;
            }
            start = lowerBody.indexOf(">", start);
            int end = lowerBody.indexOf("</title>", start);
            if (start < 0 || end < 0 || end <= start) {
                return null;
            }
            return body.substring(start + 1, end).replaceAll("\\s+", " ").trim();
        } catch (IOException exception) {
            return null;
        }
    }

    private String compact(String value) {
        String compacted = value.replaceAll("\\s+", " ").trim();
        return compacted.length() > 80 ? compacted.substring(0, 77) + "..." : compacted;
    }

    private String join(List<String> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private String readMacAddress(String ipAddress) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream("/proc/net/arp")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.trim().split("\\s+");
                if (columns.length >= 4 && columns[0].equals(ipAddress)) {
                    String mac = columns[3].toUpperCase(Locale.US);
                    return "00:00:00:00:00:00".equals(mac) ? "Unknown" : mac;
                }
            }
        } catch (IOException ignored) {
        }
        return "Unknown";
    }

    private static final class PingResult {
        final boolean reachable;
        final String label;

        PingResult(boolean reachable, String label) {
            this.reachable = reachable;
            this.label = label;
        }
    }

    private static final class ServiceProbe {
        final int port;
        final String name;

        ServiceProbe(int port, String name) {
            this.port = port;
            this.name = name;
        }
    }

    private static final class WebSummary {
        final String label;
        final String urls;

        WebSummary(String label, String urls) {
            this.label = label;
            this.urls = urls;
        }
    }
}
