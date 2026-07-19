"""shouldStopAfterTurn 成本轮次熔断 —— design ⑫ Pi 金标准契约。

每个 turn 结束跑一次成本 / 轮次检查：token 超额 / 工具调用过多 / 循环迹象 →
停当前 turn，encode 成无 tool_calls 的「我先停一下」AIMessage（→ END），防 agent
失控空转烧 token（Pi 不自带硬上限，本模块补这道安全网）。

**chunk-2a：纯逻辑模块 + 单测，不接线 live loop。** 理由：FakeChatModel 脚本模型
不会失控（确定性序列），熔断在 2a 咬不到牙；真有意义是 chunk-2c 真 qwen——失控
循环 / 成本封顶才可能发生。2c wiring 时在 agent 节点每步后调
`derive_turn_stats` + `should_stop_after_turn`，触发即返回 `encode_stop_reply()`。
"""

from collections import Counter
from dataclasses import dataclass
from typing import List, Optional, Tuple

from langchain_core.messages import AIMessage, BaseMessage

#: 熔断时 encode 给用户的降级答复（无 tool_calls → END，停当前 turn）。
STOP_REPLY = "我先停一下，需要的话请告诉我接下来怎么做。"

#: v1 默认阈值（保守；chunk-2c 据真实流量日志调，与 design ⑨ 压缩阈值同思路——
#: 可调旋钮非命门，真出问题再调降，不 spike）。
DEFAULT_MAX_TURNS = 20              # 单 turn 内 agent 循环步数安全网（Pi 无固定 max-steps，给兜底）
DEFAULT_MAX_TOKENS_PER_TURN = 16_384  # 单 turn 送 model 的 token 估算上限
DEFAULT_MAX_TOOL_CALLS_PER_TURN = 10  # 单 turn 工具调用总数上限
DEFAULT_MAX_REPEATED_TOOL_CALL = 3    # 同一 (tool_name, args) 重复次数上限 = 循环迹象

#: token 粗估系数（字符数 → token）。中文 1 字 ≈ 0.6-1 token、英文 1 词 ≈ 1-1.5 token，
#: 折中 1 字符 ≈ 0.75 token；粗估用于熔断（偏保守），不精确。chunk-2c 据真实 usage 调。
_CHARS_PER_TOKEN = 1.0 / 0.75


@dataclass
class TurnStats:
    """单 turn 统计——由 `derive_turn_stats` 从 state.messages 推导。"""

    turn_steps: int = 0        # 本 turn agent 循环步数（每条 AIMessage 算一步）
    tool_call_count: int = 0   # 本 turn 工具调用总数（所有 AIMessage 的 tool_calls 累加）
    token_estimate: int = 0    # 本 turn 送 model 的 token 粗估
    max_repeated: int = 0      # 本 turn 同一 (tool_name, args) 最大重复次数


@dataclass
class BudgetConfig:
    """熔断阈值（可调旋钮）。各字段 None = 该项不检查。"""

    max_turns: Optional[int] = DEFAULT_MAX_TURNS
    max_tokens_per_turn: Optional[int] = DEFAULT_MAX_TOKENS_PER_TURN
    max_tool_calls_per_turn: Optional[int] = DEFAULT_MAX_TOOL_CALLS_PER_TURN
    max_repeated_tool_call: Optional[int] = DEFAULT_MAX_REPEATED_TOOL_CALL


def should_stop_after_turn(
    stats: TurnStats, config: Optional[BudgetConfig] = None
) -> Tuple[bool, Optional[str]]:
    """Pi shouldStopAfterTurn：跑一次成本 / 轮次熔断检查。

    返回 `(should_stop, reason)`。`should_stop=True` 时调用方应 `encode_stop_reply()`
    作 agent 输出（无 tool_calls → END，停当前 turn）。检查顺序：轮次 → token → 工具
    调用数 → 循环迹象（先触先报）。`None` 阈值跳过该项检查。
    """
    cfg = config or BudgetConfig()
    if cfg.max_turns is not None and stats.turn_steps > cfg.max_turns:
        return True, f"轮次超限（{stats.turn_steps} > {cfg.max_turns}）"
    if cfg.max_tokens_per_turn is not None and stats.token_estimate > cfg.max_tokens_per_turn:
        return True, f"token 超额（~{stats.token_estimate} > {cfg.max_tokens_per_turn}）"
    if cfg.max_tool_calls_per_turn is not None and stats.tool_call_count > cfg.max_tool_calls_per_turn:
        return True, f"工具调用过多（{stats.tool_call_count} > {cfg.max_tool_calls_per_turn}）"
    if cfg.max_repeated_tool_call is not None and stats.max_repeated > cfg.max_repeated_tool_call:
        return True, f"循环迹象（同一调用重复 {stats.max_repeated} 次）"
    return False, None


def encode_stop_reply() -> AIMessage:
    """熔断 encode：返回无 tool_calls 的 AIMessage（→ END，停当前 turn）。

    与 design ⑫「失败 encode 不抛」同形——熔断不是异常，是可控停止；对盲人落成一句
    可理解的降级答复，而非静默截断。
    """
    return AIMessage(content=STOP_REPLY)


def derive_turn_stats(messages: List[BaseMessage]) -> TurnStats:
    """从 messages 推导当前 turn 统计（live-loop wiring / 单测用）。

    **本 turn 边界** = 最后一条 HumanMessage（含图片等）之后的所有 messages——不依赖
    外部计数器，从 checkpoint 恢复后也能正确切分（design ⑤ session 可回放树）。
    """
    last_human = -1
    for i, m in enumerate(messages):
        if m.type == "human":
            last_human = i
    turn_msgs = messages[last_human + 1:] if last_human >= 0 else list(messages)

    ai_msgs = [m for m in turn_msgs if m.type == "ai"]
    tool_calls_by_msg = [getattr(m, "tool_calls", None) or [] for m in ai_msgs]

    tool_call_count = sum(len(tcs) for tcs in tool_calls_by_msg)

    seen: Counter = Counter()
    max_repeated = 0
    for tcs in tool_calls_by_msg:
        for tc in tcs:
            key = (tc.get("name") if isinstance(tc, dict) else getattr(tc, "name", None),
                   _args_key(tc.get("args") if isinstance(tc, dict) else getattr(tc, "args", None)))
            seen[key] += 1
            if seen[key] > max_repeated:
                max_repeated = seen[key]

    token_estimate = sum(_estimate_tokens(_content_str(m)) for m in turn_msgs)

    return TurnStats(
        turn_steps=len(ai_msgs),
        tool_call_count=tool_call_count,
        token_estimate=token_estimate,
        max_repeated=max_repeated,
    )


# ───────────────────────── 内部 helper ─────────────────────────


def _args_key(args) -> str:
    """把 tool_call args 规范化成可 hash / 比较的键（dict → 排序 JSON 字符串）。"""
    if not args:
        return ""
    if isinstance(args, dict):
        return ",".join(f"{k}={args[k]}" for k in sorted(args))
    return str(args)


def _content_str(message: BaseMessage) -> str:
    """取 message 的文本内容（content 可能是 str / list（多模态）/ None）。"""
    content = getattr(message, "content", "")
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts = []
        for block in content:
            if isinstance(block, str):
                parts.append(block)
            elif isinstance(block, dict):
                parts.append(str(block.get("text", "")))
        return "".join(parts)
    return str(content or "")


def _estimate_tokens(text: str) -> int:
    """字符数 → token 粗估（`_CHARS_PER_TOKEN` 系数，偏保守）。"""
    if not text:
        return 0
    return int(len(text) * _CHARS_PER_TOKEN)
