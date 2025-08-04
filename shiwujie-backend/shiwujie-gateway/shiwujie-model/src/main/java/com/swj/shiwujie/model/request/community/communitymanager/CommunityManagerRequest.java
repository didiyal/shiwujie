package com.swj.shiwujie.model.request.community.communitymanager;

import lombok.Data;

import java.io.Serializable;

/**
 * 社区管理成员请求类
 */
@Data
public class CommunityManagerRequest implements Serializable {
    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 志愿者ID
     */
    private Long volunteerId;

    /**
     * 社区管理身份
     */
    private String roleName;

    private static final long serialVersionUID = 1L;
}