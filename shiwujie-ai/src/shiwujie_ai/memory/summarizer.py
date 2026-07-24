"""真 LLM 摘要器 —— design 3.9 compress 的 summarizer 注入（chunk-2c）。

`compress_messages(summarizer=...)` 默认用 `default_fake_summarizer`（零 token、确定性，列
角色+截断内容）；REAL 模式下 service 注 `make_llm_summarizer(model)` → 更语义、更短的 summary。

契约：`(List[BaseMessage]) -> str`。同步 invoke（compress 在 agent 节点同步路径内调；
LangGraph 把同步节点丢线程池跑，不阻塞事件循环）。

**encode-不抛同形兜底**：摘要 LLM 任何异常（网络/超时/限流/空答）→ 回退 `default_fake_summarizer`
（零 token、绝不空串），与 design ⑫「失败 encode 不杀 graph」同形——摘要失败不阻断主对话，
盲人拿到的是确定性降级摘要而非崩溃。

不在 `memory/__init__` re-export：service REAL 分支懒导入（配合 safety/__init__ whitelist 懒加载
彻底规避 llm→tools 循环 import，与 build_llm 同模式）。
"""

from typing import Callable, List

from langchain_core.messages import BaseMessage, HumanMessage, SystemMessage

from .compress import default_fake_summarizer

#: 摘要指令：保留意图/偏好/任务状态，限长，纯正文输出。
_SUMMARY_INSTRUCTION = (
    "你是会话摘要助手。把下面这段较早的对话压成一段简洁中文摘要（不超过 200 字），"
    "保留：用户关键意图、已确认的偏好或事实、进行中的任务状态（例如导航目的地、待办事项）。"
    "只输出摘要正文，不要任何前后缀或解释。"
)

#: 摘要头标记（与 default_fake_summarizer 的「早前历史已压缩」同义，便于断言/识别）。
_SUMMARY_HEADER = "（早前历史已压缩，共 {n} 条消息）\n"


def make_llm_summarizer(model) -> Callable[[List[BaseMessage]], str]:
    """构造真 LLM 摘要器：model 收 [系统指令, 序列化的早前历史] → 返回 summary str。

    model.invoke 失败 / 空答 → `default_fake_summarizer` 兜底（encode-不抛同形，不杀 graph）。
    返回的 callable 签名契合 `compress_messages(summarizer=...)`。
    """

    def summarize(messages: List[BaseMessage]) -> str:
        transcript = _format_transcript(messages)
        try:
            resp = model.invoke(
                [
                    SystemMessage(content=_SUMMARY_INSTRUCTION),
                    HumanMessage(content=transcript),
                ]
            )
            text = getattr(resp, "content", "")
            if isinstance(text, list):  # 多模态 content → 取文本块
                text = "".join(
                    b.get("text", "") if isinstance(b, dict) else str(b) for b in text
                )
            if isinstance(text, str) and text.strip():
                return _SUMMARY_HEADER.format(n=len(messages)) + text.strip()
        except Exception:
            pass
        # 兜底：确定性假摘要（零 token、绝不空），不抛不杀 graph
        return default_fake_summarizer(messages)

    return summarize


def _format_transcript(messages: List[BaseMessage]) -> str:
    """messages → 紧凑文本（角色:内容），喂给摘要模型。tool_calls 记工具名利于续任务。"""
    role_label = {"human": "用户", "ai": "助手", "tool": "工具结果", "system": "系统"}
    lines = []
    for m in messages:
        role = role_label.get(getattr(m, "type", ""), getattr(m, "type", "?"))
        content = m.content if isinstance(m.content, str) else str(m.content)
        snippet = content.replace("\n", " ")
        tcs = getattr(m, "tool_calls", None) or []
        if tcs:
            names = ",".join(tc.get("name", "?") for tc in tcs)
            lines.append(f"{role} 调用[{names}]：{snippet}")
        else:
            lines.append(f"{role}：{snippet}")
    return "\n".join(lines)
