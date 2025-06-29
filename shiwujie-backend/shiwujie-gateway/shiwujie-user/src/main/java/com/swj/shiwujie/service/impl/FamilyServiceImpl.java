package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.FamilyVO;
import com.swj.shiwujie.model.domain.Family;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.service.FamilyService;
import com.swj.shiwujie.mapper.FamilyMapper;
import com.swj.shiwujie.service.UserService;
import com.swj.shiwujie.utils.RadomUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author ldl
 * @description 针对表【family(家庭表)】的数据库操作Service实现
 * @createDate 2024-12-15 23:26:43
 */
@Service
@Slf4j
public class FamilyServiceImpl extends ServiceImpl<FamilyMapper, Family>
        implements FamilyService{

    @Resource
    private UserService userService;
    /**
     * 添加家庭
     * @param familyName 家庭名称
     * @return
     */
    @Override
    public FamilyVO addFamily(String familyName,Long loginUserId) {
        //1. **从用户公共类中获取操作用户的id**
        if(loginUserId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }
        //获取用户信息
        User loginUser = userService.getById(loginUserId);
        //用户只能创建或者加入一个家庭
        Long loginUserFamilyId = loginUser.getFamilyId();
        if(loginUserFamilyId != null && loginUserFamilyId > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"只允许创建或者加入一个家庭");
        }
        //3. 自动生成**不重复**的家庭账号
        String familyAccount = RadomUtils.generateRandomString();
        // 防止重复
        QueryWrapper<Family> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("familyAccount",familyAccount);
        while (true){
            if(this.getOne(queryWrapper) == null){
                break;
            }
            familyAccount = RadomUtils.generateRandomString();
        }
        //4. 创建**家庭类**的对象来接收信息(创建人id,传入的数据)
        Family family = new Family();
        family.setFamilyName(familyName);
        family.setFamilyAccount(familyAccount);
        family.setUserId(loginUserId);

        List<Long> userIds = new ArrayList<>();
        userIds.add(loginUserId);
        Gson gson = new Gson();
        String userIdsString = gson.toJson(userIds);
        family.setAddId(userIdsString);
        //5. 将家庭对象插入到数据库中
        boolean save = this.save(family);

        if(!save){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"创建家庭失败");
        }
        //用户的familyId记入家庭的id,并存入数据库中
        loginUser.setFamilyId(family.getId());
        boolean b = userService.updateById(loginUser);
        if(!b){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"用户信息更新错误");
        }

        return getFamilyVOByFamily(family);
    }

    /**
     * 根据家庭号获取家庭
     *
     * @param familyAccount
     * @return
     */
    @Override
    public FamilyVO getFamilyByAccount(String familyAccount,Long loginUserId) {
        //1. **从用户公共类中获取操作用户的id**
        if(loginUserId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }
        //2. 校验传入数据是否**合法**(是否为空,int类型数据是否满足大于0之类的,用户账号是否不包含特殊字符之类)
        if(familyAccount == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号为空");
        }
        QueryWrapper<Family> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("familyAccount",familyAccount);
        Family family = this.getOne(queryWrapper);
        if(family == null){
            throw new BusinessException(ErrorCode.NOT_FOUND,"家庭不存在");
        }

        return getFamilyVOByFamily(family);

    }

    /**
     * 获取家庭里的用户列表
     *
     * @param family
     * @return
     */
    @Override
    public List<User> getFamilyUsersList(Family family) {
        //1. 校验传入数据是否合法(是否为空,int类型数据是否满足大于0之类的,用户账号是否不包含特殊字符之类)
        if(family == null){
            return null;
        }
        String userIdsString = family.getAddId();
        Gson gson = new Gson();
        List<Long> userIds = gson.fromJson(userIdsString, new TypeToken<List<Long>>(){}.getType());
        if(userIds == null || userIds.size() == 0){
            return null;
        }
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id",userIds);
        List<User> userList = userService.list(userQueryWrapper);

        return userList;
    }

    /**
     * 获取家庭内部的id
     * @param family
     * @return
     */
    @Override
    public List<Long> getFamilyUserIdsList(Family family) {
        //1. 校验传入数据是否合法(是否为空,int类型数据是否满足大于0之类的,用户账号是否不包含特殊字符之类)
        if(family == null){
            return null;
        }
        String userIdsString = family.getAddId();
        Gson gson = new Gson();
        List<Long> userIds = gson.fromJson(userIdsString, new TypeToken<List<Long>>(){}.getType());

        return userIds;
    }

    /**
     * 根据家庭对象获取家庭VO对象
     * @param family
     * @return
     */
    @Override
    public FamilyVO getFamilyVOByFamily(Family family){
        FamilyVO familyVO = new FamilyVO();
        BeanUtils.copyProperties(family,familyVO);
        List<User> userList = getFamilyUsersList(family);
        familyVO.setUserList(userList);
        return familyVO;
    }
    /**
     * 加入家庭(直接加入无需验证)
     *
     * @param familyAccount
     * @param currentUserId
     * @return
     */
    @Override
    public FamilyVO joinFamilyBuAccount(String familyAccount, Long currentUserId) {
        //验证参数
        if(familyAccount == null || familyAccount.length() == 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"家庭账号为空");
        }
        if(currentUserId == null || currentUserId <= 0){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }
        //用户只创建或者加入了一个家庭
        User currentUser = userService.getById(currentUserId);
        Long currentUserFamilyId = currentUser.getFamilyId();
        if(currentUserFamilyId != null && currentUserFamilyId > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户只能创建或者加入一个家庭");
        }
        //根据家庭账号获取要加入的家庭信息
        QueryWrapper<Family> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("familyAccount", familyAccount);
        Family family = this.getOne(queryWrapper);
        //从数据库中获取该家庭的家庭用户信息并转成list
        List<Long> userIds = this.getFamilyUserIdsList(family);
        //检查是否存在将当前用户的id添加到家庭用户信息中
        if(userIds.contains(currentUserId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"已加入该家庭");
        }
        //不存在
        userIds.add(currentUserId);
        //转json
        Gson gson = new Gson();
        String userIdsToJson = gson.toJson(userIds);
        family.setAddId(userIdsToJson);
        //将家庭用户信息更新到数据库中
        this.updateById(family);
        //用户的信息里加入家庭的id

        currentUser.setFamilyId(family.getId());
        userService.updateById(currentUser);
        //返回家庭信息
        return this.getFamilyVOByFamily(family);
    }

    /**
     * 根据家庭id删除家庭
     *
     * @param id
     * @return
     */
    @Override
    public boolean removeFamilyById(Long id,Long loginUserId) {
        //2. 校验传入数据是否**合法**(是否为空,int类型数据是否满足大于0之类的)
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //获取家庭信息
        Family family = this.getById(id);
        if(family == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"删除的家庭不存在");
        }
        //只有家主可以删去家庭
        if(!family.getUserId().equals(loginUserId)){
            throw new BusinessException(ErrorCode.NO_AUTH,"只有家主可以删除家庭");
        }
        //从家庭中获取成员信息,并修改familyId然后更新数据库
        List<User> userList = this.getFamilyUsersList(family);
        userList.stream().map(user -> {
            user.setFamilyId(0L);
            return user;
        }).collect(Collectors.toList());
        userService.updateBatchById(userList);

        //删除家庭
        boolean b = this.removeById(id);
        if(!b){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }

        return true;
    }

    /**
     * 用户退出家庭
     *
     * @param loginUserId
     * @return
     */
    @Override
    public boolean userLeaveFromFamily(Long loginUserId) {
        if(loginUserId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"未登录");
        }
        User loginUser = userService.getById(loginUserId);
        Long loginUserFamilyId = loginUser.getFamilyId();
        if(loginUserFamilyId == null || loginUserFamilyId == 0L){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户未加入家庭");
        }
        //2. 删去用户信息
        //获取家庭信息
        Family family = this.getById(loginUserFamilyId);
        //从家庭中去除该用户
        List<Long> familyUserIdsList = this.getFamilyUserIdsList(family);
        familyUserIdsList.remove(loginUserId);
        Gson gson = new Gson();
        String familyUserIds = gson.toJson(familyUserIdsList);
        family.setAddId(familyUserIds);
        //用户的familyId变为0
        loginUser.setFamilyId(0L);
        //更新
        boolean b = this.updateById(family);
        if(!b){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"家庭更新失败");
        }
        b = userService.updateById(loginUser);
        if(!b){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"用户更新失败");
        }
        return b;
    }

    /**
     * 校验家庭是否有除自己外的其他用户
     *
     * @param familyId
     * @return
     */
    @Override
    public boolean familyUsersVerify(Long familyId) {
        Family family = this.getById(familyId);
        List<User> familyUsersList = this.getFamilyUsersList(family);
        if(familyUsersList.size() == 1){
            return false;
        }
        return true;
    }


}




