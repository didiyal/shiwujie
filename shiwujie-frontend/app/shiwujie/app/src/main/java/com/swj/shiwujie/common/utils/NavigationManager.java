package com.swj.shiwujie.common.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;
import androidx.core.app.ActivityCompat;

/**
 * 导航管理器
 * 负责处理导航功能，使用高德URI API和高德开放平台服务
 */
public class NavigationManager {
    private static final String TAG = "NavigationManager";
    
    // 高德开放平台NAVIGATION_KEY
    private static final String AMAP_NAVIGATION_KEY = "21661cc796f359cea0cf682ab0d8dda3";
    
    /**
     * 导航到指定目的地
     * 直接跳转到高德地图导航页面，目的地已填好，用户只需点击开始导航
     * @param context 上下文
     * @param destinationName 目的地名称
     */
    public static void navigateToDestination(Context context, String destinationName) {
        Log.d(TAG, "开始导航到目的地: " + destinationName);
        
        try {
            // 直接使用优化的导航URI，不需要先获取定位
            // 高德地图会自动使用当前位置作为起点
            openAmapNavigation(context, destinationName, null);
            
        } catch (Exception e) {
            Log.e(TAG, "导航失败", e);
            // 降级方案：直接搜索
            openAmapSearch(context, destinationName);
        }
    }
    
    /**
     * 获取当前定位
     * @param context 上下文
     * @return 当前位置，如果获取失败返回null
     */
    private static Location getCurrentLocation(Context context) {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            
            if (locationManager == null) {
                Log.w(TAG, "LocationManager为空");
                return null;
            }
            
            // 检查定位权限
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "定位权限未授予");
                return null;
            }
            
            // 优先使用GPS定位，其次使用网络定位
            Location gpsLocation = null;
            Location networkLocation = null;
            
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Log.d(TAG, "GPS定位结果: " + (gpsLocation != null ? "成功" : "失败"));
                }
            } catch (Exception e) {
                Log.w(TAG, "GPS定位异常", e);
            }
            
            try {
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    Log.d(TAG, "网络定位结果: " + (networkLocation != null ? "成功" : "失败"));
                }
            } catch (Exception e) {
                Log.w(TAG, "网络定位异常", e);
            }
            
            // 选择最佳定位结果
            if (gpsLocation != null) {
                Log.d(TAG, "使用GPS定位结果");
                return gpsLocation;
            } else if (networkLocation != null) {
                Log.d(TAG, "使用网络定位结果");
                return networkLocation;
            } else {
                Log.w(TAG, "无法获取任何定位信息");
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "获取定位信息失败", e);
            return null;
        }
    }
    
    /**
     * 打开高德地图进行路径规划
     * 直接跳转到导航页面，目的地已填好，用户只需点击开始导航
     * @param context 上下文
     * @param destinationName 目的地名称
     * @param currentLocation 当前位置（已废弃，保留参数兼容性）
     */
    private static void openAmapNavigation(Context context, String destinationName, Location currentLocation) {
        try {
            Log.d(TAG, "打开高德地图导航页面");
            
            // 使用最简洁的URI，直接跳转到导航页面
            // 高德地图会自动识别当前位置作为起点
            // 用户只需要点击"开始导航"按钮即可
            String amapUri = "androidamap://route?" +
                           "sourceApplication=视物界" +
                           "&dname=" + destinationName +
                           "&navi=1";
            
            Log.d(TAG, "高德地图导航URI: " + amapUri);
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(amapUri));
            intent.setPackage("com.autonavi.minimap");
            
            // 检查高德地图是否安装
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                Log.d(TAG, "成功打开高德地图导航页面");
            } else {
                Log.w(TAG, "高德地图未安装，尝试打开应用商店");
                openAmapDownload(context);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "打开高德地图导航页面失败", e);
            // 降级方案：直接搜索
            openAmapSearch(context, destinationName);
        }
    }
    
    /**
     * 打开高德地图搜索
     * 根据高德URI API官方文档，使用POI搜索功能
     * @param context 上下文
     * @param destinationName 目的地名称
     */
    private static void openAmapSearch(Context context, String destinationName) {
        try {
            Log.d(TAG, "打开高德地图搜索");
            
            // 根据高德URI API官方文档，正确的POI搜索URI格式
            // 参考：https://lbs.amap.com/api/uri-api/summary
            String searchUri = "androidamap://poi?" +
                             "sourceApplication=视物界" +
                             "&keywords=" + destinationName;
            
            Log.d(TAG, "高德地图搜索URI: " + searchUri);
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchUri));
            intent.setPackage("com.autonavi.minimap");
            
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                Log.d(TAG, "成功打开高德地图搜索");
            } else {
                Log.w(TAG, "高德地图未安装，打开应用商店");
                openAmapDownload(context);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "打开高德地图搜索失败", e);
            openAmapDownload(context);
        }
    }
    
    /**
     * 打开应用商店下载高德地图
     * @param context 上下文
     */
    private static void openAmapDownload(Context context) {
        try {
            Log.d(TAG, "打开应用商店下载高德地图");
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=com.autonavi.minimap"));
            
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                Log.d(TAG, "成功打开应用商店");
            } else {
                Log.w(TAG, "无法打开应用商店，尝试浏览器下载");
                openAmapBrowserDownload(context);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "打开应用商店失败", e);
            openAmapBrowserDownload(context);
        }
    }
    
    /**
     * 通过浏览器下载高德地图
     * @param context 上下文
     */
    private static void openAmapBrowserDownload(Context context) {
        try {
            Log.d(TAG, "通过浏览器下载高德地图");
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.amap.com/download"));
            context.startActivity(intent);
            
            Log.d(TAG, "成功打开浏览器下载页面");
            
        } catch (Exception e) {
            Log.e(TAG, "打开浏览器下载页面失败", e);
        }
    }
}
