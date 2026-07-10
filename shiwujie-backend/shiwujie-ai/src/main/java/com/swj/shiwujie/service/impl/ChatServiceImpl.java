package com.swj.shiwujie.service.impl;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.swj.shiwujie.app.ImageApp;
import com.swj.shiwujie.app.TextApp;
import com.swj.shiwujie.app.ToolChoiceApp;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.common.ToolCallRequest;
import com.swj.shiwujie.constants.AiConstants;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.service.ChatService;
import com.swj.shiwujie.tools.ToolChoiceCenter;
import com.swj.shiwujie.tools.mytools.WebSearchTool;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.exception.ThrowUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Resource
    private ImageApp imageApp;
    @Resource
    private TextApp textApp;

    @Resource
    private ToolChoiceApp toolChoiceApp;

    @Resource
    private ToolChoiceCenter toolChoiceCenter;

    @Resource
    private WebSearchTool webSearchTool;

    @Value("${upload.image-path:${user.home}/shiwujie/images}")
    private String imageUploadPath;


    /**
     * 与大模型文字交流（流式）
     *
     * @param text    输入的文本
     * @param blindId 用户盲ID
     * @return 大模型流式回复
     */
    @Override
    public Flux<String> doChatWithTextSSE(String text, Long blindId) {

        // 第一阶段：使用非流式调用获取AI响应
        String toolHandleResult = toolHandle(blindId, text);

        // 第二阶段：将工具结果反馈给AI并流式输出最终结果
        String finalPrompt = String.format(
                "用户的问题是：%s，工具执行结果是：%s，若执行了工具,请参考工具执行结果，回答用户的问题。",
                text, toolHandleResult);
        return textApp.doChat(finalPrompt, blindId);


    }


    @Override
    public Flux<String> imageHandle(MultipartFile imageFile) {
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long blindId = loginBlind.getBlindId();
            // 判断参数是否合法
            ThrowUtils.throwIf(ObjUtil.isNull(imageFile), ErrorCode.PARAMS_ERROR, "参数不合法");

            String filePath = this.saveImageAndgetPath(imageFile, blindId);

            // 传递给AI时使用文件系统路径
            return imageApp.doImage(filePath, blindId);

        } catch (Exception e) {
            log.error("处理图片识别请求时发生错误", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "处理图片识别请求时发生错误");
        }
    }


    @Nullable
    private String toolHandle(Long blindId, String problem) {
        String toolChoiceResult = toolChoiceApp.doChat(problem, blindId);
        log.info("工具分析AI返回: {}", toolChoiceResult);
        // 检查AI是否要求调用工具，直接根据type判断
        ToolCallRequest toolCallRequest = this.parseToolCallRequest(toolChoiceResult);
        String content = null;
        if (toolCallRequest != null) {
            switch (toolCallRequest.getToolType()) {
                case -1:// 执行业务工具调用
                    log.info("执行业务工具调用{}", toolCallRequest);
                    content = toolChoiceCenter.questionChoose(toolCallRequest.getData());
                    break;
                case -2:// 网页搜索
                    log.info("开始执行web搜索,{}", toolCallRequest);
                    content = webSearchTool.searchWeb(toolCallRequest.getData());
                    log.info("web搜索结果: {}", content);
                    break;
                case -3:// 不执行工具调用
                    log.info("不执行工具调用,{}", toolCallRequest);
                    content = "程序认为无需执行工具调用";
                    break;
                case -4:// 需要获取用户输入信息
                    log.info("需要获取用户输入信息,{}", toolCallRequest);
                    content = toolCallRequest.getData();
                    break;
            }
        }
        return content;
    }


    /**
     * (工具)保存图片并返回文件路径
     *
     * @param imageFile 图片文件
     * @param blindId   盲人id
     * @return 文件路径
     * @throws IOException 抛出IO异常
     */
    @NotNull
    private String saveImageAndgetPath(MultipartFile imageFile, Long blindId) throws IOException {
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
        return filePath;
    }


    /**
     * 解析工具调用请求
     */
    public ToolCallRequest parseToolCallRequest(String aiResponse) {
        try {
            // 检查响应是否为空
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                return null;
            }
            // 清理响应字符串，尝试提取JSON部分
            String cleanedResponse = aiResponse.trim();
            // 查找JSON的开始和结束位置
            int jsonStart = cleanedResponse.indexOf("{");
            int jsonEnd = cleanedResponse.lastIndexOf("}");

            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                // 提取JSON部分
                cleanedResponse = cleanedResponse.substring(jsonStart, jsonEnd + 1);

                // 使用Hutool解析JSON
                JSONObject jsonObject = JSONUtil.parseObj(cleanedResponse);
                Integer type = jsonObject.getInt("toolType");
                String data = jsonObject.getStr("data");

                if ((ObjUtil.isNotNull(type)) && data != null) {
                    return new ToolCallRequest(type, data);
                }
            }
        } catch (Exception e) {
            log.warn("解析工具调用请求失败，响应内容: {}，错误: {}", aiResponse, e.getMessage());
        }
        return null;
    }



}
