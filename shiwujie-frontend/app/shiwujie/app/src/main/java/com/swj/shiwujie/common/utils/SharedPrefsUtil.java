package com.swj.shiwujie.common.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsUtil {
    private static final String PREF_NAME = "shiwujie_prefs";
    private static SharedPreferences prefs;

    public static void init(Context context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public static void setToken(String token) {
        prefs.edit().putString("token", token).apply();
    }

    public static String getToken() {
        return prefs.getString("token", null);
    }

    public static void setUserType(boolean isBlind) {
        prefs.edit().putBoolean("is_blind", isBlind).apply();
    }

    public static boolean isBlind() {
        return prefs.getBoolean("is_blind", false);
    }

    public static void setUserId(Long userId) {
        prefs.edit().putLong("user_id", userId).apply();
    }

    public static Long getUserId() {
        return prefs.getLong("user_id", -1L);
    }

    public static void setPhone(String phone) {
        prefs.edit().putString("phone", phone).apply();
    }

    public static String getPhone() {
        return prefs.getString("phone", null);
    }

    public static void clearAll() {
        prefs.edit().clear().apply();
    }

    public static boolean isLoggedIn() {
        String token = getToken();
        Long userId = getUserId();
        return token != null && !token.isEmpty() && userId != null && userId > 0;
    }

    public static void setLong(Context context, String key, long value) {
        init(context);
        prefs.edit().putLong(key, value).apply();
    }

    public static long getLong(Context context, String key, long defaultValue) {
        init(context);
        return prefs.getLong(key, defaultValue);
    }
} 