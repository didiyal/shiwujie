package com.swj.shiwujie.service;

import com.swj.shiwujie.model.domain.Blind;
import com.swj.shiwujie.model.domain.Volunteer;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Administrator
* @description 针对表【Volunteer(志愿者信息表)】的数据库操作Service
* @createDate 2025-07-01 00:21:42
*/
public interface VolunteerService extends IService<Volunteer> {



    /**
     * 通过手机号查询用户信息
     * @param phone
     * @return
     */
    Volunteer getByPhone(String phone);
}
