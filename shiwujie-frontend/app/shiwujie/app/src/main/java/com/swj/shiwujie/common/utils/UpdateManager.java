package com.swj.shiwujie.common.utils;

import android.app.Activity;
import androidx.core.content.FileProvider;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.data.model.BaseResponse;
import com.swj.shiwujie.data.model.VersionVO;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * App 强制更新管理器
 *
 * <p>启动时 {@link #checkUpdate(Activity)} 查询后端 {@code /api/download/version}，
 * 服务端 versionCode 大于本地即弹<strong>不可取消</strong>弹窗（无取消按钮、禁返回键/点外部），
 * 点击「立即更新」经系统 DownloadManager 下载 APK，完成后拉起安装。</p>
 *
 * <p>依赖：manifest 声明 {@code REQUEST_INSTALL_PACKAGES} 权限与 FileProvider
 * （authority {@code com.swj.shiwujie.fileprovider}，file_paths 含 Download/）。</p>
 */
public class UpdateManager {

    private static final String TAG = "UpdateManager";
    private static final String APK_NAME = "shiwujie_update.apk";
    private static final String FILE_PROVIDER_AUTHORITY = "com.swj.shiwujie.fileprovider";

    private static long sDownloadId = -1;
    private static BroadcastReceiver sDownloadReceiver;

    /**
     * 启动时检查更新：仅当服务端 versionCode > 本地时弹强制更新弹窗；任何失败静默忽略（旧后端无此接口返回 404）。
     */
    public static void checkUpdate(Activity activity) {
        try {
            ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
            apiService.checkVersion().enqueue(new Callback<BaseResponse<VersionVO>>() {
                @Override
                public void onResponse(Call<BaseResponse<VersionVO>> call, Response<BaseResponse<VersionVO>> response) {
                    try {
                        if (!response.isSuccessful() || response.body() == null
                                || response.body().getCode() != 1 || response.body().getData() == null) {
                            Log.d(TAG, "版本检查无有效响应，跳过");
                            return;
                        }
                        VersionVO remote = response.body().getData();
                        long localCode = getLocalVersionCode(activity);
                        Log.d(TAG, "本地versionCode=" + localCode + ", 服务端versionCode=" + remote.getVersionCode());
                        if (remote.getVersionCode() != null && remote.getVersionCode() > localCode) {
                            showForceUpdateDialog(activity, remote);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "版本检查回调处理失败", e);
                    }
                }

                @Override
                public void onFailure(Call<BaseResponse<VersionVO>> call, Throwable t) {
                    Log.d(TAG, "版本检查网络失败（静默忽略）: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "版本检查启动失败", e);
        }
    }

    /** 弹不可取消的强制更新弹窗 */
    private static void showForceUpdateDialog(Activity activity, VersionVO version) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        Log.w(TAG, "发现新版本 v" + version.getVersionName() + "，弹强制更新");

        // 语音播报（视障用户无视觉冗余）
        TTSManager ttsManager = new TTSManager(activity);
        ttsManager.startSpeaking("发现新版本，必须更新后才能继续使用");

        String message = "新版本：v" + version.getVersionName() + "\n"
                + (version.getUpdateLog() != null && !version.getUpdateLog().isEmpty()
                        ? "更新内容：" + version.getUpdateLog() + "\n" : "")
                + "必须更新后才能继续使用。";

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("发现新版本")
                .setMessage(message)
                // 不可取消：无取消按钮 + 禁点外部 + 禁返回键
                .setCancelable(false)
                .setPositiveButton("立即更新", null)
                .create();

        dialog.setOnShowListener(d -> {
            // 覆盖默认点击即关弹窗的行为：下载入队前弹窗常驻
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (sDownloadId != -1 && isDownloading(activity)) {
                    Toast.makeText(activity, "更新包正在下载中，请稍候...", Toast.LENGTH_SHORT).show();
                    return;
                }
                ttsManager.stopSpeaking();
                startDownload(activity, version);
            });
        });
        dialog.show();
    }

    /** 经系统 DownloadManager 下载 APK，完成后自动拉起安装 */
    private static void startDownload(Activity activity, VersionVO version) {
        try {
            String baseUrl = RetrofitClient.getBaseUrl();
            String downloadUrl = baseUrl + (version.getDownloadUrl() != null ? version.getDownloadUrl() : "/api/download/app");
            Log.d(TAG, "开始下载更新包: " + downloadUrl);

            DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("视无界新版本 v" + version.getVersionName());
            request.setDescription("正在下载更新包");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_NAME);
            sDownloadId = dm.enqueue(request);

            Toast.makeText(activity, "开始下载更新包...", Toast.LENGTH_SHORT).show();
            registerDownloadCompleteReceiver(activity.getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "启动下载失败", e);
            Toast.makeText(activity, "下载启动失败，请检查网络后重试", Toast.LENGTH_SHORT).show();
        }
    }

    private static void registerDownloadCompleteReceiver(Context appContext) {
        if (sDownloadReceiver != null) {
            return;
        }
        sDownloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long doneId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (doneId != sDownloadId) {
                    return;
                }
                Log.d(TAG, "更新包下载完成，拉起安装");
                appContext.unregisterReceiver(this);
                sDownloadReceiver = null;
                installDownloadedApk(appContext);
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(sDownloadReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            appContext.registerReceiver(sDownloadReceiver, filter);
        }
    }

    /** 校验下载状态并拉起系统安装 */
    private static void installDownloadedApk(Context appContext) {
        try {
            File apkFile = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APK_NAME);
            if (!apkFile.exists()) {
                Log.e(TAG, "更新包文件不存在: " + apkFile.getAbsolutePath());
                sDownloadId = -1;
                return;
            }

            // 安装未知应用授权检查（Android 8+）：未授权则跳系统设置，授权后用户重按「立即更新」
            if (!appContext.getPackageManager().canRequestPackageInstalls()) {
                Log.w(TAG, "缺少安装未知应用授权，跳转设置");
                Toast.makeText(appContext, "请开启「允许安装未知应用」后重新点击立即更新", Toast.LENGTH_LONG).show();
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + appContext.getPackageName()));
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                appContext.startActivity(settingsIntent);
                return;
            }

            Uri apkUri = FileProvider.getUriForFile(appContext, FILE_PROVIDER_AUTHORITY, apkFile);
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(installIntent);
        } catch (Exception e) {
            Log.e(TAG, "拉起安装失败", e);
            sDownloadId = -1; // 允许重试
        }
    }

    private static boolean isDownloading(Context context) {
        if (sDownloadId == -1) {
            return false;
        }
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        try (Cursor cursor = dm.query(new DownloadManager.Query().setFilterById(sDownloadId))) {
            if (cursor != null && cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                return status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING;
            }
        } catch (Exception e) {
            Log.e(TAG, "查询下载状态失败", e);
        }
        return false;
    }

    private static long getLocalVersionCode(Activity activity) {
        try {
            PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            return info.getLongVersionCode();
        } catch (Exception e) {
            Log.e(TAG, "获取本地版本号失败", e);
            return 0;
        }
    }
}
