package com.swj.shiwujie.common.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.swj.shiwujie.data.model.SocketDataV0;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * AI turn 位置注入工具（chunk-2e-2 缝 A）。
 *
 * <p>给 {@code AiTurnManager} 的 requestType=100 上行帧提供 {lat,lng,address}。
 * 定位逻辑镜像 {@link NavigationManager}（GPS→NETWORK→null，{@code getLastKnownLocation} 不阻塞），
 * 另补 {@link Geocoder} 反查地址（同步、可能阻塞 → 跑后台线程，回调由调用方切主线程）。</p>
 *
 * <p><b>降级原则（盲人体验）</b>：定位失败 → 回调 {@code null}（AiFragment 照发 100 帧，position=null，
 * 后端宽松接收）；Geocoder 失败/国产 ROM 不可用 → address=""。<b>绝不因定位失败阻塞 AI turn。</b></p>
 */
public class LocationHelper {

    private static final String TAG = "LocationHelper";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "location-helper");
        t.setDaemon(true);
        return t;
    });

    private LocationHelper() {
    }

    /**
     * 异步取当前位置（后台线程，回调消费）。定位失败 → {@code callback.accept(null)}。
     * Geocoder 阻塞调用隔离在后台线程；callback 仍在后台线程，调用方需自行切主线程。
     */
    public static void getCurrent(Context context, Consumer<SocketDataV0.Position> callback) {
        EXECUTOR.execute(() -> {
            SocketDataV0.Position result = null;
            try {
                Location loc = getLastKnownLocation(context);
                if (loc != null) {
                    String address = reverseGeocode(context, loc.getLatitude(), loc.getLongitude());
                    result = new SocketDataV0.Position(loc.getLatitude(), loc.getLongitude(), address);
                }
            } catch (Exception e) {
                Log.w(TAG, "取位置失败，降级 null：" + e.getMessage());
            } finally {
                if (callback != null) {
                    callback.accept(result);
                }
            }
        });
    }

    /** GPS→NETWORK→null（镜像 NavigationManager.getCurrentLocation，{@code getLastKnownLocation} 不阻塞）。 */
    private static Location getLastKnownLocation(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            Log.w(TAG, "LocationManager 为空");
            return null;
        }
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "定位权限未授予");
            return null;
        }
        Location result = null;
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                result = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        } catch (SecurityException | IllegalArgumentException e) {
            Log.w(TAG, "GPS getLastKnownLocation 失败：" + e.getMessage());
        }
        if (result == null) {
            try {
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    result = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            } catch (SecurityException | IllegalArgumentException e) {
                Log.w(TAG, "NETWORK getLastKnownLocation 失败：" + e.getMessage());
            }
        }
        return result;
    }

    private static boolean hasLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Geocoder 反查；失败/空 → ""（国产 ROM 可能不实现 Geocoder，{@link Geocoder#isPresent} 兜底）。
     * 同步阻塞调用，须在后台线程。
     */
    private static String reverseGeocode(Context context, double lat, double lng) {
        try {
            if (!Geocoder.isPresent()) {
                return "";
            }
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<android.location.Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address a = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= a.getMaxAddressLineIndex(); i++) {
                    String line = a.getAddressLine(i);
                    if (line != null) {
                        if (sb.length() > 0) {
                            sb.append(" ");
                        }
                        sb.append(line);
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            Log.w(TAG, "Geocoder 反查失败（降级空 address）：" + e.getMessage());
        }
        return "";
    }
}
