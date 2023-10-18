package com.example.mdpapp.fragments;

import android.bluetooth.BluetoothAdapter;
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
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.bluetooth.BluetoothDevice;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mdpapp.MainActivity;
import com.example.mdpapp.R;
import com.example.mdpapp.utils.bluetooth.BluetoothConnectionManager;
import com.example.mdpapp.utils.bluetooth.BluetoothPermissionManager;
import com.example.mdpapp.utils.JSONMessagesManager;
import com.example.mdpapp.databinding.BluetoothConnectionFragmentBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
    private androidx.appcompat.app.AlertDialog reconnectionDialog;
    private AlertDialog deviceSelectionDialog;
    private View progressBarLayout;
    private Handler btConnectionHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case BluetoothConnectionManager.CONNECTION_SUCCESSFUL:
                    NavHostFragment.findNavController(BluetoothConnectionFragment.this).navigate(R.id.action_BluetoothConnectionFragment_to_HomeFragment);
                    break;
                case BluetoothConnectionManager.CONNECTION_FAILED:
                    binding.txtConnectionStatus.setText("Calling failed!");
                    break;
                case BluetoothConnectionManager.CONNECTION_LOST:
                    showConnectionLostDialog();
                    bluetoothConnectionManager.reconnect(btReconnectionHandler);
                    break;
                case BluetoothConnectionManager.RECEIVED_MESSAGE:
                    try {
                        String messageStr = (String) msg.obj;
                        Log.d("Bluetooth", messageStr);
                        JSONObject message = JSONMessagesManager.stringToMessageJSON(messageStr);
                        JSONMessagesManager.MessageHeader messageType = JSONMessagesManager.MessageHeader.valueOf((String) message.get("header"));
                        String messageContent = (String) message.get("data");
                        Log.d("Bluetooth", messageContent);
                        ((MainActivity) requireActivity()).getMessageViewModel().setMessage(messageType, messageContent);
                    } catch (Exception e) {
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
                    Toast.makeText(requireActivity(), "Back Online", Toast.LENGTH_LONG).show();
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


        final BroadcastReceiver foundReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                //Finding devices
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getName() != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        deviceList.add(device);
                        deviceListNames.add(device.getName());
                        deviceAdapter.notifyDataSetChanged();
                        Log.d("BluetoothCon", device.getName() + " : " + device.getAddress());
                    }
                }
            }
        };

        final BroadcastReceiver stoppedDiscoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Button alertBtnScan = deviceSelectionDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    alertBtnScan.setEnabled(true);

                    if (progressBarLayout != null) {
                        ProgressBar progressBar = progressBarLayout.findViewById(R.id.progressBar);
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }
        };

        binding.btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getBTDevices();

                IntentFilter foundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                requireActivity().registerReceiver(foundReceiver, foundFilter);

                IntentFilter startedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                requireActivity().registerReceiver(stoppedDiscoveryReceiver, startedFilter);

                showDeviceSelectionDialog();
            }
        });
    }

    private void showDeviceSelectionDialog() {
        progressBarLayout = getLayoutInflater().inflate(R.layout.progress_dialog, null);
        TextView title = progressBarLayout.findViewById(R.id.progressTitle);
        title.setText("Select a Device");

        ProgressBar progressBar = progressBarLayout.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setCustomTitle(progressBarLayout)
                .setAdapter(deviceAdapter, (dialog, which) -> {
                    bluetoothConnectionManager.stopScanning();
                    bluetoothConnectionManager.stopConnectionAttempt();
                    BluetoothDevice selectedDevice = deviceList.get(which);
                    Log.d("BluetoothCon", selectedDevice.getName());
                    bluetoothConnectionManager.connect(selectedDevice, btConnectionHandler);
                    binding.txtConnectionStatus.setText("Calling " + selectedDevice.getName());
                })
                .setPositiveButton("Scan", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bluetoothConnectionManager.stopScanning();
                    }
                })
                .setCancelable(false);

        deviceSelectionDialog = builder.create();
        deviceSelectionDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button btnScan = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                btnScan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getBTDevices();
                        ProgressBar progressBar = progressBarLayout.findViewById(R.id.progressBar);
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });

                btnScan.setEnabled(false);
            }
        });
        deviceSelectionDialog.show();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

        lp.copyFrom(deviceSelectionDialog.getWindow().getAttributes());
        lp.height = 1000;
        deviceSelectionDialog.getWindow().setAttributes(lp);

    }

    private void showConnectionLostDialog() {
        View progressBarLayout = getLayoutInflater().inflate(R.layout.progress_dialog, null);

        TextView title = progressBarLayout.findViewById(R.id.progressTitle);
        title.setText("Reconnecting...");

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setCustomTitle(progressBarLayout)
                .setMessage("The connection with the Bluetooth Device has been disrupted. A reconnection attempt is being made.")
                .setNegativeButton("Disconnect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            bluetoothConnectionManager.disconnect();
                            NavHostFragment.findNavController(BluetoothConnectionFragment.this).navigate(R.id.action_HomeFragment_to_BluetoothConnectionFragment);
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
        if (reconnectionDialog != null && reconnectionDialog.isShowing()) {
            reconnectionDialog.dismiss();
        }
    }

    private void getBTDevices() {
        deviceList.clear();
        deviceListNames.clear();
        deviceAdapter.notifyDataSetChanged();

        bluetoothConnectionManager.startScanning();

        Set<BluetoothDevice> pairedDevices = bluetoothConnectionManager.getPairedDevices();
        for (BluetoothDevice device : pairedDevices) {
            deviceList.add(device);
            deviceListNames.add(device.getName());
            deviceAdapter.notifyDataSetChanged();
        }

        if (deviceSelectionDialog != null) {
            Button alertBtnScan = deviceSelectionDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            alertBtnScan.setEnabled(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

