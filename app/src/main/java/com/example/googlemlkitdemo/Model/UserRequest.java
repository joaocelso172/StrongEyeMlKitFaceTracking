package com.example.googlemlkitdemo.Model;

import com.example.googlemlkitdemo.Util.SimpleDateFormatUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.example.googlemlkitdemo.Util.Constant.ID_TERMINAL;

public class UserRequest {

    private String base64Image;
    private final String idTerminal = ID_TERMINAL;

    public UserRequest(String base64Image){
        this.base64Image = base64Image;
    }

    public String getIdTerminal() {
        return idTerminal;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }
}
