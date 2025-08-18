package com.swj.shiwujie.tools.app;

import com.swj.shiwujie.common.AiToolRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;


/**
 * 业务处理工具
 */
@Slf4j
@Component
public class WorkChooseTool {


    // 用户模块业务工具
    @Resource
    private UserTools userTools;

    // 社区模块业务工具
    @Resource
    private CommunityTools communityTools;

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
                case 1:// 加入家庭 - 需要家庭创建人手机号
                    // 从jsonStr中解析出手机号
                    String phone = parseJsonParam(jsonStr, "familyVolunteerPhone");
                    if (phone == null) {
                        return "缺少必要参数：家庭创建人手机号";
                    }
                    return userTools.joinFamily(phone);

                case 2:// 查看家庭信息
                    return userTools.getFamilyInfo();

                case 3:// 退出家庭
                    return userTools.leaveFromFamily();

                case 4:// 获取用户的社区信息

                    return communityTools.getCommunityInfo();

                case 5:// 获取社区活动信息

                    return communityTools.getActivityInfo();

                case 6:// 添加活动报名 - 需要活动ID

                    String activityIdStr = parseJsonParam(jsonStr, "activityId");
                    if (activityIdStr == null) {
                        return "缺少必要参数：活动ID";
                    }
                    try {
                        Long activityId = Long.parseLong(activityIdStr);
                        return communityTools.addActivitySign(activityId);
                    } catch (NumberFormatException e) {
                        return "活动ID格式错误";
                    }

                case 7:// 获取用户报名的活动信息
                    return communityTools.getBlindAcctivitySignInfo();

                case 8:// 获取自己发布的求助帖
                    return communityTools.getHelpPostInfo();

                case 9:// 删除求助帖 - 需要求助帖ID
                    String helpPostIdStr = parseJsonParam(jsonStr, "helppostId");
                    if (helpPostIdStr == null) {
                        return "缺少必要参数：求助帖ID";
                    }
                    try {
                        Long helpPostId = Long.parseLong(helpPostIdStr);
                        return communityTools.deleteHelpPost(helpPostId);
                    } catch (NumberFormatException e) {
                        return "求助帖ID格式错误";
                    }

                case 10:// 修改求助帖 - 需要求助帖ID、内容和地点

                    String helppostIdStr = parseJsonParam(jsonStr, "helppostId");
                    String helpContent = parseJsonParam(jsonStr, "helpContent");
                    String helpLocation = parseJsonParam(jsonStr, "helpLocation");

                    if (helppostIdStr == null || helpContent == null) {
                        return "缺少必要参数：求助帖ID或内容";
                    }

                    try {
                        Long helppostId = Long.parseLong(helppostIdStr);
                        return communityTools.updateHelpPost(helppostId, helpContent, helpLocation);
                    } catch (NumberFormatException e) {
                        return "求助帖ID格式错误";
                    }

                case 11:// 添加求助帖 - 需要内容和地点
                    String content = parseJsonParam(jsonStr, "helpContent");
                    String location = parseJsonParam(jsonStr, "helpLocation");

                    if (content == null) {
                        return "缺少必要参数：求助帖内容";
                    }

                    return communityTools.addHelpPost(content, location);

                case 12:// 加入社区/退出社区功能暂不支持

                    return "此功能暂不支持自动操作，请您手动操作";

                case 13:// 修改个人信息功能

                    frontendTools.noticeJumpToUserUpdate();
                    return "此功能暂不支持自动操作，请您手动操作,我已帮您跳转到修改页面,请你手动操作";

                case 14:// 图像识别功能

//                    frontendTools.noticeTakePhoto();
//                    return "执行成功";
                    return "此功能暂不支持自动操作，请您手动操作";

                case 15:// 志愿者视频求助功能
                    frontendTools.noticeVideoHelp();
                    return "执行成功,已帮您开启视频求助";

                case 16:// 家属视频求助功能
                    frontendTools.noticeUrgentHelp();
                    return "执行成功,已帮您开启视频求助";

                case 17:// 跳转到其它软件功能
                    String appName = parseJsonParam(jsonStr, "appName");
                    if (appName == null) {
                        return "缺少必要参数：软件名称";
                    }
                    frontendTools.noticeJumpSoftware(appName);
                    return "处理成功";
                case 18:// 导航
                    String destination = parseJsonParam(jsonStr, "destination");
                    if(destination == null){
                        return "缺少必要参数：目的地";
                    }
                    frontendTools.noticeNavigation(destination);
                    return "处理成功";

                default:
                    return "无效的工具类型，请输入1-18之间的数字";
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
}
