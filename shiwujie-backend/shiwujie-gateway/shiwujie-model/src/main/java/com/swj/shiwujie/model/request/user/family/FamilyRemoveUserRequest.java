package com.swj.shiwujie.model.request.user.family;


import lombok.Data;

import java.io.Serializable;

/**
 * 删除家庭成员(家主操作)
 */

@Data
public class FamilyRemoveUserRequest  implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 家庭ID
     */
    private Long familyId;

    /**
     * 盲人id
     */
    private Long blindId;

    /**
     * 志愿者Id
     */
    private Long volunteerId;


}
