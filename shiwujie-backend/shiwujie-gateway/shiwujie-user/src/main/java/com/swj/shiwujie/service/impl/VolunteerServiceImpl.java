package com.swj.shiwujie.service.impl;

import cn.hutool.core.util.PhoneUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.VolunteerMapper;
import com.swj.shiwujie.model.domain.Blind;
import com.swj.shiwujie.model.domain.Volunteer;
import com.swj.shiwujie.service.VolunteerService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
* @author Administrator
* @description 针对表【Volunteer(志愿者信息表)】的数据库操作Service实现
* @createDate 2025-07-01 00:21:42
*/
@Service
public class VolunteerServiceImpl extends ServiceImpl<VolunteerMapper, Volunteer>
    implements VolunteerService{






    // ---------------------------

    @Override
    public Volunteer getByPhone(String phone) {
        // 手机号合法校验
        if(!PhoneUtil.isPhone(phone)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"输入数据格式错误");
        }
        // 有账号直接登录
        QueryWrapper<Volunteer> volunteerQueryWrapper = new QueryWrapper<>();
        volunteerQueryWrapper.eq("phone",phone);
        Volunteer volunteer = this.getOne(volunteerQueryWrapper);
        return volunteer;
    }

}




