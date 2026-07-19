"""FastAPI 服务壳 —— design 链路层（缝 A Python 侧逐 turn 入口）。

chunk-2a **流式版**（design ⑥）：
- POST /ai/turn {thread_id=blind_id, text, position?, image?} → StreamingResponse（ndjson 逐事件）。
- GET /health → {"status":"ok"}。

**ndjson 事件流**（streaming.sse_line 序列化；2b Java 按行解析转发 WS）：
    {"type":"turn_start","issuing_turn":N}
    {"type":"progress","event":"searching"|...}     ← agent 决策调工具即发（updates 流 tool_calls 派生）
    {"type":"delta","text":"..."}                    ← agent 末答逐 token（messages 流；2c 真 token delta）
    {"type":"turn_end","issuing_turn":N}

关键抉择：
- 模型 = SHIWUJIE_AI_REAL=1 → llm.build_llm(PRIMARY_MODEL, parallel=False) 真 qwen3.7-plus；
  默认 FakeChatModel（零 token，保 chunk-2a 烟测 + 测试套件）。chunk-2c 起 REAL 端到端验通。
- checkpointer = MemorySaver（进程内）；C2a-6 换 Redis db=2 `ai:ckpt:{blind_id}`。
- **双流 stream_mode=["messages","updates"]**：messages 取 agent 节点 AIMessageChunk.content 逐
  token 作 delta（真模型 token 流；FakeChatModel 无 stream → LangGraph 吐整条作单 chunk，协议不变）；
  updates 取 agent 节点 tool_calls 决策派生 progress。不注 writer 进工具体（见 streaming.py 注）；
  2b 消费面无感。
- **issuing_turn 从 checkpoint 现有 HumanMessage 数自推**（prior+1）——跨进程不丢（Redis 态正确），
  免维护侧带 turn 计数器；首 turn=1。
- **service 边界 set_turn_context(thread_id, issuing_turn)** 灌 emergency contextvar（design ⑬ 硬修正
  1，emergency turn-bound token 判据）。漏灌 → fail-closed（confirm 永拒，不误发紧急）。
- **turn_end 后台 fire 偏好抽取**（design 3.9，chunk-2c）：REAL + 关键词信号闸命中才 `asyncio.create_task`
  跑 `extract_preferences`（structured_output LLM 抽）→ `merge` 入 store；fire-and-forget 不阻塞 ndjson
  流、失败无害下轮重试。下 turn 起 `build_system_prompt` 读 store 注偏好短段。FakeChatModel 跳过。
- thread_id == blind_id（design ⑤ 会话/checkpoint/emergency 键统一）。
"""

import asyncio
import os
from typing import List, Optional

from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from langchain_core.messages import BaseMessage, HumanMessage
from langgraph.checkpoint.memory import MemorySaver
from pydantic import BaseModel

from ..graph import build_graph, build_system_prompt
from ..graph.fake import FakeChatModel
from ..memory import compress_messages, get_store
from ..safety import emergency
from ..streaming import progress_event_for, sse_line
from ..tools.registry import build_toolset
from ..tools.vlm import set_image_context


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


def _resolve_compress(model, real: bool):
    """compress transform 解析：REAL → 注真 LLM 摘要（design 3.9 chunk-2c）；否则默认假摘要（零 token）。

    REAL 下用 build_llm_plain(SUMMARY_MODEL)（不绑工具，省 16 schema token + 免摘要误触 tool_call）
    构摘要器，包成 compress_messages(summarizer=...) 的 transform 注 graph。懒导入同 _resolve_model
    （规避循环 import）。"""
    if not real:
        return compress_messages
    from .. import config
    from ..llm import build_llm_plain
    from ..memory.summarizer import make_llm_summarizer

    summarizer = make_llm_summarizer(build_llm_plain(config.SUMMARY_MODEL))
    return lambda msgs: compress_messages(msgs, summarizer=summarizer)


def _resolve_pref_model(real: bool):
    """偏好抽取模型解析：REAL → build_llm_plain(SUMMARY_MODEL)（复用「便宜后台 LLM」旋钮，
    不绑工具）；否则 None（FakeChatModel 不支持 structured_output，跳过抽取）。

    service turn_end 后台 fire `with_structured_output` 抽偏好（design 3.9 chunk-2c）。
    懒导入同 _resolve_model（规避循环 import）。
    """
    if not real:
        return None
    from .. import config
    from ..llm import build_llm_plain

    return build_llm_plain(config.SUMMARY_MODEL)


def _current_turn_messages(all_messages: List[BaseMessage]) -> List[BaseMessage]:
    """切片当前 turn：从最后一条 HumanMessage（含）到末尾。无 HumanMessage → 全量。

    后台偏好抽取只看本 turn（省 LLM 输入）；边界逻辑同 budget.derive_turn_stats。
    """
    last_human = -1
    for i, m in enumerate(all_messages):
        if getattr(m, "type", None) == "human":
            last_human = i
    if last_human < 0:
        return list(all_messages)
    return list(all_messages[last_human:])


async def _extract_and_merge(blind_id: str, messages: List[BaseMessage], model):
    """后台抽偏好 + merge 入库（design 3.9）。任何异常吞掉——fire-and-forget，绝不向上抛
    （task 可能被连接关闭取消；失败下轮重试，store 不污染）。"""
    try:
        from ..memory.extractor import extract_preferences, has_preference_signal

        if not has_preference_signal(messages):
            return  # 无偏好信号 → 跳过（省 LLM；多数 turn 无偏好）
        prefs = await extract_preferences(messages, model)
        if prefs:
            get_store().merge(blind_id, prefs)
    except Exception:
        pass  # 后台任务：失败无害，下轮重试


def create_app(model=None, checkpointer=None) -> FastAPI:
    """建 FastAPI app。model/checkpointer 可注入（测试用）；默认 _resolve_model（REAL 开关）。

    REAL 模式同时：① 注真 LLM 摘要到 compress（design 3.9）；② turn_end 后台 fire 偏好抽取
    （关键词信号闸 + structured_output LLM 抽，fire-and-forget 不阻塞流）。FakeChatModel 均走零 token 路径。
    """
    real = os.environ.get("SHIWUJIE_AI_REAL", "") == "1"
    app = FastAPI(title="shiwujie-ai", version="0.1.0")
    graph = build_graph(
        model or _resolve_model(),
        build_toolset(),
        build_system_prompt,
        checkpointer=checkpointer or MemorySaver(),
        compress=_resolve_compress(model, real),
    )
    # REAL → 后台偏好抽取模型（design 3.9）；FakeChatModel → None（跳过抽取）。
    pref_model = _resolve_pref_model(real)

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
        # chunk-2e-3：灌当前轮图片到 vlm contextvar（仿 emergency set_turn_context）——recognize_photo
        # 工具体取不到 state（ToolNode 只传 args），走 contextvar 读图。None = 无图（mock/纯文本 turn）。
        set_image_context(req.image)
        input_state = {
            "messages": [HumanMessage(content=req.text)],
            "blind_id": req.thread_id,
            "position": req.position,
            "issuing_turn": issuing_turn,
        }

        async def event_stream():
            yield sse_line({"type": "turn_start", "issuing_turn": issuing_turn})
            # 双流（stream_mode 列表 → 每项 (mode, chunk)）：
            # - messages：agent 节点逐 token 文本作 delta（chunk-2c 真 token delta；FakeChatModel 无
            #   stream → LangGraph 吐整条 AIMessage 作单 chunk，协议不变、仅粒度降级）。
            # - updates：agent 节点 tool_calls 决策 → progress 事件（终答无 tool_calls 时 messages 流
            #   已逐 token 发完 delta，此处不重切，避免重复）。
            async for mode, chunk in graph.astream(
                input_state, cfg, stream_mode=["messages", "updates"]
            ):
                if mode == "messages":
                    msg, metadata = chunk
                    if (metadata or {}).get("langgraph_node") != "agent":
                        continue  # tools 节点 ToolMessage 是观察值，非用户面 delta
                    content = getattr(msg, "content", "")
                    if isinstance(content, str) and content:
                        yield sse_line({"type": "delta", "text": content})
                else:  # updates
                    for node, payload in chunk.items():
                        if node != "agent":
                            continue
                        msgs = (payload or {}).get("messages", [])
                        last = msgs[-1] if msgs else None
                        if getattr(last, "tool_calls", None):
                            # agent 决策调工具 → 进度事件（此时 messages 流 content="" 不发 delta，
                            # 故 progress 必在终答 delta 之前，符合 UX：先报工具进度再出末答）。
                            ev = progress_event_for(last.tool_calls[0]["name"])
                            yield sse_line({"type": "progress", "event": ev})
            yield sse_line({"type": "turn_end", "issuing_turn": issuing_turn})
            # design 3.9 后台异步抽偏好：turn_end 已发，不阻塞流。REAL + 有信号才 fire；
            # create_task fire-and-forget（_extract_and_merge 内部全兜底，绝不抛进已结束的流；
            # 失败无害下轮重试）。
            if pref_model is not None:
                final_msgs = (graph.get_state(cfg).values or {}).get("messages") or []
                turn_msgs = _current_turn_messages(final_msgs)
                if turn_msgs:
                    asyncio.create_task(
                        _extract_and_merge(req.thread_id, turn_msgs, pref_model)
                    )

        return StreamingResponse(event_stream(), media_type="application/x-ndjson")

    return app


# 模块级单例：uvicorn 直挂（uv run python -m shiwujie_ai）。
app = create_app()
