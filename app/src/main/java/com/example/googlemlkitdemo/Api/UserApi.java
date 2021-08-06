package com.example.googlemlkitdemo.Api;

import com.example.googlemlkitdemo.Model.User;
import com.example.googlemlkitdemo.Util.Constant;

import java.util.List;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface UserApi {

    String CACHE = "Cache-Control: max-age=0";
    String AGENT = "User-Agent: StrongEye";
    String SECURITY = "Security:" + "0xxx0";
    String CONNECTION = "keep-alive";

    @Headers({CACHE, AGENT, SECURITY, CONNECTION})
    @GET("compar-face")
    Call<List<User>> getUsers();

    @Headers({CACHE, AGENT, SECURITY})
    @POST(Constant.SEARCH_ROUTE)
    Call<ResponseBody> postUsers(@Body RequestBody requestBody);

}

