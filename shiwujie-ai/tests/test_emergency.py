"""emergency turn-bound token 测试 —— design ⑬ 红队硬修正 1（graph 侧闸 ②）。

证明红队 Q18 攻击面被结构堵死：
- 同轮 prepare+confirm（模型一轮内连调/并行调）→ confirm **被拒**（确认问题没机会被问）。
- 跨轮 confirm（T1 prepare→问确认 END；T2 用户答「是」→ confirm）→ **放行**（跨轮 = 确认被问过）。

全程 FakeChatModel（脚本化）+ 进程内 store（确定性 token）+ MemorySaver checkpoint，
零 token / 零网络 / 零 Java。contextvar 在 invoke 边界设（模拟 C2a-7 service 层）。
"""

import json

from langchain_core.messages import AIMessage, HumanMessage
from langgraph.checkpoint.memory import MemorySaver

from shiwujie_ai.graph import build_graph, build_system_prompt
from shiwujie_ai.safety import emergency
from shiwujie_ai.tools.registry import build_toolset


class _ScriptChatModel:
    """固定脚本模型（按序返 AIMessage）。bind_tools 直返 self（忽略 parallel 等参）。"""

    def __init__(self, responses):
        self._r = list(responses)
        self._i = 0

    def bind_tools(self, tools, **kwargs):
        return self

    def invoke(self, messages, **kwargs):
        if self._i >= len(self._r):
            raise RuntimeError("_ScriptChatModel responses exhausted")
        r = self._r[self._i]
        self._i += 1
        return r


def _prepare_call(call_id="p1"):
    return AIMessage(
        content="",
        tool_calls=[{"name": "request_emergency_help_prepare", "args": {}, "id": call_id}],
    )


def _confirm_call(token, call_id="c1"):
    return AIMessage(
        content="",
        tool_calls=[
            {"name": "request_emergency_help_confirm", "args": {"token": token}, "id": call_id}
        ],
    )


# ───────────────────────── 单测：store 核心护栏 ─────────────────────────


def test_unit_turn_bound_rejects_same_turn_accepts_cross_turn():
    """直测 store：同轮签发同轮消费→拒；跨轮消费→放行；一次性消费。"""
    store = emergency.EmergencyTokenStore(token_factory=lambda: "EMERG-TEST")
    token = store.issue("u1", 1)

    ok, _ = store.verify(token, "u1", current_turn=1)
    assert ok is False  # 同轮拒（红队 Q18 攻击面）

    ok, msg = store.verify(token, "u1", current_turn=2)
    assert ok is True  # 跨轮放行（确认被问过）
    assert "已通知" in msg

    # 一次性消费：放行后再 verify → token 不存在
    ok2, _ = store.verify(token, "u1", current_turn=3)
    assert ok2 is False


def test_unit_store_rejects_foreign_user_token():
    """token 绑 blind_id：A 用户的 token 不能被 B 用户 confirm。"""
    store = emergency.EmergencyTokenStore(token_factory=lambda: "EMERG-TEST")
    token = store.issue("uA", 1)
    ok, msg = store.verify(token, "uB", current_turn=2)
    assert ok is False
    assert "不属于" in msg


# ──────────────────── graph 集成：同轮攻击拒 / 跨轮确认通 ────────────────────


def test_graph_emergency_same_turn_confirm_rejected():
    """T1 内模型连调 prepare→confirm（模拟红队 Q18 攻击/误用）→ confirm turn-bound 拒。"""
    emergency.reset_store(token_factory=lambda: "EMERG-TEST")
    model = _ScriptChatModel(
        [
            _prepare_call(),
            _confirm_call("EMERG-TEST"),
            AIMessage(content="确认未通过，我会重新向你确认是否要立即通知所有家属。"),
        ]
    )
    graph = build_graph(model, build_toolset(), build_system_prompt, checkpointer=MemorySaver())
    cfg = {"configurable": {"thread_id": "u1"}}

    emergency.set_turn_context("u1", issuing_turn=1)  # invoke 边界灌 turn 上下文
    r = graph.invoke(
        {"messages": [HumanMessage(content="我摔倒了，快通知家属")], "blind_id": "u1", "issuing_turn": 1},
        cfg,
    )

    confirm_msgs = [m for m in r["messages"] if m.type == "tool" and m.tool_call_id == "c1"]
    assert len(confirm_msgs) == 1
    payload = json.loads(confirm_msgs[0].content)
    assert payload["status"] == "rejected"
    assert "同轮" in payload["message"]  # turn-bound 拒（非 token 不存在等其它拒因）


def test_graph_emergency_cross_turn_confirm_succeeds():
    """T1 prepare→问确认（无 tool_call→END，HITL）；T2 用户答「是」→ confirm→放行（跨轮）。"""
    emergency.reset_store(token_factory=lambda: "EMERG-TEST")
    model = _ScriptChatModel(
        [
            # T1：prepare
            _prepare_call(),
            # T1 终答：问确认（无 tool_call → END）
            AIMessage(content="情况紧急。确认要立即通知你所有家属吗？"),
            # T2：confirm（用户已答「是」）
            _confirm_call("EMERG-TEST"),
            # T2 终答
            AIMessage(content="已通知你所有家属，请保持电话畅通。"),
        ]
    )
    graph = build_graph(model, build_toolset(), build_system_prompt, checkpointer=MemorySaver())
    cfg = {"configurable": {"thread_id": "u1"}}

    # ── T1 ──
    emergency.set_turn_context("u1", issuing_turn=1)
    r1 = graph.invoke(
        {"messages": [HumanMessage(content="我摔倒了，快通知家属")], "blind_id": "u1", "issuing_turn": 1},
        cfg,
    )
    assert r1["messages"][-1].tool_calls == []  # T1 自然停 = HITL（问确认）

    # ── T2：用户确认 ──
    emergency.set_turn_context("u1", issuing_turn=2)
    r2 = graph.invoke(
        {"messages": [HumanMessage(content="是，通知他们")], "blind_id": "u1", "issuing_turn": 2},
        cfg,
    )
    confirm_msgs = [m for m in r2["messages"] if m.type == "tool" and m.tool_call_id == "c1"]
    assert len(confirm_msgs) == 1
    payload = json.loads(confirm_msgs[0].content)
    assert payload["status"] == "ok"
    assert "已通知" in payload["message"]
