package com.revnoah.networkdevices;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DeviceDetailActivity extends AppCompatActivity {
    static final String EXTRA_NETWORK_KEY = "network_key";
    static final String EXTRA_NETWORK_LABEL = "network_label";
    static final String EXTRA_IP_ADDRESS = "ip_address";

    private DeviceCache cache;
    private String networkKey;
    private String networkLabel;
    private String ipAddress;
    private DeviceInfo device;
    private TextView title;
    private TextView details;
    private LinearLayout webButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        cache = new DeviceCache(this);
        networkKey = getIntent().getStringExtra(EXTRA_NETWORK_KEY);
        networkLabel = getIntent().getStringExtra(EXTRA_NETWORK_LABEL);
        ipAddress = getIntent().getStringExtra(EXTRA_IP_ADDRESS);

        title = findViewById(R.id.detailTitle);
        details = findViewById(R.id.detailText);
        webButtons = findViewById(R.id.webButtons);
        Button renameButton = findViewById(R.id.renameButton);

        renameButton.setOnClickListener(view -> showRenameDialog());
        loadDevice();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDevice();
    }

    private void loadDevice() {
        device = cache.findDevice(networkKey, ipAddress);
        if (device == null) {
            title.setText("Device not found");
            details.setText("This device is no longer cached for the current network.");
            webButtons.removeAllViews();
            return;
        }

        title.setText(device.displayName());
        details.setText(
                "IP: " + device.ipAddress + "\n"
                        + "Hostname: " + device.hostname + "\n"
                        + "Names: " + device.names + "\n"
                        + "MAC: " + device.macAddress + "\n"
                        + "Manufacturer: " + device.manufacturer + "\n"
                        + "Ping: " + device.pingResponse + "\n"
                        + "Open services: " + device.services + "\n"
                        + "Web: " + device.webInfo
        );
        renderWebButtons();
    }

    private void showRenameDialog() {
        if (device == null) {
            return;
        }

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setSingleLine(true);
        input.setText(device.customName);
        input.setSelectAllOnFocus(true);

        new AlertDialog.Builder(this)
                .setTitle("Rename device")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    DeviceInfo updatedDevice = device.withCustomName(input.getText().toString().trim());
                    cache.saveDevice(networkKey, networkLabel, updatedDevice);
                    device = updatedDevice;
                    loadDevice();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renderWebButtons() {
        webButtons.removeAllViews();
        if (device.webUrls == null || device.webUrls.trim().isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No web endpoints detected.");
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setTextSize(14);
            webButtons.addView(empty);
            return;
        }

        String[] urls = device.webUrls.split("\\|");
        for (String rawUrl : urls) {
            String url = rawUrl.trim();
            if (url.isEmpty()) {
                continue;
            }
            Button button = new Button(this);
            button.setText("Open " + url);
            button.setTextColor(getColor(R.color.cyber_cyan));
            button.setBackgroundResource(R.drawable.cyber_button);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = getResources().getDimensionPixelSize(R.dimen.detail_button_gap);
            button.setLayoutParams(params);
            button.setOnClickListener(view -> openUrl(url));
            webButtons.addView(button);
        }
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}
