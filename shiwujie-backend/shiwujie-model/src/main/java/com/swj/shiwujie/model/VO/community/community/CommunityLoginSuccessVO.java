package com.swj.shiwujie.model.VO.community.community;



import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import lombok.Data;

/**
 * 社区注册脱敏返回类
 */
@Data
public class CommunityLoginSuccessVO implements java.io.Serializable{

    private static final long serialVersionUID = 1L;

    /**
     * 脱敏的志愿者信息
     */
    private VolunteerVO volunteer;


    /**
     * 登录token
     */
    private String token;

}
