package com.swj.shiwujie.model.request.community.helppost;

import com.swj.shiwujie.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 求助帖查询请求
 *
 * @author swj
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HelppostQueryRequest extends PageRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private Long communityId;

    private Long volunteerId;

    private Long blindId;

    private String postStatus;

}