package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.swj.shiwujie.service.CommunitymanagerService;
import com.swj.shiwujie.mapper.CommunitymanagerMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【CommunityManager(社区管理人员表)】的数据库操作Service实现
* @createDate 2025-07-19 01:31:15
*/
@Service
public class CommunitymanagerServiceImpl extends ServiceImpl<CommunitymanagerMapper, Communitymanager>
    implements CommunitymanagerService{
    /**
     * 通过志愿者id,社区id查询信息
     */
    @Override
    public Communitymanager getByVolunteerIdAndCommunityId(Long volunteerId, Long communityId) {
        QueryWrapper<Communitymanager> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("volunteer_id", volunteerId).eq("community_id",communityId);
        return this.getOne(queryWrapper);
    }
}




