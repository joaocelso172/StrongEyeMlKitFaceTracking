package com.example.googlemlkitdemo.Util;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class Base64Converter {

    public static String bitmapToBase64(Bitmap bm){

        return Base64.encodeToString(bitmapToByteArray(bm), Base64.NO_WRAP);
    }


    private static byte[] bitmapToByteArray(Bitmap bitmap) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

}

