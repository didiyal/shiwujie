package com.swj.shiwujie.service.community;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.model.VO.community.activitysign.ActivitysignVO;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignAddRequest;
import com.swj.shiwujie.model.request.community.activitysign.ActivitySignQueryRequest;


/**
 * 内部活动报名签到服务
*/
public interface InnerActivitysignService{

    /**
     * 视障人士添加活动报名签到
     *
     * @param activitySignAddRequest 活动报名请求
     * @return 是否成功
     */
    boolean addActivitySign(ActivitySignAddRequest activitySignAddRequest);


    /**
     * 分页查询活动下的报名签到VO
     *
     * @param activitySignQueryRequest 活动报名查询请求
     * @return 活动报名签到VO分页
     */
    Page<ActivitysignVO> listActivitySignByActivity(ActivitySignQueryRequest activitySignQueryRequest);

}
