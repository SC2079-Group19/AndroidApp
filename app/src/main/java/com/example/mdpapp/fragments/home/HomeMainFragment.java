package com.example.mdpapp.fragments.home;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.FrameMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.DragEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mdpapp.R;
import com.example.mdpapp.utils.bluetooth.BluetoothConnectionManager;
import com.example.mdpapp.utils.JSONMessagesManager;
import com.example.mdpapp.databinding.HomeMainFragmentBinding;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HomeMainFragment extends Fragment {
    private static final String TAG = "HomeMainFragment";
    private HomeMainFragmentBinding binding;
    private BluetoothConnectionManager bluetoothConnectionManager = BluetoothConnectionManager.getInstance();
    private int currentHighestObstacle = 0;
    private static final int MAX_NO_OBSTACLES = 20;
    private static ArrayList<TextView> obstacles = new ArrayList<>();

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
        int cellSpacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        int cellSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());

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
        }

        for(int i = 0; i < MAX_NO_OBSTACLES; i++) {
            TextView newObstacle = new TextView(requireActivity());

            newObstacle.setText(String.valueOf(i+1));
            newObstacle.setId(i+1);
            newObstacle.setBackgroundColor(Color.BLACK);
            newObstacle.setTextColor(Color.WHITE);
            newObstacle.setGravity(Gravity.CENTER);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = 0;
            layoutParams.leftMargin = 0;
            layoutParams.width = 80;
            layoutParams.height = 80;

            newObstacle.setLayoutParams(layoutParams);

            newObstacle.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // Start the drag operation when the obstacle is long-clicked
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                    v.startDragAndDrop(null, shadowBuilder, v, 0);
                    return true;
                }
            });

            newObstacle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String[] options = new String[]{"Top", "Bottom", "Left", "Right"};

                    AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                    builder.setTitle("Select Image Location")
                            .setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String selection = options[which];
                                    switch (selection) {
                                        case "Top":
                                            v.setBackgroundResource(R.drawable.obstacle_border_top);
                                            break;
                                        case "Bottom":
                                            v.setBackgroundResource(R.drawable.obstacle_border_bottom);
                                            break;
                                        case "Left":
                                            v.setBackgroundResource(R.drawable.obstacle_border_left);
                                            break;
                                        case "Right":
                                            v.setBackgroundResource(R.drawable.obstacle_border_right);
                                            break;
                                    }
                                }
                            })
                            .setCancelable(false);

                    builder.show();
                }
            });

            obstacles.add(newObstacle);
            binding.obstacleStack.addView(newObstacle, 0);
        }

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = cellSize*3;
        params.height = cellSize*3;
        binding.robot.setLayoutParams(params);

        binding.robot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.robot.setRotation((binding.robot.getRotation() + 90) % 360);
            }
        });
        // Add the drag-and-drop functionality for the robot image
        binding.robot.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Start the drag operation when the robot image is long-clicked
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDragAndDrop(null, shadowBuilder, v, 0);
                return true;
            }
        });

        requireActivity().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSecondary, typedValue, true);
        int axisHighlightBgColor = typedValue.data;

        requireActivity().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSecondary, typedValue, true);
        int axisHighlightFgColor = typedValue.data;

        requireActivity().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        int axisNormalFgColor = typedValue.data;

        binding.frame.setOnDragListener(new View.OnDragListener() {
            private TextView highlightedAxisX = null;
            private TextView highlightedAxisY = null;

            @Override
            public boolean onDrag(View v, DragEvent event) {
                int action = event.getAction();
                float x;
                float y;
                int gridX;
                int gridY;

                switch (action) {
                    case DragEvent.ACTION_DRAG_STARTED:
                    case DragEvent.ACTION_DRAG_ENDED:
                    case DragEvent.ACTION_DRAG_ENTERED:
                        return true;

                    case DragEvent.ACTION_DRAG_EXITED:
                        if (highlightedAxisX != null) {
                            highlightAxis(highlightedAxisX, highlightedAxisY, Color.TRANSPARENT, axisNormalFgColor);
                        }

                    case DragEvent.ACTION_DROP:
                        if (highlightedAxisX != null) {
                            highlightAxis(highlightedAxisX, highlightedAxisY, Color.TRANSPARENT, axisNormalFgColor);
                        }
                        x = event.getX();
                        y = event.getY();

                        gridX = (int) (x / (cellSize + cellSpacing));
                        gridY = (int) (y / (cellSize + cellSpacing));

                        if (gridY <= 0 || gridY >= gridSize + 1 || gridX <= 0 || gridX > gridSize + 1) {
                            return false;
                        }

                        TextView cell = (TextView) gridLayout.getChildAt((gridY - 1) * (gridSize + 1) + (gridX - 1));
                        if (cell == null) {
                            return false;
                        }

                        // Calculate the center position of the grid cell
                        int left = (int) cell.getX();
                        int top = (int) cell.getY();

                        View item = (View) event.getLocalState();
                        if (item.getId() == binding.robot.getId()) {
                            ImageView robot = (ImageView) item;
                            FrameLayout.LayoutParams params = null;
                            if(((View) robot.getParent()).getId() == binding.llObstacleCar.getId()) {
                                params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                                binding.llObstacleCar.removeView(robot);
                                binding.frame.addView(robot);
                            } else {
                                params = (FrameLayout.LayoutParams) robot.getLayoutParams();
                            }
                            params.leftMargin = left - (cellSize + cellSpacing);
                            params.topMargin = top - (cellSize + cellSpacing);
                            params.width = cellSize*3;
                            params.height = cellSize*3;
                            binding.robot.setLayoutParams(params);

                        } else {
                            TextView obstacle = (TextView) item;

                            // Check if the view is not already added to the FrameLayout
                            if (((View)obstacle.getParent()).getId() == binding.obstacleStack.getId()) {
                                // Add the view to the FrameLayout
                                binding.obstacleStack.removeView(obstacle);
                                binding.frame.addView(obstacle);
                            }
                            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) obstacle.getLayoutParams();
                            layoutParams.leftMargin = left;
                            layoutParams.topMargin = top;
                            layoutParams.width = cellSize;
                            layoutParams.height = cellSize;
                            obstacle.setLayoutParams(layoutParams);

                            JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ITEM_LOCATION, obstacle.getText()+", "+(gridX-1)+", "+((gridSize+1)-gridY));
                            try {
                                bluetoothConnectionManager.sendMessage(message.toString());
                            } catch (IOException e) {
                            }
                        }

                        return true;

                    case DragEvent.ACTION_DRAG_LOCATION:
                        x = event.getX();
                        y = event.getY();

                        gridX = (int) (x / (cellSize + cellSpacing));
                        gridY = (int) (y / (cellSize + cellSpacing));

                        if (gridY > 0 && gridY < gridSize + 1 && gridX > 0 && gridX <= gridSize + 1) {
                            if (highlightedAxisX != null) {
                                highlightAxis(highlightedAxisX, highlightedAxisY, Color.TRANSPARENT, axisNormalFgColor);
                            }

                            TextView axisX = (TextView) gridLayout.getChildAt((gridY - 1) * (gridSize + 1));
                            TextView axisY = (TextView) gridLayout.getChildAt((gridSize) * (gridSize + 1) + (gridX - 1));
                            highlightAxis(axisX, axisY, axisHighlightBgColor, axisHighlightFgColor);

                            highlightedAxisX = axisX;
                            highlightedAxisY = axisY;
                        }

                        return true;

                    default:
                        return false;
                }
            }
        });

        binding.llObstacleCar.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                int action = event.getAction();

                switch (action) {
                    case DragEvent.ACTION_DRAG_STARTED:
                    case DragEvent.ACTION_DRAG_ENDED:
                    case DragEvent.ACTION_DRAG_ENTERED:
                    case DragEvent.ACTION_DRAG_EXITED:
                        return true;

                    case DragEvent.ACTION_DROP:
                        View item = (View) event.getLocalState();
                        if (item.getId() == binding.robot.getId()) {
                            ImageView robot = (ImageView) item;
                            if (((View) robot.getParent()).getId() == binding.frame.getId()) {
                                binding.frame.removeView(robot);
                            } else {
                                binding.llObstacleCar.removeView(robot);
                            }
                            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                            params.height = cellSize*3;
                            params.width = cellSize*3;
                            robot.setLayoutParams(params);
                            binding.llObstacleCar.addView(robot);
                        } else {
                            TextView obstacle = (TextView) item;
                            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(obstacle.getLayoutParams());
                            params.height = 80;
                            params.width = 80;
                            params.topMargin = 0;
                            params.leftMargin = 0;
                            obstacle.setLayoutParams(params);
                            if (((View) obstacle.getParent()).getId() == binding.frame.getId()) {
                                binding.frame.removeView(obstacle);
                            } else {
                                binding.obstacleStack.removeView(obstacle);
                            }
                            binding.obstacleStack.addView(obstacle);
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });

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

    private void highlightAxis(TextView axisX, TextView axisY, int bgColor, int fgColor) {
        axisX.setBackgroundColor(bgColor);
        axisY.setBackgroundColor(bgColor);

        axisX.setTextColor(fgColor);
        axisY.setTextColor(fgColor);
    }
}
