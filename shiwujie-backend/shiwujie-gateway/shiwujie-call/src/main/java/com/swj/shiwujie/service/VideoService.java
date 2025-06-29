package com.swj.shiwujie.service;

import com.swj.shiwujie.model.VO.VideoVO;
import com.swj.shiwujie.model.domain.Video;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.domain.User;

/**
* @author Administrator
* @description 针对表【channels(视频通话频道表)】的数据库操作Service
* @createDate 2025-03-23 05:29:41
*/
public interface VideoService extends IService<Video> {

    /**
     * 通过用户信息生成频道号
     * @param uid
     * @return
     */
    String generateChannelByUid(String uid);



    /**
     * 通过用户id获取一个视频通话的频道
     * @param currentUserId
     * @return
     */
    VideoVO getBlindChannelByUserId(Long currentUserId);


    /**
     * 志愿者退出通话,更新状态
     * @param currentUserId
     * @return
     */
    Boolean leaveVolunteerChannelByUserId(Long currentUserId);


    /**
     * 盲人退出通话,更新状态
     * @param currentUserId
     * @return
     */
    Boolean leaveBlindChannelByUserId(Long currentUserId);

    /**
     * 获取志愿者的uid与channel
     * @param currentUserId
     * @return
     */
    VideoVO getVolunteerChannelByUserId(Long currentUserId);

    /**
     * 生成uid
     *
     * @param id
     * @return
     */
    String generateUid(Long id);
}
