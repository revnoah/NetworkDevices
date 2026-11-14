package com.revnoah.networkdevices;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final List<DeviceInfo> devices = new ArrayList<>();
    private DeviceAdapter adapter;
    private NetworkScanner scanner;
    private ExecutorService scanExecutor;
    private DeviceCache cache;
    private Button scanButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private TextView networkSummary;
    private String currentNetworkKey;
    private String currentNetworkLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanButton = findViewById(R.id.scanButton);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        networkSummary = findViewById(R.id.networkSummary);
        ListView deviceList = findViewById(R.id.deviceList);

        cache = new DeviceCache(this);
        scanner = new NetworkScanner(this);

        adapter = new DeviceAdapter(this, devices);
        deviceList.setAdapter(adapter);
        deviceList.setOnItemClickListener((parent, view, position, id) -> openDeviceDetails(devices.get(position)));

        scanButton.setOnClickListener(view -> startScan());
        loadCachedDevicesForCurrentNetwork();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scanExecutor == null || scanExecutor.isShutdown()) {
            loadCachedDevicesForCurrentNetwork();
        }
    }

    @Override
    protected void onDestroy() {
        if (scanner != null) {
            scanner.cancel();
        }
        if (scanExecutor != null) {
            scanExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    private void startScan() {
        if (scanExecutor != null && !scanExecutor.isShutdown()) {
            scanner.cancel();
            scanExecutor.shutdownNow();
        }

        NetworkIdentity networkIdentity = scanner.getCurrentNetworkIdentity();
        if (networkIdentity == null) {
            statusText.setText(R.string.wifi_unavailable);
            networkSummary.setText(R.string.wifi_unavailable);
            return;
        }

        currentNetworkKey = networkIdentity.key;
        currentNetworkLabel = networkIdentity.label;
        cache.clearIfDifferentNetwork(currentNetworkKey);
        List<DeviceInfo> cachedDevices = cache.loadDevices(currentNetworkKey);

        devices.clear();
        adapter.notifyDataSetChanged();
        scanButton.setEnabled(false);
        scanButton.setText(R.string.scanning);
        statusText.setText("Starting scan...");
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);

        scanExecutor = Executors.newSingleThreadExecutor();
        scanExecutor.execute(() -> scanner.scan(new NetworkScanner.Listener() {
            @Override
            public void onScanStarted(String networkKey, String networkLabel, int totalHosts) {
                runOnUiThread(() -> {
                    currentNetworkKey = networkKey;
                    currentNetworkLabel = networkLabel;
                    networkSummary.setText("Network: " + networkLabel);
                    progressBar.setMax(totalHosts);
                    statusText.setText("Scanning 0 of " + totalHosts + " hosts");
                });
            }

            @Override
            public void onDeviceFound(DeviceInfo device) {
                runOnUiThread(() -> {
                    DeviceInfo cachedDevice = findDevice(cachedDevices, device.ipAddress);
                    upsertDevice(device.withCachedNameFrom(cachedDevice));
                    devices.sort(Comparator.comparingInt(item -> lastOctet(item.ipAddress)));
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onProgress(int scannedHosts, int totalHosts) {
                runOnUiThread(() -> {
                    progressBar.setProgress(scannedHosts);
                    statusText.setText("Scanning " + scannedHosts + " of " + totalHosts + " hosts. Found " + devices.size() + ".");
                });
            }

            @Override
            public void onScanFinished(int deviceCount) {
                runOnUiThread(() -> {
                    cache.saveDevices(currentNetworkKey, currentNetworkLabel, devices);
                    scanExecutor.shutdown();
                    scanButton.setEnabled(true);
                    scanButton.setText(R.string.scan_network);
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("Scan complete. Found " + deviceCount + " devices.");
                });
            }

            @Override
            public void onScanFailed(String message) {
                runOnUiThread(() -> {
                    scanExecutor.shutdown();
                    scanButton.setEnabled(true);
                    scanButton.setText(R.string.scan_network);
                    progressBar.setVisibility(View.GONE);
                    statusText.setText(message);
                    networkSummary.setText(R.string.wifi_unavailable);
                });
            }
        }));
    }

    private void loadCachedDevicesForCurrentNetwork() {
        if (scanner == null || cache == null) {
            return;
        }

        NetworkIdentity networkIdentity = scanner.getCurrentNetworkIdentity();
        if (networkIdentity == null) {
            if (devices.isEmpty()) {
                networkSummary.setText(R.string.wifi_unavailable);
            }
            return;
        }

        currentNetworkKey = networkIdentity.key;
        currentNetworkLabel = networkIdentity.label;
        cache.clearIfDifferentNetwork(currentNetworkKey);
        devices.clear();
        devices.addAll(cache.loadDevices(currentNetworkKey));
        devices.sort(Comparator.comparingInt(item -> lastOctet(item.ipAddress)));
        adapter.notifyDataSetChanged();

        networkSummary.setText("Network: " + currentNetworkLabel);
        if (devices.isEmpty()) {
            statusText.setText("Ready to scan. No cached devices for this network.");
        } else {
            statusText.setText("Loaded " + devices.size() + " cached devices. Tap a row for details.");
        }
    }

    private void openDeviceDetails(DeviceInfo device) {
        Intent intent = new Intent(this, DeviceDetailActivity.class);
        intent.putExtra(DeviceDetailActivity.EXTRA_NETWORK_KEY, currentNetworkKey);
        intent.putExtra(DeviceDetailActivity.EXTRA_NETWORK_LABEL, currentNetworkLabel);
        intent.putExtra(DeviceDetailActivity.EXTRA_IP_ADDRESS, device.ipAddress);
        startActivity(intent);
    }

    private DeviceInfo findDevice(List<DeviceInfo> source, String ipAddress) {
        for (DeviceInfo device : source) {
            if (device.ipAddress.equals(ipAddress)) {
                return device;
            }
        }
        return null;
    }

    private void upsertDevice(DeviceInfo updatedDevice) {
        for (int index = 0; index < devices.size(); index++) {
            if (devices.get(index).ipAddress.equals(updatedDevice.ipAddress)) {
                devices.set(index, updatedDevice);
                return;
            }
        }
        devices.add(updatedDevice);
    }

    private int lastOctet(String ipAddress) {
        int dot = ipAddress.lastIndexOf('.');
        if (dot < 0 || dot == ipAddress.length() - 1) {
            return 0;
        }
        try {
            return Integer.parseInt(ipAddress.substring(dot + 1));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
