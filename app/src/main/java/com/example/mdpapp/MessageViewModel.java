package com.example.mdpapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MessageViewModel extends ViewModel {
    private MutableLiveData<JSONMessagesManager.MessageHeader> messageType = new MutableLiveData<>();
    private MutableLiveData<String> messageContent = new MutableLiveData<>();

    public LiveData<JSONMessagesManager.MessageHeader> getMessageType() {
        return messageType;
    }

    public LiveData<String> getMessageContent() {
        return messageContent;
    }

    public void setMessage(JSONMessagesManager.MessageHeader messageType, String messageContent) {
        this.messageType.setValue(messageType);
        this.messageContent.setValue(messageContent);
    }
}
