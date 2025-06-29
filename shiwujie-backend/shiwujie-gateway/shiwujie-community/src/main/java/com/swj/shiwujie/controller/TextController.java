package com.swj.shiwujie.controller;


import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.service.InnerUserService;
import com.swj.shiwujie.utils.LoginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
public class TextController {

    @DubboReference
    private InnerUserService innerUserService;


    @GetMapping("/community/test")
    public void test(HttpServletRequest request){
        User user = innerUserService.getById(LoginUtils.getCurrentUserId(request));
        log.info("测试用户-------"+user.toString());
    }

}
