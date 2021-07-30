package com.example.googlemlkitdemo.sdk;

import com.urovo.sdklibs.SDKManager;

public class LightSensor {

    public static boolean openLight(){
        SDKManager.getInstance().openLight();
        return true;
    }

    public static boolean closeLight(){
        SDKManager.getInstance().closeLight();
        return true;
    }
}
