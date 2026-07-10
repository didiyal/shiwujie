package com.swj.shiwujie.socket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.call.SocketVO;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.call.SocketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket处理器
 * 使用Spring WebSocket的@ServerEndpoint注解实现
 */
@ServerEndpoint(value = "/ws/call")
@Component
@Slf4j
public class CoordinationSocketHandler {

    /**
     * 保存所有在线连接
     */
    public static Map<String, Session> sessionMap = new HashMap<>();

    /**
     * 保存会话与手机号的映射
     */
    public static Map<Session, String> sessionPhoneMap = new HashMap<>();

    /**
     * 连接建立成功调用的方法
     * @param session WebSocket会话
     */
    @OnOpen
    public void onOpen(Session session) {
        log.info("与客户端建立连接，通道开启！");
    }

    /**
     * 连接关闭调用的方法
     * @param session WebSocket会话
     */
    @OnClose
    public void onClose(Session session) {
        String phone = sessionPhoneMap.get(session);
        if (StrUtil.isNotBlank(phone)) {
            sessionMap.remove(phone);
            sessionPhoneMap.remove(session);
            log.info(phone + "与客户端断开连接，通道关闭！");
        }
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message 客户端发送的消息
     * @param session WebSocket会话
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info(String.format("收到客户端%s的数据：%s", sessionPhoneMap.get(session), message));

        // 解析消息
        SocketData socketData = JSONUtil.toBean(message, SocketData.class);
        Integer type = socketData.getRequestType();
        switch (type) {
            case -1:   // ping
                ping(socketData);
                break;
            case 0:   // 建立连接
                websocketLogin(socketData, session);
                break;
            case 2: // 志愿者初始化成功
                toBlindJoin(socketData);
                break;
        }
        // 回复客户端
        sendMessage(session, "收到客户端的数据");
    }

    /**
     * 发生错误时调用
     * @param session WebSocket会话
     * @param error 错误信息
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket发生错误：", error);
        String phone = sessionPhoneMap.get(session);
        if (StrUtil.isNotBlank(phone)) {
            sessionMap.remove(phone);
            sessionPhoneMap.remove(session);
            log.info(phone + "因错误断开连接！");
        }
    }

    // region 初始化与心跳

    /**
     * ping - -1
     * @param socketData socketData
     */
    private void ping(SocketData socketData) {
        String phone = socketData.getBlindPhone();
        if (StrUtil.isBlankIfStr(phone)) {
            phone = socketData.getVolunteerPhone();
        }
        
        if (sessionMap.containsKey(phone)) {
            Session session = sessionMap.get(phone);
            String response = this.getResponse(0, "pong", -1, socketData);
            sendMessage(session, response);
            log.info(phone + "心跳 - -1");
        }
    }

    /**
     * 初始化socket登录 - 0
     * 返回type,0
     *
     * @param socketData socketData
     * @param session WebSocket会话
     */
    private void websocketLogin(SocketData socketData, Session session) {
        String phone = socketData.getVolunteerPhone();
        if (StrUtil.isBlankIfStr(phone)) {
            phone = socketData.getBlindPhone();
        }
        sessionMap.put(phone, session);
        sessionPhoneMap.put(session, phone);
        log.info(phone + "登录");

        String response = this.getResponse(0, "初始化成功", 0, socketData);
        sendMessage(session, response);
    }

    // endregion

    // region 视频通话

    /**
     * 匹配成功,向志愿者发送信息 1
     *
     * @param socketData socketData
     */
    public void matchSuccess(SocketData socketData) {
        if (sessionMap.containsKey(socketData.getVolunteerPhone())) {
            Session session = sessionMap.get(socketData.getVolunteerPhone());
            String response = this.getResponse(0, "匹配成功", 1, socketData);
            sendMessage(session, response);
            log.info("匹配成功,向志愿者发送信息 - 1");
        } else {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"该用户未连接服务器");
        }
    }

    /**
     * 志愿者初始化成功,向盲人转发 2
     * @param socketData socketData
     */
    private void toBlindJoin(SocketData socketData) {
        if (sessionMap.containsKey(socketData.getBlindPhone())) {
            Session session = sessionMap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "志愿者视频初始化成功", 2, socketData);
            sendMessage(session, response);
        } else {
            Session session = sessionMap.get(socketData.getVolunteerPhone());
            String response = this.getResponse(1, "系统错误", 2, socketData);
            sendMessage(session, response);
        }
        log.info("志愿者初始化成功,向盲人转发 - 2");
    }

    /**
     * 盲人向家属紧急求助,向家属转发 3
     * @param volunteerList 家属列表
     * @param socketData 返回类型
     */
    public void urgenthelpToFamily(List<Volunteer> volunteerList, SocketData socketData) {
        for (Volunteer volunteer : volunteerList) {
            String phone = volunteer.getPhone();
            if (ObjUtil.isNotNull(phone)) {
                if (sessionMap.containsKey(phone)) {
                    Session session = sessionMap.get(phone);
                    socketData.setVolunteerPhone(phone);
                    socketData.setChannelId(volunteer.getVolunteerId());
                    String response = this.getResponse(0, "紧急求助", 3, socketData);
                    sendMessage(session, response);
                }
            }
        }
        log.info("盲人向家属紧急求助,向家属转发 - 3");
    }

    /**
     * 盲人取消求助通知 4
     * @param volunteerList 家属列表
     * @param socketData 返回类型
     */
    public void cancelUrgenthelp(List<Volunteer> volunteerList, SocketData socketData) {
        for (Volunteer volunteer : volunteerList) {
            String phone = volunteer.getPhone();
            if (ObjUtil.isNotNull(phone)) {
                if (sessionMap.containsKey(phone)) {
                    Session session = sessionMap.get(phone);
                    socketData.setVolunteerPhone(phone);
                    socketData.setChannelId(volunteer.getVolunteerId());
                    String response = this.getResponse(0, "家属取消紧急求助", 4, socketData);
                    sendMessage(session, response);
                }
            }
        }
        log.info("盲人取消求助通知 - 4");
    }

    // endregion

    //region AI与前端联动

    /**
     * 通知前端拍照识别 5001
     */
    public void noticeTakePhoto(SocketData socketData) {
        if (sessionMap.containsKey(socketData.getBlindPhone())) {
            Session session = sessionMap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "拍照识别", socketData);
            sendMessage(session, response);
            log.info("通知前端拍照识别 - 5001");
        } else {
            log.info("拍照识别找不到用户socket连接：{}", socketData.getBlindPhone());
            this.getResponse(1, "系统错误", socketData);
        }
    }

    /**
     * 通知前端视频求助 5002
     */
    public void noticeVideoHelp(SocketData socketData) {
        if (sessionMap.containsKey(socketData.getBlindPhone())) {
            Session session = sessionMap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "视频求助", socketData);
            sendMessage(session, response);
            log.info("通知前端视频求助 - 5002");
        } else {
            log.info("视频求助找不到用户{}socket连接：", socketData.getBlindPhone());
            this.getResponse(1, "系统错误", socketData);
        }
    }

    /**
     * 通知前端紧急求助 5003
     */
    public void noticeUrgentHelp(SocketData socketData) {
        if (sessionMap.containsKey(socketData.getBlindPhone())) {
            Session session = sessionMap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "紧急求助", socketData);
            sendMessage(session, response);
            log.info("通知前端紧急求助 - 5003");
        } else {
            log.info("紧急求助找不到用户{}socket连接：", socketData.getBlindPhone());
            this.getResponse(1, "系统错误", socketData);
        }
    }

    /**
     * 通知前端跳转软件 5004
     */
    public void noticeJumpSoftware(SocketData socketData) {
        if (sessionMap.containsKey(socketData.getBlindPhone())) {
            Session session = sessionMap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "跳转软件", socketData);
            sendMessage(session, response);
            log.info("通知前端跳转软件 - 5004");
        } else {
            log.info("跳转软件找不到用户{}socket连接：", socketData.getBlindPhone());
            this.getResponse(1, "系统错误", socketData);
        }
    }

    /**
     * 通知前端跳转到用户修改页面 5005
     */
    public void noticeJumpToUserUpdate(SocketData socketData) {
        if (sessionMap.containsKey(socketData.getBlindPhone())) {
            Session session = sessionMap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "跳转到用户修改页面", socketData);
            sendMessage(session, response);
            log.info("通知前端跳转到用户修改页面 - 5005");
        } else {
            log.info("跳转到用户修改页面找不到用户{}socket连接：", socketData.getBlindPhone());
            this.getResponse(1, "系统错误", socketData);
        }
    }

    /**
     * 通知前端导航 5006
     */
    public void noticeNavigation(SocketData socketData) {
        if (sessionMap.containsKey(socketData.getBlindPhone())) {
            Session session = sessionMap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "开启导航", socketData);
            sendMessage(session, response);
            log.info("通知前端开启导航 - 5006");
        } else {
            log.info("导航找不到用户{}socket连接：", socketData.getBlindPhone());
            this.getResponse(1, "系统错误", socketData);
        }
    }

    //endregion

    /**
     * 发送消息
     * @param session WebSocket会话
     * @param message 消息内容
     */
    private void sendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("发送消息失败：", e);
        }
    }

    private String getResponse(Integer code, String message, Integer requestType, SocketData socketData) {
        SocketVO socketVO = new SocketVO();
        socketVO.setSocketData(socketData);
        socketVO.setCode(code);
        socketVO.setMessage(message);
        socketData.setRequestType(requestType);
        socketVO.setSocketData(socketData);
        return JSONUtil.toJsonStr(socketVO);
    }

    private String getResponse(Integer code, String message, SocketData socketData) {
        SocketVO socketVO = new SocketVO();
        socketVO.setSocketData(socketData);
        socketVO.setCode(code);
        socketVO.setMessage(message);
        return JSONUtil.toJsonStr(socketVO);
    }

}