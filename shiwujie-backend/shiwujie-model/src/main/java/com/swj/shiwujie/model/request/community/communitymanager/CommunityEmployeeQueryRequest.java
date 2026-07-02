package com.swj.shiwujie.model.request.community.communitymanager;

import com.swj.shiwujie.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询社区员工请求类
 */
@Data
public class CommunityEmployeeQueryRequest extends PageRequest implements Serializable {
    /**
     * 社区ID
     */
    private Long communityId;

    private static final long serialVersionUID = 1L;
}