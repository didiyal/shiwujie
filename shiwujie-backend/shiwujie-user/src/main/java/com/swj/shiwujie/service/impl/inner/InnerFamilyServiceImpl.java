package com.swj.shiwujie.service.impl.inner;

import com.swj.shiwujie.model.VO.user.family.FamilyVO;
import com.swj.shiwujie.service.FamilyService;
import com.swj.shiwujie.service.user.InnerFamilyService;
import org.apache.dubbo.config.annotation.DubboService;

import jakarta.annotation.Resource;

@DubboService
public class InnerFamilyServiceImpl implements InnerFamilyService {


    @Resource
    private FamilyService familyService;

    /**
     * 根据家庭id获取家庭信息(AI调用)
     * @param familyId 家庭 id
     * @param loginUserPhone 登录用户手机号
     * @return 家庭信息
     */
    @Override
    public FamilyVO getFamilyVOById(Long familyId, String loginUserPhone) {
        return familyService.getFamilyVOById(familyId,loginUserPhone);
    }

    /**
     * 加入家庭(AI调用)
     * @param familyVolunteerPhone 家主手机号
     * @param loginBlindId 盲人 id
     * @param loginVolunteerId 志愿者 id
     * @param loginUserPhone 登录用户手机号
     * @return 是否成功加入家庭
     */
    @Override
    public boolean joinFamily(String familyVolunteerPhone, Long loginBlindId, Long loginVolunteerId, String loginUserPhone) {
        return familyService.joinFamily(familyVolunteerPhone,loginBlindId,loginVolunteerId,loginUserPhone);
    }


    /**
     * 移除用户(AI调用)
     * @param loginBlindId 盲人 id
     * @param loginVolunteerId 志愿者 id
     * @param loginUserPhone 登录用户手机号
     * @return 是否成功移除用户
     */
    @Override
    public boolean userLeaveFromFamily(Long loginBlindId, Long loginVolunteerId, String loginUserPhone) {
        return familyService.userLeaveFromFamily(loginBlindId,loginVolunteerId,loginUserPhone);
    }
}
