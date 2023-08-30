package com.example.mdpapp;


import android.Manifest;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnectionManager {

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket bluetoothSocket;

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

    public void startReceivingMessages(BluetoothMessageCallback callback) throws IOException {
        if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
            return;
        }

        Thread receiveThread = new Thread(() -> {
            try {
                InputStream inputStream = bluetoothSocket.getInputStream();
                byte[] buffer = new byte[1024];

                int bytesRead;

                while((bytesRead = inputStream.read(buffer)) != -1) {
                    String receivedMessage = new String(buffer, 0, bytesRead);
                    callback.onMessageReceived(receivedMessage);
                }
            } catch (IOException e) {
                Log.e("BluetoothConnection", e.getMessage());
            }
        });

        receiveThread.start();
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

    public interface BluetoothMessageCallback {
        void onMessageReceived(String message);
    }
}
