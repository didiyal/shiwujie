package com.swj.shiwujie.service.community;


import com.swj.shiwujie.model.domain.community.Community;

/**
 * 社区内部服务
*/
public interface InnerCommunityService {



    /**
     * 通过id查询信息
     * @param id
     * @return
     */
    Community getById(Long id);




    /**
     * 删除社区
     * @param communityId 社区ID
     * @param volunteerId 操作人ID
     * @return 是否删除成功
     */
    boolean deleteCommunity(Long communityId, Long volunteerId);








}




