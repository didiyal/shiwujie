package com.swj.shiwujie.socket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.call.SocketVO;
import com.swj.shiwujie.ai.relay.AiWsRelayService;
import com.swj.shiwujie.ai.relay.AiWsTypes;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.user.InnerBlindService;
import com.swj.shiwujie.utils.ApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket处理器
 * 使用Spring WebSocket的@ServerEndpoint注解实现
 */
@ServerEndpoint(value = "/api/ws/call")
@Component
@Slf4j
public class CoordinationSocketHandler {

    /**
     * 保存所有在线连接
     */
    public static Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 保存会话与手机号的映射
     */
    public static Map<Session, String> sessionPhoneMap = new ConcurrentHashMap<>();

    // @ServerEndpoint 实例由 WS 容器按 session new、不经 Spring DI（见 ApplicationContextHolder）。
    // 首个 AI-turn 到达时懒取 bean，按 session 缓存。2b-6 ticket 鉴权后 blindId 改由 ticket 解出。
    private AiWsRelayService aiWsRelayService;
    private InnerBlindService innerBlindService;

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
            case 100: // AI-turn 入站（design 缝 A，AiWsTypes.IN_TURN）：text+position → 流式中继 Python /ai/turn
                handleAiTurn(socketData, session);
                break;
            default:
                // design 4.2 WS 改造：协议消息（ping/login/join/AI-turn）均已有专门处理，不再回显旧逻辑
                // 对所有消息无条件发送的「收到客户端的数据」噪声；未知类型仅记录。
                log.info("收到未知 socket 消息类型：{}", type);
        }
    }

    // region AI-turn 流式中继（design 缝 A）

    /**
     * AI-turn 入站（{@link AiWsTypes#IN_TURN}=100）：App 经 WS 发 text+position → 解 blindId（phone→Blind；
     * 2b-6 ticket 鉴权后改由 ticket 解出）→ 提交 {@link AiWsRelayService#submitRelay} 后台流式中继
     * （Python /ai/turn ndjson → 逐帧推回 session）。未登录/找不到 blind/text 空 → 仅记录、不发。
     */
    private void handleAiTurn(SocketData socketData, Session session) {
        String phone = sessionPhoneMap.get(session);
        if (StrUtil.isBlank(phone)) {
            log.warn("AI-turn 收到但 session 未登录（无 phone），忽略");
            return;
        }
        Blind blind = blindService().getByPhone(phone);
        if (blind == null || blind.getBlindId() == null) {
            log.warn("AI-turn：phone={} 找不到 blind，忽略", phone);
            return;
        }
        String text = socketData.getText();
        if (StrUtil.isBlank(text)) {
            log.warn("AI-turn：text 为空，忽略（phone={}）", phone);
            return;
        }
        log.info("AI-turn 提交中继 blindId={} textLen={}", blind.getBlindId(), text.length());
        relay().submitRelay(session, blind.getBlindId(), text, socketData.getPosition());
    }

    /** 懒取 {@link AiWsRelayService}（@ServerEndpoint 不走 Spring DI，见 ApplicationContextHolder），按 session 缓存。 */
    private AiWsRelayService relay() {
        if (aiWsRelayService == null) {
            aiWsRelayService = ApplicationContextHolder.getBean(AiWsRelayService.class);
        }
        return aiWsRelayService;
    }

    /** 懒取 {@link InnerBlindService}，按 session 缓存。 */
    private InnerBlindService blindService() {
        if (innerBlindService == null) {
            innerBlindService = ApplicationContextHolder.getBean(InnerBlindService.class);
        }
        return innerBlindService;
    }

    // endregion

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
            // 盲人不在线（无 session）：旧代码 this.getResponse(1,"系统错误",socketData) 构造错误响应却
            // 丢弃未发送（死代码 bug）。design ⑫ encode-不抛——调用方（2b-3/2b-4 MCP 工具）应据信令是否
            // 送达决定如何告知 agent（换路 / 告用户），而非在此发 WS。
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
            // 盲人不在线（无 session）：旧代码 this.getResponse(1,"系统错误",socketData) 构造错误响应却
            // 丢弃未发送（死代码 bug）。design ⑫ encode-不抛——调用方（2b-3/2b-4 MCP 工具）应据信令是否
            // 送达决定如何告知 agent（换路 / 告用户），而非在此发 WS。
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
            // 盲人不在线（无 session）：旧代码 this.getResponse(1,"系统错误",socketData) 构造错误响应却
            // 丢弃未发送（死代码 bug）。design ⑫ encode-不抛——调用方（2b-3/2b-4 MCP 工具）应据信令是否
            // 送达决定如何告知 agent（换路 / 告用户），而非在此发 WS。
        }
    }

    /**
     * 通知前端紧急求助确认 token 114（design ⑬ gate ③：prepare() 签 token 后推送，App 显式确认面消费）
     */
    public void noticeEmergencyToken(SocketData socketData) {
        if (sessionMap.containsKey(socketData.getBlindPhone())) {
            Session session = sessionMap.get(socketData.getBlindPhone());
            String response = this.getResponse(0, "紧急确认", socketData);
            sendMessage(session, response);
            log.info("通知前端紧急确认 token - 114");
        } else {
            log.info("紧急确认找不到用户{}socket连接：", socketData.getBlindPhone());
            // 盲人不在线（无 session）：gate ③ 送达失败，encode-不抛——agent 仍可走 confirm() MCP
            // （gate ② 跨轮 token）兜底发 5003；调用方据送达决定换路。
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
            // 盲人不在线（无 session）：旧代码 this.getResponse(1,"系统错误",socketData) 构造错误响应却
            // 丢弃未发送（死代码 bug）。design ⑫ encode-不抛——调用方（2b-3/2b-4 MCP 工具）应据信令是否
            // 送达决定如何告知 agent（换路 / 告用户），而非在此发 WS。
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
            // 盲人不在线（无 session）：旧代码 this.getResponse(1,"系统错误",socketData) 构造错误响应却
            // 丢弃未发送（死代码 bug）。design ⑫ encode-不抛——调用方（2b-3/2b-4 MCP 工具）应据信令是否
            // 送达决定如何告知 agent（换路 / 告用户），而非在此发 WS。
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
            // 盲人不在线（无 session）：旧代码 this.getResponse(1,"系统错误",socketData) 构造错误响应却
            // 丢弃未发送（死代码 bug）。design ⑫ encode-不抛——调用方（2b-3/2b-4 MCP 工具）应据信令是否
            // 送达决定如何告知 agent（换路 / 告用户），而非在此发 WS。
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