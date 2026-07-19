"""graph MVP 烟测 —— design.md ①-④（State / 两节点 / 条件边 / checkpoint 多轮）。

三场景，全确定性（FakeChatModel 脚本化），零 token / 零外部 API：
1. 纯问答不调工具：agent → END。
2. 单工具 round-trip：agent → tools → agent → END。
3. checkpoint 多轮：同 thread_id 两次 invoke，第 2 轮模型看到第 1 轮历史（= crash-recovery 基元）。

FakeChatModel 现内联本文件；测试文件增多后再抽 conftest.py（design Mock 策略：graph 对
模型实例无感，chunk-2c 换回真 qwen 仅换实例、wiring 不变）。
"""

from langchain_core.messages import AIMessage, HumanMessage
from langchain_core.tools import tool
from langgraph.checkpoint.memory import MemorySaver

from shiwujie_ai.graph import build_graph, build_system_prompt


# ───────────────────────── FakeChatModel（脚本化 LLM）─────────────────────────


class FakeChatModel:
    """脚本化 LLM：按 responses 队列顺序返回 AIMessage。

    - bind_tools：no-op（工具由 graph 的 ToolNode 持有，模型只负责吐 tool_calls 名+参）。
    - invoke：返回下一条脚本响应；记录每次收到的 messages（断言 checkpoint 历史用）。
    - 队列耗尽再调 → RuntimeError（测试脚本响应数不够的信号）。
    """

    def __init__(self, responses):
        self._responses = list(responses)
        self._i = 0
        self.invoke_calls = []  # 每次 invoke 收到的 messages（list[list[BaseMessage]]）

    def bind_tools(self, tools, **kwargs):
        return self

    def invoke(self, messages, **kwargs):
        self.invoke_calls.append(list(messages))
        if self._i >= len(self._responses):
            raise RuntimeError("FakeChatModel responses exhausted — 测试脚本响应数不够")
        resp = self._responses[self._i]
        self._i += 1
        return resp


def _ai_text(content):
    """纯文本 AIMessage（无 tool_calls）—— agent→END 路径。"""
    return AIMessage(content=content)


def _ai_tool_call(name, args=None, id_="t1", content=""):
    """带 tool_calls 的 AIMessage —— agent→tools 路径。"""
    return AIMessage(
        content=content,
        tool_calls=[{"name": name, "args": args or {}, "id": id_}],
    )


@tool
def get_weather(location: str = None) -> str:
    """查询当前天气。"""  # MVP stub；生产 get_weather 在 tools/native.py 填真身（C2a-4）。
    return '{"weather":"晴","temperature":"22度"}'


# ───────────────────────── 测试 ─────────────────────────


def _build(model, tools, checkpointer=None):
    return build_graph(
        model=model,
        tools=tools,
        system_prompt_builder=build_system_prompt,
        checkpointer=checkpointer,
    )


def test_pure_chat_no_tool():
    """无 tool_calls → 直接 END，终答入 state，未触工具。"""
    graph = _build(FakeChatModel([_ai_text("你好！我是视无界助手。")]), [])
    result = graph.invoke({"messages": [HumanMessage(content="你好")], "blind_id": "u1"})

    assert result["messages"][-1].content == "你好！我是视无界助手。"
    assert result["messages"][-1].tool_calls == []


def test_single_tool_roundtrip():
    """agent 发 tool_call → ToolNode 跑 stub → agent 出终答（agent→tools→agent→END）。"""
    model = FakeChatModel(
        [
            _ai_tool_call("get_weather", {"location": "北京"}, id_="w1"),
            _ai_text("北京今天晴，22 度。"),
        ]
    )
    graph = _build(model, [get_weather])
    result = graph.invoke({"messages": [HumanMessage(content="北京天气？")], "blind_id": "u1"})

    messages = result["messages"]
    assert messages[-1].content == "北京今天晴，22 度。"
    tool_msgs = [m for m in messages if m.type == "tool"]
    assert len(tool_msgs) == 1
    assert "晴" in tool_msgs[0].content
    assert tool_msgs[0].tool_call_id == "w1"


def test_multiturn_checkpoint():
    """MemorySaver + 同 thread_id：第 2 轮模型看到第 1 轮历史（checkpoint 基元 = crash-recovery）。"""
    model = FakeChatModel([_ai_text("好的，记住了。"), _ai_text("您喜欢步行。")])
    graph = _build(model, [], checkpointer=MemorySaver())
    cfg = {"configurable": {"thread_id": "u1"}}

    graph.invoke({"messages": [HumanMessage(content="记一下：我喜欢步行。")], "blind_id": "u1"}, cfg)
    graph.invoke({"messages": [HumanMessage(content="我喜欢什么交通方式？")], "blind_id": "u1"}, cfg)

    # 第 1 次 invoke：[system, human1]（2 条）；第 2 次：[system, human1, ai1, human2]（4 条）
    assert len(model.invoke_calls[0]) == 2
    assert len(model.invoke_calls[1]) == 4
    contents = [m.content for m in model.invoke_calls[1]]
    assert "记一下：我喜欢步行。" in contents  # checkpoint 把第 1 轮历史载回
