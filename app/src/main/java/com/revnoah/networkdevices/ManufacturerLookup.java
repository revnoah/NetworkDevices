package com.revnoah.networkdevices;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class ManufacturerLookup {
    private static final Map<String, String> OUI_PREFIXES = buildPrefixes();

    private ManufacturerLookup() {
    }

    static String lookup(String macAddress) {
        if (macAddress == null || macAddress.equals("Unknown")) {
            return "Unknown";
        }

        String normalized = macAddress.toUpperCase(Locale.US).replace("-", ":");
        String[] parts = normalized.split(":");
        if (parts.length < 3) {
            return "Unknown";
        }

        String prefix = parts[0] + ":" + parts[1] + ":" + parts[2];
        String manufacturer = OUI_PREFIXES.get(prefix);
        return manufacturer == null ? "Unknown" : manufacturer;
    }

    private static Map<String, String> buildPrefixes() {
        Map<String, String> prefixes = new HashMap<>();
        prefixes.put("00:03:93", "Apple");
        prefixes.put("00:05:02", "Apple");
        prefixes.put("00:0A:27", "Apple");
        prefixes.put("00:0A:95", "Apple");
        prefixes.put("00:0D:93", "Apple");
        prefixes.put("00:11:24", "Apple");
        prefixes.put("00:14:51", "Apple");
        prefixes.put("00:16:CB", "Apple");
        prefixes.put("00:17:F2", "Apple");
        prefixes.put("00:19:E3", "Apple");
        prefixes.put("00:1B:63", "Apple");
        prefixes.put("00:1E:C2", "Apple");
        prefixes.put("00:1F:F3", "Apple");
        prefixes.put("00:21:E9", "Apple");
        prefixes.put("00:22:41", "Apple");
        prefixes.put("00:23:12", "Apple");
        prefixes.put("00:23:32", "Apple");
        prefixes.put("00:23:6C", "Apple");
        prefixes.put("00:25:00", "Apple");
        prefixes.put("00:25:4B", "Apple");
        prefixes.put("00:26:08", "Apple");
        prefixes.put("00:26:4A", "Apple");
        prefixes.put("00:26:B0", "Apple");
        prefixes.put("04:0C:CE", "Apple");
        prefixes.put("04:26:65", "Apple");
        prefixes.put("04:52:F3", "Apple");
        prefixes.put("08:00:27", "Oracle VirtualBox");
        prefixes.put("08:11:96", "Intel");
        prefixes.put("08:66:98", "Apple");
        prefixes.put("0C:4D:E9", "Apple");
        prefixes.put("10:9A:DD", "Apple");
        prefixes.put("14:10:9F", "Apple");
        prefixes.put("18:65:90", "Apple");
        prefixes.put("1C:1A:C0", "Apple");
        prefixes.put("24:A0:74", "Apple");
        prefixes.put("28:37:37", "Apple");
        prefixes.put("28:CF:DA", "Apple");
        prefixes.put("2C:54:CF", "LG Electronics");
        prefixes.put("30:35:AD", "Apple");
        prefixes.put("34:15:9E", "Apple");
        prefixes.put("3C:15:C2", "Apple");
        prefixes.put("3C:5A:B4", "Google");
        prefixes.put("40:B0:FA", "LG Electronics");
        prefixes.put("44:65:0D", "Amazon Technologies");
        prefixes.put("48:5A:B6", "Hon Hai Precision");
        prefixes.put("50:F5:DA", "Amazon Technologies");
        prefixes.put("54:60:09", "Google");
        prefixes.put("58:B0:35", "Apple");
        prefixes.put("5C:97:F3", "Apple");
        prefixes.put("60:33:4B", "Apple");
        prefixes.put("64:16:66", "Nest Labs");
        prefixes.put("68:37:E9", "Amazon Technologies");
        prefixes.put("6C:AD:F8", "AzureWave");
        prefixes.put("70:3E:AC", "Apple");
        prefixes.put("74:75:48", "Amazon Technologies");
        prefixes.put("78:4F:43", "Apple");
        prefixes.put("7C:D1:C3", "Apple");
        prefixes.put("80:EA:96", "Apple");
        prefixes.put("84:38:35", "Apple");
        prefixes.put("88:1F:A1", "Apple");
        prefixes.put("8C:85:90", "Apple");
        prefixes.put("90:72:40", "Apple");
        prefixes.put("98:01:A7", "Apple");
        prefixes.put("9C:20:7B", "Apple");
        prefixes.put("A0:99:9B", "Apple");
        prefixes.put("A4:5E:60", "Apple");
        prefixes.put("A4:77:33", "Google");
        prefixes.put("A8:20:66", "Apple");
        prefixes.put("AC:37:43", "HTC");
        prefixes.put("B0:34:95", "Apple");
        prefixes.put("B8:27:EB", "Raspberry Pi Foundation");
        prefixes.put("BC:52:B7", "Apple");
        prefixes.put("C0:EE:FB", "OnePlus");
        prefixes.put("C8:69:CD", "Apple");
        prefixes.put("CC:20:E8", "Apple");
        prefixes.put("D0:03:4B", "Apple");
        prefixes.put("D0:50:99", "ASUSTek Computer");
        prefixes.put("D8:BB:2C", "Apple");
        prefixes.put("DC:A6:32", "Raspberry Pi Trading");
        prefixes.put("E0:5F:45", "Apple");
        prefixes.put("E4:5F:01", "Raspberry Pi Trading");
        prefixes.put("E8:50:8B", "Samsung Electronics");
        prefixes.put("EC:35:86", "Apple");
        prefixes.put("F0:18:98", "Apple");
        prefixes.put("F4:0F:24", "Apple");
        prefixes.put("F4:F5:D8", "Google");
        prefixes.put("F8:1E:DF", "Apple");
        prefixes.put("FC:A1:3E", "Samsung Electronics");
        return Collections.unmodifiableMap(prefixes);
    }
}
