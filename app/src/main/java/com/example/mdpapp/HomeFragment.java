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

import com.example.mdpapp.databinding.HomeFragmentBinding;

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

        binding.swConnectedTo.setText("Device: " + bluetoothConnectionManager.getConnectedDevice().getName());

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

        binding.btnSendDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    bluetoothConnectionManager.sendMessage("f");
                } catch (IOException e) {
                    Log.e("BluetoothConnection", e.getMessage());
                }
            }
        });

        binding.btnReceiveDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    bluetoothConnectionManager.startReceivingMessages(new BluetoothConnectionManager.BluetoothMessageCallback() {
                        @Override
                        public void onMessageReceived(String message) {
                            Log.d("BluetoothMessage", message);
                            requireActivity().runOnUiThread(() -> {
                                binding.textView.setText(message);
                            });
                        }
                    });
                } catch (IOException e) {
                    Log.e("BluetoothConnection", e.getMessage());
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