package com.swj.shiwujie.controller;


import com.swj.shiwujie.service.ChatService;
import com.swj.shiwujie.utils.LoginUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;



@RestController
@Slf4j
@RequestMapping("/api/ai/ai")
public class ChatController {


    @Resource
    private ChatService chatService;



    /**
     * 文本聊天
     *
     * @param text 消息
     * @return 响应
     */
    @PostMapping(path = "/doChatByText", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "图片识别(流式)")
    public Flux<String> doChatByTextStream(@RequestParam(name = "text", defaultValue = "你是谁") String text) {
        long currented = System.currentTimeMillis();
        Flux<String> stringFlux = chatService.doChatWithTextSSE(text, LoginUtils.getLoginBlind().getBlindId());
        log.debug("文字消息AI处理完成，耗时:{}ms", System.currentTimeMillis() - currented);
        return stringFlux;
    }


    /**
     * 图片识别
     *
     * @param imageFile 图片文件
     * @return 图片识别结果
     */
    @PostMapping(path = "/doChatByImage", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "图片识别(流式)")
    public Flux<String> doChatByImageStream(@RequestParam("imageFile") MultipartFile imageFile) {
        long currented = System.currentTimeMillis();
        Flux<String> stringFlux = chatService.imageHandle(imageFile);
        log.debug("图片识别完成，耗时:{}ms", System.currentTimeMillis() - currented);
        return stringFlux;
    }




    /**
     * 新app测试
     */
    @PostMapping(path = "/NewApp", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "新app测试")
    @SecurityRequirement(name = "Authorization")
    public Flux<String> NewAppWithText(String text) {
        long currented = System.currentTimeMillis();
        Flux<String> stringFlux = chatService.doChatWithTextSSE(text, LoginUtils.getLoginBlind().getBlindId());
        log.debug("文字消息AI处理完成，耗时:{}ms", System.currentTimeMillis() - currented);
        return stringFlux;
    }


}