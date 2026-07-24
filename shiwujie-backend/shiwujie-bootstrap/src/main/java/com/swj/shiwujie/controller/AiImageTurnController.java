package com.swj.shiwujie.controller;

import cn.hutool.core.util.StrUtil;
import com.swj.shiwujie.ai.relay.AiWsRelayService;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.socket.CoordinationSocketHandler;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

/**
 * 图片 AI-turn HTTP 入口（chunk-2e-3，task 3.8 图片走 HTTP multipart；文本 turn 仍走 WS）。
 *
 * <p><b>设计取舍（响应骑 WS）</b>：图片 base64 达 MB 级、超 WS 帧上限，故<b>上行</b>走 HTTP multipart；
 * 但 VLM 流式答复（ndjson）<b>复用文本 turn 的 WS 中继通路</b>推回——本端点收图后查到该盲人 WS session，
 * 调 {@link AiWsRelayService#submitImageRelay} 把 Python {@code /ai/turn {image}} 的流式响应经同一 session
 * 转 110/111/112/113 帧，App 端 {@code AiTurnManager} 现成路由（onDelta→文本+TTS / onProgress→"正在识别照片"）
 * 零改动复用。免新建 StreamingResponseBody 流式 HTTP 端点 + Android ndjson 解析重复。</p>
 *
 * <p><b>路径 scope</b>：{@code /api/call/ai/**} 落 {@code LoginCheckInterceptor} 的 {@code /api/call/**}
 * 拦截器 scope（同 WsTicketController），JWT 鉴权复用、免改 WebConfig。</p>
 *
 * <p><b>WS 耦合</b>：盲人 App 持久连 WS（AI tab 在 BlindHomeActivity），拍照时 session 必在；无 session
 * 则图片 turn 无回执通道，拒（App 据响应 onError 解锁麦克风）。WS 断线场景文本 turn 同样不可用，行为一致。</p>
 */
@RestController
@RequestMapping("/api/call/ai")
@Slf4j
@Tag(name = "AI 图片 turn 接口")
public class AiImageTurnController {

    /** 拍照无口述追问时的默认提示（诱导 agent 调 recognize_photo 读图）。 */
    private static final String DEFAULT_IMAGE_PROMPT = "请描述这张图片的主要内容";

    @Resource
    private AiWsRelayService aiWsRelayService;

    /**
     * 图片 AI-turn：收 multipart 图片 + 可选 text → base64 data URL → 查 WS session → 提交中继（响应骑 WS）。
     *
     * @param image 图片文件（multipart part 名 "image"）
     * @param text  可选口述追问；空则用 {@link #DEFAULT_IMAGE_PROMPT}
     */
    @PostMapping("/image-turn")
    @Operation(summary = "图片 AI turn：multipart 上传 → 中继 Python，流式响应骑 WS 推回")
    public BaseResponse<Void> imageTurn(
            HttpServletRequest request,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "text", required = false) String text) {
        Long blindId = LoginUtils.getLoginBlindId(request);
        ThrowUtils.throwIf(blindId == null, ErrorCode.NOT_LOGIN);
        String phone = LoginUtils.getLoginUserPhone(request);
        ThrowUtils.throwIf(StrUtil.isBlank(phone), ErrorCode.PARAMS_ERROR, "用户无有效手机号");
        ThrowUtils.throwIf(image == null || image.isEmpty(), ErrorCode.PARAMS_ERROR, "图片为空");

        // 查 WS session（响应骑 WS 推回 110/111/112/113）；无 session → 图片 turn 无回执通道，拒。
        Session session = CoordinationSocketHandler.sessionMap.get(phone);
        ThrowUtils.throwIf(session == null || !session.isOpen(),
                ErrorCode.OPERATION_ERROR, "WS 未连接，无法处理图片 turn");

        String dataUrl;
        try {
            dataUrl = toDataUrl(image);
        } catch (IOException e) {
            log.error("图片转 base64 失败 blindId={}：{}", blindId, e.toString());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片读取失败");
        }

        String prompt = StrUtil.isBlank(text) ? DEFAULT_IMAGE_PROMPT : text;
        log.info("图片 AI-turn 提交中继 blindId={} promptLen={} imageBytes={}",
                blindId, prompt.length(), image.getSize());
        aiWsRelayService.submitImageRelay(session, blindId, prompt, dataUrl);
        return ResultUtils.success(null);
    }

    /** MultipartFile → {@code data:<mime>;base64,<b64>}（Python vlm contextvar / qwen3-vl-flash 直消费）。 */
    private String toDataUrl(MultipartFile image) throws IOException {
        String mime = StrUtil.isNotBlank(image.getContentType()) ? image.getContentType() : "image/jpeg";
        byte[] bytes = image.getBytes();
        String b64 = Base64.getEncoder().encodeToString(bytes);
        return "data:" + mime + ";base64," + b64;
    }
}
