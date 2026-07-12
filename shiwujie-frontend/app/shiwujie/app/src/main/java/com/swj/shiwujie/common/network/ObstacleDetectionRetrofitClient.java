package com.swj.shiwujie.common.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import java.util.concurrent.TimeUnit;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * 障碍物检测专用网络客户端
 * 配置SSL证书信任，解决HTTPS自签名证书问题
 */
public class ObstacleDetectionRetrofitClient {
    private static final String BASE_URL = "https://192.168.100.248:9989/";
    private static ObstacleDetectionRetrofitClient instance;
    private final Retrofit retrofit;

    private ObstacleDetectionRetrofitClient() {
        // 创建日志拦截器：仅 DEBUG 打印 BODY，release 关闭（与 RetrofitClient 一致，A4）
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(com.swj.shiwujie.BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        // 创建信任所有证书的TrustManager
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // 信任所有客户端证书
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // 信任所有服务器证书
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
        };

        try {
            // 创建SSL上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // 创建SSLSocketFactory
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // 创建主机名验证器，信任所有主机名
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true; // 信任所有主机名
                }
            };

            // 创建OkHttpClient，配置SSL信任
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(hostnameVerifier)
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

        } catch (Exception e) {
            throw new RuntimeException("SSL配置失败", e);
        }
    }

    public static synchronized ObstacleDetectionRetrofitClient getInstance() {
        if (instance == null) {
            instance = new ObstacleDetectionRetrofitClient();
        }
        return instance;
    }

    public <T> T createService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }
}
