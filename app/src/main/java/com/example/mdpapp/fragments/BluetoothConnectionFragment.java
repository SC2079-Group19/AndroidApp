package com.example.mdpapp.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.bluetooth.BluetoothDevice;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mdpapp.MainActivity;
import com.example.mdpapp.R;
import com.example.mdpapp.managers.BluetoothConnectionManager;
import com.example.mdpapp.managers.BluetoothPermissionManager;
import com.example.mdpapp.utils.JSONMessagesManager;
import com.example.mdpapp.databinding.BluetoothConnectionFragmentBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
    private AlertDialog reconnectionDialog;
    private Handler btConnectionHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case BluetoothConnectionManager.CONNECTION_SUCCESSFUL:
                    requireActivity().runOnUiThread(() -> {
                        NavHostFragment.findNavController(BluetoothConnectionFragment.this).navigate(R.id.action_BluetoothConnectionFragment_to_HomeFragment);
                    });
                    break;
                case BluetoothConnectionManager.CONNECTION_FAILED:
                    requireActivity().runOnUiThread(() -> {
                        binding.txtConnectionStatus.setText("Failed to connect. Try Again!");
                    });
                    break;
                case BluetoothConnectionManager.CONNECTION_LOST:
                    requireActivity().runOnUiThread(() -> {
                        showConnectionLostDialog();
                        bluetoothConnectionManager.reconnect(btReconnectionHandler);
                    });
                    break;
                case BluetoothConnectionManager.RECEIVED_MESSAGE:
                    try {
                        String messageStr = (String) msg.obj;
                        Log.d("Bluetooth", messageStr);
                        JSONObject message = JSONMessagesManager.stringToMessageJSON(messageStr);
                        JSONMessagesManager.MessageHeader messageType = JSONMessagesManager.MessageHeader.valueOf((String) message.get("header"));
                        String messageContent = (String) message.get("data");
                        ((MainActivity) requireActivity()).getMessageViewModel().setMessage(messageType, messageContent);
                    } catch (JSONException e) {
                        Log.e("Message", e.getMessage());
                    }
            }

        }
    };

    private Handler btReconnectionHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            requireActivity().runOnUiThread(() -> {
                dismissReconnectionDialog();
            });
            switch (msg.what) {
                case BluetoothConnectionManager.CONNECTION_SUCCESSFUL:
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireActivity(), "Back Online", Toast.LENGTH_LONG).show();
                    });
                    break;
                case BluetoothConnectionManager.CONNECTION_FAILED:
                    requireActivity().runOnUiThread(() -> {
                        NavHostFragment.findNavController(BluetoothConnectionFragment.this).navigate(R.id.action_HomeFragment_to_BluetoothConnectionFragment);
                        Toast.makeText(requireActivity(), "Connection Failed!", Toast.LENGTH_LONG).show();
                    });
                    break;
            }
        }
    };

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        bluetoothConnectionManager = BluetoothConnectionManager.getInstance();

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

                bluetoothConnectionManager.startScanning();
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                requireActivity().registerReceiver(receiver, filter);

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
                    bluetoothConnectionManager.stopScanning();
                    BluetoothDevice selectedDevice = deviceList.get(which);
                    Log.d("BluetoothCon", selectedDevice.getName());
                    bluetoothConnectionManager.connect(selectedDevice, btConnectionHandler);
                    binding.txtConnectionStatus.setText("Connecting to " + selectedDevice.getName());
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bluetoothConnectionManager.stopScanning();
                    }
                })
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showConnectionLostDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Reconnecting...")
                .setMessage("The connection with the Bluetooth Device has been disrupted. A reconnection attempt is being made.")
                .setNegativeButton("Disconnect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            bluetoothConnectionManager.disconnect();
                        } catch (IOException e) {
                            Log.e("BluetoothConnection", e.getMessage());
                        }
                    }
                })
                .setCancelable(false);

        reconnectionDialog = builder.create();
        reconnectionDialog.show();
    }

    private void dismissReconnectionDialog() {
        if(reconnectionDialog != null && reconnectionDialog.isShowing()) {
            reconnectionDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

