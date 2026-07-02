package com.swj.shiwujie.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerLoginSuccessVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerUpdatePasswordRequest;

import java.util.List;

/**
 * dubbo 服务提供接口
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
     * 更新志愿者信息
     * @param volunteer 要更新的志愿者
     * @return 是否成功
     */
    public boolean updateById(Volunteer volunteer);




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


    /**
     * 生成登录令牌并返回
     * 令牌包含:id,手机号,社区权限
     * @param volunteer 志愿者信息
     * @return 令牌
     */
    String generateLoginToken(Volunteer volunteer);



    /**
     * 用户信息脱敏(不含token)
     *
     * @param newVolunteer 盲人信息
     * @return 脱敏后的信息
     */
    VolunteerVO getVolunteerVO(Volunteer newVolunteer);



    /**
     * 删除社区后关联的所有用户信息
     */
    boolean removeCommunityId(Long communityId);
}
