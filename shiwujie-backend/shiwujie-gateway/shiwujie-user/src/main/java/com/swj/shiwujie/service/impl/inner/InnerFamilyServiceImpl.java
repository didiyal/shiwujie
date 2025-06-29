package com.swj.shiwujie.service.impl.inner;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swj.shiwujie.model.domain.Family;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.service.FamilyService;
import com.swj.shiwujie.service.InnerFamilyService;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.util.List;

@DubboService
public class InnerFamilyServiceImpl implements InnerFamilyService {

    @Resource
    private FamilyService familyService;

    /**
     * 校验家庭是否有除自己外的其他用户
     *
     * @param familyId
     * @return
     */
    @Override
    public boolean familyUsersVerify(Long familyId) {
        Family family = familyService.getById(familyId);
        List<User> familyUsersList = familyService.getFamilyUsersList(family);
        if(familyUsersList.size() == 1){
            return false;
        }
        return true;
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

    @Override
    public Family getById(Long id) {
        return familyService.getById(id);
    }
}
