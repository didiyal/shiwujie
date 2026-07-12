package com.swj.shiwujie.common.network;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.swj.shiwujie.ChooseIdentityActivity;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BaseResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class ApiCallback<T> implements Callback<BaseResponse<T>> {
    private static final String TAG = "ApiCallback";
    private final Context context;

    public ApiCallback(Context context) {
        this.context = context;
    }

    private boolean isLoginPage() {
        return context instanceof com.swj.shiwujie.blind.LoginActivity ||
               context instanceof com.swj.shiwujie.volunteer.LoginActivity ||
               context instanceof com.swj.shiwujie.blind.QuickLoginActivity ||
               context instanceof com.swj.shiwujie.volunteer.QuickLoginActivity;
    }

    @Override
    public void onResponse(Call<BaseResponse<T>> call, Response<BaseResponse<T>> response) {
        android.util.Log.d("ApiCallback", "API响应: " + call.request().url());
        
        if (!response.isSuccessful()) {
            android.util.Log.e("ApiCallback", "请求失败: HTTP " + response.code());
            try {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                android.util.Log.e("ApiCallback", "错误响应: " + errorBody);
            } catch (Exception e) {
                android.util.Log.e("ApiCallback", "读取错误响应失败", e);
            }
            onError(response.message());
            return;
        }
        
        if (response.body() == null) {
            android.util.Log.e("ApiCallback", "响应体为空");
            return;
        }

        BaseResponse<T> res = response.body();
        android.util.Log.d("ApiCallback", "响应码: " + res.getCode() + ", 消息: " + res.getMessage());
            
        switch (res.getCode()) {
            case 1:
                onSuccess(res.getData());
                break;
                
            case 40010:
                Log.w(TAG, "Token失效,需要重新登录");
                if (context != null && !isLoginPage()) {
                    SharedPrefsUtil.init(context);
                    SharedPrefsUtil.clearAll();
                    Intent intent = new Intent(context, ChooseIdentityActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                }
                onError(res.getMessage());
                break;
                
            case 40000:
                android.util.Log.w("ApiCallback", "家庭不存在或其他业务错误");
                // 对于家庭不存在的情况，不自动跳转，让具体页面处理
                onError(res.getMessage());
                break;
                
            default:
                android.util.Log.e("ApiCallback", "请求失败: " + res.getCode() + ", " + res.getMessage());
                onError(res.getMessage());
                break;
        }
    }

    @Override
    public void onFailure(Call<BaseResponse<T>> call, Throwable t) {
        android.util.Log.e("ApiCallback", "网络请求失败: " + call.request().url(), t);
        onError(t.getMessage());
    }

    public void onError(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public abstract void onSuccess(T response);

}