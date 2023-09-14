package com.example.mdpapp.utils.bluetooth;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;


public class BluetoothConnectionManager {
    private static BluetoothConnectionManager INSTANCE;
    private static final String TAG = "BluetoothConnectionManager";

    public static final int CONNECTION_FAILED = -1;
    public static final int CONNECTION_SUCCESSFUL = 0;
    public static final int RECEIVED_MESSAGE = 1;
    public static final int CONNECTION_LOST = 2;

    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket mSocket;
    private ConnectedThread mConnectedThread;
    private Handler mConnectionCallback;
    private final static UUID RANDOM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice lastConnectedDevice;
    private boolean isIntentionalDisconnect = false;

    private BluetoothConnectionManager() {

    }

    public static BluetoothConnectionManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BluetoothConnectionManager();
        }

        return INSTANCE;
    }

    public Set<BluetoothDevice> getPairedDevices() {
        if (mAdapter == null) {
            return null;
        }
        return mAdapter.getBondedDevices();
    }

    public void startScanning() {
        if (mAdapter == null || mAdapter.isDiscovering()) {
            return;
        }

        mAdapter.startDiscovery();
    }

    public void stopScanning() {
        if (mAdapter != null && mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
    }

    public void connect(BluetoothDevice device, Handler callback) {
        isIntentionalDisconnect = false;
        mConnectionCallback = callback;

        Thread connectThread = new Thread(() -> {
            try {
                mSocket = device.createRfcommSocketToServiceRecord(RANDOM_UUID);
                mSocket.connect();

                mConnectedThread = new ConnectedThread(mSocket);
                mConnectedThread.start();

                lastConnectedDevice = device;

                mConnectionCallback.obtainMessage(CONNECTION_SUCCESSFUL).sendToTarget();

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                mConnectionCallback.obtainMessage(CONNECTION_FAILED).sendToTarget();
            }
        });
        connectThread.start();
    }

    public void disconnect() throws IOException {
        try {
            isIntentionalDisconnect = true;
            mConnectedThread.cancel();
            mSocket.close();
        } catch (IOException e) {
            throw e;
        }
    }

    public void reconnect(Handler reconnectionCallback) {
        if (lastConnectedDevice == null) {
            reconnectionCallback.obtainMessage(CONNECTION_FAILED).sendToTarget();
        }
        Thread reconnectThread = new Thread(() -> {
            try {
                mSocket = lastConnectedDevice.createRfcommSocketToServiceRecord(RANDOM_UUID);
                mSocket.connect();

                mConnectedThread = new ConnectedThread(mSocket);
                mConnectedThread.start();

                reconnectionCallback.obtainMessage(CONNECTION_SUCCESSFUL).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                reconnectionCallback.obtainMessage(CONNECTION_FAILED).sendToTarget();
            }
        });

        reconnectThread.start();
    }

    public void sendMessage(@NonNull String messageToSend) throws IOException {
        mConnectedThread.write(messageToSend.getBytes());
    }

    public BluetoothDevice getConnectedDevice() {
        if (mSocket == null || !mSocket.isConnected()) {
            return null;
        }

        return mSocket.getRemoteDevice();
    }

    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final Handler mmHandler;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            mmHandler = mConnectionCallback;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];

            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    mmHandler.obtainMessage(RECEIVED_MESSAGE, incomingMessage).sendToTarget();
                } catch (IOException e) {
                    if (!isIntentionalDisconnect) {
                        mmHandler.obtainMessage(CONNECTION_LOST).sendToTarget();
                    }
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


}
