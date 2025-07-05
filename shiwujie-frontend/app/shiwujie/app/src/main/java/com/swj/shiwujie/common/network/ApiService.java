package com.swj.shiwujie.common.network;

import com.swj.shiwujie.data.model.BaseResponse;
import com.swj.shiwujie.data.model.BlindLoginSuccessVO;
import com.swj.shiwujie.data.model.BlindVO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/api/user/blind/login/loginAndRegisterQuickly")
    Call<BaseResponse<BlindLoginSuccessVO>> blindQuickLogin(@Query("phone") String phone);

    @POST("/api/user/blind/login/loginAndRegister")
    Call<BaseResponse<BlindLoginSuccessVO>> loginAndRegister(
            @Query("phone") String phone,
            @Query("password") String password
    );

    @GET("/api/user/blind/login/check")
    Call<BaseResponse<Void>> checkLogin(@Header("Authorization") String token);

    @POST("/api/user/blind/delete")
    Call<BaseResponse<Boolean>> deleteBlindAccount(
            @Header("Authorization") String token,
            @Query("blindId") Long blindId
    );

    @GET("/api/user/blind/get/id/vo")
    Call<BaseResponse<BlindVO>> getBlindById(
            @Header("Authorization") String token,
            @Query("blindId") Long blindId
    );
} 