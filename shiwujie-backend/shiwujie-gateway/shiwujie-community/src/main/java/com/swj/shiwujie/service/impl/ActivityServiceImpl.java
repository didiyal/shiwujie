package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.mapper.ActivityMapper;
import com.swj.shiwujie.model.domain.community.Activity;
import com.swj.shiwujie.model.VO.community.activity.ActivityVO;
import com.swj.shiwujie.model.enums.community.ActivityStatusEnum;
import com.swj.shiwujie.model.request.community.activity.ActivityAddRequest;
import com.swj.shiwujie.model.request.community.activity.ActivityQueryRequest;
import com.swj.shiwujie.model.request.community.activity.ActivityUpdateRequest;
import com.swj.shiwujie.service.ActivityService;
import com.swj.shiwujie.service.CommunitymanagerService;
import com.swj.shiwujie.utils.LoginUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【Activity(活动信息表)】的数据库操作Service实现
* @createDate 2025-07-26 22:31:08
*/
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    @Resource
    private CommunitymanagerService communitymanagerService;

    @Override
    public ActivityVO addActivity(ActivityAddRequest activityAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(activityAddRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        Activity activity = new Activity();
        BeanUtils.copyProperties(activityAddRequest, activity);
        activity.setCreateTime(new Date());
        activity.setUpdateTime(new Date());
        activity.setManagerId(LoginUtils.getLoginVolunteerId(request));
        activity.setActivityStatus(ActivityStatusEnum.WAITING.getPostStatus()); // 初始状态设为未开始
        boolean saveResult = this.save(activity);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "创建活动失败");

        // 转换为VO并返回
        return getActivityVO(activity);
    }

    @Override
    public ActivityVO getActivityVOById(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "活动ID无效");

        Activity activity = this.getById(id);
        ThrowUtils.throwIf(activity == null, ErrorCode.SYSTEM_ERROR, "活动不存在");

        return getActivityVO(activity);
    }

    @Override
    public Page<ActivityVO> listActivitiesByCommunity(ActivityQueryRequest activityQueryRequest, HttpServletRequest request) {
        Long communityId = activityQueryRequest.getCommunityId();
        String activityStatus = activityQueryRequest.getActivityStatus();
        long current = activityQueryRequest.getCurrent();
        long size = activityQueryRequest.getPageSize();

        ThrowUtils.throwIf(communityId == null || communityId <= 0, ErrorCode.PARAMS_ERROR, "社区ID无效");

        // 构建查询条件
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("community_id", communityId);

        // 活动状态转换（字符串转枚举code）
        if (StringUtils.isNotBlank(activityStatus)) {
            ActivityStatusEnum statusEnum = ActivityStatusEnum.getByName(activityStatus);
            ThrowUtils.throwIf(statusEnum == null, ErrorCode.PARAMS_ERROR, "活动状态无效");
            queryWrapper.eq("activity_status", statusEnum.getPostStatus());
        }

        // 分页查询
        Page<Activity> activityPage = this.page(new Page<>(current, size), queryWrapper);

        // 转换为VO分页结果
        Page<ActivityVO> activityVOPage = new Page<>();
        BeanUtils.copyProperties(activityPage, activityVOPage);
        List<ActivityVO> activityVOList = activityPage.getRecords().stream()
                .map(this::getActivityVO)
                .collect(Collectors.toList());
        activityVOPage.setRecords(activityVOList);

        return activityVOPage;
    }

    @Override
    public boolean deleteActivity(Long activityId, HttpServletRequest request) {
        ThrowUtils.throwIf(activityId == null || activityId <= 0, ErrorCode.PARAMS_ERROR, "活动ID无效");

        Activity activity = this.getById(activityId);
        ThrowUtils.throwIf(activity == null, ErrorCode.SYSTEM_ERROR, "活动不存在");

        return this.removeById(activityId);
    }

    @Override
    public boolean updateActivity(ActivityUpdateRequest activityUpdateRequest, HttpServletRequest request) {
        String idStr = activityUpdateRequest.getActivityId();
        ThrowUtils.throwIf(idStr == null || idStr.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "活动ID无效");
        
        try {
            Long id = Long.parseLong(idStr);
            ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "活动ID无效");
            
            Activity activity = this.getById(id);
            ThrowUtils.throwIf(activity == null, ErrorCode.SYSTEM_ERROR, "活动不存在");

        // 更新活动信息
        if (StringUtils.isNotBlank(activityUpdateRequest.getActivityName())) {
            activity.setActivityName(activityUpdateRequest.getActivityName());
        }
        if (StringUtils.isNotBlank(activityUpdateRequest.getActivityContent())) {
            activity.setActivityContent(activityUpdateRequest.getActivityContent());
        }
        if (StringUtils.isNotBlank(activityUpdateRequest.getActivityLocation())) {
            activity.setActivityLocation(activityUpdateRequest.getActivityLocation());
        }
        if (activityUpdateRequest.getMaxParticipants() != null) {
            activity.setMaxParticipants(activityUpdateRequest.getMaxParticipants());
        }
        if (activityUpdateRequest.getStartTime() != null) {
            activity.setStartTime(activityUpdateRequest.getStartTime());
        }
        if (activityUpdateRequest.getEndTime() != null) {
            activity.setEndTime(activityUpdateRequest.getEndTime());
        }

        // 活动状态转换（字符串转枚举code）
        String activityStatus = activityUpdateRequest.getActivityStatus();
        if (StringUtils.isNotBlank(activityStatus)) {
            ActivityStatusEnum statusEnum = ActivityStatusEnum.getByName(activityStatus);
            ThrowUtils.throwIf(statusEnum == null, ErrorCode.PARAMS_ERROR, "活动状态无效");
            activity.setActivityStatus(statusEnum.getPostStatus());
        }

        return this.updateById(activity);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "活动ID格式错误");
        }
    }

    /**
     * 转换Activity为ActivityVO
     */
    private ActivityVO getActivityVO(Activity activity) {
        ActivityVO activityVO = new ActivityVO();
        BeanUtils.copyProperties(activity, activityVO);
        // 设置活动状态名称
        activityVO.setActivityStatus(activity.getActivityStatus());
        return activityVO;
    }
}




