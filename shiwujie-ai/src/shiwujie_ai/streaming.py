"""流式进度事件 helper —— design ⑥（动态反馈经 StreamingResponse → 2b Java WS → App）。

两件事：
- TOOL_TO_EVENT：工具名 → 进度事件类型映射。盲人单声道 UX——让用户听见「正在搜索/识别/算路
  线…」的动态反馈，而非干等末答。
- chunk_reply：完整末答 → delta 序列。chunk-2a 模拟 token 流（FakeChatModel 给整串）；chunk-2c
  换真 AIMessageChunk token delta（stream_mode 加 "messages"）。

**进度事件仅 4 类**（design ⑥ 枚举）：thinking（思考/读手册）/ searching（搜索/查天气/查 KB）
/ recognizing_photo（拍照识别）/ routing（导航：POI/路线）。

**ndjson 协议**（service 层用 sse_line 序列化，一行一事件；2b Java 按行解析转发 WS）：
    {"type":"turn_start","issuing_turn":N}
    {"type":"progress","event":"searching"}
    {"type":"delta","text":"..."}            ← 末答分块（2a 模拟 / 2c 真 token）
    {"type":"turn_end","issuing_turn":N}

实现注：design ⑥ 原文「stream_mode='custom'」——2a 改由 service 层从 stream_mode="updates"
的 agent 节点输出**派生**进度事件（看 tool_calls 决策即发，无需把 writer 注进每个工具体/ToolNode），
对外 ndjson 协议不变（2b 消费面无感）。2c 若需更细粒度（如工具内部进度）再迁真 custom writer。
"""

import json
from typing import List

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

# chunk-2a 末答切块大小（字符；近似中文 token，模拟 delta 流）。2c 读真 token delta。
CHUNK_SIZE = 4


def progress_event_for(tool_name: str) -> str:
    """工具名 → 进度事件类型。未映射 / 幻觉名 → thinking（盲人至少听到「思考中」反馈，不报错）。"""
    return TOOL_TO_EVENT.get(tool_name, "thinking")


def chunk_reply(reply: str, size: int = CHUNK_SIZE) -> List[str]:
    """完整末答 → delta 序列（每段 ≤ size 字符，保序）。空答 → []。

    chunk-2a 模拟 token 流（FakeChatModel 给整串、切块验 delta 协议）；chunk-2c 直接读真
    AIMessageChunk.content 作 delta，不再用本函数。
    """
    if not reply:
        return []
    pieces = [reply[i : i + size] for i in range(0, len(reply), size)]
    return pieces or [reply]


def sse_line(event: dict) -> str:
    """事件 dict → ndjson 一行（结尾 \\n）。StreamingResponse 按 yield 逐行写。"""
    return json.dumps(event, ensure_ascii=False) + "\n"
