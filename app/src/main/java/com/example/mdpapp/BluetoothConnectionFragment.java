package com.example.mdpapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.bluetooth.BluetoothDevice;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mdpapp.databinding.BluetoothConnectionFragmentBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothConnectionFragment extends Fragment {

    private BluetoothConnectionFragmentBinding binding;
    private List<BluetoothDevice> deviceList = new ArrayList<>();
    private BluetoothConnectionManager bluetoothConnectionManager;
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
        bluetoothConnectionManager = new BluetoothConnectionManager(requireContext());
        bluetoothConnectionManager.requestUserPermissions();

        binding = BluetoothConnectionFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Set<BluetoothDevice> pairedDevices = bluetoothConnectionManager.getPairedDevices();

                ArrayList<String> deviceNames = new ArrayList<>();
                if (!pairedDevices.isEmpty()) {
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        deviceNames.add(deviceName);
                        deviceList.add(device);
                    }

                    String[] deviceNamesStr = deviceNames.toArray(new String[0]);
                    showDeviceSelectionDialog(deviceNamesStr);
                } else {
                    Toast.makeText(requireContext(), "No Paired devices found.", Toast.LENGTH_LONG);
                }
            }
        });
    }

    private void showDeviceSelectionDialog(String[] devices) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select a Device").setItems(devices, (dialog, which) -> {
                    BluetoothDevice selectedDevice = deviceList.get(which);
                    bluetoothConnectionManager.connect(selectedDevice, connectionCallback);
                    binding.txtConnectionStatus.setText("Connecting to " + selectedDevice.getName());
                })
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

