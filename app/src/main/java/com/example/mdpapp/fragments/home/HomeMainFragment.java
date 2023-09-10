package com.example.mdpapp.fragments.home;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.DragEvent;

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
                    // zero label at bottom left
                    gridCell.setText("0");
                } else if (row == gridSize) {
                    // x-axis at the bottom
                    gridCell.setText(String.valueOf(col));
                } else if (col == 0) {
                    // y-axis at the left
                    gridCell.setText(String.valueOf(gridSize - row));
                } else {
                    // normal cell
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

        // Add the drag-and-drop functionality for the robot image
        ImageView robotImageView = binding.robot;
        robotImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Start the drag operation when the robot image is long-clicked
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDragAndDrop(null, shadowBuilder, v, 0);
                return true;
            }
        });

        // Add the drag-and-drop functionality for the obstacle
        TextView obstacleTextView = binding.obstacle;
        obstacleTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Start the drag operation when the obstacle is long-clicked
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDragAndDrop(null, shadowBuilder, v, 0);
                return true;
            }
        });

        // Set the drag listener for the grid layout
        gridLayout.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                int action = event.getAction();
                switch (action) {
                    case DragEvent.ACTION_DROP:
                        View draggedView = (View) event.getLocalState();

                        // Calculate the drop location in grid coordinates
                        float x = event.getX();
                        float y = event.getY();

                        int gridX = (int) (x / (cellSize + cellSpacing));
                        int gridY = (int) (y / (cellSize + cellSpacing));

                        // Ensure the drop location is within the grid bounds
                        if (gridX >= 0 && gridX < gridSize && gridY >= 0 && gridY < gridSize) {
                            // Calculate the position for the dropped view within the grid cell
                            int left = gridX * (cellSize + cellSpacing);
                            int top = gridY * (cellSize + cellSpacing);

                            // Set layout parameters for the dropped view to snap into the grid cell
                            draggedView.setLayoutParams(new GridLayout.LayoutParams(
                                    new ViewGroup.LayoutParams(cellSize, cellSize)
                            ));

                            // Update the position of the dropped view within the grid cell
                            gridLayout.addView(draggedView, gridX + gridY * gridSize);
                        }

                        return true;

                    case DragEvent.ACTION_DRAG_ENDED:
                        return true;

                    default:
                        return false;
                }
            }
        });

    }
}
