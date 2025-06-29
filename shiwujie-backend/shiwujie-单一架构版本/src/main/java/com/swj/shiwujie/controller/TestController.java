package com.swj.shiwujie.controller;
import java.util.Date;


import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.utils.ResultUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class TestController {



    @GetMapping("/get1")
    public String testGET1(@RequestParam String name){
            return "测试结果"+name;
    }


    @GetMapping("/get2")
    public String testGET2(String name){
            return "测试结果"+name;
    }


    @PostMapping("/post1")
    public BaseResponse<String> testPOST1(@RequestParam String name){
        return ResultUtils.success(name);
    }


    @PostMapping("/post2")
    public BaseResponse<User> testPOST2(String name){
        User user = new User();
        user.setUserName(name);
        user.setUserAccount("88888888");
        user.setUserPassword("测试密码");
        user.setUserUrl("*******");
        user.setUserPhone("12345678989");
        user.setUserEmail("");
        user.setGender(0);
        user.setStatus(0);
        user.setIsOnline(0);
        user.setFamilyId(0L);
        user.setUserRole(0);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setIsDelete(0);
        user.setCallStatus(0);
        user.setCallChannel("");
        user.setUserCertificate("");

        return ResultUtils.success(user);
    }


}
