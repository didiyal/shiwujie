package com.swj.shiwujie.data.model;

public class BlindCommunityJoinRequest {
    private Long communityId;

    public BlindCommunityJoinRequest() {}

    public BlindCommunityJoinRequest(Long communityId) {
        this.communityId = communityId;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }
} 