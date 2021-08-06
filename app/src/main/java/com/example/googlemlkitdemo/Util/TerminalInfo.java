package com.example.googlemlkitdemo.Util;

import android.content.Context;
import android.telephony.TelephonyManager;

public class TerminalInfo {

    public static String getDeviceIMEI(Context c) {

        TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
        String device_id = tm.getDeviceId();

        return null;
    }
}
