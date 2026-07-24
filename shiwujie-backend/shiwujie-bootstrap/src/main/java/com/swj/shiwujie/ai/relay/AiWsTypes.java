package com.swj.shiwujie.ai.relay;

/**
 * AI-turn WS 消息 requestType 契约（design 缝 A）。
 *
 * <p>既有 AI→App 信令码 5001-5006（拍照/视频/紧急/跳转/资料/导航）不变；本类定义本次重写新增的
 * AI 对话通道码，二者互不冲突。</p>
 *
 * <ul>
 *   <li>入站（App→Java）：{@link #IN_TURN}=100，socketData 携带 text + position。</li>
 *   <li>出站（Java→App，流式中继 Python ndjson → WS 帧）：
 *     {@link #OUT_DELTA} 末答切块 / {@link #OUT_PROGRESS} 进度 /
 *     {@link #OUT_TURN_END} turn 收尾 / {@link #OUT_ERROR} 中继异常。</li>
 *   <li>出站（Java→App，紧急求助确认门 design ⑬ gate ③）：{@link #OUT_EMERGENCY_TOKEN}=114，
 *     prepare() 签 token 后推送，socketData.text=token，App 显式确认面消费。</li>
 * </ul>
 */
public final class AiWsTypes {


    /** 入站：AI turn 请求（socketData.text + position）。 */
    public static final int IN_TURN = 100;

    /** 出站：末答切块（socketData.text = 文本片段）。 */
    public static final int OUT_DELTA = 110;

    /** 出站：进度事件（socketData.text = searching/thinking/recognizing_photo/routing）。 */
    public static final int OUT_PROGRESS = 111;

    /** 出站：turn 结束（App 据此收尾 TTS）。 */
    public static final int OUT_TURN_END = 112;

    /** 出站：中继/Python 异常（socketData.text = 错误摘要）。 */
    public static final int OUT_ERROR = 113;

    /** 出站：紧急求助确认 token（design ⑬ gate ③，prepare() 签发后推送；socketData.text = token）。 */
    public static final int OUT_EMERGENCY_TOKEN = 114;


    private AiWsTypes() {
    }


}
