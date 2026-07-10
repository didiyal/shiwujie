package com.swj.shiwujie.service.impl.inner;


import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.service.CommunityService;
import com.swj.shiwujie.service.community.InnerCommunityService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * @author Administrator
 * @description 针对表【Community(社区信息表)】的数据库操作Service实现
 * @createDate 2025-07-18 14:44:37
 */
@Service
public class InnerCommunityServiceImpl implements InnerCommunityService {


    @Resource
    private CommunityService communityService;


    @Override
    public Community getById(Long id) {
        return communityService.getById(id);
    }

    @Override
    public boolean deleteCommunity(Long communityId, Long volunteerId) {
        return communityService.deleteCommunity(communityId, volunteerId);
    }
}




