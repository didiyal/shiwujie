package com.swj.shiwujie.controller;


import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import com.swj.shiwujie.app.EasyProblemApp;
import com.swj.shiwujie.app.GatewayApp;
import com.swj.shiwujie.common.AiToolRequest;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.utils.LoginUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;

@RestController
@RequestMapping(path = "/api/ai/ai")
@Slf4j
@Tag(name = "AI接口")
public class AiController {



    @Resource
    private GatewayApp gatewayApp;

    @Resource
    private EasyProblemApp easyProblemApp;
    
    @Value("${upload.image-path:${user.home}/shiwujie/images}")
    private String imageUploadPath;

    @PostMapping(path = "/doChatByImage",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "图片识别")
    @SecurityRequirement(name = "Authorization")
    public Flux<String> doChatByImage(@RequestParam("imageFile") MultipartFile imageFile) {
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long blindId = loginBlind.getBlindId();
            // 判断参数是否合法
            ThrowUtils.throwIf(ObjUtil.isNull(imageFile), ErrorCode.PARAMS_ERROR,"参数不合法");
            
            // 生成更合理的文件名
            String originalFilename = imageFile.getOriginalFilename();
            String fileExtension = ".png"; // 默认扩展名
            if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String name = "imageFile" + System.currentTimeMillis() + fileExtension;
            
            // 使用配置的上传目录
            String filePath = imageUploadPath + File.separator + blindId + File.separator + name;
            
            // 确保目录存在
            File directory = new File(imageUploadPath + File.separator + blindId);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // 使用Hutool工具库保存到指定目录下
            FileUtil.writeBytes(imageFile.getBytes(), filePath);
            
            log.info("图片保存成功，路径: {}", filePath);

            // 传递给AI时使用文件系统路径
            Flux<String> stringFlux = easyProblemApp.doChatWithImageSSE(filePath, 1000L);

            // 打印
            log.info("AI返回:");
            stringFlux.subscribe(System.out::print);

            return stringFlux;

        } catch (Exception e) {
            log.error("处理图片识别请求时发生错误", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "处理图片识别请求时发生错误");
        }
    }



    @PostMapping(path = "/doChatByText/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "文本对话")
    @SecurityRequirement(name = "Authorization")
    public Flux<String> doChatByTextStream(String text){
        Blind loginBlind = LoginUtils.getLoginBlind();
        Long blindId = loginBlind.getBlindId();

        // 判断参数是否合法
        ThrowUtils.throwIf(ObjUtil.hasEmpty(text), ErrorCode.PARAMS_ERROR,"参数不合法");

        Flux<String> stringFlux = easyProblemApp.doChatWithTextSSE(text, 2000L);
        // 打印
        log.info("AI返回:");
        stringFlux.subscribe(System.out::print);

        return stringFlux;

    }



    @PostMapping(path = "/doChatByText")
    @Operation(summary = "文本对话")
    @SecurityRequirement(name = "Authorization")
    public String doChatByText(String text){
        Blind loginBlind = LoginUtils.getLoginBlind();
        Long blindId = loginBlind.getBlindId();

        // 判断参数是否合法
        ThrowUtils.throwIf(ObjUtil.hasEmpty(text), ErrorCode.PARAMS_ERROR,"参数不合法");

        return easyProblemApp.doChatWithText(text, 1000L);

    }









}
