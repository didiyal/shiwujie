package com.swj.shiwujie.tools.app;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.swj.shiwujie.app.ComplexProblemApp;
import com.swj.shiwujie.app.EasyProblemApp;
import com.swj.shiwujie.common.AiToolRequest;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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


    /**
     * 根据索引值调用对应的业务工具
     * @param toolRequest 工具请求JSON字符串，包含type(问题索引1-11)和data(传入数据，可为空)
     * @return 执行结果
     */
    @Tool(name = "核心业务工具调用", description =
            "该工具处理所有与用户、家庭和社区相关的操作。当用户需要执行具体业务操作时，必须调用此工具。" +
                    "可用功能列表如下：" +
                    " 1 - 申请加入家庭(加入家庭)（需要提供家庭创建人手机号）格式示例：{ \"type\":1, \"data\":\"{\\\"familyVolunteerPhone\\\":\\\"13800138000\\\"}\" }" +
                    " 2 - 查看家庭信息(家庭里有几个人/家庭成员信息) 格式示例：{ \"type\":2 }" +
                    " 3 - 退出家庭(离开家庭) 格式示例：{ \"type\":3 }" +
                    " 4 - 获取用户的社区信息(我的社区信息) 格式示例：{ \"type\":4 }" +
                    " 5 - 获取社区活动信息(查看活动) 格式示例：{ \"type\":5 }" +
                    " 6 - 添加活动报名(报名活动)（需要提供活动ID - activityId）格式示例：{ \"type\":6, \"data\":\"{\\\"activityId\\\":123}\" }" +
                    " 7 - 获取用户报名的活动信息(查看我报名的活动/我报名了哪些活动) 格式示例：{ \"type\":7 }" +
                    " 8 - 获取自己发布的求助帖(我的求助帖) 格式示例：{ \"type\":8 }" +
                    " 9 - 删除求助帖（需要提供求助帖ID - helppostId）格式示例：{ \"type\":9, \"data\":\"{\\\"helppostId\\\":456}\" }" +
                    " 10 - 修改求助帖（需要提供求助帖ID - helppostId、新内容 - helpContent 和新地点 - helpLocation）格式示例：{ \"type\":10, \"data\":\"{\\\"helppostId\\\":456,\\\"helpContent\\\":\\\"修改后的内容\\\",\\\"helpLocation\\\":\\\"修改后的地点\\\"}\" }" +
                    " 11 - 添加求助帖(发布求助帖)（需要提供内容 - helpContent 和地点 - helpLocation）格式示例：{ \"type\":11, \"data\":\"{\\\"helpContent\\\":\\\"新的求助内容\\\",\\\"helpLocation\\\":\\\"新的求助地点\\\"}\" }" +
                    " 12 - 加入社区/修改个人信息(名字,手机号,密码等)/退出社区/跳转到其它软件/图像识别 格式示例：{ \"type\":12 }" +
                    "使用说明：调用时需要严格遵守JSON格式，传入工具类型（type：1-12）及数据（data）。" +
                    "注意：此工具是执行业务操作的唯一途径，AI只能在需要时调用此工具，并且仅在工具类型（type）有效时执行。" +
                    "例如，若用户请求'加入家庭'，则需要传入包含家主手机号的JSON格式请求。" +
                    "重要：在流式调用模式下，AI不应直接调用此工具，而应按照系统提示词中的格式返回工具调用请求。")
    public String questionChoose(@ToolParam(description = "工具请求JSON字符串，必须严格遵守格式要求: {\"type\":数字, \"data\":\"JSON字符串\"}") String toolRequest) {
        log.info("WorkChooseTool 被调用，接收到的参数: {}", toolRequest);
        
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
                case 1:
                    // 加入家庭 - 需要家庭创建人手机号
                    // 从jsonStr中解析出手机号
                    String phone = parseJsonParam(jsonStr, "familyVolunteerPhone");
                    if (phone == null) {
                        return "缺少必要参数：家庭创建人手机号";
                    }
                    // 检查userTools是否已正确初始化
                    if (userTools == null) {
                        return "错误：用户工具未正确初始化";
                    }
                    return userTools.joinFamily(phone);

                case 2:
                    // 查看家庭信息
                    if (userTools == null) {
                        return "错误：用户工具未正确初始化";
                    }
                    return userTools.getFamilyInfo();

                case 3:
                    // 退出家庭
                    if (userTools == null) {
                        return "错误：用户工具未正确初始化";
                    }
                    return userTools.leaveFromFamily();

                case 4:
                    // 获取用户的社区信息
                    if (communityTools == null) {
                        return "错误：社区工具未正确初始化";
                    }
                    return communityTools.getCommunityInfo();

                case 5:
                    // 获取社区活动信息
                    if (communityTools == null) {
                        return "错误：社区工具未正确初始化";
                    }
                    return communityTools.getActivityInfo();

                case 6:
                    // 添加活动报名 - 需要活动ID
                    if (communityTools == null) {
                        return "错误：社区工具未正确初始化";
                    }
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

                case 7:
                    // 获取用户报名的活动信息
                    if (communityTools == null) {
                        return "错误：社区工具未正确初始化";
                    }
                    return communityTools.getBlindAcctivitySignInfo();

                case 8:
                    // 获取自己发布的求助帖
                    if (communityTools == null) {
                        return "错误：社区工具未正确初始化";
                    }
                    return communityTools.getHelpPostInfo();

                case 9:
                    // 删除求助帖 - 需要求助帖ID
                    if (communityTools == null) {
                        return "错误：社区工具未正确初始化";
                    }
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

                case 10:
                    // 修改求助帖 - 需要求助帖ID、内容和地点
                    if (communityTools == null) {
                        return "错误：社区工具未正确初始化";
                    }
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

                case 11:
                    // 添加求助帖 - 需要内容和地点
                    if (communityTools == null) {
                        return "错误：社区工具未正确初始化";
                    }
                    String content = parseJsonParam(jsonStr, "helpContent");
                    String location = parseJsonParam(jsonStr, "helpLocation");

                    if (content == null) {
                        return "缺少必要参数：求助帖内容";
                    }

                    return communityTools.addHelpPost(content, location);

                case 12:
                    // 加入社区功能暂不支持
                    return "此功能暂不支持自动操作，请您手动操作";

                default:
                    return "无效的工具类型，请输入1-12之间的数字";
            }
        } catch (Exception e) {
            log.error("执行业务工具调用时发生错误", e);
            return "执行失败: " + e.getMessage();
        }
    }

    /**
     * 从JSON字符串中解析指定参数
     * @param jsonStr JSON字符串
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
