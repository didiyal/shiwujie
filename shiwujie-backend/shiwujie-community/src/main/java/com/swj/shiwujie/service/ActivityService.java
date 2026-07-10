package com.swj.shiwujie.service;

import com.swj.shiwujie.model.domain.community.Activity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.model.VO.community.activity.ActivityVO;
import com.swj.shiwujie.model.request.community.activity.ActivityAddRequest;
import com.swj.shiwujie.model.request.community.activity.ActivityQueryRequest;
import com.swj.shiwujie.model.request.community.activity.ActivityUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author Administrator
* @description 针对表【Activity(活动信息表)】的数据库操作Service
* @createDate 2025-07-26 22:31:08
*/
public interface ActivityService extends IService<Activity> {

    /**
     * 创建活动
     *
     * @param activityAddRequest 活动创建请求
     * @return 活动VO
     */
    ActivityVO addActivity(ActivityAddRequest activityAddRequest, Long longinVolunteerId);

    /**
     * 通过ID查询活动VO
     *
     * @param id      活动ID
     * @return 活动VO
     */
    ActivityVO getActivityVOById(Long id);

    /**
     * 分页查询社区下的活动
     *
     * @param activityQueryRequest 活动查询请求
     * @return 活动VO分页结果
     */
    Page<ActivityVO> listActivitiesByCommunity(ActivityQueryRequest activityQueryRequest);

    /**
     * 删除活动
     *
     * @param activityId      活动ID
     * @return 是否删除成功
     */
    boolean deleteActivity(Long activityId);

    /**
     * 修改活动信息
     *
     * @param activityUpdateRequest 活动更新请求
     * @return 是否修改成功
     */
    boolean updateActivity(ActivityUpdateRequest activityUpdateRequest);
}
