package com.swj.shiwujie.socket;

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

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WsTicketStore} 单元测试——design WS ticket 鉴权（[auth.md] L81-98，堵 known-issues #7 phone 冒充）。
 *
 * <p>@Mock {@link RedisUtils}，聚焦：
 * <ul>
 *   <li>issue：存 Redis（key 前缀 {@code ai:ws:ticket:} + TTL 60s + JSON 含 phone/role），返 {@code WSTKT-} ticket；</li>
 *   <li>consume：存在 + 一次性消费（del）；未知 / 损坏 / 空 → empty 不消费；</li>
 *   <li>issue Redis 写抛 → IllegalStateException（caller 兜底）。</li>
 * </ul>
 */
@DisplayName("WsTicketStore WS 会话身份背书（堵 phone 冒充）")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WsTicketStoreTest {

    @Mock
    private RedisUtils redisUtils;

    private WsTicketStore store;

    @BeforeEach
    void setUp() {
        store = new WsTicketStore();
        ReflectionTestUtils.setField(store, "redisUtils", redisUtils);
    }

    /** 模拟 issue 后 Redis 里该 ticket 的绑定 JSON（与 store 序列化格式一致）。 */
    private void stubStored(String ticket, String phone, String role) {
        when(redisUtils.getFromRedis("ai:ws:ticket:" + ticket))
                .thenReturn("{\"phone\":\"" + phone + "\",\"role\":\"" + role + "\"}");
    }

    // ───── issue ─────

    @Test
    @DisplayName("issue → 存 Redis key 前缀 + TTL 60s + JSON 含 phone/role，返 WSTKT- token")
    void issue_storesToRedis() {
        String ticket = store.issue("13800138000", "blind");

        assertThat(ticket).startsWith("WSTKT-");
        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valCap = ArgumentCaptor.forClass(String.class);
        verify(redisUtils).setToRedis(keyCap.capture(), valCap.capture(), eq(60L), eq(TimeUnit.SECONDS));
        assertThat(keyCap.getValue()).startsWith("ai:ws:ticket:WSTKT-");
        assertThat(valCap.getValue()).contains("\"phone\":\"13800138000\"").contains("\"role\":\"blind\"");
    }

    // ───── consume ─────

    @Test
    @DisplayName("consume ticket 存在 → 返绑定 phone/role + 一次性消费（del key）")
    void consume_matched_present_consumed() {
        stubStored("WSTKT-ABCD", "13800138000", "blind");

        Optional<WsTicketStore.Bound> r = store.consume("WSTKT-ABCD");

        assertThat(r).isPresent();
        assertThat(r.get().phone()).isEqualTo("13800138000");
        assertThat(r.get().role()).isEqualTo("blind");
        verify(redisUtils).removeToRedis("ai:ws:ticket:WSTKT-ABCD");
    }

    @Test
    @DisplayName("consume 一次性——同 ticket 二次消费返 empty 不再 del（防重复建会话）")
    void consume_oneTime_secondCallEmpty() {
        stubStored("WSTKT-ABCD", "13800138000", "blind");
        // 首次消费后 Redis 已 del（真实场景 getFromRedis 返 null）；二次调 stub 返 null。
        when(redisUtils.getFromRedis("ai:ws:ticket:WSTKT-ABCD")).thenReturn(null);

        Optional<WsTicketStore.Bound> r = store.consume("WSTKT-ABCD");

        assertThat(r).isEmpty();
        // consume 内部只在解析成功后才 del；二次 getFromRedis=null 走 early return，不再 del。
        verify(redisUtils, never()).removeToRedis(any());
    }

    @Test
    @DisplayName("consume 未知 ticket（getFromRedis null）→ empty，不消费")
    void consume_unknownToken_empty() {
        when(redisUtils.getFromRedis("ai:ws:ticket:WSTKT-NOPE")).thenReturn(null);

        assertThat(store.consume("WSTKT-NOPE")).isEmpty();
        verify(redisUtils, never()).removeToRedis(any());
    }

    @Test
    @DisplayName("consume 空/null ticket → empty，不查 Redis、不消费")
    void consume_blankTicket_empty() {
        assertThat(store.consume(null)).isEmpty();
        assertThat(store.consume("")).isEmpty();
        assertThat(store.consume("   ")).isEmpty();
        verify(redisUtils, never()).removeToRedis(any());
    }

    @Test
    @DisplayName("consume 损坏值（非 JSON）→ empty，不消费")
    void consume_corruptValue_empty() {
        when(redisUtils.getFromRedis("ai:ws:ticket:WSTKT-X")).thenReturn("not-json");

        assertThat(store.consume("WSTKT-X")).isEmpty();
        verify(redisUtils, never()).removeToRedis(any());
    }

    @Test
    @DisplayName("consume phone 缺失（JSON 解析通但 phone=null）→ empty，不消费")
    void consume_missingPhone_empty() {
        when(redisUtils.getFromRedis("ai:ws:ticket:WSTKT-X")).thenReturn("{\"phone\":null,\"role\":\"blind\"}");

        assertThat(store.consume("WSTKT-X")).isEmpty();
        verify(redisUtils, never()).removeToRedis(any());
    }

    @Test
    @DisplayName("consume 值非 String（JDK 反序列化异常对象）→ empty")
    void consume_nonStringValue_empty() {
        when(redisUtils.getFromRedis("ai:ws:ticket:WSTKT-X")).thenReturn(new Object());

        assertThat(store.consume("WSTKT-X")).isEmpty();
        verify(redisUtils, never()).removeToRedis(any());
    }

    @Test
    @DisplayName("issue Redis 写抛 → 包 IllegalStateException")
    void issue_redisThrows_propagates() {
        org.mockito.Mockito.doThrow(new RuntimeException("Redis down"))
                .when(redisUtils).setToRedis(any(), any(), anyLong(), any(TimeUnit.class));

        try {
            store.issue("13800138000", "blind");
            org.assertj.core.api.Assertions.fail("应抛 IllegalStateException");
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage()).contains("WS ticket 签发失败");
        }
    }
}
