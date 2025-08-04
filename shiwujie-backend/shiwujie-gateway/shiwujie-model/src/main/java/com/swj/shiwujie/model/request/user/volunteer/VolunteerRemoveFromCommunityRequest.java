package com.swj.shiwujie.model.request.user.volunteer;

import lombok.Data;

@Data
public class VolunteerRemoveFromCommunityRequest {
    private Long communityId;

    private Long volunteerId;
}