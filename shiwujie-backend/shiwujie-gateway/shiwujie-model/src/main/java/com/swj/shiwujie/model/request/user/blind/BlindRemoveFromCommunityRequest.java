package com.swj.shiwujie.model.request.user.blind;

import lombok.Data;

@Data
public class BlindRemoveFromCommunityRequest {
    private Long communityId;

    private Long blindId;
}