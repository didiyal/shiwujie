"""VLM wrapper 测试 —— design ① 拍照识别（两入口同一 VLM）。

验：
- mock fallback：2a 默认 mock（question 入文案可断言）。
- use_real 默认 False（不触 VLM/网络）。
- contextvar 图片注入：set_image_context + FORCE_REAL 时取 contextvar image（2c 通路，monkeypatch 真调用）。
- encode-不抛：real 路径异常回退 mock（design ⑫）。
- graph 集成：recognize_photo 体接 wrapper，round-trip。

零 token 零网络（_use_real 默认 False）。
"""

import json

from langchain_core.messages import AIMessage, HumanMessage

from shiwujie_ai.graph import build_graph, build_system_prompt
from shiwujie_ai.tools import vlm
from shiwujie_ai.tools.registry import build_toolset


def test_mock_description_default():
    """2a 默认 mock：question 入文案。"""
    out = vlm.recognize(question="包装上的保质期")
    assert "包装上的保质期" in out
    assert "共享单车" in out  # mock 固定描述


def test_mock_description_no_question():
    out = vlm.recognize()
    assert "前方场景" in out  # question=None 占位


def test_use_real_default_false():
    assert vlm._use_real() is False


def test_real_uses_contextvar_image(monkeypatch):
    """FORCE_REAL + 有 image → 走真路径取 contextvar image（monkeypatch 真调用断参）。"""
    monkeypatch.setattr(vlm, "_use_real", lambda: True)
    vlm.set_image_context("data:image/png;base64,XXXX")
    captured = {}

    def fake_real(image, question):
        captured["image"] = image
        captured["question"] = question
        return "真 VLM 描述：一瓶矿泉水。"

    monkeypatch.setattr(vlm, "_recognize_real", fake_real)
    try:
        out = vlm.recognize(question="这是什么")
        assert out == "真 VLM 描述：一瓶矿泉水。"
        assert captured["image"] == "data:image/png;base64,XXXX"  # 取了 contextvar
        assert captured["question"] == "这是什么"
    finally:
        vlm.set_image_context(None)  # 清理 contextvar


def test_real_exception_falls_back_to_mock(monkeypatch):
    """real 路径抛异常 → 回退 mock（design ⑫ encode-不抛）。"""
    monkeypatch.setattr(vlm, "_use_real", lambda: True)
    vlm.set_image_context("some-image")
    monkeypatch.setattr(vlm, "_recognize_real", lambda *a, **k: (_ for _ in ()).throw(RuntimeError("VLM 挂了")))
    try:
        out = vlm.recognize(question="前方")
        assert "共享单车" in out  # mock 兜底
    finally:
        vlm.set_image_context(None)


# ───────────────────────── graph 集成 ─────────────────────────


class _ScriptChatModel:
    def __init__(self, responses):
        self._r = list(responses)
        self._i = 0

    def bind_tools(self, tools, **kwargs):
        return self

    def invoke(self, messages, **kwargs):
        if self._i >= len(self._r):
            raise RuntimeError("script exhausted")
        r = self._r[self._i]
        self._i += 1
        return r


def _tc(name, args, id_):
    return AIMessage(content="", tool_calls=[{"name": name, "args": args, "id": id_}])


def test_graph_recognize_photo_roundtrip():
    """recognize_photo 体已接 vlm wrapper：tool_call → 真体（mock）→ 终答。"""
    model = _ScriptChatModel([
        _tc("recognize_photo", {"question": "前方有什么"}, "r1"),
        AIMessage(content="前方有一辆共享单车，注意避让。"),
    ])
    graph = build_graph(model, build_toolset(), build_system_prompt)
    r = graph.invoke({"messages": [HumanMessage(content="看看前面有什么")], "blind_id": "u1"})
    tool_msgs = [m for m in r["messages"] if m.type == "tool"]
    assert len(tool_msgs) == 1
    assert "共享单车" in tool_msgs[0].content  # 来自 vlm mock（非 stub_result JSON）
