package com.swj.shiwujie.tools.app;


import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.model.VO.community.activity.ActivityVO;
import com.swj.shiwujie.model.VO.community.activitysign.ActivitysignVO;
import com.swj.shiwujie.model.VO.community.helppost.HelppostVO;
import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.enums.community.ActivityStatusEnum;
import com.swj.shiwujie.model.enums.community.CommunityTypeEnum;
import com.swj.shiwujie.model.request.community.activity.ActivityQueryRequest;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignAddRequest;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignQueryRequest;
import com.swj.shiwujie.model.request.community.helppost.HelppostAddRequest;
import com.swj.shiwujie.model.request.community.helppost.HelppostQueryRequest;
import com.swj.shiwujie.model.request.community.helppost.HelppostUpdateRequest;
import com.swj.shiwujie.service.community.InnerActivityService;
import com.swj.shiwujie.service.community.InnerActivitysignService;
import com.swj.shiwujie.service.community.InnerCommunityService;
import com.swj.shiwujie.service.community.InnerHelppostService;
import com.swj.shiwujie.utils.LoginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 社区模块业务工具调用
 */
@Slf4j
@Component
public class CommunityTools {


    @DubboReference
    private InnerCommunityService innerCommunityService;

    @DubboReference
    private InnerActivityService innerActivityService;

    @DubboReference
    private InnerActivitysignService innerActivitysignService;

    @DubboReference
    private InnerHelppostService innerHelppostService;


    /**
     * 获取用户的社区信息
     *
     * @return 社区信息
     */
    public String getCommunityInfo() {
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long communityId = loginBlind.getCommunityId();
            if (ObjUtil.isNull(communityId)) {
                return "您未加入任何社区！";
            }
            Community community = innerCommunityService.getById(communityId);
            StringBuilder sb = new StringBuilder();
            sb.append("获取社区信息成功！")
                    .append("您加入的社区信息为")
                    .append("社区名称：").append(community.getCommunityName()).append("\n")
                    .append("社区描述：").append(community.getCommunityDescription()).append("\n")
                    .append("社区地址：").append(community.getAddress()).append("\n")
                    .append("社区类型：").append(CommunityTypeEnum.getById(community.getCommunityTypeId())).append("\n");

            return sb.toString();
        } catch (Exception e) {
            log.error("获取社区信息失败", e);
            return "获取社区信息失败" + e.getMessage();
        }
    }


    // region 社区活动相关

    /**
     * 获取社区活动信息(现在默认查询20个,市级社区内)
     *
     * @return 活动信息
     */
    public String getActivityInfo() {
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long communityId = loginBlind.getCommunityId();
            if (ObjUtil.isNull(communityId)) {
                return "您未加入任何社区！";
            }
            // 拿到市级社区
            Community community = innerCommunityService.getById(communityId);
            Long parentCommunityId = community.getParentCommunityId();

            // 查询活动
            ActivityQueryRequest activityQueryRequest = new ActivityQueryRequest();
            activityQueryRequest.setCommunityId(parentCommunityId);
            activityQueryRequest.setPageSize(20);
            activityQueryRequest.setCurrent(1);
            activityQueryRequest.setActivityStatus(ActivityStatusEnum.WAITING.getName());
            Page<ActivityVO> activityVOPage = innerActivityService.listActivitiesByCommunity(activityQueryRequest);

            StringBuilder sb = new StringBuilder();
            sb.append("获取社区活动信息成功！")
                    .append("您加入的社区活动信息为")
                    .append("活动名称：").append(activityVOPage.getRecords().get(0).getActivityName()).append("\n")
                    .append("活动描述：").append(activityVOPage.getRecords().get(0).getActivityContent()).append("\n")
                    .append("活动时间：").append(activityVOPage.getRecords().get(0).getStartTime()).append("\n")
                    .append("活动地点：").append(activityVOPage.getRecords().get(0).getActivityLocation()).append("\n");
            return sb.toString();
        } catch (Exception e) {
            log.error("获取社区活动信息失败", e);
            return "获取社区活动信息失败" + e.getMessage();
        }
    }

    /**
     * 添加活动报名
     * @param activityId 活动ID
     * @return 添加结果
     */
    public String addActivitySign( Long activityId) {
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long communityId = loginBlind.getCommunityId();
            if (ObjUtil.isNull(communityId)) {
                return "您未加入任何社区,无法报名活动";
            }

            ActivitySignAddRequest activitySignAddRequest = new ActivitySignAddRequest();
            activitySignAddRequest.setActivityId(activityId);
            activitySignAddRequest.setBlindId(loginBlind.getBlindId());

            // 添加活动报名
            boolean addActivitySign = innerActivitysignService.addActivitySign(activitySignAddRequest);
            if (addActivitySign) return "报名成功";
            else return "报名失败";
        } catch (Exception e) {
            log.error("添加活动报名失败", e);
            return "添加活动报名失败" + e.getMessage();
        }
    }


    /**
     * 获取用户报名的活动信息
     *
     * @return 活动信息
     */
    public String getBlindAcctivitySignInfo() {
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long communityId = loginBlind.getCommunityId();
            if (ObjUtil.isNull(communityId)) {
                return "您未加入任何社区,无法查看活动报名信息";
            }
            ActivitySignQueryRequest activitySignQueryRequest = new ActivitySignQueryRequest();
            activitySignQueryRequest.setBlindId(loginBlind.getBlindId());
            activitySignQueryRequest.setPageSize(20);
            // 查询本人所有报名的活动签到信息
            Page<ActivitysignVO> activitysignVOPage = innerActivitysignService.listActivitySignByActivity(activitySignQueryRequest);
            // 拿到相应的活动信息
            List<Long> activityIds = activitysignVOPage.getRecords().stream().map(ActivitysignVO::getActivityId).toList();
            List<ActivityVO> activityVOS = innerActivityService.listActivities(activityIds);
            // 移除已经结束和开始的活动
            for (ActivityVO activityVO : activityVOS) {
                String activityStatus = activityVO.getActivityStatus();
                if (!activityStatus.equals(ActivityStatusEnum.WAITING.getName())) {
                    activityVOS.remove(activityVO);
                }
            }

            StringBuilder sb = new StringBuilder();
            for (ActivityVO activityVO : activityVOS) {
                sb.append("活动名称：").append(activityVO.getActivityName()).append("\n")
                        .append("活动时间：").append(activityVO.getStartTime()).append("\n")
                        .append("活动地点：").append(activityVO.getActivityLocation()).append("\n")
                        .append("活动状态：").append(activityVO.getActivityStatus()).append("\n");
            }
            return "获取用户活动报名信息成功！" + sb.toString();
        } catch (Exception e) {
            log.error("获取用户活动报名信息失败", e);
            return "获取用户活动报名信息失败" + e.getMessage();
        }
    }


    // endregion

    // region 求助帖

    /**
     * 获取自己发布的求助帖
     *
     * @return 求助帖信息
     */
    public String getHelpPostInfo() {
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long blindId = loginBlind.getBlindId();
            Long communityId = loginBlind.getCommunityId();
            if (ObjUtil.isNull(communityId)) {
                return "您未加入任何社区！";
            }

            HelppostQueryRequest helppostQueryRequest = new HelppostQueryRequest();
            helppostQueryRequest.setBlindId(blindId);
            Page<HelppostVO> helppostVOPage = innerHelppostService.listQueryHelpposts(helppostQueryRequest);
            return "获取自己发布的求助帖成功！" + helppostVOPage.getRecords().stream().map(HelppostVO::getHelpContent).collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("获取自己发布的求助帖失败", e);
            return "获取自己发布的求助帖失败" + e.getMessage();
        }

    }

    /**
     * 删除求助帖
     *
     * @param helppostId 求助帖id
     * @return 删除结果
     */
    public String deleteHelpPost(Long helppostId) {
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long blindId = loginBlind.getBlindId();
            Long communityId = loginBlind.getCommunityId();
            if (ObjUtil.isNull(communityId)) {
                return "您未加入任何社区！";
            }
            return innerHelppostService.deleteHelppost(helppostId, blindId, null) ? "删除求助帖成功" : "删除求助帖失败";
        } catch (Exception e) {
            log.error("删除求助帖失败", e);
            return "删除求助帖失败" + e.getMessage();
        }
    }


    /**
     * 修改求助帖
     *
     * @param helppostId  求助帖id
     * @param helpContent 求助帖内容
     * @return 修改结果
     */

    public String updateHelpPost( Long helppostId, String helpContent, String helpLocation) {
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long blindId = loginBlind.getBlindId();
            Long communityId = loginBlind.getCommunityId();
            if (ObjUtil.isNull(communityId)) {
                return "您未加入任何社区！";
            }
            HelppostUpdateRequest helppostUpdateRequest = new HelppostUpdateRequest();
            helppostUpdateRequest.setHelppostId(helppostId);
            helppostUpdateRequest.setHelpContent(helpContent);
            helppostUpdateRequest.setHelpLocation(helpLocation);
            return innerHelppostService.updateHelppost(helppostUpdateRequest, blindId, null) ? "修改求助帖成功" : "修改求助帖失败";
        } catch (Exception e) {
            log.error("修改求助帖失败", e);
            return "修改求助帖失败" + e.getMessage();
        }
    }


    /**
     * 添加求助帖
     *
     * @param helpContent 求助帖内容
     * @param helpLocation 求助帖地点
     * @return 添加结果
     */
    public String addHelpPost(String helpContent, String helpLocation) {
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long blindId = loginBlind.getBlindId();
            Long communityId = loginBlind.getCommunityId();
            if (ObjUtil.isNull(communityId)) {
                return "您未加入任何社区！";

            }
            HelppostAddRequest helppostAddRequest = new HelppostAddRequest();
            helppostAddRequest.setHelpContent(helpContent);
            helppostAddRequest.setHelpLocation(helpLocation);
            HelppostVO helppostVO = innerHelppostService.addHelppost(helppostAddRequest, blindId);

            StringBuilder sb = new StringBuilder();
            sb.append("添加求助帖成功！\n")
                    .append("求助帖id：").append(helppostVO.getHelppostId()).append("\n")
                    .append("求助帖内容：").append(helppostVO.getHelpContent()).append("\n")
                    .append("求助帖地点：").append(helppostVO.getHelpLocation()).append("\n");
            return sb.toString();


        } catch (Exception e) {
            log.error("添加求助帖失败", e);
            return "添加求助帖失败" + e.getMessage();
        }
    }

    // endregion


}
