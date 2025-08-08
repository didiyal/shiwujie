package com.swj.shiwujie.service.community;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.common.PageRequest;
import com.swj.shiwujie.model.VO.community.activity.ActivityVO;
import com.swj.shiwujie.model.request.community.activity.ActivityQueryRequest;

/**
 * 社区活动服务
 */
public interface InnerActivityService {


    /**
     * 通过ID查询活动VO
     *
     * @param activityId      活动ID
     * @return 活动VO
     */
    ActivityVO getActivityVOById(Long activityId);

    /**
     * 盲人 通过社区ID分页查询活动VO
     * @return 活动VO分页
     */
    Page<ActivityVO> listActivitiesByCommunity(ActivityQueryRequest activityQueryRequest);


}