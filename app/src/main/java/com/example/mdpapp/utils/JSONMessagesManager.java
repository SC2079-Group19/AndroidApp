package com.example.mdpapp.utils;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONMessagesManager {
    private static final String TAG = "JSONMessageManager";
    public enum MessageHeader {
        ROBOT_CONTROL,
        ROBOT_STATUS,
        ITEM_LOCATION,
        ROBOT_LOCATION,
        MISC
    }

    public static JSONObject createJSONMessage(MessageHeader messageType, String message) {
        JSONObject jsonMessage = new JSONObject();
        try {
            jsonMessage.put("header", messageType.toString());
            jsonMessage.put("data", message);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        return jsonMessage;
    }

    public static JSONObject stringToMessageJSON(String str) throws JSONException {
        try {
            JSONObject jo = new JSONObject(str);
            return jo;
        } catch (JSONException e) {
            throw e;
        }
    }
}
