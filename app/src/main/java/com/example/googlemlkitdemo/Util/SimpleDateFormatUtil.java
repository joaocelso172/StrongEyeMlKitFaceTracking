package com.example.googlemlkitdemo.Util;

import java.text.SimpleDateFormat;

public class SimpleDateFormatUtil {

    public static SimpleDateFormat standardDateFormat(){
        return new SimpleDateFormat(
                "dd/MM/yyyy hh:mm:ss a");
    }
}
