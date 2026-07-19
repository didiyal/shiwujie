"""FastAPI 服务壳 —— design 链路层（缝 A Python 侧逐 turn 入口）。

chunk-2a **流式版**（design ⑥）：
- POST /ai/turn {thread_id=blind_id, text, position?, image?} → StreamingResponse（ndjson 逐事件）。
- GET /health → {"status":"ok"}。

**ndjson 事件流**（streaming.sse_line 序列化；2b Java 按行解析转发 WS）：
    {"type":"turn_start","issuing_turn":N}
    {"type":"progress","event":"searching"|...}     ← agent 决策调工具即发（streaming.progress_event_for）
    {"type":"delta","text":"..."}                    ← 末答切块（2a 模拟 / 2c 真 token）
    {"type":"turn_end","issuing_turn":N}

关键抉择：
- 模型 = SHIWUJIE_AI_REAL=1 → llm.build_llm(PRIMARY_MODEL, parallel=False) 真 qwen3.7-plus；
  默认 FakeChatModel（零 token，保 chunk-2a 烟测 + 测试套件）。chunk-2c 起 REAL 端到端验通。
- checkpointer = MemorySaver（进程内）；C2a-6 换 Redis db=2 `ai:ckpt:{blind_id}`。
- **进度事件从 stream_mode="updates" 派生**（看 agent 节点 tool_calls 决策即发），不注 writer 进
  工具体（见 streaming.py 注）；2b 消费面无感。
- **issuing_turn 从 checkpoint 现有 HumanMessage 数自推**（prior+1）——跨进程不丢（Redis 态正确），
  免维护侧带 turn 计数器；首 turn=1。
- **service 边界 set_turn_context(thread_id, issuing_turn)** 灌 emergency contextvar（design ⑬ 硬修正
  1，emergency turn-bound token 判据）。漏灌 → fail-closed（confirm 永拒，不误发紧急）。
- thread_id == blind_id（design ⑤ 会话/checkpoint/emergency 键统一）。
"""

import os
from typing import Optional

from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from langchain_core.messages import HumanMessage
from langgraph.checkpoint.memory import MemorySaver
from pydantic import BaseModel

from ..graph import build_graph, build_system_prompt
from ..graph.fake import FakeChatModel
from ..safety import emergency
from ..streaming import chunk_reply, progress_event_for, sse_line
from ..tools.registry import build_toolset


class TurnRequest(BaseModel):
    thread_id: str  # = blind_id（会话 / checkpoint / emergency 键，design ⑤）
    text: str
    position: Optional[dict] = None  # {lat, lng, address}，每轮附（design ②）
    image: Optional[str] = None  # 预留：图片 url/base64（chunk-2c 真 VLM；本薄版不消费）


def _count_human_messages(snapshot) -> int:
    """snapshot 现有 HumanMessage 数（首 turn / 全新 thread → 0）。"""
    msgs = (snapshot.values or {}).get("messages") or []
    return sum(1 for m in msgs if getattr(m, "type", None) == "human")


def _resolve_model():
    """模型解析：SHIWUJIE_AI_REAL=1 → 真 qwen build_llm(PRIMARY_MODEL, parallel=False)
    （parallel=False 满足 emergency 闸 ① 红队 Q18 硬修正——qwen 单 AIMessage 不并行 tool_calls，
    无法同轮伪造 prepare+confirm；DashScope FC 默认亦 False）；默认 FakeChatModel（零 token，
    保 chunk-2a 烟测 + 测试套件绿）。build_llm 懒导入——REAL 运行时才加载，配合 safety/__init__
    whitelist 懒加载彻底规避 llm→tools 循环 import。"""
    if os.environ.get("SHIWUJIE_AI_REAL", "") == "1":
        from .. import config
        from ..llm import build_llm

        return build_llm(config.PRIMARY_MODEL, parallel=False)
    return FakeChatModel()


def create_app(model=None, checkpointer=None) -> FastAPI:
    """建 FastAPI app。model/checkpointer 可注入（测试用）；默认 _resolve_model（REAL 开关）。"""
    app = FastAPI(title="shiwujie-ai", version="0.1.0")
    graph = build_graph(
        model or _resolve_model(),
        build_toolset(),
        build_system_prompt,
        checkpointer=checkpointer or MemorySaver(),
    )

    @app.get("/health")
    def health():
        return {"status": "ok"}

    @app.post("/ai/turn")
    async def ai_turn(req: TurnRequest):
        cfg = {"configurable": {"thread_id": req.thread_id}}
        # issuing_turn 自推：现有 HumanMessage 数 + 1（跨进程 Redis 态正确，免侧带计数器）。
        issuing_turn = _count_human_messages(graph.get_state(cfg)) + 1
        # service 边界灌 emergency 轮上下文（contextvar，同 task 内 generator 迭代可见）。
        emergency.set_turn_context(req.thread_id, issuing_turn)
        input_state = {
            "messages": [HumanMessage(content=req.text)],
            "blind_id": req.thread_id,
            "position": req.position,
            "issuing_turn": issuing_turn,
        }

        async def event_stream():
            yield sse_line({"type": "turn_start", "issuing_turn": issuing_turn})
            async for chunk in graph.astream(input_state, cfg, stream_mode="updates"):
                for node, payload in chunk.items():
                    if node != "agent":
                        # tools 节点更新不单独发事件——进度已在 agent 决策 tool_calls 时发。
                        continue
                    msgs = (payload or {}).get("messages", [])
                    last = msgs[-1] if msgs else None
                    tool_calls = getattr(last, "tool_calls", None)
                    if tool_calls:
                        # agent 决策调工具 → 发对应进度事件（首个工具名映射；2c 多并行可扩展多发）。
                        ev = progress_event_for(tool_calls[0]["name"])
                        yield sse_line({"type": "progress", "event": ev})
                    elif last is not None:
                        # 终答（无 tool_calls）→ 末答切块 delta 序列（2a 模拟 / 2c 真 token）。
                        for delta in chunk_reply(getattr(last, "content", "") or ""):
                            yield sse_line({"type": "delta", "text": delta})
            yield sse_line({"type": "turn_end", "issuing_turn": issuing_turn})

        return StreamingResponse(event_stream(), media_type="application/x-ndjson")

    return app


# 模块级单例：uvicorn 直挂（uv run python -m shiwujie_ai）。
app = create_app()
