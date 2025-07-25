package com.swj.shiwujie.model.request.community;

import com.swj.shiwujie.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;



/**
 * 社区志愿者查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommunityVolunteerQueryRequest extends PageRequest {


    private Long communityId;

}