package com.example.mdpapp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mdpapp.databinding.BluetoothConnectionFragmentBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnectionFragment extends Fragment {

    private BluetoothConnectionFragmentBinding binding;
    private List<BluetoothDevice> deviceList = new ArrayList<>();
    private BluetoothSocket bluetoothSocket;
    private BluetoothConnectionManager bluetoothConnectionManager;

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

    private void showDeviceSelectionDialog(String[] devices) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select a Device").setItems(devices, (dialog, which) -> {
                    // Perform actions with the selected device, like connecting
                    BluetoothDevice selectedDevice = deviceList.get(which);

                    try {
                        // Get a BluetoothSocket for a connection with the given device
                        UUID uuid = selectedDevice.getUuids()[0].getUuid();
                        bluetoothSocket = selectedDevice.createRfcommSocketToServiceRecord(uuid);

                        binding.textView2.setText("Connecting to... " + selectedDevice.getName());

                        // Connect the socket in a separate thread to avoid blocking the UI
                        Thread connectThread = new Thread(() -> {
                            try {
                                bluetoothSocket.connect();
                                getActivity().runOnUiThread(() -> {
                                    binding.textView2.setText("Connected!");

                                    // Passing the socket to the ViewModel
                                    ((MainActivity) requireActivity()).getBluetoothViewModel().setBluetoothSocket(bluetoothSocket);

                                    NavHostFragment.findNavController(BluetoothConnectionFragment.this)
                                            .navigate(R.id.action_BluetoothConnectionFragment_to_HomeFragment);
                                });

                            } catch (IOException e) {
                                Log.e("BluetoothConnection", e.getMessage());
                                e.printStackTrace();
                                getActivity().runOnUiThread(() -> {
                                    binding.textView2.setText("Failed to connect to " + selectedDevice.getName() + ". Try Again!");
                                });
                            }
                        });
                        connectThread.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        // Handle socket creation failure
                        // ...
                    }
                })
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Set<BluetoothDevice> pairedDevices = bluetoothConnectionManager.getPairedDevices();

                ArrayList<String> deviceNames = new ArrayList<>();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        deviceNames.add(deviceName);
                        deviceList.add(device);
                    }

                    String[] deviceNamesStr = new String[deviceNames.size()];
                    deviceNamesStr = (String[]) deviceNames.toArray(new String[0]);
                    showDeviceSelectionDialog(deviceNamesStr);
                }
            }
            });
        }
            @Override
            public void onDestroyView() {
                super.onDestroyView();
                binding = null;
            }
    }

