package com.swj.shiwujie.common.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用列表管理器
 * 负责获取、缓存和查询已安装应用信息
 * 参考：https://blog.51cto.com/u_16213421/12975926
 */
public class AppListManager {
    private static final String TAG = "AppListManager";
    
    // 应用名称到包名的映射缓存
    private Map<String, String> appNameToPackageMap;
    private Context context;
    private boolean isInitialized = false;
    
    // AI悬浮球启动回调接口
    public interface OnAppListReadyListener {
        void onAppListReady();
    }
    
    private OnAppListReadyListener appListReadyListener;
    
    public AppListManager(Context context) {
        this.context = context;
        this.appNameToPackageMap = new HashMap<>();
    }
    
    /**
     * 获取已安装应用列表
     * 在子线程中执行，避免阻塞主线程
     * 参考：https://blog.51cto.com/u_16213421/12975926
     */
    public void loadInstalledApps() {
        if (isInitialized) {
            Log.d(TAG, "应用列表已初始化，跳过重复加载");
            return;
        }
        
        try {
            Log.d(TAG, "开始获取已安装应用列表...");
            
            PackageManager packageManager = context.getPackageManager();
            
            // 使用getInstalledPackages方法，这是更标准的方法
            List<PackageInfo> packages = packageManager.getInstalledPackages(0);
            
            if (packages == null || packages.isEmpty()) {
                Log.w(TAG, "获取已安装应用列表失败：列表为空");
                return;
            }
            
            Log.d(TAG, "获取到 " + packages.size() + " 个已安装应用");
            
            // 清空现有缓存
            appNameToPackageMap.clear();
            
            int processedCount = 0;
            int userAppCount = 0;
            
            for (PackageInfo packageInfo : packages) {
                try {
                    ApplicationInfo appInfo = packageInfo.applicationInfo;
                    if (appInfo == null) continue;
                    
                    String packageName = appInfo.packageName;
                    
                    // 不过滤系统应用，保留所有应用以便测试
                    // 后续可以根据需要调整过滤策略
                    boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    if (isSystemApp) {
                        // 记录系统应用但不跳过
                        if (processedCount < 5) {
                            Log.d(TAG, "发现系统应用: " + packageName);
                        }
                    }
                    
                    // 获取应用名称
                    String appName = packageManager.getApplicationLabel(appInfo).toString();
                    
                    if (appName != null && !appName.trim().isEmpty() && packageName != null) {
                        // 清理应用名称
                        String cleanAppName = appName.trim();
                        
                        // 将应用名称转为小写，便于模糊匹配
                        String normalizedAppName = cleanAppName.toLowerCase();
                        
                        // 存储到缓存中
                        appNameToPackageMap.put(normalizedAppName, packageName);
                        appNameToPackageMap.put(cleanAppName, packageName);
                        
                        userAppCount++;
                        
                        // 每处理50个应用打印一次进度
                        if (processedCount % 50 == 0) {
                            Log.d(TAG, "已处理 " + processedCount + " 个应用...");
                        }
                        
                        // 特别关注一些常见应用
                        if (packageName.contains("tencent") || packageName.contains("wechat") || 
                            packageName.contains("qq") || packageName.contains("alipay") ||
                            packageName.contains("amap") || packageName.contains("baidu")) {
                            Log.d(TAG, "发现常见应用: '" + cleanAppName + "' -> " + packageName);
                        }
                    }
                    
                    processedCount++;
                } catch (Exception e) {
                    Log.w(TAG, "处理应用信息失败，包名: " + packageInfo.packageName + ", 错误: " + e.getMessage());
                }
            }
            
            isInitialized = true;
            Log.d(TAG, "应用列表加载完成！总应用数: " + processedCount + ", 缓存应用数: " + appNameToPackageMap.size());
            
            // 打印应用列表
            Log.d(TAG, "=== 应用列表（前30个）===");
            int printCount = 0;
            for (Map.Entry<String, String> entry : appNameToPackageMap.entrySet()) {
                if (printCount < 30) { // 显示前30个应用
                    Log.d(TAG, "应用: '" + entry.getKey() + "' -> " + entry.getValue());
                    printCount++;
                } else {
                    break;
                }
            }
            if (appNameToPackageMap.size() > 30) {
                Log.d(TAG, "... 还有 " + (appNameToPackageMap.size() - 30) + " 个应用");
            }
            Log.d(TAG, "=== 应用列表结束 ===");
            
            // 通知应用列表加载完成，可以启动AI悬浮球
            notifyAppListReady();
            
        } catch (Exception e) {
            Log.e(TAG, "获取已安装应用列表失败", e);
            Log.e(TAG, "错误详情: " + e.getMessage());
            Log.e(TAG, "错误类型: " + e.getClass().getSimpleName());
            
            if (e instanceof SecurityException) {
                Log.e(TAG, "权限不足，无法获取应用列表");
            }
        }
    }
    
    /**
     * 检查应用是否已安装
     * @param appName 应用名称
     * @return 是否已安装
     */
    public boolean isAppInstalled(String appName) {
        if (!isInitialized) {
            Log.w(TAG, "应用列表未初始化，无法检查应用: " + appName);
            return false;
        }
        
        if (appName == null || appName.trim().isEmpty()) {
            Log.w(TAG, "应用名称为空，无法检查");
            return false;
        }
        
        // 尝试精确匹配
        String normalizedAppName = appName.toLowerCase().trim();
        boolean isInstalled = appNameToPackageMap.containsKey(normalizedAppName) || 
                             appNameToPackageMap.containsKey(appName);
        
        // 添加详细的调试信息
        Log.d(TAG, "检查应用 '" + appName + "' 是否已安装: " + (isInstalled ? "是" : "否") + 
                  " | 标准化名称: '" + normalizedAppName + "'");
        
        if (!isInstalled) {
            // 如果没有找到，打印一些缓存中的应用名称作为参考
            Log.d(TAG, "缓存中的应用名称示例（前10个）:");
            int count = 0;
            for (String cachedAppName : appNameToPackageMap.keySet()) {
                if (count < 10) {
                    Log.d(TAG, "  - '" + cachedAppName + "'");
                    count++;
                } else {
                    break;
                }
            }
        }
        
        return isInstalled;
    }
    
    /**
     * 启动应用
     * @param appName 应用名称
     * @return 是否成功启动
     */
    public boolean launchApp(String appName) {
        if (!isInitialized) {
            Log.w(TAG, "应用列表未初始化，无法启动应用: " + appName);
            return false;
        }
        
        try {
            String packageName = getPackageName(appName);
            if (packageName == null) {
                Log.w(TAG, "未找到应用 '" + appName + "' 的包名");
                return false;
            }
            
            Log.d(TAG, "尝试启动应用: " + appName + " (包名: " + packageName + ")");
            
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                Log.d(TAG, "成功启动应用: " + appName);
                return true;
            } else {
                Log.w(TAG, "无法获取应用启动Intent: " + appName);
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "启动应用失败: " + appName, e);
            Log.e(TAG, "错误详情: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取应用包名
     * @param appName 应用名称
     * @return 包名，如果未找到返回null
     */
    public String getPackageName(String appName) {
        if (!isInitialized || appName == null || appName.trim().isEmpty()) {
            return null;
        }
        
        String normalizedAppName = appName.toLowerCase().trim();
        
        // 先尝试精确匹配
        String packageName = appNameToPackageMap.get(appName);
        if (packageName != null) {
            Log.d(TAG, "精确匹配找到应用: '" + appName + "' -> " + packageName);
            return packageName;
        }
        
        // 再尝试小写匹配
        packageName = appNameToPackageMap.get(normalizedAppName);
        if (packageName != null) {
            Log.d(TAG, "小写匹配找到应用: '" + appName + "' -> " + packageName);
            return packageName;
        }
        
        // 尝试包含匹配（更宽松的匹配）
        for (Map.Entry<String, String> entry : appNameToPackageMap.entrySet()) {
            String cachedAppName = entry.getKey();
            String cachedPackageName = entry.getValue();
            
            // 检查包名是否包含关键词
            if (cachedPackageName.toLowerCase().contains(normalizedAppName)) {
                Log.d(TAG, "包名匹配找到应用: '" + appName + "' -> '" + cachedAppName + "' (包名: " + cachedPackageName + ")");
                return cachedPackageName;
            }
            
            // 检查应用名称是否包含关键词
            if (cachedAppName.toLowerCase().contains(normalizedAppName)) {
                Log.d(TAG, "名称包含匹配找到应用: '" + appName + "' -> '" + cachedAppName + "' (包名: " + cachedPackageName + ")");
                return cachedPackageName;
            }
            
            // 检查关键词是否包含应用名称
            if (normalizedAppName.contains(cachedAppName.toLowerCase())) {
                Log.d(TAG, "关键词包含匹配找到应用: '" + appName + "' -> '" + cachedAppName + "' (包名: " + cachedPackageName + ")");
                return cachedPackageName;
            }
        }
        
        Log.d(TAG, "未找到匹配的应用: '" + appName + "'");
        return null;
    }
    
    /**
     * 获取应用商店下载链接
     * @param packageName 包名
     * @return 应用商店链接
     */
    public String getAppStoreLink(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return null;
        }
        
        // 返回Google Play Store链接（可以根据需要修改为其他应用商店）
        return "https://play.google.com/store/apps/details?id=" + packageName;
    }
    
    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * 获取缓存中的应用数量
     */
    public int getCachedAppCount() {
        return appNameToPackageMap.size();
    }
    
    /**
     * 清空缓存
     */
    public void clearCache() {
        appNameToPackageMap.clear();
        isInitialized = false;
        Log.d(TAG, "应用列表缓存已清空");
    }
    
    /**
     * 设置应用列表准备完成的监听器
     */
    public void setOnAppListReadyListener(OnAppListReadyListener listener) {
        this.appListReadyListener = listener;
    }
    
    /**
     * 通知应用列表准备完成
     */
    private void notifyAppListReady() {
        if (appListReadyListener != null) {
            try {
                appListReadyListener.onAppListReady();
            } catch (Exception e) {
                Log.e(TAG, "通知应用列表准备完成失败", e);
            }
        }
    }
}
