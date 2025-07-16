package com.swj.shiwujie.service;

import com.swj.shiwujie.model.domain.call.Urgenthelp;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.domain.call.Urgenthelp;

/**
* @author Administrator
* @description 针对表【UrgentHelp(紧急求助表)】的数据库操作Service
* @createDate 2025-07-11 21:26:52
*/
public interface UrgenthelpService extends IService<Urgenthelp> {

    /**
     * 盲人发起求助
     * @param loginBlindId 登录盲人id
     * @param loginUserPhone 盲人手机号
     * @return 是否成功
     */
    boolean createUrgenthelp(Long loginBlindId, String loginUserPhone);


    /**
     * 视障人士取消求助
     * @param loginBlindId 登录盲人id
     * @param loginUserPhone 盲人手机号
     * @return 是否成功
     */
    boolean removeFromUrgenthelp(Long loginBlindId, String loginUserPhone);


    /**
     * 家属加入帮助
     * @param loginVolunteerId 登录志愿者id
     * @param blindId 求助盲人ID
     * @return 是否成功
     */
    boolean joinUrgenthelp(Long blindId,Long loginVolunteerId,String loginUserPhone);



    //region 工具方法


    /**
     * 通过志愿者id查询信息
     * @param volunteerId 志愿者id
     * @return 表信息
     */
    Urgenthelp getByVolunteerId(Long volunteerId);

    /**
     * 通过志愿者id查询未通话的信息
     *
     * @param volunteerId 志愿者id
     * @return 表信息
     */
    Urgenthelp getWaitingByVolunteerId(Long volunteerId);

    /**
     * 通过志愿者id查询正在通话的信息
     *
     * @param volunteerId 志愿者id
     * @return 表信息
     */
    Urgenthelp getHelpingByVolunteerId(Long volunteerId);

    /**
     * 通过盲人id查询信息
     *
     * @param blindId 盲人id
     * @return 表信息
     */
    Urgenthelp getByBlindId(Long blindId);

    /**
     * 通过志愿者id查询正在等待的信息
     *
     * @param blindId 视障人士id
     * @return 表信息
     */
    Urgenthelp getWaitingByBlindId(Long blindId);

    /**
     * 通过志愿者id查询正在通话的信息
     *
     * @param blindId 视障人士id
     * @return 表信息
     */
    Urgenthelp getHelpingByBlindId(Long blindId);


    //endregion
}
