package com.swj.shiwujie.model.request.community;

import lombok.Data;

import java.io.Serializable;

/**
 * 加入社区请求
 */
@Data
public class CommunityJoinRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long communityId;


}