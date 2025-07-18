package com.swj.shiwujie.service;

import com.swj.shiwujie.model.VO.community.CommunityLoginSuccessVO;
import com.swj.shiwujie.model.domain.community.Community;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.request.community.CommunityRegisterRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;

/**
* @author Administrator
* @description 针对表【Community(社区信息表)】的数据库操作Service
* @createDate 2025-07-18 14:44:37
*/
public interface CommunityService extends IService<Community> {

    /**
     * 社区入驻
     *
     * @param communityRegisterRequest 社区注册信息
     * @return 脱敏后的社区信息
     */
    CommunityLoginSuccessVO communityRegister(CommunityRegisterRequest communityRegisterRequest);


    /**
     * 社区登录
     * @param volunteerLARRequest 登录人手机号与密码
     * @return 脱敏后的登录数据
     */
    CommunityLoginSuccessVO communityLogin(VolunteerLARRequest volunteerLARRequest);


    // region 工具方法


    /**
     * 通过社区名查询社区
     * @param communityName 社区名
     * @return 社区
     */
    Community getByName(String communityName);




    // endregion
}
