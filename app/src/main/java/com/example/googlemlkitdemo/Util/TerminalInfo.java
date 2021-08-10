package com.example.googlemlkitdemo.Util;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

public class TerminalInfo {

    public static String getDeviceIMEI(Context c) {
        String device_id = "tm.getDeviceId()";
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
            device_id = tm.getDeviceId();
        }

        Log.d("TerminalInfo", "Device ID: " + device_id + ", " + Build.VERSION.SDK_INT);
        return device_id;
    }
}
