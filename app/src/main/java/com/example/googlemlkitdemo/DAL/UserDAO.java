package com.example.googlemlkitdemo.DAL;

import android.util.Log;

import com.example.googlemlkitdemo.Api.RetrofitClient;
import com.example.googlemlkitdemo.Api.UserApi;
import com.example.googlemlkitdemo.Model.User;
import com.example.googlemlkitdemo.Model.UserRequest;
import com.example.googlemlkitdemo.Model.UserResponse;
import com.google.gson.Gson;

import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserDAO {

    private UserApi mUser = RetrofitClient.getRetrofitClient().create(UserApi.class);;
    private final String TAG = "UserDAO";
    private UserResponse userResponse = null;

    private void getUsers() {

        //mUser = RetrofitClient.getRetrofitClient().create(UserApi.class);

        Call<List<User>> callUsers = mUser.getUsers();
        callUsers.enqueue(new Callback<List<User>>() {


            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful()) {

                    List<User> users = response.body();
                    //showUsers(users);


                }

            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                t.getStackTrace();
            }

        });

    }

    public UserResponse sendUser(UserRequest onRequest){
        //Gson gson = new Gson();
        String json = new Gson().toJson(onRequest);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

        mUser.postUsers(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()){
                    String responseMsg = response.body().toString();
                    UserResponse res = new Gson().fromJson("responseMsg", UserResponse.class);
                    Log.d(TAG, responseMsg);
                    userResponse = res;
                } else Log.e(TAG, "Não foi possível estabelecer conexão com o servidor: " + response);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });

        return userResponse;
    }


}
