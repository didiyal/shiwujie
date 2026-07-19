package com.swj.shiwujie.mcp;

import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.service.call.InnerSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link SignalMcpTools} 单元测试。
 *
 * <p>用 Testable 子类 override {@code resolveBlindId}/{@code resolveIssuingTurn} 返固定值（避免引
 * mockito-inline mock {@link BlindMcpContext} 静态），@Mock {@link BlindService}/{@link InnerSocket}/
 * {@link EmergencyTokenStore} 经 {@link ReflectionTestUtils} 注入字段。聚焦：
 * <ul>
 *   <li>3 信令工具真身推 WS：requestType + SocketData 载荷（destination/appName 塞 volunteerPhone）正确；</li>
 *   <li>open_app 白名单硬卡（design ⑬）：白名单外拒绝、不推 WS；</li>
 *   <li>design ⑫ encode-不抛：无身份 / 盲人不存在 / service 异常都返 status:error JSON、不抛、不推 WS；</li>
 *   <li>紧急求助拆 prepare/confirm（design ⑬ 红队 Q18，2b-4b）：prepare 签 token 不推 WS；confirm 校验
 *       通过才推 WS 5003，软拒绝返 rejected 不推。</li>
 * </ul>
 */
@DisplayName("SignalMcpTools 信令 MCP 工具")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignalMcpToolsTest {

    @Mock
    private BlindService blindService;

    @Mock
    private InnerSocket innerSocket;

    @Mock
    private EmergencyTokenStore emergencyTokenStore;

    /** Testable 子类：override resolveBlindId/resolveIssuingTurn 返固定值，绕开 BlindMcpContext 静态调用。 */
    static class TestableSignalMcpTools extends SignalMcpTools {
        Long fixedBlindId = 123L;
        Integer fixedTurn = 2;

        @Override
        protected Long resolveBlindId(ToolContext toolContext) {
            return fixedBlindId;
        }

        @Override
        protected int resolveIssuingTurn(ToolContext toolContext) {
            return fixedTurn == null ? 0 : fixedTurn;
        }
    }

    private TestableSignalMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new TestableSignalMcpTools();
        ReflectionTestUtils.setField(tools, "blindService", blindService);
        ReflectionTestUtils.setField(tools, "innerSocket", innerSocket);
        ReflectionTestUtils.setField(tools, "emergencyTokenStore", emergencyTokenStore);
        lenient().when(blindService.getById(123L)).thenReturn(blindWithPhone("13800138000"));
    }

    private Blind blindWithPhone(String phone) {
        Blind b = new Blind();
        b.setPhone(phone);
        return b;
    }

    private SocketData captureSocketData() {
        return ArgumentCaptor.forClass(SocketData.class).getValue();
    }

    // ───── 3 信令真身推 WS ─────

    @Test
    @DisplayName("request_video_help → 推 WS 5002，SocketData.blindPhone + requestType 正确")
    void requestVideoHelp_pushes5002() {
        String r = tools.requestVideoHelp(null);
        assertThat(r).contains("\"status\":\"ok\"");

        ArgumentCaptor<SocketData> cap = ArgumentCaptor.forClass(SocketData.class);
        verify(innerSocket).noticeVideoHelp(cap.capture());
        assertThat(cap.getValue().getBlindPhone()).isEqualTo("13800138000");
        assertThat(cap.getValue().getRequestType()).isEqualTo(5002);
    }

    @Test
    @DisplayName("open_app 白名单内（微信）→ 推 WS 5004，appName 塞 volunteerPhone")
    void openApp_whitelisted_pushes5004() {
        String r = tools.openApp(null, "微信");
        assertThat(r).contains("\"status\":\"ok\"").contains("微信");

        ArgumentCaptor<SocketData> cap = ArgumentCaptor.forClass(SocketData.class);
        verify(innerSocket).noticeJumpSoftware(cap.capture());
        assertThat(cap.getValue().getRequestType()).isEqualTo(5004);
        assertThat(cap.getValue().getVolunteerPhone()).isEqualTo("微信");
    }

    @Test
    @DisplayName("launch_navigation → 推 WS 5006，destination 塞 volunteerPhone")
    void launchNavigation_pushes5006() {
        String r = tools.launchNavigation(null, "市第一医院", "walking");
        assertThat(r).contains("\"status\":\"ok\"").contains("市第一医院");

        ArgumentCaptor<SocketData> cap = ArgumentCaptor.forClass(SocketData.class);
        verify(innerSocket).noticeNavigation(cap.capture());
        assertThat(cap.getValue().getRequestType()).isEqualTo(5006);
        assertThat(cap.getValue().getVolunteerPhone()).isEqualTo("市第一医院");
    }

    // ───── open_app 白名单硬卡（design ⑬）─────

    @Test
    @DisplayName("open_app 白名单外（抖音）→ 拒绝，不推 WS")
    void openApp_notWhitelisted_rejected() {
        String r = tools.openApp(null, "抖音");
        assertThat(r).contains("\"status\":\"error\"").contains("白名单");
        verify(innerSocket, never()).noticeJumpSoftware(any());
    }

    @Test
    @DisplayName("open_app 白名单匹配宽松（英文/别名）：wechat / 电话 / 短信 通过")
    void openApp_whitelistFlexible() {
        for (String app : new String[]{"wechat", "WeChat", "电话", "短信", "通讯录", "高德地图"}) {
            assertThat(tools.openApp(null, app))
                    .as("app=%s 应在白名单内", app)
                    .contains("\"status\":\"ok\"");
        }
    }

    @Test
    @DisplayName("open_app 空白 → 报错，不推")
    void openApp_blank() {
        assertThat(tools.openApp(null, "")).contains("请提供");
        assertThat(tools.openApp(null, null)).contains("请提供");
        assertThat(tools.openApp(null, "  ")).contains("请提供");
        verify(innerSocket, never()).noticeJumpSoftware(any());
    }

    @Test
    @DisplayName("launch_navigation 空白目的地 → 报错")
    void launchNavigation_blank() {
        assertThat(tools.launchNavigation(null, "", "walking")).contains("请提供导航目的地");
        verify(innerSocket, never()).noticeNavigation(any());
    }

    // ───── design ⑫ encode-不抛 ─────

    @Nested
    @DisplayName("无身份（blind_id 缺失）→ encode-不抛：返 error 含 blind_id，不推 WS")
    class NoIdentity {
        @BeforeEach
        void noIdentity() {
            tools.fixedBlindId = null;
        }

        @Test
        void videoHelp() {
            assertThat(tools.requestVideoHelp(null))
                    .contains("\"status\":\"error\"").contains("blind_id");
            verifyNoInteractions(innerSocket);
        }

        @Test
        void openApp() {
            assertThat(tools.openApp(null, "微信"))
                    .contains("\"status\":\"error\"").contains("blind_id");
            verifyNoInteractions(innerSocket);
        }

        @Test
        void launchNav() {
            assertThat(tools.launchNavigation(null, "医院", null))
                    .contains("\"status\":\"error\"").contains("blind_id");
            verifyNoInteractions(innerSocket);
        }

        @Test
        void emergencyPrepare() {
            assertThat(tools.requestEmergencyHelpPrepare(null, "摔倒"))
                    .contains("\"status\":\"error\"").contains("blind_id");
            verify(emergencyTokenStore, never()).issue(anyLong(), anyInt());
            verifyNoInteractions(innerSocket);
        }

        @Test
        void emergencyConfirm() {
            // 非空 token 才会走到身份校验（空 token 先报「请提供确认码」）；noIdentity 应拦在 store.verify 前。
            assertThat(tools.requestEmergencyHelpConfirm(null, "EMERG-X"))
                    .contains("\"status\":\"error\"").contains("blind_id");
            verify(emergencyTokenStore, never()).verify(any(), any(), anyInt());
            verifyNoInteractions(innerSocket);
        }
    }

    @Test
    @DisplayName("盲人不存在（getById null）→ 报错，不推")
    void blindNotFound() {
        tools.fixedBlindId = 999L;
        when(blindService.getById(999L)).thenReturn(null);
        assertThat(tools.requestVideoHelp(null)).contains("未找到当前用户");
        verify(innerSocket, never()).noticeVideoHelp(any());
    }

    @Test
    @DisplayName("service 抛异常 → encode-不抛，返 error 含失败标签")
    void serviceThrows_encodeNotThrow() {
        when(blindService.getById(123L)).thenThrow(new RuntimeException("DB down"));
        String r = tools.requestVideoHelp(null);
        assertThat(r).contains("\"status\":\"error\"").contains("请求视频协助失败");
        verify(innerSocket, never()).noticeVideoHelp(any());
    }

    // ───── 紧急求助拆 prepare/confirm（design ⑬ 红队 Q18，2b-4b）─────

    @Test
    @DisplayName("request_emergency_help_prepare → 签 token（调 store.issue），不推 WS")
    void emergencyPrepare_issuesToken_noPush() {
        when(emergencyTokenStore.issue(123L, 2)).thenReturn("EMERG-ABCD1234");

        String r = tools.requestEmergencyHelpPrepare(null, "我摔倒了");

        assertThat(r).contains("\"status\":\"ok\"").contains("EMERG-ABCD1234").contains("token");
        verify(emergencyTokenStore).issue(123L, 2);
        verifyNoInteractions(innerSocket);
    }

    @Test
    @DisplayName("request_emergency_help_confirm 校验通过 → 推 WS 5003")
    void emergencyConfirm_verified_pushes5003() {
        when(emergencyTokenStore.verify("EMERG-ABCD1234", 123L, 2))
                .thenReturn(EmergencyTokenStore.VerifyResult.pass("已通知所有家属（信令5003）。"));

        String r = tools.requestEmergencyHelpConfirm(null, "EMERG-ABCD1234");

        assertThat(r).contains("\"status\":\"ok\"");
        ArgumentCaptor<SocketData> cap = ArgumentCaptor.forClass(SocketData.class);
        verify(innerSocket).noticeUrgentHelp(cap.capture());
        assertThat(cap.getValue().getRequestType()).isEqualTo(5003);
        assertThat(cap.getValue().getBlindPhone()).isEqualTo("13800138000");
    }

    @Test
    @DisplayName("request_emergency_help_confirm 校验拒绝（同轮/错用户/未知）→ 软拒绝返 rejected，不推 WS")
    void emergencyConfirm_rejected_noPush() {
        when(emergencyTokenStore.verify("EMERG-X", 123L, 2))
                .thenReturn(EmergencyTokenStore.VerifyResult.reject("同轮内不能既 prepare 又 confirm——"));

        String r = tools.requestEmergencyHelpConfirm(null, "EMERG-X");

        assertThat(r).contains("\"status\":\"rejected\"").contains("同轮");
        verify(innerSocket, never()).noticeUrgentHelp(any());
    }

    @Test
    @DisplayName("request_emergency_help_confirm 空 token → 报错，不查 store、不推 WS")
    void emergencyConfirm_blankToken() {
        assertThat(tools.requestEmergencyHelpConfirm(null, ""))
                .contains("请提供确认码").contains("\"status\":\"error\"");
        assertThat(tools.requestEmergencyHelpConfirm(null, "   "))
                .contains("请提供确认码");
        verify(emergencyTokenStore, never()).verify(any(), any(), anyInt());
        verifyNoInteractions(innerSocket);
    }

    @Test
    @DisplayName("request_emergency_help_prepare store 抛异常 → encode-不抛，返 error")
    void emergencyPrepare_storeThrows() {
        when(emergencyTokenStore.issue(123L, 2)).thenThrow(new IllegalStateException("Redis down"));

        String r = tools.requestEmergencyHelpPrepare(null, null);

        assertThat(r).contains("\"status\":\"error\"").contains("生成确认码失败");
        verifyNoInteractions(innerSocket);
    }
}
