package com.swj.shiwujie.service.impl.inner;


import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.service.CommunityService;
import com.swj.shiwujie.service.community.InnerCommunityService;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

/**
 * @author Administrator
 * @description 针对表【Community(社区信息表)】的数据库操作Service实现
 * @createDate 2025-07-18 14:44:37
 */
@DubboService
public class InnerCommunityServiceImpl implements InnerCommunityService {


    @Resource
    private CommunityService communityService;


    @Override
    public Community getById(Long id) {
        return communityService.getById(id);
    }
}




