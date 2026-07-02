package com.swj.shiwujie.service.impl.inner;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.model.VO.community.activitysign.ActivitysignVO;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignAddRequest;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignQueryRequest;
import com.swj.shiwujie.service.ActivitysignService;
import com.swj.shiwujie.service.community.InnerActivitysignService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
* 内部服务实现类
*/
@DubboService
public class InnerActivitysignServiceImpl implements InnerActivitysignService {


    @Resource
    private ActivitysignService activitysignService;


    @Override
    public boolean addActivitySign(ActivitySignAddRequest activitySignAddRequest) {
        return activitysignService.addActivitySign(activitySignAddRequest);
    }


    @Override
    public Page<ActivitysignVO> listActivitySignByActivity(ActivitySignQueryRequest activitySignQueryRequest) {
        return activitysignService.listActivitySignByActivity(activitySignQueryRequest);
    }



}




