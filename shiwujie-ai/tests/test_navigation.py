"""navigation skill 多轮烟测 —— design ⑧（read-on-demand skill）+ ④（HITL 自然 turn + checkpoint）。

证明 design 杀手特性：navigation 多步流程靠 read_skill 载真 SKILL.md + checkpoint 跨轮续接，
**全程无 interrupt()**——问交通方式时模型不调工具→自然 END→用户下轮答→checkpoint 载回历史续。
FakeChatModel 脚本化模型行为；真 read_skill（载真 SKILL.md）+ 真 poi/route/launch（stub 体）。
零 token / 零网络。
"""

from langchain_core.messages import AIMessage, HumanMessage
from langgraph.checkpoint.memory import MemorySaver

from shiwujie_ai.graph import build_graph, build_system_prompt
from shiwujie_ai.tools.registry import build_toolset


class _RecChatModel:
    """脚本化 + 记录每次 invoke 入参的 LLM（断言 checkpoint 跨轮历史用）。"""

    def __init__(self, responses):
        self._r = list(responses)
        self._i = 0
        self.invoke_calls = []

    def bind_tools(self, tools, **kwargs):
        return self

    def invoke(self, messages, **kwargs):
        self.invoke_calls.append(list(messages))
        if self._i >= len(self._r):
            raise RuntimeError("_RecChatModel responses exhausted")
        r = self._r[self._i]
        self._i += 1
        return r


def _txt(content):
    return AIMessage(content=content)


def test_navigation_multiturn_skill_and_checkpoint():
    """T1：read_skill(载真 SKILL.md) + poi → 问交通方式（无 tool_calls → END）。
    T2：用户答「步行」→ route → launch → 导航已开始（END）。

    断言：① read_skill 载了真 SKILL.md（含 stub 没有的独有短语）；
         ② T2 首次 invoke 的消息含 T1 历史（skill body + poi 结果）= checkpoint 续接；
         ③ T1 末消息无 tool_calls = HITL 自然 turn 停。
    """
    model = _RecChatModel(
        [
            # T1：并行 read_skill + poi_search
            AIMessage(
                content="",
                tool_calls=[
                    {"name": "read_skill", "args": {"name": "navigation"}, "id": "rs1"},
                    {"name": "gaode_poi_search", "args": {"query": "医院"}, "id": "p1"},
                ],
            ),
            # T1 终答：问交通方式（无 tool_calls → END，HITL 节点）
            _txt("找到了附近的医院，约 500 米。您想步行、坐公交还是驾车？"),
            # T2：route（用户已答步行）
            AIMessage(
                content="",
                tool_calls=[
                    {"name": "gaode_route", "args": {"destination": "医院", "mode": "walking"}, "id": "g1"},
                ],
            ),
            # T2：launch
            AIMessage(
                content="",
                tool_calls=[
                    {
                        "name": "launch_navigation",
                        "args": {
                            "destination_name": "医院",
                            "lat": 30.5728,
                            "lng": 104.0668,
                            "mode": "walking",
                        },
                        "id": "ln1",
                    },
                ],
            ),
            # T2 终答
            _txt("好的，步行约 800 米、10 分钟。导航已开始，请跟着语音指引走。"),
        ]
    )
    graph = build_graph(
        model, build_toolset(), build_system_prompt, checkpointer=MemorySaver()
    )
    cfg = {"configurable": {"thread_id": "u1"}}

    # ── T1 ──
    r1 = graph.invoke(
        {"messages": [HumanMessage(content="导航去最近的医院")], "blind_id": "u1"}, cfg
    )
    # ① read_skill 载了真 SKILL.md（"每轮只推进一两步" 是 SKILL.md 独有，stub NAV_SKILL_BODY 无此句）
    rs_msgs = [m for m in r1["messages"] if m.type == "tool" and m.tool_call_id == "rs1"]
    assert len(rs_msgs) == 1
    assert "每轮只推进一两步" in rs_msgs[0].content
    # ③ T1 末无 tool_calls（自然 turn 停 = HITL）
    assert r1["messages"][-1].tool_calls == []

    # ── T2：用户答交通方式 ──
    r2 = graph.invoke({"messages": [HumanMessage(content="步行")], "blind_id": "u1"}, cfg)
    # ② T2 首次 invoke 的消息含 T1 历史：
    #    T1 占 2 次 invoke（tool_call 轮 + 终答轮）→ T2 首次 = invoke_calls[2]
    t2_first = model.invoke_calls[2]
    t2_contents = [m.content for m in t2_first]
    assert any("每轮只推进一两步" in c for c in t2_contents)  # skill body 仍在历史
    assert any('"distance":"500米"' in c for c in t2_contents)  # poi 结果仍在历史
    # T2 末：导航已开始（launch 已执行）
    assert "导航已开始" in r2["messages"][-1].content
