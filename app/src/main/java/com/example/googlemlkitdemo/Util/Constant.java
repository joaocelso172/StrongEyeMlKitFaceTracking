package com.example.googlemlkitdemo.Util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import static androidx.core.content.ContextCompat.getSystemService;

public class Constant {

    public static final String BASE_URL = "http://18.230.70.36:3000";  // url principal do app
    public static final boolean DEBUG = Boolean.parseBoolean("false"); // TROCAR PARA false CASO FOR INSTALAR PRA ALGUM CLIENTE
    public static final String ID_TERMINAL = "/reconhecimento-facial";
    public static final String SEARCH_ROUTE = BASE_URL + ID_TERMINAL + "/buscar";


    public static final String getDeviceIMEI(Context c) {
        String imeinum;

        TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
        String device_id = tm.getDeviceId();

        return null;
    }
}


