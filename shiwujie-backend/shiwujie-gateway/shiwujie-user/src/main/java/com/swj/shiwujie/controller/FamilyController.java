package com.swj.shiwujie.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.FamilyVO;
import com.swj.shiwujie.model.domain.Family;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.service.FamilyService;
import com.swj.shiwujie.service.UserService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.RadomUtils;
import com.swj.shiwujie.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;



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

    @Resource
    private UserService userService;



    /**
     * 创建家庭
     *
     * @param familyName 家庭名称
     * @return
     */
    @GetMapping("/add")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<FamilyVO> createFamily(String familyName, HttpServletRequest request) {
        //1. **从用户公共类中获取操作用户的id**
        if (LoginUtils.getCurrentUserId(request) == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        //2. 校验传入数据是否**合法**(是否为空,int类型数据是否满足大于0之类的)
        if (familyName == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "家庭名称为空");
        }

        FamilyVO familyVO = familyService.addFamily(familyName,LoginUtils.getCurrentUserId(request));

        //6. 将数据返回给前端(可以封装一个返回类)
        return ResultUtils.success(familyVO);
    }


    /**
     * 根据用户账号获取家庭信息
     *
     * @param familyAccount
     * @return
     */
    @GetMapping("/get/account")
    public BaseResponse<FamilyVO> getFamilyByAccount(String familyAccount, HttpServletRequest request) {
        //1. **从用户公共类中获取操作用户的id**
        if (LoginUtils.getCurrentUserId(request) == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        //2. 校验传入数据是否**合法**(是否为空,int类型数据是否满足大于0之类的)
        if (familyAccount == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "家庭账号为空");
        }
        FamilyVO familyVO = familyService.getFamilyByAccount(familyAccount,LoginUtils.getCurrentUserId(request));
        //6. 将数据返回给前端(可以封装一个返回类)
        return ResultUtils.success(familyVO);
    }


    /**
     * 根据id获取家庭信息
     *
     * @param id id
     * @return
     */

    @GetMapping("/get/id")
    public BaseResponse<FamilyVO> getFamilyById(Long id, HttpServletRequest request) {
        //1. **从用户公共类中获取操作用户的id**
        if (LoginUtils.getCurrentUserId(request) == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        //2. 校验传入数据是否**合法**(是否为空,int类型数据是否满足大于0之类的)
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Family family = familyService.getById(id);
        if (family == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "家庭不存在");
        }

        FamilyVO familyVO = familyService.getFamilyVOByFamily(family);
        //6. 将数据返回给前端(可以封装一个返回类)
        return ResultUtils.success(familyVO);
    }


    /**
     * 更新家庭名称
     *
     * @param familyName
     * @param id
     * @return
     */

    @PutMapping("/update/name")
    public BaseResponse<FamilyVO> updateFamilyName(String familyName, Long id, HttpServletRequest request) {
        //1. **从用户公共类中获取操作用户的id**
        Long currentUserId = LoginUtils.getCurrentUserId(request);
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        //2. 校验传入数据是否**合法**(是否为空,int类型数据是否满足大于0之类的)
        if (familyName == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "家庭名称为空");
        }
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //3. 从数据库中查询家庭信息
        QueryWrapper<Family> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        Family family = familyService.getOne(queryWrapper);
        if (family == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "家庭不存在");
        }
        //校验是否是家主
        if(!family.getUserId().equals(currentUserId)){
            throw new BusinessException(ErrorCode.NO_AUTH,"只有家主可以修改家庭名");
        }
        //4. 更新数据库中的家庭信息
        family.setFamilyName(familyName);
        boolean b = familyService.updateById(family);
        if (!b) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }

        //5. 返回数据
        FamilyVO familyVO = familyService.getFamilyVOByFamily(family);
        return ResultUtils.success(familyVO);
    }


    /**
     * 从家庭中移除用户
     */
    @DeleteMapping("/remove/user")
    public BaseResponse<FamilyVO> removeUserFromFamily(Long id, HttpServletRequest request) {
        //1. **从用户公共类中获取操作用户的id**
        Long currentUserId = LoginUtils.getCurrentUserId(request);
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        //2. 校验传入数据是否**合法**(是否为空,int类型数据是否满足大于0之类的)
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //3. 从数据库中查询家庭信息
        QueryWrapper<Family> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", currentUserId);
        Family family = familyService.getOne(queryWrapper);
        //只有家主可以修改
        if (family == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "家主才能修改");
        }
        //如果家主删去自己,相当于解散家庭
        if(id.equals(currentUserId)){
            boolean b = familyService.removeFamilyById(family.getId(), currentUserId);
            if(!b){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"解散家庭失败");
            }
            return ResultUtils.success(null);

        }
        String userIdsString = family.getAddId();
        Gson gson = new Gson();
        List<Long> userIds = gson.fromJson(userIdsString, new TypeToken<List<Long>>() {
        }.getType());
        //4. 移除用户
        userIds.remove(id);
        userIdsString = gson.toJson(userIds);
        family.setAddId(userIdsString);

        //被移除的用户的家庭信息改为空
        User removeUser = userService.getById(id);
        removeUser.setFamilyId(0L);
        //更新信息
        userService.updateById(removeUser);

        //4. 更新数据库中的家庭信息
        boolean result = familyService.updateById(family);
        if(!result){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"修改失败");
        }

        FamilyVO familyVO = familyService.getFamilyVOByFamily(family);
        return ResultUtils.success(familyVO);
    }


    /**
     * 删除家庭
     * @param id
     * @return
     */
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deleteFamily(Long id, HttpServletRequest request) {
        //1. **从用户公共类中获取操作用户的id**
        Long loginUserId = LoginUtils.getCurrentUserId(request);
        if (loginUserId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        boolean b = familyService.removeFamilyById(id,loginUserId);
        return ResultUtils.success(b);
    }


    /**
     * 加入家庭(直接加入无需验证)
     * @param familyAccount
     * @param request
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<FamilyVO> joinFamily(String familyAccount, HttpServletRequest request){
        //1.获取操作用户的id
        if(LoginUtils.getCurrentUserId(request) == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        //2.校验传入数据是否合法
        if(familyAccount == null || familyAccount.length() == 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "家庭账号为空");
        }
        FamilyVO familyVO = familyService.joinFamilyBuAccount(familyAccount,LoginUtils.getCurrentUserId(request));

        return ResultUtils.success(familyVO);

    }


    /**
     * 用户主动退出家庭
     * @param request
     * @return
     */

    @DeleteMapping("/leave")
    public BaseResponse<Boolean> leaveFamily(HttpServletRequest request){
        //1. 检查用户是否加入了家庭
        //  登录的用户信息
        Long loginUserId = LoginUtils.getCurrentUserId(request);

        boolean result = familyService.userLeaveFromFamily(loginUserId);


        return ResultUtils.success(result);
    }


}
