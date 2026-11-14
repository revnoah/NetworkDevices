package com.revnoah.networkdevices;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

final class DeviceAdapter extends ArrayAdapter<DeviceInfo> {
    DeviceAdapter(Context context, List<DeviceInfo> devices) {
        super(context, 0, devices);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_device, parent, false);
        }

        DeviceInfo device = getItem(position);
        if (device == null) {
            return view;
        }

        ((TextView) view.findViewById(R.id.ipAddress)).setText(device.displayName());
        ((TextView) view.findViewById(R.id.hostname)).setText("Hostname: " + device.hostname);
        ((TextView) view.findViewById(R.id.ipLine)).setText("IP: " + device.ipAddress);
        ((TextView) view.findViewById(R.id.macAddress)).setText("MAC: " + device.macAddress);
        ((TextView) view.findViewById(R.id.manufacturer)).setText("Manufacturer: " + device.manufacturer);
        ((TextView) view.findViewById(R.id.ping)).setText("Ping response: " + device.pingResponse);
        ((TextView) view.findViewById(R.id.names)).setText("Names: " + device.names);
        ((TextView) view.findViewById(R.id.services)).setText("Open services: " + device.services);
        ((TextView) view.findViewById(R.id.webInfo)).setText("Web: " + device.webInfo);
        return view;
    }
}
