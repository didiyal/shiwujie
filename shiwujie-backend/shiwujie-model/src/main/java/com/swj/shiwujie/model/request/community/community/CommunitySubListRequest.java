package com.swj.shiwujie.model.request.community.community;

import com.swj.shiwujie.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分页查询子社区请求
 *
 * @author swj
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommunitySubListRequest extends PageRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private Long communityId;

}