"""短期记忆压缩 —— design 3.9（≈ Pi transformContext）。

**非破坏 transform**：checkpoint 存全量 messages（崩溃可恢复、导航多轮状态完整）；
agent 节点送 model 前对 messages 做一次 transform——超阈则把 recent-tail 之外压成一条
summary，model 看到的窗口 = [SystemMessage(prompt), SystemMessage(summary), *recent_tail]；
未超阈原样。state.messages **不被修改**（chunk-2a 纯 view；2c 再加 summary 缓存进 state +
RemoveMessage 限界避免每轮重算）。

两条护栏：
- **recent-tail（默认 10）永不压缩**——护住活跃导航（poi→问交通→route→launch 跨 2-3 轮）
  + 多轮 in-flight 状态（解 design 3.6「长对话把导航中状态压没」风险）。
- **tail 起点不拆 tool_call(AIMessage)→tool_result(ToolMessage) 对**——落在 ToolMessage 上
  时向左扩到含其对应 AIMessage，免模型报 unpaired tool 错。

chunk-2a：`default_fake_summarizer` 确定性假摘要（列角色+截断内容，零 token、可断言）。
chunk-2c：换注入的便宜 LLM 真摘要（更语义、更短），`compress_messages(summarizer=...)`
签名不变；接口对模型无感（DI）。
"""

from typing import Callable, List, Optional

from langchain_core.messages import BaseMessage, SystemMessage

# qwen 上下文 ~32k；阈值保守取 ~6k 给 system prompt + 工具 schema + 终答 + 偏好段留余量。
# 精确值 Phase 5 实测（design 3.9 ⏳）。判「该不该压」够用，不求精。
DEFAULT_THRESHOLD = 6000
# recent-tail 永不压缩——护住活跃导航/多轮 in-flight 状态（design 3.6 风险）。
DEFAULT_TAIL_SIZE = 10


def estimate_tokens(messages: List[BaseMessage]) -> int:
    """粗估 messages 的 token 数（判阈值用，非精确）。

    CJK ~1 token/字（保守偏高→阈值触发更早更安全）；latin/数字 ~4 字符/token。
    带 tool_calls 的消息把 args JSON 也计入。
    """
    total = 0
    for m in messages:
        content = m.content if isinstance(m.content, str) else str(m.content)
        cjk = sum(1 for c in content if "一" <= c <= "鿿")
        other = len(content) - cjk
        total += cjk + other // 4
        for tc in getattr(m, "tool_calls", None) or []:
            total += max(1, len(str(tc.get("args", ""))) // 4)
    return total


def default_fake_summarizer(messages: List[BaseMessage]) -> str:
    """chunk-2a 确定性假摘要：列每条消息角色 + 截断内容（带 tool_calls 记工具名）。

    零 token、可断言。chunk-2c 换真便宜 LLM（更语义、更短），签名
    `(List[BaseMessage]) -> str` 不变。
    """
    role_label = {"human": "用户", "ai": "助手", "tool": "工具结果", "system": "系统"}
    lines = [f"（早前历史已压缩，共 {len(messages)} 条消息）"]
    for m in messages:
        role = role_label.get(getattr(m, "type", ""), getattr(m, "type", "未知"))
        content = m.content if isinstance(m.content, str) else str(m.content)
        snippet = content.replace("\n", " ")[:50]
        tcs = getattr(m, "tool_calls", None) or []
        if tcs:
            names = ",".join(tc.get("name", "?") for tc in tcs)
            lines.append(f"- {role} 调用[{names}]：{snippet}")
        else:
            lines.append(f"- {role}：{snippet}")
    return "\n".join(lines)


def _find_tool_call_owner(messages: List[BaseMessage], before_idx: int, tc_id: str) -> Optional[int]:
    """往前找发 tc_id 这个 tool_call 的 AIMessage 索引（None = 找不到配对）。"""
    for j in range(before_idx - 1, -1, -1):
        for tc in getattr(messages[j], "tool_calls", None) or []:
            if tc.get("id") == tc_id:
                return j
    return None


def _safe_tail_start(messages: List[BaseMessage], tail_size: int) -> int:
    """recent-tail 起点 = len - tail_size；落在 ToolMessage 上则向左扩到含其对应 AIMessage。

    不拆 tool_call(AIMessage)→tool_result(ToolMessage) 对——免模型收 unpaired tool 报错。
    连续多条 ToolMessage（并行工具结果）共用同一 AIMessage，扫任一都收敛到它。
    """
    start = max(0, len(messages) - tail_size)
    while start > 0 and getattr(messages[start], "type", "") == "tool":
        tc_id = getattr(messages[start], "tool_call_id", None)
        owner = _find_tool_call_owner(messages, start, tc_id)
        if owner is None or owner >= start:
            break  # 找不到配对或无进展，放弃调整（防死循环）
        start = owner
    return start


def compress_messages(
    messages: List[BaseMessage],
    threshold: int = DEFAULT_THRESHOLD,
    tail_size: int = DEFAULT_TAIL_SIZE,
    summarizer: Optional[Callable[[List[BaseMessage]], str]] = None,
) -> List[BaseMessage]:
    """非破坏压缩 transform。

    返回 model 应看到的 messages view（**不修改入参**；checkpoint 仍存全量）：
    - 太短（len <= tail_size）或未超阈 → 原样返回（拷贝）。
    - 超阈 → [SystemMessage(summary), *recent_tail]。
      recent-tail 之外压成一条 summary；tail 起点经 `_safe_tail_start` 不拆 tool 对。
      若整段是不可拆的单条 tool 链（start 回到 0）→ 放弃压缩原样返回。

    summary 作 SystemMessage（role=system 让模型当上下文而非新指令），夹在 agent 节点
    拼的 system prompt 与 recent-tail 之间 = design 3.9 [system][summary][tail] 窗口。
    """
    if len(messages) <= tail_size:
        return list(messages)
    if estimate_tokens(messages) <= threshold:
        return list(messages)

    start = _safe_tail_start(messages, tail_size)
    old = messages[:start]
    if not old:
        return list(messages)  # 整段一个不可拆 tool 链，放弃压缩

    summarize_fn = summarizer or default_fake_summarizer
    summary = SystemMessage(content=summarize_fn(old))
    return [summary, *messages[start:]]
