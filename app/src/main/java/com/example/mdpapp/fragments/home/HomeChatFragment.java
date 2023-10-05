package com.example.mdpapp.fragments.home;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mdpapp.MainActivity;
import com.example.mdpapp.view_models.MessageViewModel;
import com.example.mdpapp.utils.bluetooth.BluetoothConnectionManager;
import com.example.mdpapp.utils.JSONMessagesManager;
import com.example.mdpapp.databinding.HomeChatFragmentBinding;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class HomeChatFragment extends Fragment {
    private static final String TAG = "HomeChatFragment";
    private HomeChatFragmentBinding binding;
    private BluetoothConnectionManager bluetoothConnectionManager = BluetoothConnectionManager.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = HomeChatFragmentBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MessageViewModel messageViewModel = ((MainActivity) requireActivity()).getMessageViewModel();

        messageViewModel.getMessageType().observe(getViewLifecycleOwner(), messageHeader -> {
            String dateTimePattern = "hh:mm:ss dd/MM/yy";
            SimpleDateFormat dateFormat = new SimpleDateFormat(dateTimePattern);
            String dateTime = dateFormat.format(new Date());

            String message = messageViewModel.getMessageContent().getValue();
            String header = messageHeader.toString();
            SpannableString formattedMsg = new SpannableString(header+" | "+dateTime+"\n"+message+"\n\n");
            formattedMsg.setSpan(new RelativeSizeSpan(0.6f), 0, header.length()+dateTimePattern.length()+3, 0);

            binding.txtReceivedMsg.append(formattedMsg);
        });

        binding.txtReceivedMsg.setMovementMethod(new ScrollingMovementMethod());
        binding.btnSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = binding.editTextSend.getText().toString();
                binding.editTextSend.setText("");
                if (binding.radioBtnGrpMsgHeader.getCheckedRadioButtonId() == -1) {
                    Toast.makeText(requireActivity(), "Please select message type", Toast.LENGTH_LONG).show();
                } else {
                    RadioButton selectedBtn = requireActivity().findViewById(binding.radioBtnGrpMsgHeader.getCheckedRadioButtonId());
                    String msgHeader = selectedBtn.getText().toString();
                    JSONObject message = JSONMessagesManager.createJSONMessage(JSONMessagesManager.MessageHeader.valueOf(msgHeader), msg);
                    try {
                        bluetoothConnectionManager.sendMessage(message.toString());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        });
    }
}
