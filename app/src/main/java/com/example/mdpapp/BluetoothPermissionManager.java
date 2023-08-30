package com.example.mdpapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class BluetoothPermissionManager {
    private String[] permissions = {android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT};
    private Context context;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public static final int PERMISSION_REQUEST_BLUETOOTH = 1;
    public static final int REQUEST_ENABLE_BT = 2;

    public BluetoothPermissionManager(Context context) {
        this.context = context;
    }

    public void requestUserPermissions() {
        // checking if the version of Android is >= 6.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissionsToRequest = checkUserPermissions();

            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions((MainActivity) context, permissionsToRequest.toArray(new String[1]), PERMISSION_REQUEST_BLUETOOTH);
            }
        }

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((MainActivity) context).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public List<String> checkUserPermissions() {
        List<String> notGranted = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                notGranted.add(permission);
            }
        }

        return notGranted;
    }
}
