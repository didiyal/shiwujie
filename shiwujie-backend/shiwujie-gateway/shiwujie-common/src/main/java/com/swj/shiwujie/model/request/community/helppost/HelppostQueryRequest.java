package com.swj.shiwujie.model.request.community.helppost;

import com.swj.shiwujie.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 求助帖查询请求
 *
 * @author swj
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HelppostQueryRequest extends PageRequest {

    private Long communityId;

    private String postStatus;

}