"""budget 熔断测试 —— design ⑫ shouldStopAfterTurn（Pi 金标准契约）。

纯逻辑：TurnStats + BudgetConfig + should_stop_after_turn + derive_turn_stats。
零 token（不调模型）。
"""

from langchain_core.messages import AIMessage, HumanMessage, ToolMessage

from shiwujie_ai.graph.budget import (
    STOP_REPLY,
    BudgetConfig,
    TurnStats,
    derive_turn_stats,
    encode_stop_reply,
    should_stop_after_turn,
)


# ───────────────────────── should_stop_after_turn：四类熔断 ─────────────────────────


def test_no_stop_on_clean_stats():
    """全绿统计不停。"""
    ok, reason = should_stop_after_turn(TurnStats(turn_steps=3, tool_call_count=2, token_estimate=500, max_repeated=1))
    assert ok is False
    assert reason is None


def test_stop_on_turn_steps_exceeded():
    ok, reason = should_stop_after_turn(TurnStats(turn_steps=21))
    assert ok is True
    assert "轮次" in reason


def test_stop_on_token_exceeded():
    ok, reason = should_stop_after_turn(TurnStats(token_estimate=20_000))
    assert ok is True
    assert "token" in reason


def test_stop_on_tool_call_count_exceeded():
    ok, reason = should_stop_after_turn(TurnStats(tool_call_count=11))
    assert ok is True
    assert "工具调用" in reason


def test_stop_on_loop_sign():
    """循环迹象（同一调用重复超阈值）。"""
    ok, reason = should_stop_after_turn(TurnStats(max_repeated=4))
    assert ok is True
    assert "循环" in reason


# ───────────────────────── 阈值边界（> 严格，等于不触发）─────────────────────────


def test_boundary_equal_not_triggered():
    """等于阈值不触发（>，非 >=）。"""
    cfg = BudgetConfig(max_turns=10, max_tokens_per_turn=1000, max_tool_calls_per_turn=5, max_repeated_tool_call=3)
    ok, _ = should_stop_after_turn(
        TurnStats(turn_steps=10, token_estimate=1000, tool_call_count=5, max_repeated=3), cfg
    )
    assert ok is False


def test_boundary_over_by_one_triggered():
    cfg = BudgetConfig(max_turns=10)
    ok, _ = should_stop_after_turn(TurnStats(turn_steps=11), cfg)
    assert ok is True


# ───────────────────────── 先触先报顺序 ─────────────────────────


def test_first_match_reported():
    """轮次 → token → 工具调用 → 循环，先触先报。"""
    # 轮次 + token 同时超，报轮次（先检查）。
    _, reason = should_stop_after_turn(TurnStats(turn_steps=99, token_estimate=99_999))
    assert "轮次" in reason


# ───────────────────────── BudgetConfig：自定义 + None 跳过 ─────────────────────────


def test_custom_config_overrides_defaults():
    """自定义阈值生效（比默认更严）。"""
    cfg = BudgetConfig(max_tool_calls_per_turn=2)
    ok, _ = should_stop_after_turn(TurnStats(tool_call_count=3), cfg)
    assert ok is True


def test_none_threshold_skips_check():
    """阈值 None = 该项不检查（关掉某道闸）。"""
    cfg = BudgetConfig(max_turns=None, max_tokens_per_turn=None, max_tool_calls_per_turn=None, max_repeated_tool_call=None)
    ok, reason = should_stop_after_turn(TurnStats(turn_steps=9999, token_estimate=9999, tool_call_count=9999, max_repeated=9999), cfg)
    assert ok is False
    assert reason is None


# ───────────────────────── encode_stop_reply ─────────────────────────


def test_encode_stop_reply_ends_turn():
    """熔断 encode：无 tool_calls 的 AIMessage（→ END，停当前 turn）。"""
    msg = encode_stop_reply()
    assert msg.type == "ai"
    assert msg.content == STOP_REPLY
    assert not getattr(msg, "tool_calls", None)


# ───────────────────────── derive_turn_stats：从 messages 推导 ─────────────────────────


def _tc(name, args=None, id_="x"):
    return {"name": name, "args": args or {}, "id": id_}


def test_derive_single_turn_stats():
    """单 turn：两条 AIMessage（一条带 tool_call）+ 一条 ToolMessage + 末答。"""
    messages = [
        HumanMessage(content="导航去最近医院"),
        AIMessage(content="", tool_calls=[_tc("gaode_poi_search", {"query": "医院"}, "1")]),
        ToolMessage(content="找到 A 医院", tool_call_id="1"),
        AIMessage(content="找到 A 医院，去这个吗？"),
    ]
    stats = derive_turn_stats(messages)
    assert stats.turn_steps == 2          # 两条 AIMessage
    assert stats.tool_call_count == 1     # 一个 tool_call
    assert stats.max_repeated == 1        # 无重复
    assert stats.token_estimate > 0


def test_derive_multi_turn_boundary():
    """多 turn：只算最后一条 HumanMessage 之后（前 turn 不计入）。"""
    messages = [
        HumanMessage(content="第一轮"),
        AIMessage(content="", tool_calls=[_tc("web_search", {"query": "a"}, "1")]),
        ToolMessage(content="r1", tool_call_id="1"),
        AIMessage(content="答一"),
        HumanMessage(content="第二轮"),  # 本 turn 边界
        AIMessage(content="答二"),        # 仅这一条算本 turn
    ]
    stats = derive_turn_stats(messages)
    assert stats.turn_steps == 1          # 只算"答二"
    assert stats.tool_call_count == 0     # 第一轮的 web_search 不计入


def test_derive_detects_repeated_loop():
    """循环迹象：同一 (name, args) 重复 3 次 → max_repeated=3。"""
    messages = [
        HumanMessage(content="x"),
        AIMessage(content="", tool_calls=[_tc("web_search", {"query": "同"}, "1")]),
        AIMessage(content="", tool_calls=[_tc("web_search", {"query": "同"}, "2")]),
        AIMessage(content="", tool_calls=[_tc("web_search", {"query": "同"}, "3")]),
    ]
    stats = derive_turn_stats(messages)
    assert stats.max_repeated == 3
    assert stats.tool_call_count == 3


def test_derive_different_args_not_repeated():
    """同名工具不同 args 不算重复。"""
    messages = [
        HumanMessage(content="x"),
        AIMessage(content="", tool_calls=[_tc("web_search", {"query": "a"}, "1")]),
        AIMessage(content="", tool_calls=[_tc("web_search", {"query": "b"}, "2")]),
    ]
    stats = derive_turn_stats(messages)
    assert stats.max_repeated == 1


def test_derive_token_estimate_scales_with_content():
    """token 估算随内容长度增长。"""
    short = derive_turn_stats([HumanMessage(content="x"), AIMessage(content="短")])
    long = derive_turn_stats([HumanMessage(content="x"), AIMessage(content="这是一段非常非常非常长的回答" * 10)])
    assert long.token_estimate > short.token_estimate


def test_derive_multimodal_content_does_not_crash():
    """多模态 content（list 形式，design ② 拍照结果回灌）不崩，提取 text 块估算。"""
    messages = [
        HumanMessage(content=[{"type": "text", "text": "这是什么"}, {"type": "image_url", "image_url": "data:..."}]),
        AIMessage(content="一张桌子"),
    ]
    stats = derive_turn_stats(messages)
    assert stats.turn_steps == 1
    assert stats.token_estimate > 0


def test_derive_empty_messages():
    stats = derive_turn_stats([])
    assert stats == TurnStats()
