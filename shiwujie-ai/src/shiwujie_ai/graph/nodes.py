"""graph 节点 —— design.md ②⑫⑨。

标准 ReAct 两节点（agent 原生 FC / ToolNode 跑工具）。
- **encode-不抛**（design ⑫ Pi 金标准契约）：agent 节点捕获 model.invoke 异常 → encode 友好
  AIMessage（无 tool_calls → END），绝不抛、绝不杀 graph。盲人拿到降级答复而非崩溃。
- **工具失败→isError 自愈**：ToolNode 注入自定义 `_handle_tool_error`——langgraph 默认
  `_default_handle_tool_errors` 只捕 `ToolInvocationError`、其余 re-raise 会崩 graph（实测
  langgraph 1.2.9）；盲人场景外部 API 抖动是常态，必须兜所有异常 → encode error observation
  回灌，agent 下轮自愈。

agent 节点送 model 前对 state.messages 做**非破坏压缩 transform**（design ⑨ ≈ Pi
transformContext）：compress 默认 = memory.compress_messages（生产 ON），短历史 no-op；
传 None 关闭、传自定义 callable 注 2c 真 LLM 摘要。checkpoint 仍存全量 messages。
"""

from typing import Callable, List, Optional

from langchain_core.messages import AIMessage, BaseMessage, SystemMessage
from langgraph.prebuilt import ToolNode

from ..memory import compress_messages
from .budget import (
    BudgetConfig,
    derive_turn_stats,
    encode_stop_reply,
    should_stop_after_turn,
)

#: design ⑫ encode-不抛：model.invoke 异常时的降级答复（无 tool_calls → END，不杀 graph）。
ENCODE_ERROR_REPLY = "抱歉，系统暂时遇到问题，请稍后再试。"


def _handle_tool_error(e: Exception) -> str:
    """design ⑫ 工具失败 encode：任何异常 → observation 文本回灌（agent 下轮读后自愈），绝不杀 graph。

    取代 LangGraph 默认 `_default_handle_tool_errors`（只捕 ToolInvocationError、其余 re-raise → 真实
    工具网络/API 异常会崩 graph）。盲人场景工具失败是常态（外部 API 抖动），必须 encode 自愈。
    """
    return f"工具调用失败：{e}"


def make_agent_node(
    model,
    system_prompt_builder,
    compress: Optional[Callable[[List[BaseMessage]], List[BaseMessage]]] = compress_messages,
    budget_cfg: Optional[BudgetConfig] = BudgetConfig(),
):
    """agent 节点工厂：注 system prompt（builder 按 state 拼装）→ 压缩 transform → 模型原生 FC
    → budget 熔断检查（design ⑫）。

    返回 {"messages":[response]}——只把模型回复追加进 state（system prompt 不入 state，
    每轮由 builder 现拼，避免被压缩/污染历史）。

    compress：messages → 压缩后 view（默认 compress_messages 生产 ON，短历史 no-op）；
    None 关闭压缩。无论压不压，**只产 model 的 view，不动 state.messages**（checkpoint 存全量）。

    budget_cfg：invoke 后跑 should_stop_after_turn（token/轮次/工具调用/循环迹象超限）→
    encode_stop_reply（无 tool_calls → END 停当前 turn，防失控烧 token）。默认 BudgetConfig()
    开启；None 关闭（测试关 budget 用）。统计从 state.messages 推本 turn 边界（design ⑤
    session 可回放树，崩溃恢复后也正确切分）。
    """

    def agent(state):
        sys = system_prompt_builder(state)
        history = state["messages"] if compress is None else compress(state["messages"])
        messages = [SystemMessage(content=sys), *history]
        try:
            response = model.invoke(messages)
        except Exception:
            # design ⑫ encode-不抛：模型异常（网络/超时/schema 违例/限流）→ encode 降级 AIMessage，
            # 不抛不杀 graph（Pi 金标准契约）。无 tool_calls → END，用户拿降级答复而非崩溃。
            response = AIMessage(content=ENCODE_ERROR_REPLY)
            return {"messages": [response]}
        # design ⑫ budget 熔断：invoke 后检查本 turn 累计统计（含本次 response），超限 encode
        # 停止答复（无 tool_calls → END，不再进 tools 节点，防 agent 失控空转烧 token）。
        if budget_cfg is not None:
            stats = derive_turn_stats(list(state["messages"]) + [response])
            stop, _reason = should_stop_after_turn(stats, budget_cfg)
            if stop:
                response = encode_stop_reply()
        return {"messages": [response]}

    return agent


def make_tools_node(tools):
    """ToolNode：跑工具。工具体抛任何异常 → encode 成 error observation 回灌（design ⑫ isError 自愈，
    `_handle_tool_error` 取代 LangGraph 默认只捕 ToolInvocationError 的行为），agent 下轮自愈。"""
    return ToolNode(tools, handle_tool_errors=_handle_tool_error)
