"""短期记忆压缩测试 —— design 3.9（≈ Pi transformContext，非破坏 transform）。

验：
- estimate_tokens：CJK ~1/字、latin ~4 字符/token。
- 短历史（len<=tail 或 未超阈）→ 原样 no-op。
- 长历史 → [SystemMessage(summary), *recent_tail]：summary 在、tail 原样、总数骤减。
- recent-tail 永不被压（护导航/多轮 in-flight）。
- tail 起点不拆 tool_call→tool_result 对。
- summarizer DI（注入自定义，签名不变）。
- graph 集成：agent 节点接 compress，长历史经 transform 后模型收 [prompt, summary, *tail]。

非破坏：入参 messages 不被修改（checkpoint 存全量靠它）。零 token / 零网络。
"""

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, ToolMessage

from shiwujie_ai.graph import build_graph, build_system_prompt
from shiwujie_ai.memory import compress as compress_mod
from shiwujie_ai.memory import compress_messages, default_fake_summarizer, estimate_tokens


# ───────────────────────── estimate_tokens ─────────────────────────


def test_estimate_tokens_cjk_vs_latin():
    """CJK ~1 token/字；latin ~4 字符/token。"""
    cjk_only = estimate_tokens([HumanMessage(content="你好世界天气")])  # 6 CJK → ~6
    assert cjk_only == 6
    latin_only = estimate_tokens([HumanMessage(content="abcdefgh")])  # 8 latin → 2
    assert latin_only == 2


# ───────────────────────── no-op 路径 ─────────────────────────


def test_short_history_under_tail_size_noop():
    """len <= tail_size → 原样返回（拷贝，非同一 list）。"""
    msgs = [HumanMessage(content="hi"), AIMessage(content="hello")]
    out = compress_messages(msgs, tail_size=10, threshold=1)
    assert [m.content for m in out] == [m.content for m in msgs]
    assert out is not msgs  # 拷贝非原 list


def test_under_threshold_noop():
    """len > tail_size 但未超阈 → 原样。"""
    msgs = [HumanMessage(content=f"第{i}轮") for i in range(15)]  # 短内容，token 低
    out = compress_messages(msgs, tail_size=5, threshold=10_000)
    assert [m.content for m in out] == [m.content for m in msgs]


def test_non_destructive_does_not_mutate_input():
    """压缩不修改入参（checkpoint 存全量靠它）。"""
    msgs = [HumanMessage(content="用户问题" * 200) for _ in range(15)]
    snapshot_before = [m.content for m in msgs]
    _ = compress_messages(msgs, tail_size=3, threshold=10)  # 必触发压缩
    assert [m.content for m in msgs] == snapshot_before  # 入参原样
    assert len(msgs) == 15  # 长度没被改


# ───────────────────────── 压缩路径 ─────────────────────────


def test_long_history_compresses_to_summary_plus_tail():
    """超阈 → [SystemMessage(summary), *tail]；summary 含压缩标记，tail 原样，总数骤减。"""
    msgs = [HumanMessage(content="用户长问题" * 100) for _ in range(12)]
    out = compress_messages(msgs, tail_size=4, threshold=10)

    assert isinstance(out[0], SystemMessage)
    assert "压缩" in out[0].content  # summary 标记
    # tail 原样（末 4 条内容不变）
    assert [m.content for m in out[1:]] == [m.content for m in msgs[-4:]]
    # 总数 = 1(summary) + 4(tail) = 5 << 12
    assert len(out) == 5
    assert len(out) < len(msgs)


def test_recent_tail_never_compressed():
    """recent-tail 的内容逐字出现在输出里（导航/多轮 in-flight 状态受护）。"""
    tail_msgs = [AIMessage(content=f"受保护的尾部{i}") for i in range(5)]
    head_msgs = [HumanMessage(content="早前历史" * 80) for _ in range(10)]
    msgs = head_msgs + tail_msgs
    out = compress_messages(msgs, tail_size=5, threshold=10)

    out_contents = [m.content for m in out]
    for tm in tail_msgs:
        assert tm.content in out_contents  # 每条 tail 逐字保留


def test_default_summarizer_records_tool_names():
    """假摘要记 tool_calls 工具名（利于续导航：模型知前轮调过 poi/route）。"""
    ai = AIMessage(
        content="",
        tool_calls=[{"name": "gaode_poi_search", "args": {"q": "医院"}, "id": "p1"}],
    )
    summary = default_fake_summarizer([HumanMessage(content="导航去医院"), ai])
    assert "gaode_poi_search" in summary
    assert "导航去医院" in summary


# ───────────────────────── tool 对不拆 ─────────────────────────


def test_tail_boundary_does_not_split_tool_pair():
    """tail 起点落 ToolMessage 上 → 向左扩到含其对应 AIMessage（不拆对）。"""
    msgs = [
        HumanMessage(content="早前" * 100),  # 0
        AIMessage(content="早前" * 100),  # 1
        AIMessage(  # 2：发 tool_call
            content="",
            tool_calls=[{"name": "get_weather", "args": {"location": "北京"}, "id": "w1"}],
        ),
        ToolMessage(content='{"weather":"晴"}', tool_call_id="w1"),  # 3
        HumanMessage(content="尾部用户"),  # 4
    ]
    # tail_size=2 → 起点 = index 3（ToolMessage）→ 扩到 index 2（AIMessage）
    out = compress_messages(msgs, tail_size=2, threshold=10)
    # tail 必含 AIMessage(2) + ToolMessage(3) + HumanMessage(4)，不被拆
    out_contents = [m.content for m in out]
    assert any(getattr(m, "tool_calls", None) for m in out)  # AIMessage(带 tool_calls) 在 tail
    assert '{"weather":"晴"}' in out_contents  # ToolMessage 在 tail
    assert "尾部用户" in out_contents
    assert isinstance(out[0], SystemMessage)  # summary 在头


# ───────────────────────── summarizer DI ─────────────────────────


def test_custom_summarizer_injected():
    """注入自定义 summarizer（签名 (msgs)->str），其输出被用作 summary 内容。"""

    def fake_llm_summarize(messages):
        return f"FAKE-LLM 摘了 {len(messages)} 条"

    msgs = [HumanMessage(content="问题" * 100) for _ in range(8)]
    out = compress_messages(msgs, tail_size=3, threshold=10, summarizer=fake_llm_summarize)
    assert out[0].content.startswith("FAKE-LLM 摘了")
    # chunk-2c：fake_llm_summarize 换成便宜 LLM 真摘要，签名不变


# ───────────────────────── graph 集成 ─────────────────────────


class _RecChatModel:
    """脚本化 + 记录每次 invoke 入参的 LLM（断言 agent 压缩 transform 后模型收到的 view）。"""

    def __init__(self, response):
        self._resp = response
        self.invoke_calls = []

    def bind_tools(self, tools, **kwargs):
        return self

    def invoke(self, messages, **kwargs):
        self.invoke_calls.append(list(messages))
        return self._resp


def test_graph_agent_compresses_long_history():
    """graph 接 compress（默认 ON）：长历史经 agent transform → 模型收 [prompt, summary, *tail]。"""
    long = "用户想了解今天的天气以及附近餐厅还想导航回家" * 12  # 长 CJK content
    history = []
    for i in range(8):
        history.append(HumanMessage(content=f"第{i}轮：{long}"))
        history.append(AIMessage(content=f"回复第{i}轮：{long}"))
    # 16 条长历史

    model = _RecChatModel(AIMessage(content="终答"))
    # 极低阈值 + 小 tail 强制压缩
    compress = lambda msgs: compress_messages(msgs, threshold=10, tail_size=3)
    graph = build_graph(
        model, [], build_system_prompt, checkpointer=None, compress=compress
    )
    graph.invoke({"messages": history, "blind_id": "u1"})

    sent = model.invoke_calls[0]  # 模型收到的 messages
    # [0] = system prompt（agent 节点拼的基底）；[1] = summary（compress 产的 SystemMessage）
    assert isinstance(sent[0], SystemMessage)
    assert isinstance(sent[1], SystemMessage)
    assert "压缩" in sent[1].content
    # 末 3 条 = recent-tail 原样（最后 3 条历史内容）
    assert [m.content for m in sent[-3:]] == [m.content for m in history[-3:]]
    # 总数 = 1(prompt) + 1(summary) + 3(tail) = 5 << 16
    assert len(sent) == 5


def test_graph_short_history_unaffected():
    """短历史（默认 compress ON）→ 模型收全量，无 summary（no-op 不改变现有 graph 行为）。"""
    model = _RecChatModel(AIMessage(content="你好"))
    graph = build_graph(model, [], build_system_prompt)  # compress 默认 ON
    graph.invoke({"messages": [HumanMessage(content="你好")], "blind_id": "u1"})

    sent = model.invoke_calls[0]
    # [system_prompt, human] —— 无 summary SystemMessage 夹中间
    summary_msgs = [m for m in sent if isinstance(m, SystemMessage) and "压缩" in (m.content or "")]
    assert summary_msgs == []  # 短历史不压
    assert len(sent) == 2
