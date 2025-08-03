package com.swj.shiwujie.data.model;

/**
 * 加入社区请求数据模型
 */
public class CommunityJoinRequest {
    private Long communityId;

    public CommunityJoinRequest() {
    }

    public CommunityJoinRequest(Long communityId) {
        this.communityId = communityId;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }
} 