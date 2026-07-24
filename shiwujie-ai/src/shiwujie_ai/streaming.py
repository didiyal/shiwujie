"""流式进度事件 helper —— design ⑥（动态反馈经 StreamingResponse → 2b Java WS → App）。

两件事：
- TOOL_TO_EVENT：工具名 → 进度事件类型映射。盲人单声道 UX——让用户听见「正在搜索/识别/算路
  线…」的动态反馈，而非干等末答。
- sse_line：事件 dict → ndjson 一行。

**delta 不在本模块产**：chunk-2c 起 service 层用双流 `stream_mode=["messages","updates"]`，
agent 节点的 AIMessageChunk.content 逐 token 直接作 delta（真模型 token 流；FakeChatModel 无
stream → LangGraph 吐整条作单 chunk）。早期 chunk-2a 的 `chunk_reply` 字符切块模拟已删（死代码）。

**进度事件仅 4 类**（design ⑥ 枚举）：thinking（思考/读手册）/ searching（搜索/查天气/查 KB）
/ recognizing_photo（拍照识别）/ routing（导航：POI/路线）。

**ndjson 协议**（service 层用 sse_line 序列化，一行一事件；2b Java 按行解析转发 WS）：
    {"type":"turn_start","issuing_turn":N}
    {"type":"progress","event":"searching"}
    {"type":"delta","text":"..."}            ← agent 末答逐 token（messages 流）
    {"type":"turn_end","issuing_turn":N}

实现注：design ⑥ 原文「stream_mode='custom'」——改由 service 层从 stream_mode="updates"
的 agent 节点输出**派生**进度事件（看 tool_calls 决策即发，无需把 writer 注进每个工具体/ToolNode），
对外 ndjson 协议不变（2b 消费面无感）。
"""

import json

# 工具名 → 进度事件。未列 → thinking（默认）。
TOOL_TO_EVENT = {
    "web_search": "searching",
    "get_weather": "searching",
    "search_kb": "searching",
    "recognize_photo": "recognizing_photo",
    "gaode_poi_search": "routing",
    "gaode_route": "routing",
    "read_skill": "thinking",
}

# 4 类进度事件（design ⑥ 枚举；越界一律收敛到 thinking）。
PROGRESS_EVENTS = ("thinking", "searching", "recognizing_photo", "routing")


def progress_event_for(tool_name: str) -> str:
    """工具名 → 进度事件类型。未映射 / 幻觉名 → thinking（盲人至少听到「思考中」反馈，不报错）。"""
    return TOOL_TO_EVENT.get(tool_name, "thinking")


def sse_line(event: dict) -> str:
    """事件 dict → ndjson 一行（结尾 \\n）。StreamingResponse 按 yield 逐行写。"""
    return json.dumps(event, ensure_ascii=False) + "\n"
