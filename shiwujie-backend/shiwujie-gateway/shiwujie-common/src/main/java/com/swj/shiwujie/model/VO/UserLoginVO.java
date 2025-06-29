package com.swj.shiwujie.model.VO;

import com.swj.shiwujie.model.domain.User;
import lombok.Data;

/**
 * 无论一键登录还是密码登录返回的信息
 */
@Data
public class UserLoginVO {
    UserVO user;
    String token;
}
