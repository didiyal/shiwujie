package com.swj.shiwujie.service.impl.inner;

import com.swj.shiwujie.model.VO.user.family.FamilyVO;
import com.swj.shiwujie.service.FamilyService;
import com.swj.shiwujie.service.user.InnerFamilyService;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

@DubboService
public class InnerFamilyServiceImpl implements InnerFamilyService {


    @Resource
    private FamilyService familyService;

    @Override
    public FamilyVO getFamilyVOById(Long familyId, String loginUserPhone) {
        return familyService.getFamilyVOById(familyId,loginUserPhone);
    }

    @Override
    public boolean joinFamily(String familyVolunteerPhone, Long loginBlindId, Long loginVolunteerId, String loginUserPhone) {
        return familyService.joinFamily(familyVolunteerPhone,loginBlindId,loginVolunteerId,loginUserPhone);
    }

    @Override
    public boolean userLeaveFromFamily(Long loginBlindId, Long loginVolunteerId, String loginUserPhone) {
        return familyService.userLeaveFromFamily(loginBlindId,loginVolunteerId,loginUserPhone);
    }
}
