package com.swj.shiwujie.model.request.user.volunteer;

import com.swj.shiwujie.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;


/**
 * 社区志愿者查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommunityVolunteerQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long communityId;

}