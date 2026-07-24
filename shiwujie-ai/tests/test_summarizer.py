"""真 LLM 摘要器测试 —— design 3.9 compress summarizer 注入（chunk-2c）。

验 make_llm_summarizer 契约 `(msgs)->str`：
- 正常：模型返回文本 → 摘要头标记 + 模型文本。
- 异常兜底：invoke 抛任何异常 → default_fake_summarizer（零 token、绝不空，encode-不抛同形）。
- 空答兜底：模型返回空串/空白 → 同样回退假摘要。
- 多模态 content（list）→ 取文本块拼接（VLM 路径兼容）。
- 注入 compress_messages：summarizer=llm_summarizer 的输出成为 summary SystemMessage 内容。

零 token / 零网络（脚本化假模型）。
"""

from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage

from shiwujie_ai.memory import compress_messages
from shiwujie_ai.memory.summarizer import make_llm_summarizer


# ───────────────────────── 脚本化假模型 ─────────────────────────


class _ScriptModel:
    """脚本化模型：按 ctor 给的 behavior 响应 invoke。

    - content 正常返回
    - raise_on_invoke=True → invoke 抛异常（验兜底）
    - empty=True → 返回空 content（验空答兜底）
    - multimodal=True → content 为 list（验多模态取文本）
    """

    def __init__(self, content="这是摘要。", *, raise_on_invoke=False, empty=False, multimodal=False):
        self._content = content
        self._raise = raise_on_invoke
        self._empty = empty
        self._multimodal = multimodal
        self.invoke_calls = []

    def bind_tools(self, tools, **kwargs):
        return self

    def invoke(self, messages, **kwargs):
        self.invoke_calls.append(list(messages))
        if self._raise:
            raise RuntimeError("模拟摘要模型网络炸了")
        if self._empty:
            return AIMessage(content="   ")
        if self._multimodal:
            return AIMessage(content=[{"type": "text", "text": self._content}, {"type": "text", "text": "(续)"}])
        return AIMessage(content=self._content)


def _sample_history():
    """早前历史样本（被压缩段）。"""
    return [
        HumanMessage(content="我想去附近的医院"),
        AIMessage(content="", tool_calls=[{"name": "gaode_poi_search", "args": {"q": "医院"}, "id": "p1"}]),
    ]


# ───────────────────────── 正常路径 ─────────────────────────


def test_normal_summary_has_header_and_model_text():
    """模型返回文本 → 摘要 = 头标记（含消息数）+ 模型文本 strip。"""
    model = _ScriptModel(content="  用户要去附近的医院。  ")
    summarize = make_llm_summarizer(model)
    out = summarize(_sample_history())
    assert "2 条消息" in out  # 头标记含消息数
    assert "用户要去附近的医院。" in out  # 模型文本（已 strip）
    # 模型收到 [系统指令, 序列化历史]
    sent = model.invoke_calls[0]
    assert isinstance(sent[0], SystemMessage)
    assert isinstance(sent[1], HumanMessage)
    assert "医院" in sent[1].content  # 历史已序列化喂入


def test_multimodal_content_text_extracted():
    """模型返回多模态 content（list）→ 取 text 块拼接（VLM 路径兼容）。"""
    model = _ScriptModel(content="前方通畅", multimodal=True)
    summarize = make_llm_summarizer(model)
    out = summarize(_sample_history())
    assert "前方通畅(续)" in out  # 两块 text 拼接


# ───────────────────────── 兜底路径（encode-不抛同形）─────────────────────────


def test_invoke_exception_falls_back_to_fake_summarizer():
    """invoke 抛异常 → default_fake_summarizer 兜底（零 token、不抛、绝不空串）。"""
    model = _ScriptModel(raise_on_invoke=True)
    summarize = make_llm_summarizer(model)
    out = summarize(_sample_history())
    # 假摘要特征：列角色 + 记 tool_calls 工具名 + 截断内容
    assert "用户" in out
    assert "gaode_poi_search" in out  # tool_calls 工具名被记
    assert "医院" in out


def test_empty_answer_falls_back_to_fake_summarizer():
    """模型返回空白 → 回退假摘要（不把空串当 summary，免模型收空 SystemMessage）。"""
    model = _ScriptModel(empty=True)
    summarize = make_llm_summarizer(model)
    out = summarize(_sample_history())
    assert "gaode_poi_search" in out  # 假摘要兜底
    assert out.strip() != ""


# ───────────────────────── compress 集成 ─────────────────────────


def test_llm_summarizer_injected_into_compress():
    """summarizer=llm_summarizer 注 compress_messages → summary SystemMessage 内容来自 LLM。

    契合 design 3.9：超阈 → [SystemMessage(llm_summary), *tail]，summary 是真 LLM 文本。
    """
    model = _ScriptModel(content="用户在找医院并查过 POI。")
    summarize = make_llm_summarizer(model)
    # 长历史强制压缩（极低阈值 + 小 tail）
    msgs = [HumanMessage(content="早前问题" * 100) for _ in range(8)]
    out = compress_messages(msgs, threshold=10, tail_size=3, summarizer=summarize)
    assert isinstance(out[0], SystemMessage)
    assert "用户在找医院" in out[0].content  # LLM 摘要文本进 summary
    # tail 是末 3 条原 HumanMessage（非 summary），总数 = 1(summary) + 3(tail)
    assert not isinstance(out[1], SystemMessage)
    assert len(out) == 4
