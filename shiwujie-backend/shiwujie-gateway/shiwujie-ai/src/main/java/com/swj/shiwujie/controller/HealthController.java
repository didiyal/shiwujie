package com.swj.shiwujie.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * ai 调用接口
 */
@RestController
@RequestMapping("/ai")
public class HealthController {


    @GetMapping("/health")
    public String health(){
        return "healthy";
    }


}
