package com.swj.shiwujie.service;

import com.swj.shiwujie.model.request.UserRegisterRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class UserServiceTest {
    @Resource
    private UserService userService;
@Resource
private UserRegisterRequest userRegisterRequest;
    @Test
    void userRegister() {

    }
}
