package com.swj.shiwujie.model.request.user.blind;

import com.swj.shiwujie.common.PageRequest;
import lombok.Data;


/**
 * 社区视障人士查询请求
 */
@Data
public class CommunityBlindQueryRequest extends PageRequest {

    private Long communityId;

}