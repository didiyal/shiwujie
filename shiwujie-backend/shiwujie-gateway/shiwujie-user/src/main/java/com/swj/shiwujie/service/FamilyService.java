package com.swj.shiwujie.service;

import com.swj.shiwujie.model.VO.user.family.FamilyVO;
import com.swj.shiwujie.model.domain.Family;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.request.user.family.FamilyRemoveUserRequest;
import com.swj.shiwujie.model.request.user.family.FamilyUpdateRequest;

/**
* @author Administrator
* @description 针对表【Family(家庭信息表)】的数据库操作Service
* @createDate 2025-07-01 00:21:42
*/
public interface FamilyService extends IService<Family> {

    /**
     * 创建家庭
     * @param loginVolunteerId 登录用户id
     * @param loginUserPhone 登录用户手机号
     * @return 脱敏后的家庭信息
     */
    FamilyVO createFamily(Long loginVolunteerId,String loginUserPhone);



    /**
     * 删除家庭
     * @param loginVolunteerId 登录志愿者id
     * @param loginUserPhone 登录志愿者手机号
     * @return 是否成功
     */
    boolean deleteFamily(Long loginVolunteerId, String loginUserPhone);

    // region 工具方法

    /**
     * 家庭脱敏
     * @param family 家庭信息
     * @return 脱敏后的家庭信息
     */
    FamilyVO getFamilyVO(Family family);

    /**
     * 更新家庭信息
     * @param familyUpdateRequest 家庭id,更新信息
     * @param loginVolunteerId 家主id
     * @param loginUserPhone 家主手机号
     * @return 更新后的脱敏家庭信息
     */
    FamilyVO updateFamily(FamilyUpdateRequest familyUpdateRequest, Long loginVolunteerId, String loginUserPhone);


    /**
     * 从家庭中移除用户
     * @param familyRemoveUserRequest 家庭id,用户id
     * @param loginVolunteerId 家主id
     * @param loginUserPhone 家主手机号
     * @return 脱敏后的家庭信息
     */
    Boolean removeUserFromFamily(FamilyRemoveUserRequest familyRemoveUserRequest, Long loginVolunteerId, String loginUserPhone);

    /**
     * 用户主动退出家庭
     * @param loginBlindId 登录盲人id(可以为空)
     * @param loginVolunteerId 登录志愿者id(可以为空)
     * @param loginUserPhone 登录手机号
     * @return
     */
    boolean userLeaveFromFamily(Long loginBlindId, Long loginVolunteerId, String loginUserPhone);


    // endregion

}
