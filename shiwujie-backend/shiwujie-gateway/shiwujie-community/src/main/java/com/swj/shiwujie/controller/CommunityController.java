package com.swj.shiwujie.controller;


import cn.hutool.core.util.ObjUtil;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.community.CommunityLoginSuccessVO;
import com.swj.shiwujie.model.VO.community.CommunityVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerLoginSuccessVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.community.community.CommunityRegisterRequest;
import com.swj.shiwujie.model.request.community.community.CommunitySubListRequest;
import com.swj.shiwujie.model.request.community.community.CommunityUpdateRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.model.enums.community.CommunityRolePermissionEnum;
import com.swj.shiwujie.service.CommunityService;
import com.swj.shiwujie.service.CommunitymanagerService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * 社区管理接口
 */
@RestController
@RequestMapping("/community")
@Slf4j
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
    public BaseResponse<Boolean> checkLogin(HttpServletRequest request) {
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.NOT_LOGIN, "未登录");
        communityService.checkLogin(loginVolunteerId);
        return ResultUtils.success(true);
    }


    /**
     * 社区入驻
     *
     * @param communityRegisterRequest 社区注册信息
     * @return 脱敏后的社区信息
     */
    @PostMapping("/Register")
    public BaseResponse<CommunityLoginSuccessVO> communityRegister(CommunityRegisterRequest communityRegisterRequest) {
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
    public BaseResponse<CommunityLoginSuccessVO> communityLogin(VolunteerLARRequest volunteerLARRequest) {
        ThrowUtils.throwIf(ObjUtil.hasEmpty(volunteerLARRequest), ErrorCode.PARAMS_ERROR, "输入数据格式错误");

        CommunityLoginSuccessVO res = communityService.communityLogin(volunteerLARRequest);

        return ResultUtils.success(res);
    }


    //endregion


    /**
     * 根据id获取封装类
     *
     * @param communityId
     * @return
     */
    @GetMapping("/get/id/vo")
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
    public BaseResponse<CommunityVO> updateCommunity(CommunityUpdateRequest request, HttpServletRequest httpRequest) {
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
     * @param httpRequest 登录信息
     * @return 子社区列表
     */
    @GetMapping("/sub/list")
    public BaseResponse<List<CommunityVO>> getSubCommunities(CommunitySubListRequest request, HttpServletRequest httpRequest) {
        Long communityId = request.getCommunityId();
        long current = request.getCurrent();
        long size = request.getPageSize();

        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(httpRequest);
        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.NOT_LOGIN, "未登录");

        List<CommunityVO> subCommunities = communityService.getSubCommunities(communityId, current, size, loginVolunteerId);
        return ResultUtils.success(subCommunities);
    }


}
