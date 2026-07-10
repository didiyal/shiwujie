package com.swj.shiwujie.service.impl.inner;



import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.swj.shiwujie.service.CommunitymanagerService;
import com.swj.shiwujie.service.community.InnerCommunitymanagerService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
* @author Administrator
* @description 针对表【CommunityManager(社区管理人员表)】的数据库操作Service实现
* @createDate 2025-07-19 01:31:15
*/
@Service
public class InnerCommunitymanagerServiceImpl implements InnerCommunitymanagerService {

    @Resource
    private CommunitymanagerService communitymanagerService;



    /**
     * 通过志愿者id,社区id查询信息数量
     */
    @Override
    public Long getCountByVolunteerIdAndCommunityId(Long volunteerId, Long communityId) {
        return communitymanagerService.getCountByVolunteerIdAndCommunityId(volunteerId,communityId);
    }




    /**
     * 通过志愿者id,社区id查询信息
     */
    @Override
    public Communitymanager getByVolunteerIdAndCommunityId(Long volunteerId, Long communityId) {
        return communitymanagerService.getByVolunteerIdAndCommunityId(volunteerId,communityId);
    }

    /**
     * 通过志愿者id,社区id删除信息
     */
    @Override
    public int removeByVolunteerIdAndCommunityId(Long volunteerId, Long communityId) {
        return communitymanagerService.removeByVolunteerIdAndCommunityId(volunteerId,communityId);
    }
}




