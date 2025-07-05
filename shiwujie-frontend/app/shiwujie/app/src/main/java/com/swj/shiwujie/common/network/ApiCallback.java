package com.swj.shiwujie.common.network;

import android.content.Context;
import android.content.Intent;

import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BaseResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class ApiCallback<T> implements Callback<BaseResponse<T>> {
    
    @Override
    public void onResponse(Call<BaseResponse<T>> call, Response<BaseResponse<T>> response) {
        if (response.isSuccessful() && response.body() != null) {
            BaseResponse<T> res = response.body();
            
            // code=1表示成功，存储用户信息
            if (res.getCode() == 1) {
                onSuccess(res.getData());
                return;
            }
            
            // code=40010表示未登录，跳转登录页
            if (res.getCode() == 40010) {
                Context context = getContext();
                if (context != null) {
                    SharedPrefsUtil.init(context);
                    // 根据用户跳转
                    Intent intent;
                    if (SharedPrefsUtil.isBlind()) {
                        intent = new Intent(context, com.swj.shiwujie.blind.LoginActivity.class);
                    } else {
                        intent = new Intent(context, com.swj.shiwujie.volunteer.LoginActivity.class);
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
                return;
            }
            onError(res.getMessage());
        } else {
            onError("网络请求失败");
        }
    }

    @Override
    public void onFailure(Call<BaseResponse<T>> call, Throwable t) {
        onError(t.getMessage());
    }

    protected abstract Context getContext();

    public abstract void onSuccess(T data);

    public abstract void onError(String message);
}