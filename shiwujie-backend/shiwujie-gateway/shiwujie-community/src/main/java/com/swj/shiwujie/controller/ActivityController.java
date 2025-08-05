package com.swj.shiwujie.controller;

import com.swj.shiwujie.utils.ResultUtils;
import io.swagger.annotations.Api;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.community.activity.ActivityVO;
import com.swj.shiwujie.model.request.community.activity.ActivityAddRequest;
import com.swj.shiwujie.model.request.community.activity.ActivityQueryRequest;
import com.swj.shiwujie.model.request.community.activity.ActivityUpdateRequest;
import com.swj.shiwujie.service.ActivityService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


/**
 * 活动管理接口
 *
 * @author swj
 */
@RestController
@RequestMapping("/activity")
@Api(tags = "活动管理接口")
@Slf4j
public class ActivityController {

    @Resource
    private ActivityService activityService;

    /**
     * 社区管理人员创建活动
     */
    @PostMapping("/add")
    @ApiOperation("社区管理人员创建活动")
    public BaseResponse<ActivityVO> addActivity(@RequestBody ActivityAddRequest activityAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(activityAddRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        ActivityVO activityVO = activityService.addActivity(activityAddRequest, request);
        return ResultUtils.success(activityVO);
    }

    /**
     * 通过ID查询活动
     */
    @GetMapping("/get/vo")
    @ApiOperation("通过ID查询活动")
    public BaseResponse<ActivityVO> getActivityVOById(String id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "活动ID无效");
        try {
            Long activityId = Long.parseLong(id);
            ThrowUtils.throwIf(activityId <= 0, ErrorCode.PARAMS_ERROR, "活动ID无效");
            ActivityVO activityVO = activityService.getActivityVOById(activityId, request);
            return ResultUtils.success(activityVO);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "活动ID格式错误");
        }
    }

    /**
     * 分页查询社区下的活动
     */
    @GetMapping("/list/vo")
    @ApiOperation("分页查询社区下的活动列表")
    public BaseResponse<Page<ActivityVO>> listActivitiesByCommunity(ActivityQueryRequest activityQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(activityQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Page<ActivityVO> activityVOPage = activityService.listActivitiesByCommunity(activityQueryRequest, request);
        return ResultUtils.success(activityVOPage);
    }

    /**
     * 删除活动
     */
    @PostMapping("/delete")
    @ApiOperation("删除活动")
    public BaseResponse<Boolean> deleteActivity(String activityId, HttpServletRequest request) {
        ThrowUtils.throwIf(activityId == null || activityId.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "活动ID无效");
        try {
            Long id = Long.parseLong(activityId);
            ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "活动ID无效");
            boolean result = activityService.deleteActivity(id, request);
            return ResultUtils.success(result);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "活动ID格式错误");
        }
    }

    /**
     * 修改活动
     */
    @PostMapping("/update")
    @ApiOperation("修改活动信息")
    public BaseResponse<Boolean> updateActivity(@RequestBody ActivityUpdateRequest activityUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(activityUpdateRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        boolean result = activityService.updateActivity(activityUpdateRequest, request);
        return ResultUtils.success(result);
    }
}
