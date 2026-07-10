package com.swj.shiwujie.controller;


import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.community.community.CommunityLoginSuccessVO;
import com.swj.shiwujie.model.VO.community.community.CommunityVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.request.community.community.CommunityRegisterRequest;
import com.swj.shiwujie.model.request.community.community.CommunitySubListRequest;
import com.swj.shiwujie.model.request.community.community.CommunityUpdateRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.service.CommunityService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;


/**
 * 社区管理接口
 */
@RestController
@RequestMapping("/community")
@Slf4j
@Api(tags = "社区操作接口")
public class CommunityController {


    @Resource
    private CommunityService communityService;

    //region 注册登录

    /**
     * 测试是否登录
     *
     * @return 是否成功
     */
    @GetMapping("/login/check")
    @ApiOperation("检查社区管理员登录状态")
    public BaseResponse<VolunteerVO> checkLogin(HttpServletRequest request) {
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.NOT_LOGIN, "未登录");
        VolunteerVO volunteerVO = communityService.checkLogin(loginVolunteerId);
        return ResultUtils.success(volunteerVO);
    }


    /**
     * 社区入驻
     *
     * @param communityRegisterRequest 社区注册信息
     * @return 脱敏后的社区信息
     */
    @PostMapping("/Register")
    @ApiOperation("社区入驻注册")
    public BaseResponse<CommunityLoginSuccessVO> communityRegister(@RequestBody CommunityRegisterRequest communityRegisterRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(communityRegisterRequest), ErrorCode.PARAMS_ERROR, "信息填写不全");
        ThrowUtils.throwIf(ObjUtil.hasEmpty(communityRegisterRequest), ErrorCode.PARAMS_ERROR, "信息填写不全");

        CommunityLoginSuccessVO res = communityService.communityRegister(communityRegisterRequest);

        return ResultUtils.success(res);
    }


    /**
     * 社区管理人员登录
     *
     * @param volunteerLARRequest 用户的手机号与密码
     * @return 脱敏后的用户信息
     */
    @PostMapping("/Login")
    @ApiOperation("社区管理人员登录")
    public BaseResponse<CommunityLoginSuccessVO> communityLogin(@RequestBody VolunteerLARRequest volunteerLARRequest) {
        ThrowUtils.throwIf(ObjUtil.hasEmpty(volunteerLARRequest), ErrorCode.PARAMS_ERROR, "输入数据格式错误");

        CommunityLoginSuccessVO res = communityService.communityLogin(volunteerLARRequest);

        return ResultUtils.success(res);
    }


    //endregion


    /**
     * 通过ID查询社区信息vo
     *
     * @param communityId
     * @return
     */
    @GetMapping("/get/id/vo")
    @ApiOperation("通过ID查询社区信息vo")
    public BaseResponse<CommunityVO> getVOById(Long communityId) {
        ThrowUtils.throwIf(ObjUtil.isNull(communityId), ErrorCode.PARAMS_ERROR);

        return ResultUtils.success(communityService.getCommunityVO(communityService.getById(communityId)));
    }

    /**
     * 修改社区信息
     *只有注册人可以修改
     * @param request     请求参数
     * @param httpRequest 登录信息
     * @return 更新后的社区信息
     */
    @PostMapping("/update")
    @ApiOperation("修改社区信息")
    public BaseResponse<CommunityVO> updateCommunity(@RequestBody CommunityUpdateRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(request), ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        Long communityId = request.getCommunityId();
        ThrowUtils.throwIf(ObjUtil.isNull(communityId) || communityId <= 0, ErrorCode.PARAMS_ERROR, "社区ID不合法");

        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(httpRequest);
        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.NOT_LOGIN, "未登录");

        CommunityVO updatedCommunity = communityService.updateCommunity(request, loginVolunteerId);
        return ResultUtils.success(updatedCommunity);
    }

    /**
     * 删除社区
     *
     * @param communityId 社区ID
     * @param httpRequest 登录信息
     * @return 是否成功
     */
    @PostMapping("/delete")
    @ApiOperation("删除社区")
    public BaseResponse<Boolean> deleteCommunity(Long communityId, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(communityId) || communityId <= 0, ErrorCode.PARAMS_ERROR, "社区ID不合法");

        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(httpRequest);
        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.NOT_LOGIN, "未登录");

        boolean result = communityService.deleteCommunity(communityId, loginVolunteerId);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询社区下的子社区
     *
     * @param request 分页查询请求
     * @param httpRequest 登录信息
     * @return 子社区分页列表
     */
    @GetMapping("/sub/list/vo")
    @ApiOperation("分页查询社区下的子社区列表")
    public BaseResponse<Page<CommunityVO>> getSubCommunities(CommunitySubListRequest request, HttpServletRequest httpRequest) {
        Long communityId = request.getCommunityId();
        long current = request.getCurrent();
        long size = request.getPageSize();

        // 参数校验
        ThrowUtils.throwIf(communityId == null || communityId <= 0, ErrorCode.PARAMS_ERROR, "社区ID不合法");
        ThrowUtils.throwIf(current <= 0 || size <= 0 || size > 100, ErrorCode.PARAMS_ERROR, "分页参数不合法");

        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(httpRequest);
        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.NOT_LOGIN, "未登录");


        Page<CommunityVO> subCommunitiesPage = communityService.getSubCommunities(communityId, current, size, loginVolunteerId);
        return ResultUtils.success(subCommunitiesPage);
    }


}
