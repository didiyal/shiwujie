package com.swj.shiwujie.common.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.swj.shiwujie.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限管理工具类
 * 统一管理应用所需的所有权限
 */
public class PermissionManager {
    
    public static final int PERMISSION_REQUEST_CODE = 100;
    public static final int OVERLAY_PERMISSION_REQUEST_CODE = 101;
    
    // 核心权限（视频通话必需）
    private static final String[] CORE_PERMISSIONS = {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    };
    
    // 辅助权限（可选）
    private static final String[] OPTIONAL_PERMISSIONS = {
        Manifest.permission.BLUETOOTH_CONNECT
    };
    
    // 所有权限
    private static final String[] ALL_PERMISSIONS = {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.BLUETOOTH_CONNECT
    };
    
    /**
     * 检查核心权限是否已授予
     */
    public static boolean hasCorePermissions(Context context) {
        for (String permission : CORE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查视频通话所需的所有权限（包括蓝牙权限）
     */
    public static boolean hasVideoCallPermissions(Context context) {
        // 检查核心权限
        if (!hasCorePermissions(context)) {
            return false;
        }
        
        // 检查蓝牙权限（AnyRTC SDK需要）
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查所有权限是否已授予
     */
    public static boolean hasAllPermissions(Context context) {
        for (String permission : ALL_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查悬浮窗权限
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }
    
    /**
     * 获取缺失的核心权限列表
     */
    public static List<String> getMissingCorePermissions(Context context) {
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : CORE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        return missingPermissions;
    }
    
    /**
     * 获取缺失的所有权限列表
     */
    public static List<String> getMissingPermissions(Context context) {
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : ALL_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        return missingPermissions;
    }
    
    /**
     * 请求核心权限（用于登录后）
     */
    public static void requestCorePermissions(Activity activity) {
        List<String> missingPermissions = getMissingCorePermissions(activity);
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                missingPermissions.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    /**
     * 请求所有权限（用于登录后）
     */
    public static void requestAllPermissions(Activity activity) {
        List<String> missingPermissions = getMissingPermissions(activity);
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                missingPermissions.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    /**
     * 请求悬浮窗权限
     */
    public static void requestOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
        }
    }
    
    /**
     * 检查视频通话权限（在视频通话前调用）
     */
    public static boolean checkVideoCallPermissions(Context context) {
        return hasVideoCallPermissions(context);
    }
    
    /**
     * 检查语音通话权限（在语音通话前调用）
     */
    public static boolean checkAudioCallPermissions(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 显示权限说明对话框
     */
    public static void showPermissionRequiredDialog(Context context, String message) {
        new AlertDialog.Builder(context)
            .setTitle("权限说明")
            .setMessage(message)
            .setPositiveButton("去设置", (dialog, which) -> {
                openAppSettings(context);
            })
            .setNegativeButton("取消", (dialog, which) -> {
                // 退出APP
                if (context instanceof Activity) {
                    ((Activity) context).finish();
                }
                System.exit(0);
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * 显示权限被拒绝的对话框
     */
    public static void showPermissionDeniedDialog(Context context) {
        new AlertDialog.Builder(context)
            .setTitle("权限被拒绝")
            .setMessage("需要相关权限才能使用完整功能。请在设置中开启权限后重新启动应用。")
            .setPositiveButton("去设置", (dialog, which) -> {
                openAppSettings(context);
            })
            .setNegativeButton("退出", (dialog, which) -> {
                // 退出APP
                if (context instanceof Activity) {
                    ((Activity) context).finish();
                }
                System.exit(0);
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * 打开应用设置页面
     */
    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    
    /**
     * 处理权限请求结果
     */
    public static boolean handlePermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                // 检查是否有视频通话必需权限被拒绝
                boolean hasVideoPermissionDenied = false;
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        // 检查是否是视频通话必需的权限
                        String deniedPermission = permissions[i];
                        if (deniedPermission.equals(Manifest.permission.CAMERA) ||
                            deniedPermission.equals(Manifest.permission.RECORD_AUDIO) ||
                            deniedPermission.equals(Manifest.permission.BLUETOOTH_CONNECT)) {
                            hasVideoPermissionDenied = true;
                            break;
                        }
                    }
                }
                
                if (hasVideoPermissionDenied) {
                    return false; // 视频通话必需权限被拒绝，需要特殊处理
                }
            }
        }
        return true;
    }
    
    /**
     * 检查并请求登录后需要的权限
     */
    public static void checkAndRequestLoginPermissions(Activity activity) {
        // 请求所有权限（包括蓝牙权限）
        requestAllPermissions(activity);
        
        // 再请求悬浮窗权限
        requestOverlayPermission(activity);
    }
} 