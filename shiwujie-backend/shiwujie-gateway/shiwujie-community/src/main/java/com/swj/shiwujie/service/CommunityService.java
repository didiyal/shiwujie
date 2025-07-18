package com.swj.shiwujie.service;

import com.swj.shiwujie.model.VO.community.CommunityLoginSuccessVO;
import com.swj.shiwujie.model.domain.community.Community;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.request.community.CommunityRegisterRequest;

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





    // region 工具方法

    // endregion
}
