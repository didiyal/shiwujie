"""graph 包：StateGraph 装配（design.md ①-③⑫）。

MVP 导出 build_graph / build_system_prompt / State —— 标准 ReAct 环 + checkpointer。
对齐 shiwujie-ai/docs/design.md（LangGraph 版，源真值）。
"""

from .graph import build_graph
from .prompts import build_system_prompt
from .state import State

__all__ = ["build_graph", "build_system_prompt", "State"]
