package com.swj.shiwujie.common.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.utils.PermissionManager;

/**
 * 权限设置页面
 * 让用户查看和管理应用权限
 */
public class PermissionSettingsActivity extends AppCompatActivity {
    
    private TextView tvCameraStatus;
    private TextView tvMicrophoneStatus;
    private TextView tvBluetoothStatus;
    private TextView tvOverlayStatus;
    private Button btnRequestAllPermissions;
    private Button btnOpenSettings;
    private ImageButton btnBack;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_settings);
        
        initViews();
        initListeners();
        updatePermissionStatus();
    }
    
    private void initViews() {
        tvCameraStatus = findViewById(R.id.tv_camera_status);
        tvMicrophoneStatus = findViewById(R.id.tv_microphone_status);
        tvBluetoothStatus = findViewById(R.id.tv_bluetooth_status);
        tvOverlayStatus = findViewById(R.id.tv_overlay_status);
        btnRequestAllPermissions = findViewById(R.id.btn_request_all_permissions);
        btnOpenSettings = findViewById(R.id.btn_open_settings);
        btnBack = findViewById(R.id.btn_back);
    }
    
    private void initListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnRequestAllPermissions.setOnClickListener(v -> {
            PermissionManager.checkAndRequestLoginPermissions(this);
        });
        
        btnOpenSettings.setOnClickListener(v -> {
            PermissionManager.openAppSettings(this);
        });
    }
    
    private void updatePermissionStatus() {
        // 更新摄像头权限状态
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        tvCameraStatus.setText(cameraGranted ? "已授权" : "未授权");
        tvCameraStatus.setTextColor(getResources().getColor(cameraGranted ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        
        // 更新麦克风权限状态
        boolean microphoneGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        tvMicrophoneStatus.setText(microphoneGranted ? "已授权" : "未授权");
        tvMicrophoneStatus.setTextColor(getResources().getColor(microphoneGranted ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        
        // 更新蓝牙权限状态
        boolean bluetoothGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        tvBluetoothStatus.setText(bluetoothGranted ? "已授权" : "未授权");
        tvBluetoothStatus.setTextColor(getResources().getColor(bluetoothGranted ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        
        // 更新悬浮窗权限状态
        boolean overlayGranted = PermissionManager.hasOverlayPermission(this);
        tvOverlayStatus.setText(overlayGranted ? "已授权" : "未授权");
        tvOverlayStatus.setTextColor(getResources().getColor(overlayGranted ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        
        // 更新按钮状态
        boolean allGranted = PermissionManager.hasAllPermissions(this) && PermissionManager.hasOverlayPermission(this);
        btnRequestAllPermissions.setEnabled(!allGranted);
        btnRequestAllPermissions.setText(allGranted ? "所有权限已授权" : "请求所有权限");
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.PERMISSION_REQUEST_CODE) {
            boolean success = PermissionManager.handlePermissionResult(requestCode, permissions, grantResults);
            if (success) {
                Toast.makeText(this, "权限请求成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "部分权限被拒绝，请在设置中手动开启", Toast.LENGTH_LONG).show();
            }
            updatePermissionStatus();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionManager.OVERLAY_PERMISSION_REQUEST_CODE) {
            updatePermissionStatus();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 从设置页面返回时更新状态
        updatePermissionStatus();
    }
} 