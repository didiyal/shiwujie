package com.swj.shiwujie.model.VO.download;

import lombok.Data;

import java.io.Serializable;

/**
 * App 版本信息（强制更新检查）
 *
 * <p>由 {@code DownloadController#latestVersion()} 返回，App 启动时比对
 * {@code versionCode} 与本地包版本，服务端更大即弹不可取消的强制更新弹窗。</p>
 */
@Data
public class VersionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 最新包版本号（整数，单调递增，App 与本地 versionCode 比对） */
    private Integer versionCode;

    /** 最新包版本名（展示用，如 1.2） */
    private String versionName;

    /** APK 下载地址（相对路径，App 端拼 base url；走 /api/download/app） */
    private String downloadUrl;

    /** 更新说明（展示在更新弹窗里） */
    private String updateLog;
}
