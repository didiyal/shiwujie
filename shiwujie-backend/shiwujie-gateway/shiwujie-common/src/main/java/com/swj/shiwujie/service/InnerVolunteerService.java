package com.swj.shiwujie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerLoginSuccessVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerUpdatePasswordRequest;

import java.util.List;

/**
 * @author Administrator
 * @description 针对表【Volunteer(视障人士信息表)】的数据库操作Service
 * @createDate 2025-07-01 00:21:42
 */
public interface InnerVolunteerService  {

    /**
     * 通过id查询信息
     * @param id
     * @return
     */
    Volunteer getById(Long id);

    /**
     * 插入数据
     *
     * @param volunteer 志愿者信息
     * @return 信息
     */
    boolean save(Volunteer volunteer);

    /**
     * 通过手机号查询用户信息
     * @param phone 手机号
     * @return 信息
     */
    Volunteer getByPhone(String phone);


    /**
     * 通过家庭id获取用户信息
     * @param familyId 家庭id
     * @return 用户列表
     */
    List<Volunteer> getListByFamilyId(Long familyId);


}
