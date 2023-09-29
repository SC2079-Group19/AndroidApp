package com.example.mdpapp.fragments.home;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.DragEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mdpapp.MainActivity;
import com.example.mdpapp.R;
import com.example.mdpapp.utils.bluetooth.BluetoothConnectionManager;
import com.example.mdpapp.utils.JSONMessagesManager;
import com.example.mdpapp.databinding.HomeMainFragmentBinding;
import com.example.mdpapp.view_models.MessageViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HomeMainFragment extends Fragment {
    private static final String TAG = "HomeMainFragment";
    private HomeMainFragmentBinding binding;
    private BluetoothConnectionManager bluetoothConnectionManager = BluetoothConnectionManager.getInstance();
    private static final int MAX_NO_OBSTACLES = 20;
    private static ArrayList<TextView> obstacles = new ArrayList<>();
    private int cellSize;
    private int cellSpacing;
    private int gridSize;

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

        gridSize = 20;
        cellSpacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        cellSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());

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
            newObstacle.setTextSize(10);

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
                                    JSONObject message = null;
                                    String obstacle_id = ((TextView) v).getText().toString();

                                    int gridX = (int) (v.getX() / (cellSize + cellSpacing));
                                    int gridY = (int) (v.getY() / (cellSize + cellSpacing));

                                    int direction = 8;

                                    switch (selection) {
                                        case "Top":
                                            v.setBackgroundResource(R.drawable.obstacle_border_top);
                                            direction = 0;

                                            break;
                                        case "Bottom":
                                            direction = 4;
                                            v.setBackgroundResource(R.drawable.obstacle_border_bottom);
                                            break;
                                        case "Left":
                                            direction = 6;
                                            v.setBackgroundResource(R.drawable.obstacle_border_left);
                                            break;
                                        case "Right":
                                            direction = 2;
                                            v.setBackgroundResource(R.drawable.obstacle_border_right);
                                            break;
                                    }

                                    JSONObject messageData = null;
                                    try {
                                        messageData = new JSONObject(String.format("{'id': %s, 'x': %d, 'y': %d, 'd': %d}", obstacle_id, gridX, ((gridSize+1)-gridY)-1, direction));
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                    message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ITEM_LOCATION, messageData.toString());
                                    try {
                                        bluetoothConnectionManager.sendMessage(message.toString());
                                    } catch (IOException e) {
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

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

        requireActivity().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true);
        int robotUnderneathColor = typedValue.data;

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
                        // remove highlight from the previously highlighted axis labels
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

                        // Check for collision with existing obstacles
                        Log.d("Checking", "X: "+gridX);
                        Log.d("Checking", "Y: "+gridY);
                        if(isObstacleOnRobot((gridX-1), (gridSize+1)-gridY)) {
                            return false;
                        }
                        if (item.getId() != binding.robot.getId() && isObstacleCollision((gridX-1), ((gridSize+1)-gridY)))  {
                            return false; // Reject the drop if there's a collision
                        }

                        if (item.getId() == binding.robot.getId()) {
                            if(isRobotCollision((gridX-1), (gridSize+1)-gridY)) {
                                return false;
                            }
                            if (gridY <= 1  || gridY >= gridSize || gridX <= 2 || gridX > gridSize) {
                                return false;
                            }
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
                            robot.setLayoutParams(params);
                            robot.setBackgroundColor(robotUnderneathColor);

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

                            JSONObject message = null;
                            JSONObject messageData = null;
                            try {
                                messageData = new JSONObject(String.format("{'id': %s, 'x': %d, 'y': %d, 'd': %d}", obstacle.getText(), gridX - 1, (gridSize+1)-gridY, 8));
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
//                            String messageData = String.format("{'id': %s, 'x:': %d, 'y': %d, 'd': %d}", obstacle.getText(), gridX - 1, (gridSize+1)-gridY, 8);
                            message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ITEM_LOCATION, messageData.toString());
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
                            robot.setBackgroundColor(Color.TRANSPARENT);
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

        MessageViewModel messageViewModel = ((MainActivity) requireActivity()).getMessageViewModel();

        messageViewModel.getMessageContent().observe(getViewLifecycleOwner(), messageContent -> {
            JSONMessagesManager.MessageHeader header = messageViewModel.getMessageType().getValue();
            switch (header) {
                case IMAGE_RESULT:
                    JSONObject targetImage = null;
                    try {
                        targetImage = new JSONObject(messageViewModel.getMessageContent().getValue());
                        int obstacleId = (int) targetImage.get("obstacle_id");
                        int targetId = (int) targetImage.get("target_id");

                        TextView obstacle = binding.frame.findViewById(obstacleId);
                        if(obstacle != null && targetId >= 10 && targetId <= 40) {
                            obstacle.setText(String.valueOf(targetId));
                            obstacle.setBackgroundColor(Color.BLACK);
                            obstacle.setTextSize(16);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Robot Location JSON Error");
                    }
                    break;

                case ROBOT_LOCATION:
                    if(((View) binding.robot.getParent()).getId() == binding.llObstacleCar.getId()) {
                        break;
                    }

                    try {
                        JSONObject robotLocation = new JSONObject(messageViewModel.getMessageContent().getValue());
                        int gridX = (int) robotLocation.get("x");
                        int gridY = (int) robotLocation.get("y");
                        int orientation = (int) robotLocation.get("d");

                        if(isRobotCollision(gridX, gridY)) {
                            break;
                        }

                        if (gridY <= 1  || gridY > gridSize-1 || gridX <= 1 || gridX > gridSize-1) {
                            break;
                        }

                        gridX--;
                        gridY--;

                        gridY = gridSize - gridY;

                        TextView cell = (TextView) gridLayout.getChildAt((gridY - 1) * (gridSize + 1) + (gridX + 1));
                        if (cell == null) {
                            break;
                        }

                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) binding.robot.getLayoutParams();
                        layoutParams.leftMargin = ((int) cell.getX()) - (cellSize + cellSpacing);
                        layoutParams.topMargin= ((int) cell.getY()) - (cellSize + cellSpacing);
                        binding.robot.setLayoutParams(layoutParams);
                        switch (orientation) {
                            case 0:
                                binding.robot.setRotation(0);
                                break;
                            case 2:
                                binding.robot.setRotation(90);
                                break;
                            case 4:
                                binding.robot.setRotation(180);
                                break;
                            case 6:
                                binding.robot.setRotation(270);
                                break;
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "Robot Location JSON Error");
                    }
                    break;
            }
        });

        binding.dpad.setOnDirectionClickListener(direction -> {
            int stepSize = cellSize+(2*cellSpacing);
            float orientation = binding.robot.getRotation();

            if(((View) binding.robot.getParent()).getId() == binding.llObstacleCar.getId()) {
                return null;
            }

            switch (direction) {
                case UP:
                    if (orientation == 0) {         // when robot is facing up
                        moveRobot(0, -stepSize);    // robot moves up
                    }

                    else if (orientation == 90) {   // when robot is facing right
                        moveRobot(stepSize, 0);     // robot moves right
                    }

                    else if (orientation == 180) {  // when robot is facing down
                        moveRobot(0, stepSize);     // robot moves down
                    }

                    else {                          // when robot is facing left
                        moveRobot(-stepSize, 0);    // robot moves left
                    }

                    try {
                        JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "FW10");
                        bluetoothConnectionManager.sendMessage(message.toString());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case DOWN:
                    if (orientation == 0) {         // when robot is facing up
                        moveRobot(0, stepSize);     // robot moves down
                    }

                    else if (orientation == 90) {   // when robot is facing right
                        moveRobot(-stepSize, 0);    // robot moves left
                    }

                    else if (orientation == 180) {  // when robot is facing down
                        moveRobot(0, -stepSize);    // robot moves up
                    }

                    else {                          // when robot is facing left
                        moveRobot(stepSize, 0);     // robot moves right
                    }

                    try {
                        JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "BW10");
                        bluetoothConnectionManager.sendMessage(message.toString());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case LEFT:
                    if (orientation == 0) {                                                 // when robot is facing up
                        moveRobot(-stepSize, -stepSize);                                    // robot moves up and left
                        binding.robot.setRotation((binding.robot.getRotation() + 270));     // robot faces left
                    }

                    else if (orientation == 90) {                                           // when robot is facing right
                        moveRobot(stepSize, -stepSize);                                     // robot moves right and up
                        binding.robot.setRotation((binding.robot.getRotation() - 90));      // robot faces up
                    }

                    else if (orientation == 180) {                                          // when robot is facing down
                        moveRobot(stepSize, stepSize);                                      // robot moves down and right
                        binding.robot.setRotation((binding.robot.getRotation() - 90));      // robot faces right
                    }

                    else {                                                                  // when robot is facing left
                        moveRobot(-stepSize, stepSize);                                     // robot moves left and down
                        binding.robot.setRotation((binding.robot.getRotation() - 90));      // robot faces down
                    }

                    try {
                        JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ROBOT_CONTROL, "FL00");
                        bluetoothConnectionManager.sendMessage(message.toString());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case RIGHT:
                    if (orientation == 0) {                                                 // when robot is facing up
                        moveRobot(stepSize, -stepSize);                                     // robot moves up and right
                        binding.robot.setRotation((binding.robot.getRotation() + 90));      // robot faces right
                    }

                    else if (orientation == 90) {                                           // when robot is facing right
                        moveRobot(stepSize, stepSize);                                      // robot moves right and down
                        binding.robot.setRotation((binding.robot.getRotation() + 90));      // robot faces down
                    }

                    else if (orientation == 180) {                                          // when robot is facing down
                        moveRobot(-stepSize, stepSize);                                     // robot moves down and left
                        binding.robot.setRotation((binding.robot.getRotation() + 90));      // robot faces left
                    }

                    else {                                                                  // when robot is facing left
                        moveRobot(-stepSize, -stepSize);                                    // robot moves left and up
                        binding.robot.setRotation((binding.robot.getRotation() - 270));     // robot faces up
                    }

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

    // Define the moveRobot method to update the robot's position
    private void moveRobot(int deltaX, int deltaY) {
        // Get the current LayoutParams of the robot ImageView
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) binding.robot.getLayoutParams();

        // Update the position based on the input delta values
        layoutParams.leftMargin += deltaX;
        layoutParams.topMargin += deltaY;

        // Apply the updated LayoutParams to the ImageView
        binding.robot.setLayoutParams(layoutParams);
    }

    private void highlightAxis(TextView axisX, TextView axisY, int bgColor, int fgColor) {
        axisX.setBackgroundColor(bgColor);
        axisY.setBackgroundColor(bgColor);

        axisX.setTextColor(fgColor);
        axisY.setTextColor(fgColor);
    }

    private boolean isObstacleCollision(int gridX, int gridY) {
        for (TextView obstacle : obstacles) {
            if(((View) obstacle.getParent()).getId() != binding.frame.getId()) {
                continue;
            }
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) obstacle.getLayoutParams();
            int obstacleGridX = (layoutParams.leftMargin / (cellSize + cellSpacing));
            int obstacleGridY = (layoutParams.topMargin / (cellSize + cellSpacing));

            obstacleGridY = (gridSize)-obstacleGridY;

            if (gridX == obstacleGridX && gridY == obstacleGridY) {
                return true; // Collision detected
            }
        }
        return false; // No collision detected
    }

    private boolean isObstacleOnRobot(int gridX, int gridY) {
        Log.d("Checking", "Im here");
        if(((View) binding.robot.getParent()).getId() != binding.frame.getId()) {
            return false;
        }

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) binding.robot.getLayoutParams();
        int robotX = (layoutParams.leftMargin / (cellSize + cellSpacing));
        int robotY = (layoutParams.topMargin / (cellSize + cellSpacing));
        robotX++;
        robotY = (gridSize-1) - robotY;

        Log.d("Checking", "gridX: "+gridX);
        Log.d("Checking", "gridY "+gridY);

        Log.d("Checking", "robotX: "+robotX);
        Log.d("Checking", "robotY: "+robotY);

        if (robotX - 1 <= gridX && robotX + 1 >= gridX && robotY - 1 <= gridY && robotY + 1 >= gridY) {
            return true;
        }

        return false;
    }

    private boolean isRobotCollision(int gridX, int gridY) {
        return isObstacleCollision(gridX, gridY) ||
                isObstacleCollision(gridX-1, gridY) ||
                isObstacleCollision(gridX+1, gridY) ||
                isObstacleCollision(gridX, gridY-1) ||
                isObstacleCollision(gridX, gridY+1) ||
                isObstacleCollision(gridX-1, gridY-1) ||
                isObstacleCollision(gridX+1, gridY+1) ||
                isObstacleCollision(gridX-1, gridY+1) ||
                isObstacleCollision(gridX+1, gridY-1);
    }
}
