package com.swj.shiwujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Administrator
* @description 针对表【CommunityManager(社区管理人员表)】的数据库操作Service
* @createDate 2025-07-19 01:31:15
*/
public interface CommunitymanagerService extends IService<Communitymanager> {



    /**
     * 通过志愿者id,社区id查询信息
     */
    Communitymanager getByVolunteerIdAndCommunityId(Long volunteerId, Long communityId);



}
