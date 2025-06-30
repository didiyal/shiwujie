package com.swj.shiwujie.service;

import com.swj.shiwujie.model.domain.Family;


import java.util.List;

/**
 * @author DELL
 * @description 针对表【family(家庭表)】的数据库操作Service
 * @createDate 2024-12-15 23:26:43
 */

public interface InnerFamilyService {


    /**
     * 校验家庭是否有除自己外的其他用户
     * @param familyId
     * @return
     */
    boolean familyUsersVerify(Long familyId);

    /**
     * 获取家庭用户id列表
     * @param family
     * @return
     */
    List<Long> getFamilyUserIdsList(Family family);


    Family getById(Long id);
}
