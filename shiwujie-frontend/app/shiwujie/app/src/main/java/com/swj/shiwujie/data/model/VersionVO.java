package com.swj.shiwujie.data.model;

/**
 * App 最新版本信息（强制更新检查，对应后端 /api/download/version）
 */
public class VersionVO {

    /** 最新包版本号（整数，单调递增，与本地 versionCode 比对） */
    private Integer versionCode;

    /** 最新包版本名（展示用） */
    private String versionName;

    /** APK 下载地址（相对路径） */
    private String downloadUrl;

    /** 更新说明 */
    private String updateLog;

    public Integer getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(Integer versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getUpdateLog() {
        return updateLog;
    }

    public void setUpdateLog(String updateLog) {
        this.updateLog = updateLog;
    }
}
