package com.swj.shiwujie.service;

import com.swj.shiwujie.model.VO.FamilyVO;
import com.swj.shiwujie.model.domain.Family;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.domain.User;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author DELL
 * @description 针对表【family(家庭表)】的数据库操作Service
 * @createDate 2024-12-15 23:26:43
 */

public interface FamilyService extends IService<Family> {

    /**
     * 添加家庭
     * @param familyName 家庭名称
     * @return
     */
    FamilyVO addFamily(String familyName,Long loginUserId);


    /**
     * 根据家庭号获取家庭
     * @param familyAccount
     * @return
     */
    FamilyVO getFamilyByAccount(String familyAccount,Long loginUserId);


    /**
     * 获取家庭里的用户列表
     * @param family
     * @return
     */
    List<User> getFamilyUsersList(Family family);


    /**
     * 获取家庭用户id列表
     * @param family
     * @return
     */
    List<Long> getFamilyUserIdsList(Family family);

    /**
     * 根据家庭对象获取家庭vo对象
     * @param family
     * @return
     */
    FamilyVO getFamilyVOByFamily(Family family);

    /**
     * 根据家庭号加入家庭(无需验证)
     * @param familyAccount
     * @param currentUserId
     * @return
     */
    FamilyVO joinFamilyBuAccount(String familyAccount, Long currentUserId);

    /**
     * 根据家庭id删除家庭
     * @param id
     * @return
     */
    boolean removeFamilyById(Long id,Long loginUserId);


    /**
     * 用户退出家庭
     * @param loginUserId
     * @return
     */
    boolean userLeaveFromFamily(Long loginUserId);


    /**
     * 校验家庭是否有除自己外的其他用户
     * @param familyId
     * @return
     */
    boolean familyUsersVerify(Long familyId);
}
