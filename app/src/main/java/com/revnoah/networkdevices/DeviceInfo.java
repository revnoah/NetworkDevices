package com.revnoah.networkdevices;

final class DeviceInfo {
    final String ipAddress;
    final String macAddress;
    final String hostname;
    final String manufacturer;
    final String pingResponse;
    final String names;
    final String services;
    final String webInfo;
    final String webUrls;
    final String customName;

    DeviceInfo(
            String ipAddress,
            String macAddress,
            String hostname,
            String manufacturer,
            String pingResponse,
            String names,
            String services,
            String webInfo,
            String webUrls,
            String customName
    ) {
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.hostname = hostname;
        this.manufacturer = manufacturer;
        this.pingResponse = pingResponse;
        this.names = names;
        this.services = services;
        this.webInfo = webInfo;
        this.webUrls = webUrls;
        this.customName = customName;
    }

    String displayName() {
        if (customName != null && !customName.trim().isEmpty()) {
            return customName;
        }
        if (!"Unknown".equals(hostname)) {
            return hostname;
        }
        return ipAddress;
    }

    DeviceInfo withCustomName(String newName) {
        return new DeviceInfo(ipAddress, macAddress, hostname, manufacturer, pingResponse, names, services, webInfo, webUrls, newName);
    }

    DeviceInfo withCachedNameFrom(DeviceInfo cachedDevice) {
        if (cachedDevice == null) {
            return this;
        }
        return withCustomName(cachedDevice.customName);
    }
}
