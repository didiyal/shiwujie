package com.swj.shiwujie.service.community;


import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.swj.shiwujie.model.domain.user.Blind;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【CommunityManager(社区管理人员表)】的数据库操作Service实现
* @createDate 2025-07-19 01:31:15
*/
public interface InnerCommunitymanagerService {



    /**
     * 通过志愿者id,社区id查询信息
     */
    Communitymanager getByVolunteerIdAndCommunityId(Long volunteerId, Long communityId);


    /**
     * 删除信息
     * @return
     */
    int removeByVolunteerIdAndCommunityId(Long volunteerId,Long communityId);

}




