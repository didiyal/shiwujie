package com.swj.shiwujie.controller;

import com.swj.shiwujie.model.request.community.activitysign.ActivitySignQueryRequest;
import com.swj.shiwujie.service.ActivitysignService;
import com.swj.shiwujie.utils.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.model.VO.community.activitysign.ActivitysignVO;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignAddRequest;


/**
 * 活动报名签到管理接口
 *
 * @author swj
 */
@RestController
@RequestMapping("/activitysign")
@Api(tags = "活动报名签到管理接口")
public class ActivitysignController {

    @Resource
    private ActivitysignService activitysignService;

    /**
     * 添加活动报名签到
     */
    @PostMapping("/add")
    @ApiOperation("添加活动报名签到")
    public BaseResponse<Boolean> addActivitySign(@RequestBody ActivitySignAddRequest activitySignAddRequest, HttpServletRequest request) {
        boolean result = activitysignService.addActivitySign(activitySignAddRequest);
        return ResultUtils.success(result);
    }

    /**
     * 通过id查询活动报名签到VO
     */
    @GetMapping("/get/vo")
    @ApiOperation("通过id查询活动报名签到VO")
    public BaseResponse<ActivitysignVO> getActivitySignVOById(Long signId, HttpServletRequest request) {
        ActivitysignVO activitysignVO = activitysignService.getActivitySignVOById(signId);
        return ResultUtils.success(activitysignVO);
    }

    /**
     * 分页查询活动下的报名签到VO
     */
    @GetMapping("/list/page/vo")
    @ApiOperation("分页查询活动下的报名签到VO")
    public BaseResponse<Page<ActivitysignVO>> listActivitySignByActivity(ActivitySignQueryRequest activitySignQueryRequest) {
        Page<ActivitysignVO> activitysignVOPage = activitysignService.listActivitySignByActivity(activitySignQueryRequest);
        return ResultUtils.success(activitysignVOPage);
    }
}
