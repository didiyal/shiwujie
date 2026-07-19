"""encode-不抛 测试 —— design ⑫（Pi 金标准契约：失败 encode 成结构化消息，绝不抛、绝不杀 graph）。

两场景：
1. **模型异常 encode-不抛**：agent 节点 model.invoke 抛（网络/超时/schema 违例）→ 捕获 → encode
   降级 AIMessage（无 tool_calls → END），graph 不崩，用户拿降级答复。
2. **工具失败 isError 自愈**：工具体抛异常 → ToolNode 自动 encode 成 error observation 回灌 →
   agent 下轮读 observation 自愈出末答（换工具/换参/告用户）。

零 token（_ThrowingModel / @tool 抛异常 / _ScriptChatModel）。
"""

from langchain_core.messages import AIMessage, HumanMessage
from langchain_core.tools import tool

from shiwujie_ai.graph import build_graph, build_system_prompt
from shiwujie_ai.graph.nodes import ENCODE_ERROR_REPLY


# ───────────────────────── 模型异常 encode-不抛 ─────────────────────────


class _ThrowingModel:
    """invoke 永远抛 RuntimeError（模拟网络/超时/schema 违例）。"""

    def bind_tools(self, tools, **kwargs):
        return self

    def invoke(self, messages, **kwargs):
        raise RuntimeError("模拟模型调用失败（网络挂了）")


def test_model_exception_encoded_not_thrown():
    """model.invoke 抛 → agent encode 降级 AIMessage → graph 不崩，末答 = ENCODE_ERROR_REPLY。"""
    graph = build_graph(_ThrowingModel(), [], build_system_prompt)
    result = graph.invoke({"messages": [HumanMessage(content="你好")], "blind_id": "u1"})
    last = result["messages"][-1]
    assert last.type == "ai"
    assert last.content == ENCODE_ERROR_REPLY
    assert not getattr(last, "tool_calls", None)  # 无 tool_calls → END（不进 tools 死循环）


def test_model_exception_after_tool_still_recovers():
    """先正常 tool_call 跑通，下一轮模型抛 → 仍 encode 降级（中途异常不杀 graph）。"""

    class _ThenThrow:
        def __init__(self):
            self._i = 0

        def bind_tools(self, tools, **kwargs):
            return self

        def invoke(self, messages, **kwargs):
            self._i += 1
            if self._i == 1:
                return AIMessage(content="", tool_calls=[{"name": "ok_tool", "args": {}, "id": "k1"}])
            raise RuntimeError("第二轮模型挂了")

    @tool
    def ok_tool() -> str:
        """测试用：永远成功。"""
        return "ok"

    graph = build_graph(_ThenThrow(), [ok_tool], build_system_prompt)
    result = graph.invoke({"messages": [HumanMessage(content="用工具")], "blind_id": "u2"})
    assert result["messages"][-1].content == ENCODE_ERROR_REPLY  # 第二轮抛 → encode 降级


# ───────────────────────── 工具失败 isError 自愈 ─────────────────────────


class _ScriptChatModel:
    def __init__(self, responses):
        self._r = list(responses)
        self._i = 0

    def bind_tools(self, tools, **kwargs):
        return self

    def invoke(self, messages, **kwargs):
        r = self._r[self._i]
        self._i += 1
        return r


@tool
def broken_tool(query: str) -> str:
    """测试用：永远抛异常（模拟外部 API 挂）。"""
    raise RuntimeError("外部服务挂了")


def test_tool_exception_iserror_self_heal():
    """工具体抛 → ToolNode encode error observation 回灌 → agent 读 observation 自愈出末答。

    design ⑫：工具失败不杀 graph，agent 拿到错误 observation 后换路（这里 scripted 直接出致歉末答）。
    """
    model = _ScriptChatModel([
        AIMessage(content="", tool_calls=[{"name": "broken_tool", "args": {"query": "x"}, "id": "b1"}]),
        AIMessage(content="抱歉，搜索暂时不可用，请稍后再试。"),
    ])
    graph = build_graph(model, [broken_tool], build_system_prompt)
    result = graph.invoke({"messages": [HumanMessage(content="搜一下")], "blind_id": "u3"})
    # ToolNode 回灌了 error observation（工具体抛被 encode，非 graph 崩）。
    tool_msgs = [m for m in result["messages"] if m.type == "tool"]
    assert tool_msgs, "ToolNode 应已回灌 error observation"
    assert tool_msgs[0].content  # 非空（错误描述）
    # agent 自愈出末答。
    assert result["messages"][-1].type == "ai"
    assert "不可用" in result["messages"][-1].content
