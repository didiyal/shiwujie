"""graph 装配 —— design.md ③ 标准 ReAct 环 + 条件边 + checkpointer。

build_graph(model, tools, system_prompt_builder, checkpointer=None, compress=compress_messages)：
- model：ChatOpenAI（生产）/ FakeChatModel（测试）—— graph 对实例无感（DI）。
- tools：BaseTool 列表（MVP 测试用本地 stub；生产由 tools/registry.py 提供 16 工具）。
- system_prompt_builder：prompts.build_system_prompt（按 state 注 position/偏好/skills）。
- checkpointer：MemorySaver（测试）/ Redis（生产，design ⑤）。
- compress：agent 节点送 model 前的非破坏压缩 transform（design ⑨）。默认 compress_messages
  （生产 ON，短历史 no-op）；传 None 关闭、传自定义 callable 注 2c 真 LLM 摘要。

HITL 无 interrupt()（design ④）：agent 无 tool_calls → END（自然 turn 停 + checkpoint 续）。
"""

from typing import Callable, List, Optional

from langchain_core.messages import BaseMessage
from langgraph.graph import END, START, StateGraph

from ..memory import compress_messages
from .nodes import make_agent_node, make_tools_node
from .state import State


def _route_after_agent(state):
    """有 tool_calls → tools 节点；无 → END（自然 turn 停，HITL 靠此 design ④）。"""
    last = state["messages"][-1]
    return "tools" if getattr(last, "tool_calls", None) else END


def build_graph(
    model,
    tools,
    system_prompt_builder,
    checkpointer=None,
    compress: Optional[Callable[[List[BaseMessage]], List[BaseMessage]]] = compress_messages,
):
    graph = StateGraph(State)
    graph.add_node("agent", make_agent_node(model, system_prompt_builder, compress))
    graph.add_node("tools", make_tools_node(tools))
    graph.add_edge(START, "agent")
    graph.add_conditional_edges("agent", _route_after_agent, ["tools", END])
    graph.add_edge("tools", "agent")
    return graph.compile(checkpointer=checkpointer)
