package com.swj.shiwujie.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.swj.shiwujie.common.BaseResponse;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.socket.WsTicketStore;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WS ticket 签发端点（design WS ticket 鉴权，[auth.md](../../../../docs/architecture/auth.md)）。
 *
 * <p>已鉴权 HTTP（{@code LoginCheckInterceptor} 复用——路径 {@code /api/call/ws/**} 在拦截器
 * {@code addPathPatterns("/api/call/**")} scope 内，免改 WebConfig）换短时一次性 WS ticket：客户端连 WS
 * 前调本端点取 ticket，塞进 type=0 登录消息，{@link com.swj.shiwujie.socket.CoordinationSocketHandler#websocketLogin}
 * 校验 ticket 绑 session（堵 known-issues #7 phone 冒充）。WS upgrade 本身仍零鉴权（jakarta.websocket
 * 不经 DispatcherServlet，HandlerInterceptor 不作用），身份背书全部走 ticket。
 *
 * <p>盲人/志愿者共用：{@code LoginCheckInterceptor} 已注入 {@code loginBlindId}/{@code loginVolunteerId}/
 * {@code phone}，本端点据谁非空定 role 后签票。
 */
@RestController
@RequestMapping("/api/call/ws")
@Slf4j
@Tag(name = "WS ticket 接口")
public class WsTicketController {

    @Resource
    private WsTicketStore wsTicketStore;

    /**
     * 换取 WS ticket（已鉴权用户）。
     * <p>{@code LoginCheckInterceptor} 已校验 JWT，本端点仅读注入的身份签票——不再做 token 解析。</p>
     */
    @PostMapping("/ticket")
    @Operation(summary = "换取 WS ticket（连 WS 前调，复用 JWT 鉴权）")
    public BaseResponse<String> issueTicket(HttpServletRequest request) {
        Long blindId = LoginUtils.getLoginBlindId(request);
        Long volunteerId = LoginUtils.getLoginVolunteerId(request);
        // 拦截器已保登录，两者至少一非空；both-null 为防御性兜底（拦截器 scope 外直访）。
        ThrowUtils.throwIf(ObjUtil.isNull(blindId) && ObjUtil.isNull(volunteerId),
                ErrorCode.NOT_LOGIN);
        String phone = LoginUtils.getLoginUserPhone(request);
        ThrowUtils.throwIf(StrUtil.isBlank(phone), ErrorCode.PARAMS_ERROR, "用户无有效手机号");
        String role = blindId != null ? "blind" : "volunteer";
        String ticket = wsTicketStore.issue(phone, role);
        return ResultUtils.success(ticket);
    }
}
