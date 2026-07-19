package com.swj.shiwujie.ai.relay;

import com.swj.shiwujie.model.VO.call.SocketVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AiTurnEvent#parse}（ndjson 行解析）与 {@link AiWsRelayService#toFrame}（事件→SocketVO 帧映射）
 * 纯逻辑测试。不起 Spring、不连 Python——HTTP/WS 胶水由 chunk-2c 真 qwen 端到端 / 手工 curl 验。
 */
@DisplayName("AI-turn 中继：ndjson 解析 + 帧映射")
class AiWsRelayServiceTest {


    // ---------- AiTurnEvent.parse ----------

    @Nested
    @DisplayName("parse(line): ndjson 行 → 事件")
    class Parse {

        @Test
        @DisplayName("null / 空串 / 纯空白 → null")
        void parse_blankReturnsNull() {
            assertThat(AiTurnEvent.parse(null)).isNull();
            assertThat(AiTurnEvent.parse("")).isNull();
            assertThat(AiTurnEvent.parse("   ")).isNull();
        }

        @Test
        @DisplayName("delta 行：type + text 解出，event/issuingTurn 为 null")
        void parse_delta() {
            AiTurnEvent ev = AiTurnEvent.parse("{\"type\":\"delta\",\"text\":\"你好\"}");
            assertThat(ev.getType()).isEqualTo("delta");
            assertThat(ev.getText()).isEqualTo("你好");
            assertThat(ev.getEvent()).isNull();
            assertThat(ev.getIssuingTurn()).isNull();
        }

        @Test
        @DisplayName("progress 行：type + event 解出")
        void parse_progress() {
            AiTurnEvent ev = AiTurnEvent.parse("{\"type\":\"progress\",\"event\":\"searching\"}");
            assertThat(ev.getType()).isEqualTo("progress");
            assertThat(ev.getEvent()).isEqualTo("searching");
        }

        @Test
        @DisplayName("turn_start 行：type + issuing_turn 解出")
        void parse_turnStart() {
            AiTurnEvent ev = AiTurnEvent.parse("{\"type\":\"turn_start\",\"issuing_turn\":3}");
            assertThat(ev.getType()).isEqualTo("turn_start");
            assertThat(ev.getIssuingTurn()).isEqualTo(3);
        }

        @Test
        @DisplayName("turn_end 行：type + issuing_turn 解出")
        void parse_turnEnd() {
            AiTurnEvent ev = AiTurnEvent.parse("{\"type\":\"turn_end\",\"issuing_turn\":3}");
            assertThat(ev.getType()).isEqualTo("turn_end");
            assertThat(ev.getIssuingTurn()).isEqualTo(3);
        }

        @Test
        @DisplayName("缺 type 字段：不抛，返回 type=null 的事件（toFrame 会跳过）")
        void parse_missingType_returnsNullType() {
            AiTurnEvent ev = AiTurnEvent.parse("{\"text\":\"孤儿\"}");
            assertThat(ev).isNotNull();
            assertThat(ev.getType()).isNull();
            assertThat(ev.getText()).isEqualTo("孤儿");
        }
    }


    // ---------- AiWsRelayService.toFrame ----------

    @Nested
    @DisplayName("toFrame(event): 事件 → SocketVO 帧")
    class ToFrame {

        @Test
        @DisplayName("null 事件 → null")
        void toFrame_null() {
            assertThat(AiWsRelayService.toFrame(null)).isNull();
        }

        @Test
        @DisplayName("type=null → null（不发给 App）")
        void toFrame_nullType() {
            assertThat(AiWsRelayService.toFrame(new AiTurnEvent(null, "x", null, null))).isNull();
        }

        @Test
        @DisplayName("delta → code=0 / requestType=110 / message=AI回复 / text 透传")
        void toFrame_delta() {
            SocketVO vo = AiWsRelayService.toFrame(new AiTurnEvent("delta", "你好", null, null));
            assertThat(vo).isNotNull();
            assertThat(vo.getCode()).isEqualTo(0);
            assertThat(vo.getMessage()).isEqualTo("AI回复");
            assertThat(vo.getSocketData().getRequestType()).isEqualTo(AiWsTypes.OUT_DELTA);
            assertThat(vo.getSocketData().getText()).isEqualTo("你好");
        }

        @Test
        @DisplayName("progress → requestType=111 / text=事件名")
        void toFrame_progress() {
            SocketVO vo = AiWsRelayService.toFrame(new AiTurnEvent("progress", null, "routing", null));
            assertThat(vo.getSocketData().getRequestType()).isEqualTo(AiWsTypes.OUT_PROGRESS);
            assertThat(vo.getSocketData().getText()).isEqualTo("routing");
        }

        @Test
        @DisplayName("turn_end → requestType=112")
        void toFrame_turnEnd() {
            SocketVO vo = AiWsRelayService.toFrame(new AiTurnEvent("turn_end", null, null, 5));
            assertThat(vo.getSocketData().getRequestType()).isEqualTo(AiWsTypes.OUT_TURN_END);
        }

        @Test
        @DisplayName("turn_start → null（Python 内部轮次号，App 不消费）")
        void toFrame_turnStartSkipped() {
            assertThat(AiWsRelayService.toFrame(new AiTurnEvent("turn_start", null, null, 1))).isNull();
        }

        @Test
        @DisplayName("未知 type → null")
        void toFrame_unknownSkipped() {
            assertThat(AiWsRelayService.toFrame(new AiTurnEvent("mystery", null, null, null))).isNull();
        }
    }


}
