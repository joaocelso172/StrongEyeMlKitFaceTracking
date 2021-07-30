package com.example.googlemlkitdemo.Util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import static androidx.core.content.ContextCompat.getSystemService;

public class Constant {

    public static final String BASE_URL = "https://the-tokenizer-314814.rj.r.appspot.com/";  // url principal do app
    public static final boolean DEBUG = Boolean.parseBoolean("false"); // TROCAR PARA false CASO FOR INSTALAR PRA ALGUM CLIENTE
    public static final String ID_TERMINAL = "getDeviceIMEI()";


    public static final String getDeviceIMEI(Context c) {
        String imeinum;

        TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
        String device_id = tm.getDeviceId();

        return null;
    }
}


