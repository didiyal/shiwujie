package com.swj.shiwujie.ai.relay;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Python {@code /ai/turn} ndjson 单行事件（design ⑥ 动态反馈协议）。
 *
 * <p>四种 type（详见 shiwujie-ai {@code streaming.sse_line}）：</p>
 * <ul>
 *   <li>{@code turn_start}：携带 {@code issuing_turn}（Python 内部轮次号，emergency turn-bound 用，Java 不消费）。</li>
 *   <li>{@code progress}：携带 {@code event}（searching/thinking/recognizing_photo/routing）。</li>
 *   <li>{@code delta}：携带 {@code text}（末答切块，chunk-2a 模拟 / 2c 真 token）。</li>
 *   <li>{@code turn_end}：携带 {@code issuing_turn}。</li>
 * </ul>
 */
@Data
@AllArgsConstructor
public class AiTurnEvent {


    /** turn_start / progress / delta / turn_end */
    private final String type;

    /** delta 的文本片段 */
    private final String text;

    /** progress 的事件名 */
    private final String event;

    /** turn_start / turn_end 携带的轮次号 */
    private final Integer issuingTurn;


    /**
     * 解析 ndjson 一行为事件。空行 → null；非法 JSON 抛异常（调用方按需 skip + 记录）。
     */
    public static AiTurnEvent parse(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        JSONObject o = JSONUtil.parseObj(line);
        return new AiTurnEvent(
                o.getStr("type"),
                o.getStr("text"),
                o.getStr("event"),
                o.getInt("issuing_turn"));
    }


}
