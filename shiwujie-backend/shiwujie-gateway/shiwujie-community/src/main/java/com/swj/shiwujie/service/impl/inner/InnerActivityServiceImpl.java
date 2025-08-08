package com.swj.shiwujie.service.impl.inner;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.model.VO.community.activity.ActivityVO;
import com.swj.shiwujie.model.request.community.activity.ActivityQueryRequest;
import com.swj.shiwujie.service.ActivityService;
import com.swj.shiwujie.service.community.InnerActivityService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 内部服务：活动服务
 */
@Service
public class InnerActivityServiceImpl  implements InnerActivityService {

    @Resource
    private ActivityService activityService;


    @Override
    public ActivityVO getActivityVOById(Long activityId) {
        return activityService.getActivityVOById(activityId);
    }

    @Override
    public Page<ActivityVO> listActivitiesByCommunity(ActivityQueryRequest activityQueryRequest) {
        return activityService.listActivitiesByCommunity(activityQueryRequest);
    }


}




