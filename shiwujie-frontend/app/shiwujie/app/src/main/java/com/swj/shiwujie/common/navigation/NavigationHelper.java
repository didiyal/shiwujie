package com.swj.shiwujie.common.navigation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.swj.shiwujie.blind.BlindHomeActivity;
import com.swj.shiwujie.blind.LoginActivity;
import com.swj.shiwujie.blind.QuickLoginActivity;
import com.swj.shiwujie.volunteer.VolunteerHomeActivity;

public class NavigationHelper {
    
    /**
     * 跳转到盲人用户一键登录页面
     */
    public static void toBlindQuickLogin(Context context) {
        Intent intent = new Intent(context, QuickLoginActivity.class);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    /**
     * 跳转到盲人用户密码登录页面
     */
    public static void toBlindPasswordLogin(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
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
} 