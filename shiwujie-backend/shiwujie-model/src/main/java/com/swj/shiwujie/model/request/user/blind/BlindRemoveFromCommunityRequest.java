package com.swj.shiwujie.model.request.user.blind;

import lombok.Data;

import java.io.Serializable;

@Data
public class BlindRemoveFromCommunityRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long communityId;

    private Long blindId;
}