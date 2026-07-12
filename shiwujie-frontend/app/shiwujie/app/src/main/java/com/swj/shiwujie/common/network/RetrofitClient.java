package com.swj.shiwujie.common.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static final String BASE_URL = "http://47.112.114.139:8100";
    private static RetrofitClient instance;
    private final Retrofit retrofit;

    private RetrofitClient() {
        // 日志拦截器：仅 DEBUG 包打印 BODY（含 token/明文），release 关闭以避免泄露
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(com.swj.shiwujie.BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        // 鉴权拦截器：仅当请求未自带 Authorization 头时，从本地注入 Bearer token。
        // 兜住历史上漏带 token 的调用；既有手拼 "Bearer "+token 的调用不受影响（已有头则跳过）。
        okhttp3.Interceptor authInterceptor = chain -> {
            okhttp3.Request request = chain.request();
            if (request.header("Authorization") == null) {
                String token = null;
                try {
                    token = com.swj.shiwujie.common.utils.SharedPrefsUtil.getToken();
                } catch (Exception ignored) {
                    // SharedPrefsUtil 可能尚未 init（prefs==null），忽略
                }
                if (token != null && !token.isEmpty()) {
                    request = request.newBuilder()
                            .addHeader("Authorization", "Bearer " + token)
                            .build();
                }
            }
            return chain.proceed(request);
        };

        // 创建OkHttpClient，支持流式响应和更长的超时时间
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .readTimeout(60, TimeUnit.SECONDS)      // 读取超时60秒
                .writeTimeout(30, TimeUnit.SECONDS)     // 写入超时30秒
                .connectTimeout(30, TimeUnit.SECONDS)   // 连接超时30秒
                .build();

        // 创建Retrofit实例
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())  // 支持字符串和基本类型
                .addConverterFactory(GsonConverterFactory.create())     // 支持JSON
                .build();
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public <T> T createService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }
} 