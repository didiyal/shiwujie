package com.swj.shiwujie.common.navigation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.swj.shiwujie.ChooseIdentityActivity;
import com.swj.shiwujie.blind.BlindHomeActivity;
import com.swj.shiwujie.volunteer.VolunteerHomeActivity;

public class NavigationHelper {
    private static final String TAG = "NavigationHelper";
    
    /**
     * 返回身份选择页面
     */
    public static void backToChooseIdentity(Context context) {
        try {
            Log.d(TAG, "返回身份选择页面");
            Intent intent = new Intent(context, ChooseIdentityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "返回身份选择页面失败", e);
            throw e;
        }
    }

    /**
     * 跳转到盲人用户一键登录页面
     */
    public static void toBlindQuickLogin(Context context) {
        try {
            Log.d(TAG, "跳转到盲人一键登录页面");
            Intent intent = new Intent(context, com.swj.shiwujie.blind.QuickLoginActivity.class);
            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "跳转到盲人一键登录页面失败", e);
            throw e;
        }
    }

    /**
     * 跳转到盲人用户密码登录页面
     */
    public static void toBlindPasswordLogin(Context context) {
        try {
            Log.d(TAG, "跳转到盲人密码登录页面");
            Intent intent = new Intent(context, com.swj.shiwujie.blind.LoginActivity.class);
            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "跳转到盲人密码登录页面失败", e);
            throw e;
        }
    }

    /**
     * 跳转到志愿者一键登录页面
     */
    public static void toVolunteerQuickLogin(Context context) {
        try {
            Log.d(TAG, "跳转到志愿者一键登录页面");
            Intent intent = new Intent(context, com.swj.shiwujie.volunteer.QuickLoginActivity.class);
            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "跳转到志愿者一键登录页面失败", e);
            throw e;
        }
    }

    /**
     * 跳转到志愿者密码登录页面
     */
    public static void toVolunteerPasswordLogin(Context context) {
        Intent intent = new Intent(context, com.swj.shiwujie.volunteer.LoginActivity.class);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    /**
     * 根据用户类型跳转到对应的主页
     * @param isBlind true表示盲人用户，false表示志愿者
     */
    public static void toUserHome(Context context, boolean isBlind) {
        Intent intent = new Intent(context, 
            isBlind ? BlindHomeActivity.class : VolunteerHomeActivity.class);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    /**
     * 跳转到盲人用户主页
     */
    public static void toBlindHome(Context context) {
        toUserHome(context, true);
    }

    /**
     * 跳转到志愿者主页
     */
    public static void toVolunteerHome(Context context) {
        toUserHome(context, false);
    }

    /**
     * 跳转到盲人编辑个人信息页面
     */
    public static void toBlindEditProfile(Context context) {
        Intent intent = new Intent(context, com.swj.shiwujie.blind.EditProfileActivity.class);
        context.startActivity(intent);
    }

    /**
     * 跳转到志愿者编辑个人信息页面
     */
    public static void toVolunteerEditProfile(Context context) {
        Intent intent = new Intent(context, com.swj.shiwujie.volunteer.EditProfileActivity.class);
        context.startActivity(intent);
    }
    
    /**
     * 跳转到权限设置页面
     */
    public static void toPermissionSettings(Context context) {
        Intent intent = new Intent(context, com.swj.shiwujie.common.activity.PermissionSettingsActivity.class);
        context.startActivity(intent);
    }
} 