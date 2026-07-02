package com.swj.shiwujie.service;

import com.swj.shiwujie.model.domain.community.Activitysign;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.model.VO.community.activitysign.ActivitysignVO;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignAddRequest;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignQueryRequest;

import javax.servlet.http.HttpServletRequest;

/**
* @author Administrator
* @description 针对表【ActivitySign(活动报名签到表)】的数据库操作Service
* @createDate 2025-07-26 23:37:53
*/
public interface ActivitysignService extends IService<Activitysign> {

    /**
     * 添加活动报名签到
     *
     * @param activitySignAddRequest 活动报名请求
     * @return 是否成功
     */
    boolean addActivitySign(ActivitySignAddRequest activitySignAddRequest);

    /**
     * 通过id查询活动报名签到VO
     *
     * @param signId 活动报名签到ID
     * @return 活动报名签到VO
     */
    ActivitysignVO getActivitySignVOById(Long signId);

    /**
     * 分页查询活动下的报名签到VO
     *
     * @param activitySignQueryRequest 活动报名查询请求
     * @return 活动报名签到VO分页
     */
    Page<ActivitysignVO> listActivitySignByActivity(ActivitySignQueryRequest activitySignQueryRequest);
}
