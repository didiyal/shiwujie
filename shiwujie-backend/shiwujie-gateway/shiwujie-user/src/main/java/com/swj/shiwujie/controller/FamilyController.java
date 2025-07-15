package com.swj.shiwujie.controller;


import cn.hutool.core.util.ObjUtil;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.user.family.FamilyVO;
import com.swj.shiwujie.model.domain.user.Family;
import com.swj.shiwujie.model.request.user.family.FamilyRemoveUserRequest;
import com.swj.shiwujie.model.request.user.family.FamilyUpdateRequest;
import com.swj.shiwujie.service.FamilyService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;



/**
 * 家庭接口
 * @author  ldl
 *
 */
@RestController
@CrossOrigin
@Slf4j
@RequestMapping("/family")
public class FamilyController {


    @Resource
    private FamilyService familyService;



    // region 家主操作

    /**
     * 创建家庭
     *
     * @return 脱敏后的家庭信息
     */
    @GetMapping("/add")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<FamilyVO> createFamily(HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.PARAMS_ERROR, "只有志愿者才可以创建家庭");

        FamilyVO familyVO = familyService.createFamily(loginVolunteerId,loginUserPhone);


        return ResultUtils.success(familyVO);
    }


    /**
     * 删除家庭
     * @return 是否成功
     */
    @DeleteMapping("/delete/family")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> deleteFamily(HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);
        ThrowUtils.throwIf(ObjUtil.isNull(loginVolunteerId), ErrorCode.NOT_LOGIN, "未登录");


        boolean b = familyService.deleteFamily(loginVolunteerId,loginUserPhone);


        return ResultUtils.success(b);
    }

    /**
     * 更新家庭信息
     * @param familyUpdateRequest 家庭更新内容
     * @param request 请求
     * @return 更新后脱敏后的家庭信息
     */
    @PutMapping("/update")
    public BaseResponse<Boolean> updateFamily(FamilyUpdateRequest familyUpdateRequest, HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);

        //2. 校验传入数据是否合法
        Long familyId = familyUpdateRequest.getFamilyId();
        ThrowUtils.throwIf(ObjUtil.isNull(familyUpdateRequest),ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isNull(familyId),ErrorCode.PARAMS_ERROR);

        //3. 检验
        Boolean result = familyService.updateFamily(familyUpdateRequest,loginVolunteerId);


        return ResultUtils.success(result);
    }


    /**
     * 从家庭中移除用户
     * @param familyRemoveUserRequest 家庭id,用户id
     * @param request 请求
     * @return 更新后脱敏后的家庭信息
     */
    @DeleteMapping("/delete/user")
    public BaseResponse<Boolean> removeUserFromFamily(FamilyRemoveUserRequest familyRemoveUserRequest, HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);


        //2. 校验传入数据是否合法
        Long familyId = familyRemoveUserRequest.getFamilyId();
        ThrowUtils.throwIf(ObjUtil.isNull(familyRemoveUserRequest),ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isNull(familyId),ErrorCode.PARAMS_ERROR);

        //3. 检验
        Boolean result = familyService.removeUserFromFamily(familyRemoveUserRequest,loginVolunteerId,loginUserPhone);


        return ResultUtils.success(result);
    }


    // endregion


    /**
     * 根据id获取家庭信息
     * @param familyId 家庭id
     * @return 脱敏后的家庭信息
     */

    @GetMapping("/get/id/vo")
    public BaseResponse<FamilyVO> getFamilyVOById(Long familyId, HttpServletRequest request) {
        //1. 获取操作用户的id与手机号
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);


        synchronized (loginUserPhone.intern()){
            Family family = familyService.getById(familyId);
            ThrowUtils.throwIf(ObjUtil.isNull(family),ErrorCode.PARAMS_ERROR,"家庭不存在");
            FamilyVO result = familyService.getFamilyVO(family);

            return ResultUtils.success(result);
        }

    }

    /**
     * 加入家庭(需要验证)登录用户
     * @param familyId 家庭id
     * @param request 请求
     * @return 脱敏后的家庭信息
     */
    @PostMapping("/join")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> joinFamily(Long familyId, HttpServletRequest request){
        //1. 获取操作用户的id与手机号
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        Long loginBlindId = LoginUtils.getLoginBlindId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);


        //3.处理
        Boolean b = familyService.joinFamily(familyId, loginBlindId, loginVolunteerId, loginUserPhone);

        return ResultUtils.success(b);

    }


    /**
     * 用户主动退出家庭
     * @param request 请求
     * @return 是否成功
     */
    @DeleteMapping("/delete/leave")
    public BaseResponse<Boolean> leaveFamily(HttpServletRequest request){
        //1. 获取操作用户的id与手机号
        Long loginVolunteerId = LoginUtils.getLoginVolunteerId(request);
        Long loginBlindId = LoginUtils.getLoginBlindId(request);
        String loginUserPhone = LoginUtils.getLoginUserPhone(request);

        boolean result = familyService.userLeaveFromFamily(loginBlindId,loginVolunteerId,loginUserPhone);


        return ResultUtils.success(result);
    }


}
