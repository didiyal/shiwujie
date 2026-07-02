package com.swj.shiwujie.service.user;


import com.swj.shiwujie.model.VO.user.family.FamilyVO;
import com.swj.shiwujie.model.request.community.CommunityJoinRequest;

/**
 * dubbo 服务提供接口
 */
public interface InnerFamilyService {


    /**
     * 获取家庭信息(AI调用)
     * @param familyId 家庭id
     * @param loginUserPhone 登录用户手机号
     * @return 脱敏后的家庭信息
     */
    FamilyVO getFamilyVOById(Long familyId, String loginUserPhone);



    /**
     * 申请加入家庭(AI调用)
     * @param familyVolunteerPhone 家庭志愿者手机号
     * @param loginBlindId 加入盲人信息
     * @param loginVolunteerId 加入志愿者信息
     * @param loginUserPhone 登录手机号
     * @return 是否申请成功
     */
    boolean joinFamily(String familyVolunteerPhone, Long loginBlindId, Long loginVolunteerId, String loginUserPhone);



    /**
     * 用户主动退出家庭(AI调用)
     * @param loginBlindId 登录盲人id(可以为空)
     * @param loginVolunteerId 登录志愿者id(可以为空)
     * @param loginUserPhone 登录手机号
     * @return
     */
    boolean userLeaveFromFamily(Long loginBlindId, Long loginVolunteerId, String loginUserPhone);

}
