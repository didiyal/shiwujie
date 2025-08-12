package com.swj.shiwujie.model.request.user.volunteer;

import lombok.Data;

import java.io.Serializable;

@Data
public class VolunteerRemoveFromCommunityRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long communityId;

    private Long volunteerId;
}