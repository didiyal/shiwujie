package com.swj.shiwujie.test;

import com.swj.shiwujie.common.AiToolRequest;
import com.swj.shiwujie.constant.AiConstants;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.tools.app.FrontendTools;
import com.swj.shiwujie.utils.LoginUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NewAppTools {

    @Resource
    private FrontendTools frontendTools;


    /**
     * 根据索引值调用对应的业务工具
     *
     * @param toolRequest 工具请求JSON字符串，包含type(问题索引1-11)和data(传入数据，可为空)
     * @return 执行结果
     */
    public String questionChoose(String toolRequest) {
        // 添加对null或空字符串的处理，防止流式调用中出现空参数导致异常
        if (toolRequest == null || toolRequest.trim().isEmpty()) {
            log.warn("工具调用参数为空");
            return "此功能暂不支持自动操作，请您手动操作";
        }

        try {
            log.info("开始解析工具请求参数");
            // 添加参数验证，防止null或空字符串导致的错误
            if (toolRequest == null || toolRequest.trim().isEmpty()) {
                log.warn("工具请求参数为空或仅包含空白字符");
                return "此功能暂不支持自动操作,请您手动操作";
            }

            // 解析工具请求
            AiToolRequest request = AiToolRequest.fromJson(toolRequest);
            log.info("解析后的工具请求: type={}, data={}", request.getType(), request.getData());

            Integer indexNum = request.getType();
            String jsonStr = request.getData();

            // 检查type是否为空
            if (indexNum == null) {
                log.warn("工具类型(type)为空");
                return "错误：请提供有效的工具类型(type)";
            }

            // 如果data为空，设置默认值
            if (jsonStr == null) {
                log.info("工具数据(data)为空，设置默认值");
                jsonStr = "{}";
            }

            switch (indexNum) {
                case 1:// 图像识别功能
                    frontendTools.noticeTakePhoto();
                    return "执行成功";
                case 2:// 跳转到其它软件功能
                    String appName = parseJsonParam(jsonStr, "appName");
                    if (appName == null) {
                        return "缺少必要参数：软件名称";
                    }
                    frontendTools.noticeJumpSoftware(appName);
                    return "处理成功";
                case 3:// 导航
                    String destination = parseJsonParam(jsonStr, "destination");
                    if(destination == null){
                        return "缺少必要参数：目的地";
                    }
                    frontendTools.noticeNavigation(destination);
                    return "处理成功";
                case 4:// 切换语气
                    String tone = parseJsonParam(jsonStr, "tone");
                    if (tone == null) {
                        return "缺少必要参数：语气";
                    }
                    return this.switchModelTone(tone);
                default:
                    return "无效的工具类型，请输入1-4之间的数字";
            }
        } catch (Exception e) {
            log.error("执行业务工具调用时发生错误", e);
            return "执行失败: " + e.getMessage();
        }
    }

    /**
     * 从JSON字符串中解析指定参数
     *
     * @param jsonStr   JSON字符串
     * @param paramName 参数名
     * @return 参数值，如果不存在则返回null
     */
    private String parseJsonParam(String jsonStr, String paramName) {
        // 添加参数验证，防止null或空字符串导致的错误
        if (jsonStr == null || jsonStr.isEmpty() || jsonStr.trim().equals("{}")) {
            return null;
        }

        try {
            // 使用Hutool的JSON工具解析
            cn.hutool.json.JSONObject jsonObject = cn.hutool.json.JSONUtil.parseObj(jsonStr);
            return jsonObject.getStr(paramName);
        } catch (Exception e) {
            log.warn("解析JSON参数时发生错误: {}", e.getMessage());
            return null;
        }
    }


    /**
     * 切换大模型回答语气
     */
    public String switchModelTone(String tone) {
        Blind blind = LoginUtils.getLoginBlind();
        Long blindId = blind.getBlindId();
        AiConstants.clientTone.put(blindId, tone);
        return "已切换至" + tone + "语气";
    }


}
