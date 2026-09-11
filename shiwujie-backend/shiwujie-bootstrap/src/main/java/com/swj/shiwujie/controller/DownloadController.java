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

/**
 * 文件下载接口
 *
 * @author swj
 */
@RestController
@RequestMapping("/api/download")
@Tag(name = "文件下载接口")
public class DownloadController {

    @Value("${app.download.apk-path}")
    private String apkPath;

    @Value("${app.version.code:1}")
    private Integer versionCode;

    @Value("${app.version.name:1.0}")
    private String versionName;

    @Value("${app.version.update-log:}")
    private String updateLog;

    /**
     * 下载 Android APK 安装包
     */
    @GetMapping("/app")
    @Operation(summary = "下载 Android App 安装包")
    public ResponseEntity<Resource> downloadApp() {
        File file = new File(apkPath);
        if (!file.exists()) {
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
