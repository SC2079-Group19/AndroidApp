package com.example.mdpapp;

import android.bluetooth.BluetoothSocket;

import androidx.lifecycle.ViewModel;

public class BluetoothViewModel extends ViewModel {
    private BluetoothSocket bluetoothSocket;

    public BluetoothSocket getBluetoothSocket() {
        return this.bluetoothSocket;
    }

    public void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;
    }
}
