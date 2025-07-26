package com.swj.shiwujie.model.request.community.community;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改社区信息请求类
 */
@Data
public class CommunityUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 社区名称
     */
    private String communityName;

    /**
     * 社区介绍
     */
    private String communityDescription;
}