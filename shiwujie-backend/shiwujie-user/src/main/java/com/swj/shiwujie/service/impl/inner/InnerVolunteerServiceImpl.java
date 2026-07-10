package com.swj.shiwujie.service.impl.inner;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.PhoneUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.mapper.VolunteerMapper;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.service.VolunteerService;
import com.swj.shiwujie.service.user.InnerVolunteerService;
import com.swj.shiwujie.utils.JwtUtils;
import com.swj.shiwujie.utils.RedisUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.swj.shiwujie.constants.UserConstants.REDIS_SECRETKEY;
import static com.swj.shiwujie.constants.UserConstants.TOKEN_SECRETKEY;

/**
 * @author Administrator
 * @description 针对表【Volunteer(视障人士信息表)】的数据库操作Service实现
 * @createDate 2025-07-01 00:21:42
 */
@Service
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
     * 插入数据
     *
     * @param volunteer 志愿者信息
     * @return 信息
     */
    @Override
    public boolean save(Volunteer volunteer) {
        if(ObjUtil.isNull(volunteer)){
            return false;
        }
        return volunteerService.save(volunteer);
    }



    /**
     * 更新志愿者信息
     * @param volunteer 要更新的志愿者
     * @return 是否成功
     */
    @Override
    public boolean updateById(Volunteer volunteer) {
        return volunteerService.updateById(volunteer);
    }

    /**
     * 通过手机号查询用户(志愿者)信息
     *
     * @param phone 手机号
     * @return 信息
     */
    @Override
    public Volunteer getByPhone(String phone) {
        return volunteerService.getByPhone(phone);
    }



    /**
     * 通过家庭id获取用户信息
     * @param familyId 家庭id
     * @return 用户列表
     */
    @Override
    public List<Volunteer> getListByFamilyId(Long familyId) {
        return volunteerService.getListByFamilyId(familyId);
    }


    /**
     * 登录成功实现令牌生成与redis储存
     *
     * @param volunteer 盲人信息
     * @return token
     */
    @Override
    public String generateLoginToken(Volunteer volunteer) {
        return volunteerService.generateLoginToken(volunteer);
    }





    /**
     * 用户信息脱敏(不含token)
     *
     * @param newVolunteer 盲人信息
     * @return 脱敏后的信息
     */
    @Override
    public VolunteerVO getVolunteerVO(Volunteer newVolunteer){
        return volunteerService.getVolunteerVO(newVolunteer);
    }


    /**
     * 删除社区后关联的所有用户信息
     */
    @Override
    public boolean removeCommunityId(Long communityId) {
        return volunteerService.removeCommunityId(communityId);
    }


}




