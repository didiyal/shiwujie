package com.swj.shiwujie.common.utils;

import android.content.Context;
import android.widget.Toast;

import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.data.model.BlindVO;

/**
 * 用户信息管理工具类
 * 用于获取和缓存用户完整信息
 */
public class UserInfoManager {
    private static BlindVO currentUserInfo;
    private static ApiService apiService;

    public static void init() {
        apiService = RetrofitClient.getInstance().createService(ApiService.class);
    }

    /**
     * 获取用户完整信息
     * @param context 上下文
     * @param callback 回调接口，用于处理获取结果
     */
    public static void fetchUserInfo(Context context, UserInfoCallback callback) {
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            if (callback != null) {
                callback.onError("用户信息无效，请重新登录");
            }
            return;
        }

        apiService.getBlindById("Bearer " + token, userId).enqueue(new ApiCallback<BlindVO>(context) {
            @Override
            public void onSuccess(BlindVO data) {
                currentUserInfo = data; // 缓存用户信息
                if (callback != null) {
                    callback.onSuccess(data);
                }
            }

            @Override
            public void onError(String message) {
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }

    /**
     * 获取当前缓存的用户信息
     * @return 用户信息，如果未缓存则返回null
     */
    public static BlindVO getCurrentUserInfo() {
        return currentUserInfo;
    }

    /**
     * 清除缓存的用户信息
     */
    public static void clearUserInfo() {
        currentUserInfo = null;
    }

    /**
     * 用户信息获取回调接口
     */
    public interface UserInfoCallback {
        void onSuccess(BlindVO userInfo);
        void onError(String message);
    }
} 