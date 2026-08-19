package com.christopher.weighttracker;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SmsUtil {
    public static final int REQ_SEND_SMS = 1001;

    public static boolean hasSmsPermission(Activity a) {
        return ContextCompat.checkSelfPermission(a, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestSmsPermission(Activity a) {
        ActivityCompat.requestPermissions(a,
                new String[]{Manifest.permission.SEND_SMS}, REQ_SEND_SMS);
    }

    public static void sendGoalReached(String phone, double goal, double current, Activity a) {
        try {
            String msg = "Congrats! You reached your goal weight " + goal +
                    " (current: " + current + ").";
            SmsManager.getDefault().sendTextMessage(phone, null, msg, null, null);
            Toast.makeText(a, "SMS sent", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(a, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
