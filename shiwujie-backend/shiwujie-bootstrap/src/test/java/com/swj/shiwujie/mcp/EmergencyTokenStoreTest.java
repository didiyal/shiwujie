package com.swj.shiwujie.mcp;

import com.swj.shiwujie.utils.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link EmergencyTokenStore} 单元测试——直测 design ⑬ 红队 Q18 核心护栏（turn-bound token）。
 *
 * <p>@Mock {@link RedisUtils}，聚焦：
 * <ul>
 *   <li>issue：存 Redis（key 前缀 {@code ai:emerg:} + TTL 300s + JSON 含 blindId/issuingTurn）；</li>
 *   <li>verify 同轮拒（红队 Q18 核心护栏，token 不消费）；</li>
 *   <li>verify 跨轮通（一次性消费，del key）；</li>
 *   <li>verify 未知 token / 错用户 / 异常轮序 / 损坏值 → 拒。</li>
 * </ul>
 */
@DisplayName("EmergencyTokenStore turn-bound token（红队 Q18）")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmergencyTokenStoreTest {

    @Mock
    private RedisUtils redisUtils;

    private EmergencyTokenStore store;

    @BeforeEach
    void setUp() {
        store = new EmergencyTokenStore();
        ReflectionTestUtils.setField(store, "redisUtils", redisUtils);
    }

    /** 模拟 issue 后 Redis 里该 token 的绑定 JSON（与 store 序列化格式一致）。 */
    private void stubStored(String token, long blindId, int turn) {
        when(redisUtils.getFromRedis("ai:emerg:" + token))
                .thenReturn("{\"blindId\":" + blindId + ",\"issuingTurn\":" + turn + "}");
    }

    // ───── issue ─────

    @Test
    @DisplayName("issue → 存 Redis key 前缀 + TTL 300s + JSON 含 blindId/turn，返 EMERG- token")
    void issue_storesToRedis() {
        String token = store.issue(123L, 2);

        assertThat(token).startsWith("EMERG-");
        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valCap = ArgumentCaptor.forClass(String.class);
        verify(redisUtils).setToRedis(keyCap.capture(), valCap.capture(), eq(300L), eq(TimeUnit.SECONDS));
        assertThat(keyCap.getValue()).startsWith("ai:emerg:EMERG-");
        assertThat(valCap.getValue()).contains("\"blindId\":123").contains("\"issuingTurn\":2");
    }

    // ───── verify 红队 Q18 核心护栏 ─────

    @Test
    @DisplayName("verify 同轮（issuingTurn==currentTurn）→ 拒，token 不消费")
    void verify_sameTurn_rejected_notConsumed() {
        stubStored("EMERG-X", 123L, 2);

        EmergencyTokenStore.VerifyResult r = store.verify("EMERG-X", 123L, 2);

        assertThat(r.ok()).isFalse();
        assertThat(r.message()).contains("同轮");
        verify(redisUtils, never()).removeToRedis(any());
    }

    @Test
    @DisplayName("verify 跨轮（currentTurn>issuingTurn）→ 通过，一次性消费（del key）")
    void verify_crossTurn_passed_consumed() {
        stubStored("EMERG-X", 123L, 2);

        EmergencyTokenStore.VerifyResult r = store.verify("EMERG-X", 123L, 3);

        assertThat(r.ok()).isTrue();
        assertThat(r.message()).contains("信令5003");
        verify(redisUtils).removeToRedis("ai:emerg:EMERG-X");
    }

    @Test
    @DisplayName("verify turn=0（fail-closed，Python 未跨进程传 turn）→ prepare 存 0、confirm 同轮 0 拒")
    void verify_failClosed_turn0() {
        stubStored("EMERG-X", 123L, 0);

        EmergencyTokenStore.VerifyResult r = store.verify("EMERG-X", 123L, 0);

        assertThat(r.ok()).isFalse();
        verify(redisUtils, never()).removeToRedis(any());
    }

    @Test
    @DisplayName("verify 未知 token（getFromRedis null）→ 拒")
    void verify_unknownToken_rejected() {
        when(redisUtils.getFromRedis("ai:emerg:EMERG-NOPE")).thenReturn(null);

        EmergencyTokenStore.VerifyResult r = store.verify("EMERG-NOPE", 123L, 5);

        assertThat(r.ok()).isFalse();
        assertThat(r.message()).contains("不存在");
        verify(redisUtils, never()).removeToRedis(any());
    }

    @Test
    @DisplayName("verify 错用户（token 绑 blindId≠当前）→ 拒，不消费")
    void verify_wrongUser_rejected() {
        stubStored("EMERG-X", 123L, 2);

        EmergencyTokenStore.VerifyResult r = store.verify("EMERG-X", 999L, 5);

        assertThat(r.ok()).isFalse();
        assertThat(r.message()).contains("不属于当前用户");
        verify(redisUtils, never()).removeToRedis(any());
    }

    @Test
    @DisplayName("verify 异常轮序（issuingTurn>currentTurn）→ 拒")
    void verify_futureTurn_rejected() {
        stubStored("EMERG-X", 123L, 5);

        EmergencyTokenStore.VerifyResult r = store.verify("EMERG-X", 123L, 3);

        assertThat(r.ok()).isFalse();
        assertThat(r.message()).contains("晚于当前轮");
    }

    @Test
    @DisplayName("verify 损坏值（非 JSON）→ 拒")
    void verify_corruptValue_rejected() {
        when(redisUtils.getFromRedis("ai:emerg:EMERG-X")).thenReturn("not-json");

        EmergencyTokenStore.VerifyResult r = store.verify("EMERG-X", 123L, 5);

        assertThat(r.ok()).isFalse();
        assertThat(r.message()).contains("格式异常");
    }

    @Test
    @DisplayName("verify 值非 String（JDK 反序列化异常对象）→ 拒")
    void verify_nonStringValue_rejected() {
        when(redisUtils.getFromRedis("ai:emerg:EMERG-X")).thenReturn(new Object());

        EmergencyTokenStore.VerifyResult r = store.verify("EMERG-X", 123L, 5);

        assertThat(r.ok()).isFalse();
        verify(redisUtils, never()).removeToRedis(any());
    }

    // ───── consumeByApp（chunk-2e-4 gate ③，App 显式确认面消费；非-MCP HTTP 端点调用）─────

    @Test
    @DisplayName("consumeByApp token 存在 + 用户匹配 → 通过 + 一次性消费（不查轮次）")
    void consumeByApp_matched_passed_consumed() {
        stubStored("EMERG-X", 123L, 2);

        EmergencyTokenStore.VerifyResult r = store.consumeByApp("EMERG-X", 123L);

        assertThat(r.ok()).isTrue();
        assertThat(r.message()).contains("信令5003");
        verify(redisUtils).removeToRedis("ai:emerg:EMERG-X");
    }

    @Test
    @DisplayName("consumeByApp 不查轮次——即使 verify 会判同轮拒的 token，App 人工确认仍放行（gate ③ 超越 gate ②）")
    void consumeByApp_ignoresTurn_gate3SupersedesGate2() {
        // token 绑 turn=2；若走 verify(currentTurn=2) 会被 gate ② 同轮拒。但 consumeByApp 是 App 屏幕确认路径，
        // 人工点击天然跨轮 + 用户知情，不做 same-turn 检查 → 直接放行消费。
        stubStored("EMERG-X", 123L, 2);

        EmergencyTokenStore.VerifyResult r = store.consumeByApp("EMERG-X", 123L);

        assertThat(r.ok()).isTrue();
        verify(redisUtils).removeToRedis("ai:emerg:EMERG-X");
    }

    @Test
    @DisplayName("consumeByApp 未知 token → 拒，不消费")
    void consumeByApp_unknownToken_rejected() {
        when(redisUtils.getFromRedis("ai:emerg:EMERG-NOPE")).thenReturn(null);

        EmergencyTokenStore.VerifyResult r = store.consumeByApp("EMERG-NOPE", 123L);

        assertThat(r.ok()).isFalse();
        assertThat(r.message()).contains("不存在");
        verify(redisUtils, never()).removeToRedis(any());
    }

    @Test
    @DisplayName("consumeByApp 错用户（token 绑 blindId≠当前登录）→ 拒，不消费")
    void consumeByApp_wrongUser_rejected() {
        stubStored("EMERG-X", 123L, 2);

        EmergencyTokenStore.VerifyResult r = store.consumeByApp("EMERG-X", 999L);

        assertThat(r.ok()).isFalse();
        assertThat(r.message()).contains("不属于当前用户");
        verify(redisUtils, never()).removeToRedis(any());
    }

    @Test
    @DisplayName("issue Redis 写抛 → 包 IllegalStateException（caller SignalMcpTools encode-不抛兜底）")
    void issue_redisThrows_propagates() {
        org.mockito.Mockito.doThrow(new RuntimeException("Redis down"))
                .when(redisUtils).setToRedis(any(), any(), anyLong(), any(TimeUnit.class));

        try {
            store.issue(123L, 2);
            org.assertj.core.api.Assertions.fail("应抛 IllegalStateException");
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage()).contains("token 签发失败");
        }
    }
}
