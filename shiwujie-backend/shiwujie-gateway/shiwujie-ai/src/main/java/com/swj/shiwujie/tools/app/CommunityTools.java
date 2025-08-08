package com.swj.shiwujie.tools.app;


import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.model.VO.community.activity.ActivityVO;
import com.swj.shiwujie.model.VO.community.activitysign.ActivitysignVO;
import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.enums.community.ActivityStatusEnum;
import com.swj.shiwujie.model.enums.community.CommunityTypeEnum;
import com.swj.shiwujie.model.request.community.activity.ActivityQueryRequest;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignAddRequest;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignQueryRequest;
import com.swj.shiwujie.service.community.InnerActivityService;
import com.swj.shiwujie.service.community.InnerActivitysignService;
import com.swj.shiwujie.service.community.InnerCommunityService;
import com.swj.shiwujie.utils.LoginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 社区模块业务工具调用
 */
@Slf4j
public class CommunityTools {


    @DubboReference
    private InnerCommunityService innerCommunityService;

    @DubboReference
    private InnerActivityService innerActivityService;

    @DubboReference
    private InnerActivitysignService innerActivitysignService;


    /**
     * 获取用户的社区信息
     * @return 社区信息
     */
    @Tool(description = "APP功能操作:获取用户的社区信息")
    public String getCommunityInfo()
    {
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long communityId = loginBlind.getCommunityId();
            if(ObjUtil.isNull(communityId)){
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

            return  sb.toString();
        } catch (Exception e){
            log.error("获取社区信息失败",e);
            return "获取社区信息失败"+e.getMessage();
        }
    }



    // region 社区活动相关

    /**
     * 获取社区活动信息(现在默认查询20个,市级社区内)
     * @return 活动信息
     */
    @Tool(description = "APP功能操作:获取社区活动信息")
    public String getActivityInfo()
    {
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long communityId = loginBlind.getCommunityId();
            if(ObjUtil.isNull(communityId)){
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
            return  sb.toString();
        } catch (Exception e){
            log.error("获取社区活动信息失败",e);
            return "获取社区活动信息失败"+e.getMessage();
        }
    }

    @Tool(description = "APP功能操作:添加活动报名")
    public String addActivitySign(@ToolParam(description = "活动id") Long activityId){
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long communityId = loginBlind.getCommunityId();
            if(ObjUtil.isNull(communityId)){
                return "您未加入任何社区,无法报名活动";
            }

            ActivitySignAddRequest activitySignAddRequest = new ActivitySignAddRequest();
            activitySignAddRequest.setActivityId(activityId);
            activitySignAddRequest.setBlindId(loginBlind.getBlindId());

            // 添加活动报名
            boolean addActivitySign = innerActivitysignService.addActivitySign(activitySignAddRequest);
            if(addActivitySign) return "报名成功";
            else return "报名失败";
        } catch (Exception e){
            log.error("添加活动报名失败",e);
            return "添加活动报名失败"+e.getMessage();
        }
    }



//    public String getBlindAcctivitySignInfo(){
//        try {
//            Blind loginBlind = LoginUtils.getLoginBlind();
//            Long communityId = loginBlind.getCommunityId();
//            if(ObjUtil.isNull(communityId)){
//                return "您未加入任何社区,无法查看活动报名信息";
//            }
//            ActivitySignQueryRequest activitySignQueryRequest = new ActivitySignQueryRequest();
//            activitySignQueryRequest.setBlindId(loginBlind.getBlindId());
//            activitySignQueryRequest.s
//            Page<ActivitysignVO> activitysignVOPage = innerActivitysignService.listActivitySignByActivity(activitySignQueryRequest);
//            StringBuilder sb = new StringBuilder();
//            for (ActivitysignVO activitysignVO : activitysignVOPage.getRecords()) {
//                sb.append("活动名称：").append(activitysignVO.()).append("\n")
//                        .append("活动时间：").append(activitysignVO.getSignUpTime()).append("\n")
//                        .append("活动地点：").append(activitysignVO.getCheckInLocation()).append("\n")
//                        .append("活动状态：").append(ActivityStatusEnum.getById(activitysignVO.getActivityStatus())).append("\n");
//            }
//        } catch (Exception e){
//            log.error("获取用户活动报名信息失败",e);
//            return "获取用户活动报名信息失败"+e.getMessage();
//        }
//    }


    // endregion




}
