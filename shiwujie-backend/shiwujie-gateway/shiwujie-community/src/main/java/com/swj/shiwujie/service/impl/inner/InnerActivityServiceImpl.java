package com.swj.shiwujie.service.impl.inner;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.model.VO.community.activity.ActivityVO;
import com.swj.shiwujie.model.request.community.activity.ActivityQueryRequest;
import com.swj.shiwujie.service.ActivityService;
import com.swj.shiwujie.service.community.InnerActivityService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 内部服务：活动服务
 */
@DubboService
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

    /**
     * 查询活动VOs
     *
     * @param activityIds
     * @return
     */
    @Override
    public List<ActivityVO> listActivities(Long[] activityIds) {
        return List.of();
    }


}




