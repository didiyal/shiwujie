package com.swj.shiwujie.common.utils;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import com.swj.shiwujie.R;

public class BackButtonHelper {
    
    /**
     * 为Activity添加返回按钮
     * @param activity 需要添加返回按钮的Activity
     * @param container 返回按钮的容器
     * @return 返回按钮实例
     */
    public static ImageButton addBackButton(@NonNull Activity activity, @NonNull ViewGroup container) {
        View backButton = activity.getLayoutInflater().inflate(R.layout.layout_back_button, container, false);
        container.addView(backButton, 0);
        
        ImageButton btnBack = backButton.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> activity.finish());
        
        return btnBack;
    }
    
    /**
     * 为Activity添加返回按钮，并自定义点击事件
     * @param activity 需要添加返回按钮的Activity
     * @return 返回按钮
     */
    public static ImageButton addBackButton(
            @NonNull Activity activity,
            @NonNull ViewGroup container,
            @NonNull View.OnClickListener clickListener) {
        ImageButton btnBack = addBackButton(activity, container);
        btnBack.setOnClickListener(clickListener);
        return btnBack;
    }
} 