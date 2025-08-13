package com.swj.shiwujie.blind.ui.ai;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.swj.shiwujie.R;

/**
 * WebView Activity
 * 用于显示AI协助网页内容
 */
public class WebViewActivity extends AppCompatActivity {
    private static final String TAG = "WebViewActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    
    private WebView webView;
    private ProgressBar progressBar;
    private String url;
    private String title;
    private PermissionRequest pendingPermissionRequest;
    
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        
        // 获取传递的参数
        Intent intent = getIntent();
        if (intent != null) {
            url = intent.getStringExtra("url");
            title = intent.getStringExtra("title");
        }
        
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "无效的网址", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化视图
        initViews();
        
        // 检查并请求摄像头权限
        checkCameraPermission();
        
        // 设置WebView
        setupWebView();
        
        // 加载网页
        loadWebPage();
    }
    
    /**
     * 检查摄像头权限
     */
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "请求摄像头权限");
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, 
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "摄像头权限已获得");
        }
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(title != null ? title : "AI协助");
            }
        }
        
        // 初始化WebView和进度条
        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    /**
     * 设置WebView配置
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        if (webView == null) return;
        
        WebSettings settings = webView.getSettings();
        
        // 基本设置
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        
        // 缓存设置
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // settings.setAppCacheEnabled(true); // 已废弃，在 Android API 33+ 中移除
        settings.setDatabaseEnabled(true);
        
        // 缩放设置
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        
        // 其他设置
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setDefaultTextEncodingName("utf-8");
        
        // 设置WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                Log.w(TAG, "SSL证书错误: " + error.toString());
                Log.w(TAG, "SSL错误类型: " + getSslErrorType(error.getPrimaryError()));
                Log.w(TAG, "证书信息: " + error.getCertificate().toString());
                
                // 开发环境忽略SSL错误，生产环境应该提示用户
                Log.i(TAG, "开发环境：忽略SSL错误，继续加载页面");
                handler.proceed();
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "页面加载完成: " + url);
                if (progressBar != null) {
                    progressBar.setVisibility(android.view.View.GONE);
                }
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "页面加载错误: " + description + ", URL: " + failingUrl);
                Toast.makeText(WebViewActivity.this, "页面加载失败: " + description, Toast.LENGTH_SHORT).show();
                if (progressBar != null) {
                    progressBar.setVisibility(android.view.View.GONE);
                }
            }
        });
        
        // 设置WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                    if (newProgress >= 100) {
                        progressBar.setVisibility(android.view.View.GONE);
                    } else {
                        progressBar.setVisibility(android.view.View.VISIBLE);
                    }
                }
            }
            
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                Log.d(TAG, "WebView请求权限: " + request.getResources()[0]);
                
                // 检查摄像头权限
                if (request.getResources()[0].equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    if (ContextCompat.checkSelfPermission(WebViewActivity.this, 
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        // 已有权限，直接授权
                        request.grant(request.getResources());
                        Log.d(TAG, "摄像头权限已授权");
                    } else {
                        // 保存权限请求，等待用户授权
                        pendingPermissionRequest = request;
                        ActivityCompat.requestPermissions(WebViewActivity.this, 
                                new String[]{Manifest.permission.CAMERA}, 
                                CAMERA_PERMISSION_REQUEST_CODE);
                    }
                } else {
                    // 其他权限直接授权
                    request.grant(request.getResources());
                }
            }
        });
    }
    
    /**
     * 获取SSL错误类型的描述
     */
    private String getSslErrorType(int errorType) {
        switch (errorType) {
            case SslError.SSL_DATE_INVALID:
                return "SSL日期无效";
            case SslError.SSL_EXPIRED:
                return "SSL证书过期";
            case SslError.SSL_IDMISMATCH:
                return "SSL主机名不匹配";
            case SslError.SSL_NOTYETVALID:
                return "SSL证书尚未生效";
            case SslError.SSL_UNTRUSTED:
                return "SSL证书不受信任";
            default:
                return "未知SSL错误: " + errorType;
        }
    }
    
    /**
     * 加载网页
     */
    private void loadWebPage() {
        if (webView != null && url != null) {
            Log.d(TAG, "开始加载网页: " + url);
            webView.loadUrl(url);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                        @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "摄像头权限已获得");
                // 权限获得后，处理之前保存的权限请求
                if (pendingPermissionRequest != null) {
                    pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
                    pendingPermissionRequest = null;
                }
            } else {
                Log.w(TAG, "摄像头权限被拒绝");
                Toast.makeText(this, "需要摄像头权限才能使用此功能", Toast.LENGTH_SHORT).show();
                // 权限被拒绝，拒绝WebView的权限请求
                if (pendingPermissionRequest != null) {
                    pendingPermissionRequest.deny();
                    pendingPermissionRequest = null;
                }
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
