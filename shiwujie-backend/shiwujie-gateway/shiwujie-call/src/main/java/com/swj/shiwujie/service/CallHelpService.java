package com.swj.shiwujie.service;

import com.swj.shiwujie.model.VO.CallHelpVO;
import com.swj.shiwujie.model.domain.CallHelp;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.model.request.CallHelpJoinRequest;

import javax.servlet.http.HttpServletRequest;

/**
* @author ldl
* @description 针对表【callHelp(视频通话频道表)】的数据库操作Service
* @createDate 2025-04-12 23:35:39
*/
public interface CallHelpService extends IService<CallHelp> {

    /**
     * 盲人紧急求助
     * 向家人求助
     * @param currentUserId 请求头
     * @return
     */
    CallHelpVO getCallHelpBlindChannel(Long currentUserId);


    /**
     * 加入求助者视频
     * @param callHelpJoinRequest 求助者uid
     * @param currentUserId
     * @return
     */
    CallHelpVO joinCallHelpBlindChannel(CallHelpJoinRequest callHelpJoinRequest, Long currentUserId);


    /**
     * 求助的盲人挂断通话
     * 更新双方状态
     * @param currentUserId
     * @return
     */
    Boolean leaveCallHelpByBlind(Long currentUserId);

    /**
     * 协助家属主动挂断通信
     * @param currentUserId
     * @return
     */
    Boolean leaveCallHelpByHelpUser(Long currentUserId);

//-------------------------------------------------------------------------------------------------------------------------------------



    /**
     * 创建一个初始的callHelp表信息
     * @param blindUser 盲人信息
     * @param channel 频道
     * @param uid uid
     * @return
     */
    CallHelp createNewCallHelpContent(User blindUser,String channel,String uid);


    /**
     * 生成一个uid
     * @return
     */
    String generateUid();


    /**
     * 生成channel
     * @param uid
     * @param userAccount
     * @return
     */
    String generateChannel(String uid,String userAccount);


    /**
     * 获取正在求助的盲人信息
     * @param blindUid
     * @return
     */
    CallHelp getCallHelpWaitBlindByBlindUid(String blindUid);



}
