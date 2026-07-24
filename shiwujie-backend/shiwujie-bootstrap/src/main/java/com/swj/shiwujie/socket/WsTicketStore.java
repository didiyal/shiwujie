package com.swj.shiwujie.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swj.shiwujie.utils.RedisUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * WS ticket 存储（design WS ticket 鉴权，[auth.md](../../../../../../docs/architecture/auth.md) L81-98；
 * 堵 [known-issues #7](../../../../../../shiwujie-backend/docs/known-issues.md) phone 冒充）。
 *
 * <p>现状 {@code @ServerEndpoint("/api/ws/call")} 的 HTTP upgrade 零鉴权——type=0 登录消息信客户端
 * <b>自报 phone</b>，{@link CoordinationSocketHandler#websocketLogin} 直接绑 session，任何人可冒充任意
 * 手机号建 WS 会话、收他人信令（5003 紧急 / 5006 导航）、以他人身份发 AI-turn。修复：盲人/志愿者先经
 * <b>已鉴权 HTTP</b>（复用 JWT 拦截，{@code WsTicketController}）换一张短时一次性 WS ticket（绑 phone+role），
 * type=0 登录消息<b>带 ticket</b>，{@code websocketLogin} 校验 ticket → 取绑定的 phone 绑 session（不再
 * 信自报 phone）。WS 会话身份由 HTTP 鉴权链背书。
 *
 * <p><b>Redis</b>：key {@code ai:ws:ticket:{ticket}}，value JSON {@code {phone, role}}，TTL 60s，
 * {@link #consume} 通过即一次性消费（del，防 ticket 被重复使用建多会话）。ticket = {@code WSTKT-} + 4 字节
 * hex（32 bit），短 TTL + 一次性 + 绑 phone，碰撞可忽略。
 *
 * <p>与 {@link com.swj.shiwujie.mcp.EmergencyTokenStore} 同构（同 RedisUtils 模式），但语义独立：emergency
 * token 绑 {@code (blindId, issuingTurn)} 做 gate ②/③ 轮次闸；ws ticket 绑 {@code (phone, role)} 做会话
 * 身份背书，无轮次概念。
 */
@Service
@Slf4j
public class WsTicketStore {

    /** Redis key 前缀（与 emergency token / checkpoint / 偏好同 db=2，前缀避撞）。 */
    private static final String KEY_PREFIX = "ai:ws:ticket:";

    /** ticket TTL（秒）：足够 App 取 ticket → 连 WS → 发 type=0 登录的窗口；过期重取。 */
    private static final long TTL_SECONDS = 60;

    @Resource
    private RedisUtils redisUtils;

    private final SecureRandom rng = new SecureRandom();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** ticket 校验通过后返给调用方的绑定记录：phone 绑 sessionMap key；role 仅日志/回显用。 */
    public record Bound(String phone, String role) {
    }

    private record Stored(String phone, String role) {
    }

    /**
     * 签发短时一次性 WS ticket（已鉴权 HTTP 端点 {@code WsTicketController} 调）。
     *
     * @param phone 已鉴权用户手机号（{@code LoginUtils.getLoginUserPhone}）
     * @param role  {@code "blind"} / {@code "volunteer"}（据 loginBlindId/loginVolunteerId 谁非空）
     * @return ticket（客户端塞进 type=0 登录消息）
     */
    public String issue(String phone, String role) {
        String ticket = newTicket();
        try {
            String json = MAPPER.writeValueAsString(new Stored(phone, role));
            redisUtils.setToRedis(KEY_PREFIX + ticket, json, TTL_SECONDS, TimeUnit.SECONDS);
            log.info("WS ticket 签发 phone={} role={}", phone, role);
            return ticket;
        } catch (Exception e) {
            log.warn("WS ticket 签发失败 phone={} role={}", phone, role, e);
            throw new IllegalStateException("WS ticket 签发失败", e);
        }
    }

    /**
     * 消费 ticket：存在即一次性消费（del）返绑定记录；缺失/损坏/空返 {@link Optional#empty()}。
     * <p>empty → 调用方（{@code websocketLogin}）拒登录、不绑 session；客户端收不到信令即知失败，重取 ticket 重连。</p>
     *
     * @param ticket type=0 登录消息携带的 ticket
     * @return 绑定记录（phone + role）
     */
    public Optional<Bound> consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        Object raw = redisUtils.getFromRedis(KEY_PREFIX + ticket);
        if (!(raw instanceof String s)) {
            return Optional.empty();
        }
        Stored rec = parse(s);
        if (rec == null || rec.phone == null || rec.phone.isBlank()) {
            return Optional.empty();
        }
        redisUtils.removeToRedis(KEY_PREFIX + ticket); // 一次性消费
        return Optional.of(new Bound(rec.phone, rec.role));
    }

    private Stored parse(String json) {
        try {
            return MAPPER.readValue(json, Stored.class);
        } catch (Exception e) {
            log.warn("WS ticket 解析失败 raw={}", json, e);
            return null;
        }
    }

    /** 随机 ticket：{@code WSTKT-} + 4 字节 hex。 */
    private String newTicket() {
        byte[] b = new byte[4];
        rng.nextBytes(b);
        StringBuilder sb = new StringBuilder("WSTKT-");
        for (byte x : b) {
            sb.append(Character.toUpperCase(Character.forDigit((x >> 4) & 0xF, 16)));
            sb.append(Character.toUpperCase(Character.forDigit(x & 0xF, 16)));
        }
        return sb.toString();
    }
}
