package com.swj.shiwujie.common.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.swj.shiwujie.R;

public class EmergencyHelpIncomingWindow {
    private Context context;
    private WindowManager windowManager;
    private View floatingView;
    private TextView tvBlindPhone;
    private Button btnAccept, btnReject;
    private boolean isShowing = false;

    public EmergencyHelpIncomingWindow(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initView();
    }

    private void initView() {
        floatingView = LayoutInflater.from(context).inflate(R.layout.emergency_help_incoming_window, null);
        tvBlindPhone = floatingView.findViewById(R.id.tvBlindPhone);
        btnAccept = floatingView.findViewById(R.id.btnAccept);
        btnReject = floatingView.findViewById(R.id.btnReject);
    }

    public void setBlindPhone(String phone) {
        if (tvBlindPhone != null) {
            tvBlindPhone.setText("手机号：" + phone);
        }
    }

    public void setOnAcceptListener(View.OnClickListener listener) {
        if (btnAccept != null) {
            btnAccept.setOnClickListener(listener);
        }
    }

    public void setOnRejectListener(View.OnClickListener listener) {
        if (btnReject != null) {
            btnReject.setOnClickListener(listener);
        }
    }

    public void show() {
        if (isShowing) return;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT
        );
        params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
        params.y = 100;
        windowManager.addView(floatingView, params);
        isShowing = true;
    }

    public void hide() {
        if (!isShowing) return;
        windowManager.removeView(floatingView);
        isShowing = false;
    }

    public void destroy() {
        if (isShowing) hide();
        context = null;
        windowManager = null;
        floatingView = null;
        tvBlindPhone = null;
        btnAccept = null;
        btnReject = null;
    }
} 