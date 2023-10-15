package com.example.mdpapp.fragments.home;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.DragEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mdpapp.MainActivity;
import com.example.mdpapp.R;
import com.example.mdpapp.utils.bluetooth.BluetoothConnectionManager;
import com.example.mdpapp.utils.JSONMessagesManager;
import com.example.mdpapp.databinding.HomeMainFragmentBinding;
import com.example.mdpapp.utils.constants.Direction;
import com.example.mdpapp.view_models.MessageViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class HomeMainFragment extends Fragment {
    private static final String TAG = "HomeMainFragment";
    private HomeMainFragmentBinding binding;
    private BluetoothConnectionManager bluetoothConnectionManager = BluetoothConnectionManager.getInstance();
    private static final int MAX_NO_OBSTACLES = 20;
    private static ArrayList<TextView> obstacles = new ArrayList<>();
    private static HashMap<Integer, TextView> obstaclesOnGrid = new HashMap<Integer, TextView>();
    private int cellSize;
    private int cellSpacing;
    private int gridSize;
    private boolean timerRunning;
    private Handler timerHandler = new Handler();
    private long startTimeMilli = 0;

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

        gridSize = 20;
        cellSpacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        cellSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());
        int cellColor = getAttrValue(com.google.android.material.R.attr.colorPrimary);

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

        for (int i = 0; i < MAX_NO_OBSTACLES; i++) {
            TextView newObstacle = new TextView(requireActivity());

            newObstacle.setId(i + 1);
            resetObstacle(newObstacle);

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
                    String[] options = new String[]{"North", "South", "East", "West"};

                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
                    builder.setTitle("Select Image Location")
                            .setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String selection = options[which];
                                    int direction = Direction.UNSET;

                                    switch (selection) {
                                        case "North":
                                            v.setBackgroundResource(R.drawable.obstacle_border_top);
                                            direction = Direction.NORTH;
                                            break;
                                        case "South":
                                            direction = Direction.SOUTH;
                                            v.setBackgroundResource(R.drawable.obstacle_border_bottom);
                                            break;
                                        case "West":
                                            direction = Direction.WEST;
                                            v.setBackgroundResource(R.drawable.obstacle_border_left);
                                            break;
                                        case "East":
                                            direction = Direction.EAST;
                                            v.setBackgroundResource(R.drawable.obstacle_border_right);
                                            break;
                                    }

                                    v.setTag(R.id.obstacleD, direction);
                                }
                            })
                            .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    TextView obstacle = (TextView) v;
                                    resetObstacle(obstacle);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .setCancelable(false);

                    builder.show();
                }
            });

            obstacles.add(newObstacle);
            binding.obstacleStack.addView(newObstacle, 0);
        }

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = cellSize * 3;
        params.height = cellSize * 3;
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

        int axisHighlightBgColor = getAttrValue(com.google.android.material.R.attr.colorSecondary);
        int axisHighlightFgColor = getAttrValue(com.google.android.material.R.attr.colorOnSecondary);
        int axisNormalFgColor = getAttrValue(com.google.android.material.R.attr.colorOnSurface);

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

                        int xPos = getCoordinateX(x);
                        int yPos = getCoordinateY(y);

                        View obj = (View) event.getLocalState();


                        // Check for collision with existing obstacles
                        if (isObstacleOnRobot(xPos, yPos)) {
                            return false;
                        }
                        if (obj.getId() != binding.robot.getId() && isObstacleCollision(xPos, yPos)) {
                            return false; // Reject the drop if there's a collision
                        }

                        if (obj.getId() == binding.robot.getId()) {
//                            if (collides(xPos, yPos, true)) {
//                                return false;
//                            }
                            if (isRobotCollision(xPos, yPos)) {
                                return false;
                            }
                            placeRobotOnGrid(binding.robot, xPos, yPos);

                            binding.robot.setTag(R.id.obstacleX, xPos);
                            binding.robot.setTag(R.id.obstacleY, yPos);
                        } else {
//                            if (collides(xPos, yPos, false)) {
//                                return false;
//                            }
                            TextView obstacle = (TextView) obj;
                            placeObstacleOnGrid(obstacle, xPos, yPos);

                            obstacle.setTag(R.id.obstacleX, xPos);
                            obstacle.setTag(R.id.obstacleY, yPos);

                            obstaclesOnGrid.put(obstacle.getId(), obstacle);
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
                            params.height = cellSize * 3;
                            params.width = cellSize * 3;
                            robot.setLayoutParams(params);
                            robot.setBackgroundColor(Color.TRANSPARENT);
                            binding.llObstacleCar.addView(robot);
                        } else {
                            TextView obstacle = (TextView) item;
                            obstaclesOnGrid.remove(obstacle.getId());
                            resetObstacle(obstacle);
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
                        if (obstacle != null && targetId >= 10 && targetId <= 40) {
                            obstacle.setText(String.valueOf(targetId));
                            obstacle.setBackgroundColor(Color.DKGRAY);
                            obstacle.setTextSize(16);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Robot Location JSON Error");
                    }
                    break;

                case ROBOT_LOCATION:
                    if (((View) binding.robot.getParent()).getId() == binding.llObstacleCar.getId()) {
                        break;
                    }

                    try {
                        JSONObject robotLocation = new JSONObject(messageViewModel.getMessageContent().getValue());
                        int gridX = ((int) robotLocation.get("x")) + 1;
                        int gridY = ((int) robotLocation.get("y")) + 1;
                        int orientation = (int) robotLocation.get("d");

                        if (isRobotCollision(gridX, gridY)) {
                            break;
                        }

                        placeRobotOnGrid(binding.robot, gridX, gridY);
                        switch (orientation) {
                            case Direction.NORTH:
                                binding.robot.setRotation(0);
                                break;
                            case Direction.EAST:
                                binding.robot.setRotation(90);
                                break;
                            case Direction.SOUTH:
                                binding.robot.setRotation(180);
                                break;
                            case Direction.WEST:
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
            int stepSize = cellSize + (2 * cellSpacing);
            float orientation = binding.robot.getRotation();

            if (((View) binding.robot.getParent()).getId() == binding.llObstacleCar.getId()) {
                return null;
            }

            int gridX = (int) (binding.robot.getX() / (cellSize + cellSpacing));
            int gridY = (int) (binding.robot.getY() / (cellSize + cellSpacing));

            switch (direction) {
                case UP:
                    if (orientation == 0) {         // when robot is facing up
                        if (gridY > 0) {            // check top boundary
                            moveRobot(0, -stepSize);    // robot moves up
                        }
                    } else if (orientation == 90) {   // when robot is facing right
                        if (gridX < gridSize - 2) {     // check right boundary
                            moveRobot(stepSize, 0);     // robot moves right
                        }
                    } else if (orientation == 180) {  // when robot is facing down
                        if (gridY < gridSize - 3) {     // check bottom boundary
                            moveRobot(0, stepSize);     // robot moves down
                        }
                    } else {                          // when robot is facing left
                        if (gridX > 1) {            // check left boundary
                            moveRobot(-stepSize, 0);    // robot moves left
                        }
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
                        if (gridY < gridSize - 3) {     // check bottom boundary
                            moveRobot(0, stepSize);     // robot moves down
                        }
                    } else if (orientation == 90) {   // when robot is facing right
                        if (gridX > 1) {                // check left boundary
                            moveRobot(-stepSize, 0);    // robot moves left
                        }
                    } else if (orientation == 180) {  // when robot is facing down
                        if (gridY > 0) {                // check top boundary
                            moveRobot(0, -stepSize);    // robot moves up
                        }
                    } else {                          // when robot is facing left
                        if (gridX < gridSize - 2) {     // check right boundary
                            moveRobot(stepSize, 0);     // robot moves right
                        }
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
                        if ((gridX > 1) && (gridY > 0)) {                                    // check top and left boundary
                            moveRobot(-stepSize, -stepSize);                                    // robot moves up and left
                            binding.robot.setRotation((binding.robot.getRotation() + 270));     // robot faces left
                        }
                    } else if (orientation == 90) {                                           // when robot is facing right
                        if ((gridY > 0) && (gridX < gridSize - 2)) {                            // check top and right boundary
                            moveRobot(stepSize, -stepSize);                                     // robot moves right and up
                            binding.robot.setRotation((binding.robot.getRotation() - 90));      // robot faces up
                        }
                    } else if (orientation == 180) {                                          // when robot is facing down
                        if ((gridY < gridSize - 3) && (gridX < gridSize - 2)) {                 // check bottom and right boundary
                            moveRobot(stepSize, stepSize);                                      // robot moves down and right
                            binding.robot.setRotation((binding.robot.getRotation() - 90));      // robot faces right
                        }
                    } else {                                                                  // when robot is facing left
                        if ((gridX > 1) && (gridY < gridSize - 3)) {                            // check bottom and left boundary
                            moveRobot(-stepSize, stepSize);                                     // robot moves left and down
                            binding.robot.setRotation((binding.robot.getRotation() - 90));      // robot faces down
                        }
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
                        if ((gridY > 0) && (gridX < gridSize - 2)) {                            // check top and right boundary
                            moveRobot(stepSize, -stepSize);                                     // robot moves up and right
                            binding.robot.setRotation((binding.robot.getRotation() + 90));      // robot faces right
                        }
                    } else if (orientation == 90) {                                           // when robot is facing right
                        if ((gridX < gridSize - 2) && (gridY < gridSize - 3)) {                 // check bottom and right boundary
                            moveRobot(stepSize, stepSize);                                      // robot moves right and down
                            binding.robot.setRotation((binding.robot.getRotation() + 90));      // robot faces down
                        }
                    } else if (orientation == 180) {                                          // when robot is facing down
                        if ((gridY < gridSize - 3) && (gridX > 1)) {                            // check bottom and left boundary
                            moveRobot(-stepSize, stepSize);                                     // robot moves down and left
                            binding.robot.setRotation((binding.robot.getRotation() + 90));      // robot faces left
                        }
                    } else {                                                                  // when robot is facing left
                        if ((gridX > 1) && (gridY > 0)) {                                       // check top and left boundary
                            moveRobot(-stepSize, -stepSize);                                    // robot moves left and up
                            binding.robot.setRotation((binding.robot.getRotation() - 270));     // robot faces up
                        }
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

        binding.btnSendObstacles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (obstaclesOnGrid.size() == 0) {
                    Toast.makeText(requireActivity(), "No Obstacles Set", Toast.LENGTH_LONG).show();
                    return;
                }

                ArrayList<String> obstacleObjects = new ArrayList<>();
                for (TextView obstacle : obstaclesOnGrid.values()) {
                    String messageData = String.format(
                            "{'id': %s, 'x': %d, 'y': %d, 'd': %d}",
                            obstacle.getId(),
                            ((int) obstacle.getTag(R.id.obstacleX)) - 1,
                            ((int) obstacle.getTag(R.id.obstacleY)) - 1,
                            obstacle.getTag(R.id.obstacleD));
                    obstacleObjects.add(messageData.toString());
                }

                String[] obstacleObjectsArr = Arrays.copyOf(obstacleObjects.toArray(), obstacleObjects.size(), String[].class);

                JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.ITEM_LOCATION, Arrays.toString(obstacleObjectsArr));
                try {
                    bluetoothConnectionManager.sendMessage(message.toString());
                } catch (IOException e) {
                    Log.e("BluetoothConnectionError", e.getMessage());
                }
            }
        });

        binding.btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placeRobotOnGrid(binding.robot, 2, 2);
                binding.robot.setRotation(0);
                JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.START_MOVEMENT, "1");
                try {
                    bluetoothConnectionManager.sendMessage(message.toString());
                } catch (IOException e) {
                    Log.e("BluetoothConnectionError", e.getMessage());
                }
            }
        });

        binding.btnStartTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerRunning) {
                    timerRunning = false;
                    binding.btnStartTimer.setText("Start Timer");
                    Animation anim = new AlphaAnimation(0.7f, 1.0f);
                    anim.setDuration(700); //You can manage the blinking time with this parameter
                    anim.setRepeatMode(Animation.REVERSE);
                    anim.setRepeatCount(Animation.INFINITE);
                    binding.txtTimer.startAnimation(anim);
                } else {
                    timerRunning = true;
                    binding.btnStartTimer.setText("Stop Timer");
                    if (startTimeMilli == 0) {
                        startTimeMilli = System.currentTimeMillis();
                    }
                    binding.txtTimer.clearAnimation();
                    runTimer();
                }
            }
        });

        binding.btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerRunning = false;
                binding.btnStartTimer.setText("Start Timer");
                startTimeMilli = 0;
                binding.txtTimer.clearAnimation();
                updateTimer(0);
            }
        });
    }

    private void runTimer() {
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (timerRunning) {
                    long elapsedTime = System.currentTimeMillis() - startTimeMilli;
                    updateTimer(elapsedTime);
                }

                timerHandler.postDelayed(this, 1);
            }
        });
    }

    private void updateTimer(long elapsedTime) {
        long minutes = (elapsedTime / 1000 / 60) % 60;
        long seconds = (elapsedTime / 1000) % 60;
        long milliseconds = (elapsedTime % 1000);

        binding.txtTimer.setText(String.format("%02d:%02d:%03d", minutes, seconds, milliseconds));
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
            if (((View) obstacle.getParent()).getId() != binding.frame.getId()) {
                continue;
            }
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) obstacle.getLayoutParams();
            int obstacleGridX = (layoutParams.leftMargin / (cellSize + cellSpacing));
            int obstacleGridY = (layoutParams.topMargin / (cellSize + cellSpacing));

            obstacleGridY = (gridSize) - obstacleGridY;

            if (gridX == obstacleGridX && gridY == obstacleGridY) {
                return true; // Collision detected
            }
        }
        return false; // No collision detected
    }

    private boolean isObstacleOnRobot(int gridX, int gridY) {
        if (((View) binding.robot.getParent()).getId() != binding.frame.getId()) {
            return false;
        }

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) binding.robot.getLayoutParams();
        int robotX = (layoutParams.leftMargin / (cellSize + cellSpacing));
        int robotY = (layoutParams.topMargin / (cellSize + cellSpacing));
        robotX++;
        robotY = (gridSize - 1) - robotY;

        if (robotX - 1 <= gridX && robotX + 1 >= gridX && robotY - 1 <= gridY && robotY + 1 >= gridY) {
            return true;
        }

        return false;
    }

    private boolean isRobotCollision(int gridX, int gridY) {
        return isObstacleCollision(gridX, gridY) ||
                isObstacleCollision(gridX - 1, gridY) ||
                isObstacleCollision(gridX + 1, gridY) ||
                isObstacleCollision(gridX, gridY - 1) ||
                isObstacleCollision(gridX, gridY + 1) ||
                isObstacleCollision(gridX - 1, gridY - 1) ||
                isObstacleCollision(gridX + 1, gridY + 1) ||
                isObstacleCollision(gridX - 1, gridY + 1) ||
                isObstacleCollision(gridX + 1, gridY - 1);
    }

    private boolean robotOnGrid() {
        return (((View) binding.robot.getParent()).getId() == binding.frame.getId());
    }

    private void resetObstacle(TextView obstacle) {
        obstacle.setText(String.valueOf(obstacle.getId()));
        obstacle.setTextSize(10);
        obstacle.setTextColor(Color.WHITE);
        obstacle.setBackgroundColor(Color.BLACK);
        obstacle.setGravity(Gravity.CENTER);
        obstacle.setTag(R.id.obstacleD, 8);
    }

    private int getCoordinateX(float x) {
        int converted = (int) (x / (cellSize + cellSpacing));
        return converted - 1;
    }

    private int getCoordinateY(float y) {
        int converted = (int) (y / (cellSize + cellSpacing));
        return (gridSize + 1) - converted;
    }

    private boolean placeObjectOnGrid(View object, int xPos, int yPos, int size) {
        if (xPos < 1 || xPos > gridSize || yPos < 1 || yPos > gridSize) {
            return false;
        }

        int xInd = xPos;
        int yInd = (gridSize + 1) - yPos;
        View cell = binding.grid.getChildAt((yInd - 1) * (gridSize + 1) + xInd);

        if (cell == null) {
            return false;
        }

        ViewGroup parentObj = (ViewGroup) object.getParent();
        FrameLayout.LayoutParams layoutParams = null;
        if (parentObj.getId() != binding.frame.getId()) {
            layoutParams = new FrameLayout.LayoutParams((cellSize * size) + (cellSpacing * (size + 1)), (cellSize * size) + (cellSpacing * (size + 1)));
            parentObj.removeView(object);
            binding.frame.addView(object);
        } else {
            layoutParams = (FrameLayout.LayoutParams) object.getLayoutParams();
        }
        layoutParams.leftMargin = (int) cell.getX();
        layoutParams.topMargin = (int) cell.getY();
        object.setLayoutParams(layoutParams);

        return true;
    }

    private boolean placeObstacleOnGrid(TextView obstacle, int xPos, int yPos) {
        return placeObjectOnGrid(obstacle, xPos, yPos, 1);
    }

    private boolean placeRobotOnGrid(ImageView robot, int xPos, int yPos) {
        if (xPos < 2 || xPos > 19 || yPos < 2 || yPos > 19) {
            return false;
        }

        boolean result = placeObjectOnGrid(robot, xPos - 1, yPos + 1, 3);
        binding.robot.setBackgroundColor(getAttrValue(com.google.android.material.R.attr.colorOnPrimary));

        return result;
    }

    private int getAttrValue(int res) {
        TypedValue typedValue = new TypedValue();
        requireActivity().getTheme().resolveAttribute(res, typedValue, true);
        return typedValue.data;
    }
}
