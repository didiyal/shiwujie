package com.swj.shiwujie.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swj.shiwujie.utils.RedisUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 紧急求助 turn-bound token 存储（design ⑬ 红队硬修正 1，Java 侧闸 ②）。
 *
 * <p>红队 Q18 致命洞：qwen 可在单个 AIMessage 发并行 tool_calls，ToolNode 并发执行——一轮内可同时
 * prepare() + confirm(竞态命中真 token)，同轮内发 WS 5003，**确认问题从没问过**。结构修复：token 绑
 * (blind_id, issuing_turn)，{@link #verify} **拒同轮 token**——token 必须在更早轮签发，即确认问题在跨轮
 * 边界被问过（用户在中间轮答了「是」）。配 emergency 工具 parallel_tool_calls=False（Python 侧，
 * gate ①）+ App 显式确认面（chunk-2e，gate ③）= 三道闸。
 *
 * <p><b>token 存 Redis</b>（生产共享态，跨进程/重启不丢）：key {@code ai:emerg:{token}}，value JSON
 * {@code {blindId, issuingTurn}}，TTL 5min，{@link #verify} 通过即一次性消费（del）。沿用项目惯例
 * {@link RedisUtils}（{@code RedisTemplate<String,Object>}，String 值 JDK 序列化可正确往返）。
 *
 * <p><b>fail-closed</b>：issuing_turn 缺失（Python 未跨进程传，chunk-2a stub / chunk-2c 未接）→ 调用方
 * 降级 turn=0 → prepare 存 turn=0 / confirm 校验 turn=0 → 同轮必拒 → **confirm 永拒、绝不误发 5003**。
 *
 * <p>与 Python 侧 {@code shiwujie_ai/safety/emergency.py}（chunk-2a 进程内 stub）语义一致；生产由 Java
 * MCP 工具（{@link SignalMcpTools#requestEmergencyHelpConfirm}）权威执行（真实 WS 5003 在 Java）。
 */
@Service
@Slf4j
public class EmergencyTokenStore {

    /** Redis key 前缀（与 Python 偏好层 / checkpoint 同 db=2，前缀避撞）。 */
    private static final String KEY_PREFIX = "ai:emerg:";

    /** token TTL（秒）：足够跨轮确认窗口，过期重新 prepare。 */
    private static final long TTL_SECONDS = 300;

    @Resource
    private RedisUtils redisUtils;

    private final SecureRandom rng = new SecureRandom();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** verify 结果：{@code ok=true} 放行（已一次性消费 token）；{@code ok=false} 软拒绝（token 仍保留，agent 可重试/解释）。 */
    public record VerifyResult(boolean ok, String message) {
        public static VerifyResult pass(String msg) {
            return new VerifyResult(true, msg);
        }

        public static VerifyResult reject(String msg) {
            return new VerifyResult(false, msg);
        }
    }

    /** Redis 内存储的绑定记录。 */
    private record Stored(long blindId, int issuingTurn) {
    }

    /**
     * 签发 turn-bound token（**不通知家属**，仅生成确认码）。
     *
     * @param blindId      当前盲人 id
     * @param issuingTurn  签发轮次（fail-closed 时 caller 传 0）
     * @return 确认码 token（LLM 读出后传给 confirm）
     */
    public String issue(Long blindId, int issuingTurn) {
        String token = newToken();
        try {
            String json = MAPPER.writeValueAsString(new Stored(blindId, issuingTurn));
            redisUtils.setToRedis(KEY_PREFIX + token, json, TTL_SECONDS, TimeUnit.SECONDS);
            log.info("紧急求助 token 签发 blindId={} turn={}", blindId, issuingTurn);
            return token;
        } catch (Exception e) {
            log.warn("紧急求助 token 签发失败 blindId={} turn={}", blindId, issuingTurn, e);
            throw new IllegalStateException("token 签发失败", e);
        }
    }

    /**
     * 校验 turn-bound token：拒同轮（红队 Q18 核心护栏）/ 拒错用户 / 拒异常轮序；跨轮通过即一次性消费。
     *
     * @param token        LLM 传入的确认码
     * @param blindId      当前盲人 id
     * @param currentTurn  当前轮次（fail-closed 时 caller 传 0）
     * @return {@link VerifyResult}（软拒绝 message 回灌 agent，不抛）
     */
    public VerifyResult verify(String token, Long blindId, int currentTurn) {
        Object raw = redisUtils.getFromRedis(KEY_PREFIX + token);
        if (!(raw instanceof String s)) {
            return VerifyResult.reject("token 不存在或已失效——必须先用 request_emergency_help_prepare 生成。");
        }
        Stored rec = parse(s);
        if (rec == null) {
            return VerifyResult.reject("token 格式异常。");
        }
        if (rec.blindId != blindId) {
            return VerifyResult.reject("token 不属于当前用户。");
        }
        if (rec.issuingTurn == currentTurn) {
            // 🔴 核心护栏（红队 Q18）：同轮 prepare+confirm = 确认问题没机会被问。
            return VerifyResult.reject(
                    "同轮内不能既 prepare 又 confirm——必须先 prepare、向用户确认后，"
                            + "等用户下一轮明确答复再 confirm。");
        }
        if (rec.issuingTurn > currentTurn) {
            return VerifyResult.reject("token 签发轮晚于当前轮（异常，拒绝）。");
        }
        // bound_turn < current_turn → 跨了轮，确认问题在中间轮被问过 → 一次性消费放行。
        redisUtils.removeToRedis(KEY_PREFIX + token);
        return VerifyResult.pass("已通知所有家属（信令5003）。");
    }

    /**
     * App 侧 gate ③ 消费 token（design ⑬ 红队第三道门，chunk-2e-4）。
     *
     * <p>盲人在手机屏幕显式确认面点击确认 → 经非-MCP HTTP 端点（{@code UrgenthelpController /confirm}）
     * 回传 token。人工屏幕确认天然跨轮、天然用户知情，<b>不做 same-turn 检查</b>（gate ② 轮次闸仅约束
     * agent 走 confirm() MCP 路径）。仅校验 token 存在 + 属于当前盲人，通过即一次性消费。</p>
     *
     * <p>非-MCP 端点 = agent 无此路径（堵 agent 自确认）；HTTP 鉴权链解析 loginBlindId 与 token 绑定盲人比对。</p>
     *
     * @param token   App 回传的确认码（来自 114 帧 socketData.text）
     * @param blindId 当前登录盲人 id（LoginUtils.getLoginBlindId）
     * @return {@link VerifyResult}（通过即已消费，调用方推 WS 5003）
     */
    public VerifyResult consumeByApp(String token, Long blindId) {
        Object raw = redisUtils.getFromRedis(KEY_PREFIX + token);
        if (!(raw instanceof String s)) {
            return VerifyResult.reject("确认码不存在或已失效。");
        }
        Stored rec = parse(s);
        if (rec == null) {
            return VerifyResult.reject("确认码格式异常。");
        }
        if (rec.blindId != blindId) {
            return VerifyResult.reject("确认码不属于当前用户。");
        }
        // gate ③ = 人工屏幕点击确认（超越轮次闸），通过即一次性消费。
        redisUtils.removeToRedis(KEY_PREFIX + token);
        return VerifyResult.pass("已通知所有家属（信令5003）。");
    }

    private Stored parse(String json) {
        try {
            return MAPPER.readValue(json, Stored.class);
        } catch (Exception e) {
            log.warn("紧急求助 token 解析失败 raw={}", json, e);
            return null;
        }
    }

    /** 随机 token：{@code EMERG-} + 4 字节 hex（32 bit，一次性 + TTL 5min + 绑 blind_id，碰撞可忽略）。 */
    private String newToken() {
        byte[] b = new byte[4];
        rng.nextBytes(b);
        StringBuilder sb = new StringBuilder("EMERG-");
        for (byte x : b) {
            sb.append(Character.toUpperCase(Character.forDigit((x >> 4) & 0xF, 16)));
            sb.append(Character.toUpperCase(Character.forDigit(x & 0xF, 16)));
        }
        return sb.toString();
    }
}
