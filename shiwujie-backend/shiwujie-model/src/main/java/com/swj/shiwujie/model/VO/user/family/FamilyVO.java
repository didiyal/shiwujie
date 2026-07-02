package com.swj.shiwujie.model.VO.user.family;

import com.swj.shiwujie.model.VO.user.blind.BlindVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 家庭信息返回通用类
 * 脱敏
 */
@Data
public class FamilyVO implements Serializable {


    private static final long serialVersionUID = -2796022435981093530L;
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


    /**
     * 家庭创建人信息（关联志愿者表）脱敏
     */
    private VolunteerVO creatorVolunteer;


    /**
     * 家庭成员信息,脱敏(不包含家主)
     */
    private List<VolunteerVO> volunteerVOList;


    /**
     * 家庭成员信息,脱敏
     */
    private List<BlindVO> blindVOList;

}