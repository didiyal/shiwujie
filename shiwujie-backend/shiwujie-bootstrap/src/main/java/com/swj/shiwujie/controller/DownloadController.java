package com.swj.shiwujie.controller;

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
}
