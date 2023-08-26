package com.example.mdpapp;

import androidx.lifecycle.ViewModel;

public class BluetoothViewModel extends ViewModel {
    private BluetoothConnectionManager bluetoothConnectionManager;

    public BluetoothConnectionManager getBluetoothConnectionManager() {
        return this.bluetoothConnectionManager;
    }

    public void setBluetoothConnectionManager(BluetoothConnectionManager bluetoothConnectionManager) {
        this.bluetoothConnectionManager = bluetoothConnectionManager;
    }
}
