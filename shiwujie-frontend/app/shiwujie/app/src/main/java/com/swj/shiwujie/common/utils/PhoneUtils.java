package com.swj.shiwujie.common.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.core.app.ActivityCompat;

import java.util.List;

public class PhoneUtils {
    
    public static String getPhoneNumber(Context context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10及以上版本，使用SubscriptionManager
                SubscriptionManager subscriptionManager = context.getSystemService(SubscriptionManager.class);
                
                // 检查权限
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) 
                        != PackageManager.PERMISSION_GRANTED) {
                    return null;
                }
                
                List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
                if (subscriptionInfos == null || subscriptionInfos.isEmpty()) {
                    return null;
                }
                
                String phone = subscriptionInfos.get(0).getNumber();
                if (phone == null || phone.isEmpty()) {
                    return null;
                }
                
                if (phone.startsWith("+86")) {
                    phone = phone.substring(3);
                }
                return phone;
                
            } else {
                // Android 9及以下版本，使用TelephonyManager
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
                        != PackageManager.PERMISSION_GRANTED) {
                    return null;
                }
                
                String phone = tm.getLine1Number();
                if (phone == null || phone.isEmpty()) {
                    return null;
                }
                
                if (phone.startsWith("+86")) {
                    phone = phone.substring(3);
                }
                return phone;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
} 