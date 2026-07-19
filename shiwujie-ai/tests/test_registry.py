"""registry 烟测：build_toolset 返 16 工具 + graph 跑真实工具集（stub 体）端到端。

复用 MVP 的 FakeChatModel 模式（多测试文件后抽 conftest，此处再内联一份精简版）。
零 token / 零网络：native 真 schema + stub 体、java_mcp 真 schema + stub 体。
"""

from langchain_core.messages import AIMessage, HumanMessage

from shiwujie_ai.graph import build_graph, build_system_prompt
from shiwujie_ai.tools import WRITE_TOOLS
from shiwujie_ai.tools.registry import build_toolset


class _FakeChatModel:
    def __init__(self, responses):
        self._r = list(responses)
        self._i = 0

    def bind_tools(self, tools, **kwargs):
        return self

    def invoke(self, messages, **kwargs):
        if self._i >= len(self._r):
            raise RuntimeError("FakeChatModel responses exhausted")
        r = self._r[self._i]
        self._i += 1
        return r


def _tc(name, args, id_="t1"):
    return AIMessage(content="", tool_calls=[{"name": name, "args": args, "id": id_}])


def _build(model):
    return build_graph(model, build_toolset(), build_system_prompt)


def test_build_toolset_returns_16_with_guardrails():
    tools = build_toolset()
    names = {t.name for t in tools}
    assert len(tools) == 16
    # 三道安全护栏工具在列（emergency 拆分 / update_profile 字段门 / open_app 白名单）
    assert {"request_emergency_help_prepare", "request_emergency_help_confirm"} <= names
    assert "update_profile" in names
    assert "open_app" in names
    # WRITE_TOOLS 是 ALL_TOOLS 的子集（误调用监控口径）
    assert WRITE_TOOLS <= names


def test_graph_runs_real_native_tool_stub_body():
    """graph 用真 toolset，FakeChatModel 调真 get_weather（native stub 体）→ ToolNode 跑通返回。"""
    model = _FakeChatModel(
        [
            _tc("get_weather", {"location": "北京"}, id_="w1"),
            AIMessage(content="北京今天晴，22 度。"),
        ]
    )
    result = _build(model).invoke(
        {"messages": [HumanMessage(content="北京天气？")], "blind_id": "u1"}
    )

    tool_msgs = [m for m in result["messages"] if m.type == "tool"]
    assert len(tool_msgs) == 1
    assert "晴" in tool_msgs[0].content  # 真 native get_weather stub 体返回
    assert tool_msgs[0].tool_call_id == "w1"
    assert result["messages"][-1].content == "北京今天晴，22 度。"


def test_graph_runs_java_mcp_tool_stub_body():
    """graph 用真 toolset，FakeChatModel 调真 join_family（java_mcp stub 体）→ 跑通。"""
    model = _FakeChatModel(
        [
            _tc("join_family", {"family_phone": "13800000000"}, id_="j1"),
            AIMessage(content="已为您申请加入该家庭。"),
        ]
    )
    result = _build(model).invoke(
        {"messages": [HumanMessage(content="帮我加入 13800000000 的家庭")], "blind_id": "u1"}
    )

    tool_msgs = [m for m in result["messages"] if m.type == "tool"]
    assert len(tool_msgs) == 1
    assert "加入家庭" in tool_msgs[0].content  # 真 java_mcp join_family stub 体返回
    assert tool_msgs[0].tool_call_id == "j1"
