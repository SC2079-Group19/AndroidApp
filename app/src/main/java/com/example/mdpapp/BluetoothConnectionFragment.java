package com.example.mdpapp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.bluetooth.BluetoothDevice;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mdpapp.databinding.BluetoothConnectionFragmentBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothConnectionFragment extends Fragment {

    private BluetoothConnectionFragmentBinding binding;
    private List<BluetoothDevice> deviceList = new ArrayList<>();
    private List<String> deviceListNames = new ArrayList<>();
    private BluetoothConnectionManager bluetoothConnectionManager;
    private ArrayAdapter<String> deviceAdapter;
    private BluetoothPermissionManager bluetoothPermissionManager;
    private BluetoothConnectionManager.BluetoothSocketCallback connectionCallback = new BluetoothConnectionManager.BluetoothSocketCallback() {
        @Override
        public void onConnected() {
            requireActivity().runOnUiThread(() -> {
                ((MainActivity) requireActivity()).getBluetoothViewModel().setBluetoothConnectionManager(bluetoothConnectionManager);
                NavHostFragment.findNavController(BluetoothConnectionFragment.this).navigate(R.id.action_BluetoothConnectionFragment_to_HomeFragment);
            });
        }

        @Override
        public void onConnectionFailed() {
            requireActivity().runOnUiThread(() -> {
                binding.txtConnectionStatus.setText("Failed to connect. Try Again!");
            });
        }
    };

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        bluetoothConnectionManager = new BluetoothConnectionManager(requireActivity());

        bluetoothPermissionManager = new BluetoothPermissionManager(requireActivity());
        bluetoothPermissionManager.requestUserPermissions();

        binding = BluetoothConnectionFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        deviceAdapter = new ArrayAdapter<String>(requireActivity(), android.R.layout.simple_list_item_1, deviceListNames);

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                //Finding devices
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getName() != null) {
                        deviceList.add(device);
                        deviceListNames.add(device.getName());
                        deviceAdapter.notifyDataSetChanged();
                        Log.d("BluetoothCon", device.getName() + " : " + device.getAddress());
                    }
                }
            }
        };

        binding.btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deviceList.clear();
                deviceListNames.clear();
                deviceAdapter.notifyDataSetChanged();
                bluetoothConnectionManager.startScanning(receiver);
                Set<BluetoothDevice> pairedDevices = bluetoothConnectionManager.getPairedDevices();
                if (!pairedDevices.isEmpty()) {
                    for (BluetoothDevice device : pairedDevices) {
                        deviceList.add(device);
                        deviceListNames.add(device.getName());
                        deviceAdapter.notifyDataSetChanged();
                    }
                    showDeviceSelectionDialog();
                } else {
                    Toast.makeText(requireContext(), "No Paired devices found.", Toast.LENGTH_LONG);
                }
            }
        });
    }

    private void showDeviceSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select a Device").setAdapter(deviceAdapter, (dialog, which) -> {
                    BluetoothDevice selectedDevice = deviceList.get(which);
                    Log.d("BluetoothCon", selectedDevice.getName());
                    bluetoothConnectionManager.connect(selectedDevice, connectionCallback);
                    binding.txtConnectionStatus.setText("Connecting to " + selectedDevice.getName());
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bluetoothConnectionManager.stopScanning();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

