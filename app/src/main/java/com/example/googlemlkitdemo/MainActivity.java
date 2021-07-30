package com.example.googlemlkitdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.View;

import com.example.googlemlkitdemo.FaceTracking.FaceTrackingActivity;
import com.urovo.sdklibs.OnTempListener;
import com.urovo.sdklibs.SDKManager;
import com.urovo.sdklibs.utils.ToastTool;

import java.util.ArrayList;

import static androidx.core.content.ContextCompat.getSystemService;

public class MainActivity extends AppCompatActivity {
    private String[] denied;
    private String[] mPermission = {
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_PHONE_STATE,};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applyPermission();
    }

    protected void applyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ArrayList<String> list = new ArrayList<String>();
            for (int i = 0; i < mPermission.length; i++) {
                if (PermissionChecker.checkSelfPermission(this, mPermission[i]) == PermissionChecker.PERMISSION_DENIED) {
                    list.add(mPermission[i]);
                }
            }
            if (list.size() != 0) {
                denied = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    denied[i] = list.get(i);
                }
                ActivityCompat.requestPermissions(this, denied, 5);
            }
        }
    }



    public String getDeviceIMEI() {
        String imeinum;

        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return "imei error";
        }else{

            imeinum = tm.getDeviceId();

            return imeinum;
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button:
                switchActivity(FaceTrackingActivity.class);
                break;
            case R.id.btnTemp:
                //showTem();
                break;
        }
    }

    private void switchActivity(Class c) {
        Intent intent = new Intent(this, c);
        this.startActivity(intent);
    }



}