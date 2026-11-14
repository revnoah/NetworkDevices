package com.revnoah.networkdevices;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class DeviceCache {
    private static final String PREFS_NAME = "device_cache";
    private static final String KEY_NETWORK = "network_key";
    private static final String KEY_NETWORK_LABEL = "network_label";
    private static final String KEY_DEVICES = "devices";

    private final SharedPreferences preferences;

    DeviceCache(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    String networkKey() {
        return preferences.getString(KEY_NETWORK, null);
    }

    String networkLabel() {
        return preferences.getString(KEY_NETWORK_LABEL, null);
    }

    void clearIfDifferentNetwork(String networkKey) {
        String cachedNetwork = networkKey();
        if (cachedNetwork != null && !cachedNetwork.equals(networkKey)) {
            preferences.edit().clear().apply();
        }
    }

    List<DeviceInfo> loadDevices(String networkKey) {
        if (networkKey == null || !networkKey.equals(networkKey())) {
            return new ArrayList<>();
        }

        List<DeviceInfo> devices = new ArrayList<>();
        String encodedDevices = preferences.getString(KEY_DEVICES, "[]");
        try {
            JSONArray array = new JSONArray(encodedDevices);
            for (int index = 0; index < array.length(); index++) {
                devices.add(fromJson(array.getJSONObject(index)));
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return devices;
    }

    DeviceInfo findDevice(String networkKey, String ipAddress) {
        for (DeviceInfo device : loadDevices(networkKey)) {
            if (device.ipAddress.equals(ipAddress)) {
                return device;
            }
        }
        return null;
    }

    void saveDevices(String networkKey, String networkLabel, List<DeviceInfo> devices) {
        JSONArray array = new JSONArray();
        for (DeviceInfo device : devices) {
            array.put(toJson(device));
        }
        preferences.edit()
                .putString(KEY_NETWORK, networkKey)
                .putString(KEY_NETWORK_LABEL, networkLabel)
                .putString(KEY_DEVICES, array.toString())
                .apply();
    }

    void saveDevice(String networkKey, String networkLabel, DeviceInfo updatedDevice) {
        List<DeviceInfo> devices = loadDevices(networkKey);
        boolean replaced = false;
        for (int index = 0; index < devices.size(); index++) {
            if (devices.get(index).ipAddress.equals(updatedDevice.ipAddress)) {
                devices.set(index, updatedDevice);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            devices.add(updatedDevice);
        }
        saveDevices(networkKey, networkLabel, devices);
    }

    private JSONObject toJson(DeviceInfo device) {
        JSONObject object = new JSONObject();
        try {
            object.put("ipAddress", device.ipAddress);
            object.put("macAddress", device.macAddress);
            object.put("hostname", device.hostname);
            object.put("manufacturer", device.manufacturer);
            object.put("pingResponse", device.pingResponse);
            object.put("names", device.names);
            object.put("services", device.services);
            object.put("webInfo", device.webInfo);
            object.put("webUrls", device.webUrls);
            object.put("customName", device.customName);
        } catch (JSONException ignored) {
        }
        return object;
    }

    private DeviceInfo fromJson(JSONObject object) {
        return new DeviceInfo(
                object.optString("ipAddress", "Unknown"),
                object.optString("macAddress", "Unknown"),
                object.optString("hostname", "Unknown"),
                object.optString("manufacturer", "Unknown"),
                object.optString("pingResponse", "Unknown"),
                object.optString("names", "Unknown"),
                object.optString("services", "None detected"),
                object.optString("webInfo", "No public web server detected"),
                object.optString("webUrls", ""),
                object.optString("customName", "")
        );
    }
}
