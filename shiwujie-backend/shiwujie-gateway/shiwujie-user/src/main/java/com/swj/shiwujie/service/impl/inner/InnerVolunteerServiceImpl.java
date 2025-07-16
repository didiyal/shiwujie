package com.swj.shiwujie.service.impl.inner;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.PhoneUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.mapper.VolunteerMapper;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.service.InnerVolunteerService;
import com.swj.shiwujie.service.VolunteerService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Administrator
 * @description 针对表【Volunteer(视障人士信息表)】的数据库操作Service实现
 * @createDate 2025-07-01 00:21:42
 */
@DubboService
public class InnerVolunteerServiceImpl implements InnerVolunteerService {

    @Resource
    private VolunteerService volunteerService;

    /**
     * 通过手机号查询信息
     *
     * @param id id
     * @return 信息
     */
    @Override
    public Volunteer getById(Long id) {
        if(ObjUtil.isNull(id)){
            return null;
        }
        return volunteerService.getById(id);
    }

    /**
     * 通过手机号查询用户(志愿者)信息
     *
     * @param phone 手机号
     * @return 信息
     */
    @Override
    public Volunteer getByPhone(String phone) {
        // 手机号合法校验
        ThrowUtils.throwIf(!PhoneUtil.isPhone(phone), ErrorCode.PARAMS_ERROR, "输入数据格式错误");
        // 有账号直接登录
        QueryWrapper<Volunteer> volunteerQueryWrapper = new QueryWrapper<>();
        volunteerQueryWrapper.eq("phone", phone);
        return volunteerService.getOne(volunteerQueryWrapper);
    }



    /**
     * 通过家庭id获取用户信息
     * @param familyId 家庭id
     * @return 用户列表
     */
    @Override
    public List<Volunteer> getListByFamilyId(Long familyId) {
        QueryWrapper<Volunteer> volunteerQueryWrapper = new QueryWrapper<>();
        volunteerQueryWrapper.eq("family_id", familyId);
        List<Volunteer> volunteerList = volunteerService.list(volunteerQueryWrapper);
        return volunteerList;
    }


}




