package com.example.mdpapp.fragments.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.motion.widget.OnSwipe;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mdpapp.MainActivity;
import com.example.mdpapp.utils.JSONMessagesManager;
import com.example.mdpapp.utils.OnSwipeTouchListener;
import com.example.mdpapp.view_models.MessageViewModel;
import com.example.mdpapp.R;
import com.example.mdpapp.utils.bluetooth.BluetoothConnectionManager;
import com.example.mdpapp.databinding.HomeFragmentBinding;
import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private HomeFragmentBinding binding;
    private HomeMainFragment homeMainFragment;
    private HomeChatFragment homeChatFragment;
    private BluetoothConnectionManager bluetoothConnectionManager;
    private ArrayList<String> statusMessages = new ArrayList<>();
    private int currMsgPtr = -1;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        bluetoothConnectionManager = BluetoothConnectionManager.getInstance();

        binding = HomeFragmentBinding.inflate(inflater, container, false);

        homeMainFragment = new HomeMainFragment();
        homeChatFragment = new HomeChatFragment();

        getChildFragmentManager().beginTransaction().replace(R.id.fragmentContainer, homeMainFragment).commit();

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        getChildFragmentManager().beginTransaction().replace(R.id.fragmentContainer, homeMainFragment).commit();
                        break;
                    case 1:
                        getChildFragmentManager().beginTransaction().replace(R.id.fragmentContainer, homeChatFragment).commit();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MessageViewModel messageViewModel = ((MainActivity) requireActivity()).getMessageViewModel();

        messageViewModel.getMessageContent().observe(getViewLifecycleOwner(), messageContent -> {
            JSONMessagesManager.MessageHeader header = messageViewModel.getMessageType().getValue();
            switch (header) {
                case ROBOT_STATUS:
                    statusMessages.add(messageViewModel.getMessageContent().getValue());
                    currMsgPtr++;
                    displayStatus();
                    break;
            }
        });

        if (bluetoothConnectionManager.getConnectedDevice() != null) {
            binding.swConnectedTo.setText("Device: " + bluetoothConnectionManager.getConnectedDevice().getName());
        } else {
            return;
        }

        binding.swConnectedTo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    bluetoothConnectionManager.disconnect();
                } catch (IOException e) {
                    Log.e("BluetoothSocket", e.getMessage());
                }
                NavHostFragment.findNavController(HomeFragment.this).navigate(R.id.action_HomeFragment_to_BluetoothConnectionFragment);
            }
        });

        binding.txtStatus.setOnTouchListener(new OnSwipeTouchListener(requireActivity()) {
            @Override
            public void onSwipeRight() {
                if (currMsgPtr - 1 >= 0) {
                    currMsgPtr--;
                    displayStatus();
                    changeArrows();
                }
            }

            @Override
            public void onSwipeLeft() {
                if (currMsgPtr + 1 < statusMessages.size()) {
                    currMsgPtr++;
                    displayStatus();
                    changeArrows();
                }
            }
        });
    }

    private void displayStatus() {
        if (currMsgPtr >= 0 && currMsgPtr < statusMessages.size()) {
            binding.txtStatus.setText(statusMessages.get(currMsgPtr));
        }
    }

    private void changeArrows() {
        if (statusMessages.size() == 1) {
            binding.leftArrowStatus.setVisibility(View.INVISIBLE);
            binding.rightArrowStatus.setVisibility(View.INVISIBLE);
            return;
        }

        if (currMsgPtr == statusMessages.size() - 1) {
            binding.leftArrowStatus.setVisibility(View.VISIBLE);
            binding.rightArrowStatus.setVisibility(View.INVISIBLE);
            return;
        }

        if (currMsgPtr == 0) {
            binding.rightArrowStatus.setVisibility(View.VISIBLE);
            binding.leftArrowStatus.setVisibility(View.INVISIBLE);
            return;
        }

        binding.leftArrowStatus.setVisibility(View.VISIBLE);
        binding.rightArrowStatus.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}