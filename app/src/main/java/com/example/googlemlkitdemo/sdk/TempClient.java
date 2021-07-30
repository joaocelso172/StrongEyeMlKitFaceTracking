package com.example.googlemlkitdemo.sdk;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.urovo.sdklibs.OnTempListener;
import com.urovo.sdklibs.SDKManager;
import com.urovo.sdklibs.utils.ToastTool;

public class TempClient extends Thread {

    private Handler handler;
    public static String TAG = "TempClient";

    public TempClient(Handler handler) {
        this.handler = handler;
            showTemp();
    }

    private void showTemp() {
        try {
            SDKManager.getInstance().receiveTemp(new OnTempListener() {
                @Override
                public void onDataReceive(final double temp) {
                    Message msg = new Message();
                    msg.what = 0;
                    msg.obj = temp;
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
