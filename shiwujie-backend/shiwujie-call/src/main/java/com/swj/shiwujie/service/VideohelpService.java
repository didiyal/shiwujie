package com.swj.shiwujie.service;

import com.swj.shiwujie.model.domain.call.Videohelp;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.domain.user.Blind;

import java.util.List;

/**
* @author Administrator
* @description 针对表【VideoHelp(视频求助表)】的数据库操作Service
* @createDate 2025-07-11 21:26:52
*/
public interface VideohelpService extends IService<Videohelp> {

    /**
     * 志愿者加入匹配
     * redis有加入,无创建
     * @param loginVolunteerId 登录志愿者id
     * @param loginUserPhone 志愿者手机号
     * @return 是否成功
     */
    boolean createVideohelp(Long loginVolunteerId, String loginUserPhone);


    /**
     * 志愿者退出匹配
     * @param loginVolunteerId 登录志愿者id
     * @param loginUserPhone 志愿者手机号
     * @return 是否成功
     */
    boolean removeVolunteerFromVideohelp(Long loginVolunteerId, String loginUserPhone);


    /**
     * 视障人士加入匹配
     * @param loginBlindId 登录视障人士id
     * @param loginUserPhone 登录手机号
     * @return 是否成功
     */
    boolean joinVideohelp(Long loginBlindId, String loginUserPhone);



    //region 工具方法


    /**
     * 通过志愿者id查询信息
     * @param volunteerId 志愿者id
     * @return 表信息
     */
    Videohelp getByVolunteerId(Long volunteerId);

    /**
     * 通过志愿者id查询未通话的信息
     *
     * @param volunteerId 志愿者id
     * @return 表信息
     */
    Videohelp getWaitingByVolunteerId(Long volunteerId);

    /**
     * 通过志愿者id查询正在通话的信息
     *
     * @param volunteerId 志愿者id
     * @return 表信息
     */
    List<Videohelp> getHelpingByVolunteerId(Long volunteerId);

    /**
     * 通过盲人id查询信息
     *
     * @param blindId 盲人id
     * @return 表信息
     */
    Videohelp getByBlindId(Long blindId);



    /**
     * 通过志愿者id查询正在通话的信息
     *
     * @param blindId 视障人士id
     * @return 表信息
     */
    List<Videohelp> getHelpingByBlindId(Long blindId);


    //endregion

}
