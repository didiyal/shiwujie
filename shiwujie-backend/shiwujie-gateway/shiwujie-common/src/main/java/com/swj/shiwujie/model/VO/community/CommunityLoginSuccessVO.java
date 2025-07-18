package com.swj.shiwujie.model.VO.community;



import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;

/**
 * 社区注册脱敏返回类
 */
public class CommunityLoginSuccessVO extends CommunityVO {



    /**
     * 登录token
     */
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
