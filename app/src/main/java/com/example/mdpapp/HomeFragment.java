package com.example.mdpapp;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.codertainment.dpadview.DPadView;
import com.example.mdpapp.databinding.HomeFragmentBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class HomeFragment extends Fragment {

    private HomeFragmentBinding binding;

    private BluetoothConnectionManager bluetoothConnectionManager;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        bluetoothConnectionManager = ((MainActivity) requireActivity()).getBluetoothViewModel().getBluetoothConnectionManager();

        binding = HomeFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MessageViewModel messageViewModel = ((MainActivity) requireActivity()).getMessageViewModel();

        messageViewModel.getMessageType().observe(getViewLifecycleOwner(), messageHeader -> {
            switch (messageHeader) {
                case ROBOT_STATUS:
                    binding.txtStatus.setText(messageViewModel.getMessageContent().getValue());
                    break;
            }
        });

        if(bluetoothConnectionManager.getConnectedDevice() != null) {
            binding.swConnectedTo.setText("Device: " + bluetoothConnectionManager.getConnectedDevice().getName());
        } else {
            NavHostFragment.findNavController(HomeFragment.this).navigate(R.id.action_HomeFragment_to_BluetoothConnectionFragment);
        }

        binding.swConnectedTo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    bluetoothConnectionManager.disconnect();
                }
                catch (IOException e) {
                    Log.e("BluetoothSocket", e.getMessage());
                }
                NavHostFragment.findNavController(HomeFragment.this).navigate(R.id.action_HomeFragment_to_BluetoothConnectionFragment);
            }
        });

        binding.dpad.setOnDirectionClickListener(direction -> {
            switch (direction) {
                case UP:
                    try {
                        JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "FW");
                        bluetoothConnectionManager.sendMessage(message.toString());
                    } catch (IOException e) {
                        Log.e("HomeFragment", e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case DOWN:
                    try {
                        JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "BW");
                        bluetoothConnectionManager.sendMessage(message.toString());
                    } catch (IOException e) {
                        Log.e("HomeFragment", e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case LEFT:
                    try {
                        JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "FL");
                        bluetoothConnectionManager.sendMessage(message.toString());
                    } catch (IOException e) {
                        Log.e("HomeFragment", e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case RIGHT:
                    try {
                        JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "FR");
                        bluetoothConnectionManager.sendMessage(message.toString());
                    } catch (IOException e) {
                        Log.e("HomeFragment", e.getMessage());
                        e.printStackTrace();
                    }
                    break;
            }
            return null;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}