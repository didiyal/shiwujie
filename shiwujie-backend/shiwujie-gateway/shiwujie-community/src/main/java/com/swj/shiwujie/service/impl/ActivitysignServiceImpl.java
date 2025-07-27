package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.mapper.ActivitysignMapper;
import com.swj.shiwujie.model.VO.community.activitysign.ActivitysignVO;
import com.swj.shiwujie.model.domain.community.Activity;
import com.swj.shiwujie.model.domain.community.Activitysign;
import com.swj.shiwujie.model.enums.community.CommunityRolePermissionEnum;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignAddRequest;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignQueryRequest;
import com.swj.shiwujie.service.ActivityService;
import com.swj.shiwujie.service.ActivitysignService;
import com.swj.shiwujie.utils.LoginUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
* @author Administrator
* @description 针对表【ActivitySign(活动报名签到表)】的数据库操作Service实现
* @createDate 2025-07-26 23:37:53
*/
@Service
public class ActivitysignServiceImpl extends ServiceImpl<ActivitysignMapper, Activitysign>
    implements ActivitysignService{

    @Resource
    private ActivityService activityService;


    @Override
    public boolean addActivitySign(ActivitySignAddRequest activitySignAddRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(activitySignAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long activityId = activitySignAddRequest.getActivityId();
        Long blindId = activitySignAddRequest.getBlindId();
        Long volunteerId = activitySignAddRequest.getVolunteerId();

        // 活动ID校验
        ThrowUtils.throwIf(activityId == null || activityId <= 0, ErrorCode.PARAMS_ERROR, "活动ID无效");

        // 二选一校验
        ThrowUtils.throwIf((blindId == null || blindId <= 0) && (volunteerId == null || volunteerId <= 0), ErrorCode.PARAMS_ERROR, "视障人士ID和志愿者ID必须提供一个");


        // 查询活动是否存在
        Activity activity = activityService.getById(activityId);
        ThrowUtils.throwIf(activity == null, ErrorCode.SYSTEM_ERROR, "活动不存在");

        // 创建活动报名签到记录
        Activitysign activitysign = new Activitysign();
        BeanUtils.copyProperties(activitySignAddRequest, activitysign);
        activitysign.setSignUpTime(new Date());

        // 保存到数据库
        return this.save(activitysign);
    }

    @Override
    public ActivitysignVO getActivitySignVOById(Long signId, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(signId == null || signId <= 0, ErrorCode.PARAMS_ERROR, "签到ID无效");

        // 查询报名记录
        Activitysign activitysign = this.getById(signId);
        ThrowUtils.throwIf(activitysign == null, ErrorCode.SYSTEM_ERROR, "报名记录不存在");

        // 转换为VO
        return this.getActivitySignVO(activitysign);
    }

    @Override
    public Page<ActivitysignVO> listActivitySignByActivity(ActivitySignQueryRequest activitySignQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(activitySignQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long activityId = activitySignQueryRequest.getActivityId();
        Integer current = activitySignQueryRequest.getCurrent();
        Integer pageSize = activitySignQueryRequest.getPageSize();

        // 活动ID和社区ID校验
        ThrowUtils.throwIf(activityId == null || activityId <= 0, ErrorCode.PARAMS_ERROR, "活动ID无效");

        // 分页查询
        Page<Activitysign> page = new Page<>(current, pageSize);
        QueryWrapper<Activitysign> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId);
        Page<Activitysign> activitysignPage = this.page(page, queryWrapper);

        // 转换为VO分页
        Page<ActivitysignVO> activitysignVOPage = new Page<>();
        BeanUtils.copyProperties(activitysignPage, activitysignVOPage);
        activitysignVOPage.setRecords(activitysignPage.getRecords().stream().map(this::getActivitySignVO).collect(java.util.stream.Collectors.toList()));

        return activitysignVOPage;
    }

    /**
     * 转换为VO
     */
    private ActivitysignVO getActivitySignVO(Activitysign activitysign) {
        ActivitysignVO activitysignVO = new ActivitysignVO();
        BeanUtils.copyProperties(activitysign, activitysignVO);
        return activitysignVO;
    }
}




