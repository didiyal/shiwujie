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
     * 智能选择已安装的地图应用进行导航
     * @param context 上下文
     * @param destinationName 目的地名称
     */
    public static void navigateToDestination(Context context, String destinationName) {
        Log.d(TAG, "开始导航到目的地: " + destinationName);
        
        try {
            // 智能选择地图应用进行导航
            openMapNavigation(context, destinationName);
            
        } catch (Exception e) {
            Log.e(TAG, "导航失败", e);
            // 降级方案：使用高德地图
            openAmapNavigation(context, destinationName, null);
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
     * 智能选择地图应用进行导航
     * 优先选择已安装的地图应用，按优先级：高德 > 百度 > 腾讯
     * @param context 上下文
     * @param destinationName 目的地名称
     */
    private static void openMapNavigation(Context context, String destinationName) {
        Log.d(TAG, "智能选择地图应用进行导航");
        
        // 检查已安装的地图应用
        boolean isGaodeInstalled = isAmapInstalled(context);
        boolean isBaiduInstalled = isBaiduMapInstalled(context);
        boolean isTencentInstalled = isTencentMapInstalled(context);
        
        Log.d(TAG, "地图应用检测结果 - 高德: " + isGaodeInstalled + 
                   ", 百度: " + isBaiduInstalled + 
                   ", 腾讯: " + isTencentInstalled);
        
        // 按优先级选择地图应用
        if (isGaodeInstalled) {
            Log.d(TAG, "选择高德地图进行导航");
            openAmapNavigation(context, destinationName, null);
        } else if (isBaiduInstalled) {
            Log.d(TAG, "选择百度地图进行导航");
            openBaiduMapNavigation(context, destinationName);
        } else if (isTencentInstalled) {
            Log.d(TAG, "选择腾讯地图进行导航");
            openTencentMapNavigation(context, destinationName);
        } else {
            Log.d(TAG, "未检测到地图应用，使用高德地图网页版");
            openAmapWebNavigation(context, destinationName);
        }
    }
    
    /**
     * 打开高德地图进行路径规划
     * 使用您提供的API格式，t=2表示步行模式
     * @param context 上下文
     * @param destinationName 目的地名称
     * @param currentLocation 当前位置（已废弃，保留参数兼容性）
     */
    private static void openAmapNavigation(Context context, String destinationName, Location currentLocation) {
        try {
            Log.d(TAG, "打开高德地图导航页面");
            
            // 使用您提供的API格式：amapuri://route/plan/
            // t=2 表示步行模式，sourceApplication=视物界
            String routeUri = "amapuri://route/plan/?" +
                            "sourceApplication=视物界" +
                            "&dname=" + destinationName +
                            "&t=2";
            
            Log.d(TAG, "高德地图导航URI: " + routeUri);
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(routeUri));
            
            // 直接启动，让系统自动选择合适的应用
            try {
                context.startActivity(intent);
                Log.d(TAG, "成功打开高德地图导航页面");
            } catch (Exception e) {
                Log.w(TAG, "无法打开导航URI，尝试降级方案", e);
                openAmapDownload(context);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "打开高德地图导航页面失败", e);
            // 降级方案：直接搜索
            openAmapSearch(context, destinationName);
        }
    }
    
    /**
     * 打开高德地图路径规划（步行模式）
     * 使用您提供的API格式，t=2表示步行模式
     * @param context 上下文
     * @param destinationName 目的地名称
     */
    private static void openAmapSearch(Context context, String destinationName) {
        try {
            Log.d(TAG, "打开高德地图路径规划（步行模式）");
            
            // 使用您提供的API格式：amapuri://route/plan/
            // t=2 表示步行模式，sourceApplication=视物界
            String routeUri = "amapuri://route/plan/?" +
                            "sourceApplication=视物界" +
                            "&dname=" + destinationName +
                            "&t=2";
            
            Log.d(TAG, "高德地图路径规划URI: " + routeUri);
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(routeUri));
            
            // 直接启动，让系统自动选择合适的应用
            try {
                context.startActivity(intent);
                Log.d(TAG, "成功打开高德地图路径规划");
            } catch (Exception e) {
                Log.w(TAG, "无法打开路径规划URI，尝试应用商店", e);
                openAmapDownload(context);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "打开高德地图路径规划失败", e);
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
    
    /**
     * 检查高德地图是否安装
     * @param context 上下文
     * @return 是否安装
     */
    private static boolean isAmapInstalled(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.autonavi.minimap", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 检查百度地图是否安装
     * @param context 上下文
     * @return 是否安装
     */
    private static boolean isBaiduMapInstalled(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.baidu.BaiduMap", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 检查腾讯地图是否安装
     * @param context 上下文
     * @return 是否安装
     */
    private static boolean isTencentMapInstalled(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.tencent.map", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 打开百度地图进行导航
     * 使用您提供的API格式，mode=walking表示步行模式
     * @param context 上下文
     * @param destinationName 目的地名称
     */
    private static void openBaiduMapNavigation(Context context, String destinationName) {
        try {
            Log.d(TAG, "打开百度地图进行导航");
            
            // 使用您提供的API格式：baidumap://map/direction
            // mode=walking 表示步行模式，coord_type=wgs84，src=视物界
            String baiduUri = "baidumap://map/direction?" +
                             "destination=name:" + destinationName +
                             "&mode=walking" +
                             "&coord_type=wgs84" +
                             "&src=视物界";
            
            Log.d(TAG, "百度地图导航URI: " + baiduUri);
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(baiduUri));
            intent.setPackage("com.baidu.BaiduMap");
            
            try {
                context.startActivity(intent);
                Log.d(TAG, "成功打开百度地图导航");
            } catch (Exception e) {
                Log.w(TAG, "无法打开百度地图，尝试网页版", e);
                openBaiduWebNavigation(context, destinationName);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "打开百度地图导航失败", e);
            openBaiduWebNavigation(context, destinationName);
        }
    }
    
    /**
     * 打开腾讯地图进行导航
     * 使用您提供的API格式，type=walk表示步行模式
     * @param context 上下文
     * @param destinationName 目的地名称
     */
    private static void openTencentMapNavigation(Context context, String destinationName) {
        try {
            Log.d(TAG, "打开腾讯地图进行导航");
            
            // 使用您提供的API格式：qqmap://map/routeplan
            // type=walk 表示步行模式
            String tencentUri = "qqmap://map/routeplan?" +
                               "type=walk" +
                               "&to=" + destinationName;
            
            Log.d(TAG, "腾讯地图导航URI: " + tencentUri);
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tencentUri));
            intent.setPackage("com.tencent.map");
            
            try {
                context.startActivity(intent);
                Log.d(TAG, "成功打开腾讯地图导航");
            } catch (Exception e) {
                Log.w(TAG, "无法打开腾讯地图，尝试网页版", e);
                openTencentWebNavigation(context, destinationName);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "打开腾讯地图导航失败", e);
            openTencentWebNavigation(context, destinationName);
        }
    }
    
    /**
     * 打开高德地图网页版导航
     * 使用您提供的网页版API格式
     * @param context 上下文
     * @param destinationName 目的地名称
     */
    private static void openAmapWebNavigation(Context context, String destinationName) {
        try {
            Log.d(TAG, "打开高德地图网页版导航");
            
            // 使用您提供的网页版API：http://wap.amap.com/
            String webUri = "http://wap.amap.com/";
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            context.startActivity(intent);
            
            Log.d(TAG, "成功打开高德地图网页版");
            
        } catch (Exception e) {
            Log.e(TAG, "打开高德地图网页版失败", e);
        }
    }
    
    /**
     * 打开百度地图网页版导航
     * 使用您提供的网页版API格式
     * @param context 上下文
     * @param destinationName 目的地名称
     */
    private static void openBaiduWebNavigation(Context context, String destinationName) {
        try {
            Log.d(TAG, "打开百度地图网页版导航");
            
            // 使用您提供的网页版API：http://map.baidu.com/zt/qudao/newfengchao/1012337a/html/slide.html
            String webUri = "http://map.baidu.com/zt/qudao/newfengchao/1012337a/html/slide.html";
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            context.startActivity(intent);
            
            Log.d(TAG, "成功打开百度地图网页版");
            
        } catch (Exception e) {
            Log.e(TAG, "打开百度地图网页版失败", e);
        }
    }
    
    /**
     * 打开腾讯地图网页版导航
     * 使用您提供的网页版API格式
     * @param context 上下文
     * @param destinationName 目的地名称
     */
    private static void openTencentWebNavigation(Context context, String destinationName) {
        try {
            Log.d(TAG, "打开腾讯地图网页版导航");
            
            // 使用您提供的网页版API：https://apis.map.qq.com/uri/v1/routeplan
            // type=walk 表示步行模式，policy=1
            String webUri = "https://apis.map.qq.com/uri/v1/routeplan?" +
                           "type=walk" +
                           "&to=" + destinationName +
                           "&policy=1";
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            context.startActivity(intent);
            
            Log.d(TAG, "成功打开腾讯地图网页版");
            
        } catch (Exception e) {
            Log.e(TAG, "打开腾讯地图网页版失败", e);
        }
        }
}
