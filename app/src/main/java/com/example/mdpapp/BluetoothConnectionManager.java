package com.example.mdpapp;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class BluetoothConnectionManager {

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket bluetoothSocket;
    private Context context;
    private BroadcastReceiver receiver;
    private final static UUID RANDOM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BluetoothConnectionManager(Context context) {
        this.context = context;
    }

    public Set<BluetoothDevice> getPairedDevices() {
        return bluetoothAdapter.getBondedDevices();
    }

    public void startScanning(BroadcastReceiver receiver) {
        if(bluetoothAdapter.isDiscovering()) {
            return;
        }

        bluetoothAdapter.startDiscovery();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        ((MainActivity) context).registerReceiver(receiver, filter);
    }

    public void stopScanning() {
        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    public void connect(BluetoothDevice device, BluetoothSocketCallback callback) {
        Thread connectThread = new Thread(() -> {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(RANDOM_UUID);
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
