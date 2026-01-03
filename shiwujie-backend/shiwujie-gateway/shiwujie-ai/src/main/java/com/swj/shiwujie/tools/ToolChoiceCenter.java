package com.swj.shiwujie.tools;


import com.swj.shiwujie.common.AiToolRequest;
import com.swj.shiwujie.tools.app.FrontendTools;
import com.swj.shiwujie.tools.app.UserTools;
import com.swj.shiwujie.tools.mytools.AiModelTools;
import com.swj.shiwujie.tools.mytools.WebSearchTool;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Data
public class ToolChoiceCenter {

    @Resource
    private FrontendTools frontendTools;

    @Resource
    private UserTools userTools;

    @Resource
    private WebSearchTool webSearchTool;

    @Resource
    private AiModelTools aiModelTools;

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
        log.debug("开始解析工具请求参数");
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
            log.debug("工具数据(data)为空，设置默认值");
            jsonStr = "{}";
        }
        switch (indexNum) {
            case 1:// 图像识别功能
                frontendTools.noticeTakePhoto();
                return "执行成功";
            case 2:// 图片信息追问
                String message = parseJsonParam(jsonStr, "message");
                if (message == null) {
                    return "缺少必要参数：图片追问内容";
                }
                return aiModelTools.TakePhoto(message);
            case 3:// 跳转到其它软件功能
                String appName = parseJsonParam(jsonStr, "appName");
                if (appName == null) {
                    return "缺少必要参数：软件名称";
                }
                return frontendTools.noticeJumpSoftware(appName);
            case 4:// 导航
                String destination = parseJsonParam(jsonStr, "destination");
                if (destination == null) {
                    return "缺少必要参数：目的地";
                }
                return frontendTools.noticeNavigation(destination);
            case 5:// 志愿者视频求助功能
                frontendTools.noticeVideoHelp();
                return "执行成功,已帮您开启视频求助";
            case 6:// 家属视频求助功能
                frontendTools.noticeUrgentHelp();
                return "执行成功,已帮您开启视频求助";
            case 7:// 加入家庭 - 需要家庭创建人手机号
                String phone = parseJsonParam(jsonStr, "familyVolunteerPhone");
                if (phone == null) {
                    return "缺少必要参数：家庭创建人手机号";
                }
                return userTools.joinFamily(phone);
            case 8:// 查看家庭信息
                return userTools.getFamilyInfo();
            case 9:// 退出家庭
                return userTools.leaveFromFamily();

            default:
                return "无效的工具类型，请输入1-18之间的数字";
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


}
