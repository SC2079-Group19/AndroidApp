package com.example.mdpapp;


import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnectionManager {

    private String[] permissions = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT};
    private Context context;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket bluetoothSocket;
    public static final int PERMISSION_REQUEST_BLUETOOTH = 1;
    public static final int REQUEST_ENABLE_BT = 2;

    public BluetoothConnectionManager(Context context) {
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

    public Set<BluetoothDevice> getPairedDevices() {
        return bluetoothAdapter.getBondedDevices();
    }

    public void connect(BluetoothDevice device, BluetoothSocketCallback callback) {
        Thread connectThread = new Thread(() -> {
            try {
                UUID uuid = device.getUuids()[0].getUuid();
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();

                callback.onConnected();
            } catch (IOException e) {
                Log.e("BluetoothConnection", e.getMessage());
                callback.onConnectionFailed();
            }
        });
        connectThread.start();
    }

    public void disconnect() throws IOException {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            throw e;
        }
    }

    public void sendMessage(String messageToSend) throws IOException {
        if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
            return;
        }

        try {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(messageToSend.getBytes());
            outputStream.flush();
        } catch (IOException e) {
            throw e;
        }
    }

    public BluetoothDevice getConnectedDevice() {
        if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
            return null;
        }

        return bluetoothSocket.getRemoteDevice();
    }


    public interface BluetoothSocketCallback {
        void onConnected();

        void onConnectionFailed();
    }
}
