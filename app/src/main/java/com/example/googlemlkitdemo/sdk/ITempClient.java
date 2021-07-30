package com.example.googlemlkitdemo.sdk;

import android.os.Message;

public interface ITempClient {

    default void runTempClient(){}

    void returnTemp(Message msg);
}
