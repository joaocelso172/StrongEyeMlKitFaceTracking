package com.example.googlemlkitdemo.sdk;

import com.truesen.face.sdk.NativeVisionApi;

public class DoorControl {

    private void openDoor() {
        NativeVisionApi.setGpioDirection(124, 1);
    }

    private void closeDoor() {
        NativeVisionApi.setGpioDirection(124, 0);
    }
}
