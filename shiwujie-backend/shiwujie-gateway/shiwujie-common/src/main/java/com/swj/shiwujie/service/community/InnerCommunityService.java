package com.swj.shiwujie.service.community;


import com.swj.shiwujie.model.domain.community.Community;

/**
* @author Administrator
* @description 针对表【CommunityManager(社区管理人员表)】的数据库操作Service实现
* @createDate 2025-07-19 01:31:15
*/
public interface InnerCommunityService {



    /**
     * 通过id查询信息
     * @param id
     * @return
     */
    Community getById(Long id);



}




