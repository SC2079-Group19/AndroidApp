package com.example.mdpapp.fragments.home;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mdpapp.utils.bluetooth.BluetoothConnectionManager;
import com.example.mdpapp.utils.JSONMessagesManager;
import com.example.mdpapp.databinding.HomeMainFragmentBinding;

import org.json.JSONObject;

import java.io.IOException;

public class HomeMainFragment extends Fragment {
    private static final String TAG = "HomeMainFragment";
    private HomeMainFragmentBinding binding;
    private BluetoothConnectionManager bluetoothConnectionManager = BluetoothConnectionManager.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = HomeMainFragmentBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GridLayout gridLayout = binding.grid;

        // Obtain the colorSurface attribute from the current theme
        TypedValue typedValue = new TypedValue();
        requireActivity().getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);

        int cellColor = typedValue.data;

        int gridSize = 20;
        int cellSpacing = 1;
        int cellSize = 33;

        // Populate the grid
        for (int row = 0; row < gridSize + 1; row++) { // +1 to include the indicators
            for (int col = 0; col < gridSize + 1; col++) { // +1 to include the indicators
                TextView gridCell = new TextView(requireActivity());
                gridCell.setGravity(Gravity.CENTER);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(row, 1);
                params.columnSpec = GridLayout.spec(col, 1);
                params.setMargins(cellSpacing, cellSpacing, cellSpacing, cellSpacing);
                params.width = cellSize;
                params.height = cellSize;
                gridCell.setLayoutParams(params);

                if (row == gridSize && col == 0) {
                    gridCell.setText("0");
                } else if (row == gridSize) {
                    gridCell.setText(String.valueOf(col));
                } else if (col == 0) {
                    gridCell.setText(String.valueOf(gridSize - row));
                } else {
                    gridCell.setBackgroundColor(cellColor);
                }

                gridLayout.addView(gridCell);
            }


            binding.dpad.setOnDirectionClickListener(direction -> {
                switch (direction) {
                    case UP:
                        try {
                            JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "FW10");
                            bluetoothConnectionManager.sendMessage(message.toString());
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    case DOWN:
                        try {
                            JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "BW10");
                            bluetoothConnectionManager.sendMessage(message.toString());
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    case LEFT:
                        try {
                            JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "FL00");
                            bluetoothConnectionManager.sendMessage(message.toString());
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    case RIGHT:
                        try {
                            JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "FR00");
                            bluetoothConnectionManager.sendMessage(message.toString());
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                }
                return null;
            });
        }
    }
}
