package com.example.mdpapp.fragments.home;

import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mdpapp.MainActivity;
import com.example.mdpapp.R;
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
            String message = messageViewModel.getMessageContent().getValue();
            String header = messageHeader.toString();

            binding.txtReceivedMsg.append(getFormattedMessage(message, header, false));
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
                    binding.txtReceivedMsg.append(getFormattedMessage(msg, msgHeader, true));
                }
            }
        });
    }

    private SpannableString getFormattedMessage(String msg, String msgHeader, boolean sent) {
        String dateTimePattern = "hh:mm:ss dd/MM/yy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(dateTimePattern);

        String dateTime = dateFormat.format(new Date());
        String delimiter = " | ";
        String fullString = msgHeader+delimiter+dateTime+"\n"+msg+"\n\n";

        SpannableString formattedMsg = new SpannableString(fullString);
        formattedMsg.setSpan(new RelativeSizeSpan(0.6f), 0, msgHeader.length()+dateTimePattern.length()+delimiter.length(), 0);

        if (sent) {
            formattedMsg.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, fullString.length(), 0);
        }

        return formattedMsg;
    }
}
