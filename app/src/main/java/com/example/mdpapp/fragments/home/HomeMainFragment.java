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

import com.example.mdpapp.managers.BluetoothConnectionManager;
import com.example.mdpapp.utils.JSONMessagesManager;
import com.example.mdpapp.databinding.HomeMainFragmentBinding;

import org.json.JSONObject;

import java.io.IOException;

public class HomeMainFragment extends Fragment {
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
//        int cellColor = Color.GRAY;

        int gridSize = 20;
        int cellSpacing = 1;
        int cellSize = 33;
//        int colorResourceId = getResources().getColor(com.google.android.material.R.color.design_default_color_primary);
//        int cellColor = Color.GRAY;
        // Populate the grid
        for (int i = 0; i < gridSize + 1; i++) { // +1 to include the indicators
            for (int j = 0; j < gridSize + 1; j++) { // +1 to include the indicators
                if (i == gridSize && j == 0) {
                    TextView zeroIndicator = new TextView(requireActivity());
                    zeroIndicator.setText("0");
                    zeroIndicator.setGravity(Gravity.CENTER);
//                    zeroIndicator.setBackgroundColor(cellColor);
                    GridLayout.LayoutParams zeroIndicatorParams = new GridLayout.LayoutParams();
                    zeroIndicatorParams.rowSpec = GridLayout.spec(i, 1);
                    zeroIndicatorParams.columnSpec = GridLayout.spec(j, 1);
                    zeroIndicatorParams.setMargins(cellSpacing, cellSpacing, cellSpacing, cellSpacing);
                    zeroIndicatorParams.width = cellSize;
                    zeroIndicatorParams.height = cellSize;
                    zeroIndicator.setLayoutParams(zeroIndicatorParams);
                    gridLayout.addView(zeroIndicator);
                } else if (i == gridSize) {
                    // Top row (set the values for the top row indicators)
                    TextView topIndicator = new TextView(requireActivity());
                    topIndicator.setText(String.valueOf(j));
                    topIndicator.setGravity(Gravity.CENTER);
//                    topIndicator.setBackgroundColor(cellColor);
                    GridLayout.LayoutParams topIndicatorParams = new GridLayout.LayoutParams();
                    topIndicatorParams.rowSpec = GridLayout.spec(i, 1);
                    topIndicatorParams.columnSpec = GridLayout.spec(j, 1);
                    topIndicatorParams.setMargins(cellSpacing, cellSpacing, cellSpacing, cellSpacing);
                    topIndicatorParams.width = cellSize;
                    topIndicatorParams.height = cellSize;
                    topIndicator.setLayoutParams(topIndicatorParams);
                    gridLayout.addView(topIndicator);
                } else if (j == 0) {
                    // Left column (set the values for the left column indicators)
                    TextView leftIndicator = new TextView(requireActivity());
                    leftIndicator.setText(String.valueOf(gridSize - i));
                    leftIndicator.setGravity(Gravity.CENTER);
//                    leftIndicator.setBackgroundColor(cellColor);
                    GridLayout.LayoutParams leftIndicatorParams = new GridLayout.LayoutParams();
                    leftIndicatorParams.rowSpec = GridLayout.spec(i, 1);
                    leftIndicatorParams.columnSpec = GridLayout.spec(j, 1);
                    leftIndicatorParams.setMargins(cellSpacing, cellSpacing, cellSpacing, cellSpacing);
                    leftIndicatorParams.width = cellSize;
                    leftIndicatorParams.height = cellSize;
                    leftIndicator.setLayoutParams(leftIndicatorParams);
                    gridLayout.addView(leftIndicator);
                } else {
                    // Normal grid cell
                    TextView cell = new TextView(requireActivity());
                    cell.setLayoutParams(new GridLayout.LayoutParams());
                    cell.setGravity(Gravity.CENTER);
                    cell.setBackgroundColor(cellColor);
                    GridLayout.LayoutParams cellParams = new GridLayout.LayoutParams();
                    cellParams.rowSpec = GridLayout.spec(i, 1);
                    cellParams.columnSpec = GridLayout.spec(j, 1);
                    cellParams.setMargins(cellSpacing, cellSpacing, cellSpacing, cellSpacing);
                    cellParams.width = cellSize;
                    cellParams.height = cellSize;
                    cell.setLayoutParams(cellParams);
                    gridLayout.addView(cell);
                }
            }
        }


        binding.dpad.setOnDirectionClickListener(direction -> {
            switch (direction) {
                case UP:
                    try {
                        JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "FW10");
                        bluetoothConnectionManager.sendMessage(message.toString());
                    } catch (IOException e) {
                        Log.e("HomeFragment", e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case DOWN:
                    try {
                        JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "BW10");
                        bluetoothConnectionManager.sendMessage(message.toString());
                    } catch (IOException e) {
                        Log.e("HomeFragment", e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case LEFT:
                    try {
                        JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "FL00");
                        bluetoothConnectionManager.sendMessage(message.toString());
                    } catch (IOException e) {
                        Log.e("HomeFragment", e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case RIGHT:
                    try {
                        JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "FR00");
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
}
