package com.swj.shiwujie.controller;

import com.swj.shiwujie.model.VO.download.VersionVO;
import com.swj.shiwujie.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 文件下载接口
 *
 * <p>APK 上传约定（2026-09-12 起）：把 APK 放入 {@code app.download.apk-dir} 目录即可，
 * <b>文件名任意</b>——下载接口自动提供目录内「最新修改」的那一个，不再要求固定名 app-download.apk。
 * 旧的 apk-path 固定文件仍兼容（目录内无 APK 时回退用它）。</p>
 */
@RestController
@RequestMapping("/api/download")
@Tag(name = "文件下载接口")
public class DownloadController {

    /** 历史固定 APK 路径（兼容保留：目录扫描无结果时回退） */
    @Value("${app.download.apk-path}")
    private String apkPath;

    /** APK 存放目录（推荐）：上传任意名的 .apk 到此目录，自动提供最新一个 */
    @Value("${app.download.apk-dir:/www/wwwroot/shiwujie/shiwujie-frontend/}")
    private String apkDir;

    @Value("${app.version.code:1}")
    private Integer versionCode;

    @Value("${app.version.name:1.0}")
    private String versionName;

    @Value("${app.version.update-log:}")
    private String updateLog;

    /**
     * 下载 Android APK 安装包（自动提供 APK 目录内最新的一个，文件名任意）
     */
    @GetMapping("/app")
    @Operation(summary = "下载 Android App 安装包")
    public ResponseEntity<Resource> downloadApp() {
        File file = resolveLatestApk();
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"shiwujie.apk\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .contentLength(file.length())
                .body(resource);
    }

    /**
     * 定位要提供的 APK：目录内按「修改时间最新」选 .apk；目录不可用/为空时回退历史固定路径。
     */
    private File resolveLatestApk() {
        File dir = new File(apkDir);
        if (dir.isDirectory()) {
            File[] apks = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".apk"));
            if (apks != null && apks.length > 0) {
                return Arrays.stream(apks)
                        .max(Comparator.comparingLong(File::lastModified))
                        .orElse(null);
            }
        }
        return new File(apkPath);
    }

    /**
     * 查询线上最新 App 版本（不鉴权：更新检查须在登录前后均可发起）。
     * App 启动比对 versionCode，服务端更大即弹强制更新弹窗并引导下载 /api/download/app。
     */
    @GetMapping("/version")
    @Operation(summary = "查询最新 App 版本（强制更新检查）")
    public com.swj.shiwujie.common.BaseResponse<VersionVO> latestVersion() {
        VersionVO vo = new VersionVO();
        vo.setVersionCode(versionCode);
        vo.setVersionName(versionName);
        vo.setDownloadUrl("/api/download/app");
        vo.setUpdateLog(updateLog);
        return ResultUtils.success(vo);
    }
}
