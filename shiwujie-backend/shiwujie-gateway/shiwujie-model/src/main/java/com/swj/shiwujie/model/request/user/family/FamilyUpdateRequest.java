package com.swj.shiwujie.model.request.user.family;


import lombok.Data;

import java.math.BigDecimal;

/**
 * 家庭信息更新
 */

@Data
public class FamilyUpdateRequest {

    /**
     * 家庭ID
     */
    private Long familyId;

    /**
     * 家庭名字
     */
    private String familyName;

    /**
     * 家庭详细介绍
     */
    private String familyDescription;


}
