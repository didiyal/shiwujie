"""searchapi.io wrapper 测试 —— web_search 工具（design ⑦ 同款自建 REST）。

验：
- 解析：searchapi 响应 organic_results → 归一化。
- 剪裁：drop link、top-k title+snippet。
- mock fallback：2a 默认 mock（query 入 title 可断言）。
- use_real 默认 False。
- encode-不抛：real 异常回退 mock。
- graph 集成：web_search 体接 wrapper round-trip。

零网络零 token。
"""

import json

from langchain_core.messages import AIMessage, HumanMessage

from shiwujie_ai.graph import build_graph, build_system_prompt
from shiwujie_ai.tools import searchapi
from shiwujie_ai.tools.registry import build_toolset


def test_parse_results():
    raw = {"organic_results": [
        {"title": "结果一", "snippet": "摘要一", "link": "http://a"},
        {"title": "结果二", "snippet": "摘要二", "link": "http://b"},
    ]}
    rs = searchapi._parse_results(raw)
    assert rs[0] == {"title": "结果一", "snippet": "摘要一", "link": "http://a"}


def test_trim_results_drops_link():
    """剪裁 drop link（盲人朗读用 snippet，link 是噪声）。"""
    rs = [{"title": "结果一", "snippet": "摘要一", "link": "http://a"}]
    out = json.loads(searchapi._trim_results(rs))
    assert out == [{"title": "结果一", "snippet": "摘要一"}]
    assert "link" not in out[0]


def test_trim_results_topk():
    rs = [{"title": f"t{i}", "snippet": f"s{i}"} for i in range(10)]
    out = json.loads(searchapi._trim_results(rs, k=3))
    assert len(out) == 3


def test_trim_results_skips_empty_title():
    rs = [{"title": "", "snippet": "无标题"}, {"title": "有标题", "snippet": "摘要"}]
    out = json.loads(searchapi._trim_results(rs))
    assert len(out) == 1  # 空 title 的被跳过
    assert out[0]["title"] == "有标题"


def test_search_mock_default():
    """2a 默认 mock：query 入 title。"""
    out = json.loads(searchapi.search("今天新闻"))
    assert isinstance(out, list)
    assert "今天新闻" in out[0]["title"]


def test_use_real_default_false():
    assert searchapi._use_real() is False


def test_search_real_exception_falls_back_to_mock(monkeypatch):
    """real 异常 → 回退 mock（design ⑫ encode-不抛）。"""
    monkeypatch.setattr(searchapi, "_use_real", lambda: True)
    monkeypatch.setattr(searchapi, "_fetch_real", lambda q: (_ for _ in ()).throw(RuntimeError("网络挂了")))
    out = json.loads(searchapi.search("新闻"))
    assert "新闻" in out[0]["title"]  # mock 兜底


def test_search_empty_results_message(monkeypatch):
    """真搜索返空 organic_results → 给「未搜到」提示（盲人无感裸空）。"""
    monkeypatch.setattr(searchapi, "_use_real", lambda: True)
    monkeypatch.setattr(searchapi, "_fetch_real", lambda q: {"organic_results": []})
    out = json.loads(searchapi.search("冷门词"))
    assert "message" in out  # 未搜到提示


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


def test_graph_web_search_roundtrip():
    """web_search 体已接 searchapi wrapper：tool_call → 真体（mock）→ 终答。"""
    model = _ScriptChatModel([
        _tc("web_search", {"query": "今天天气新闻"}, "s1"),
        AIMessage(content="为您找到一些新闻。"),
    ])
    graph = build_graph(model, build_toolset(), build_system_prompt)
    r = graph.invoke({"messages": [HumanMessage(content="今天有什么新闻")], "blind_id": "u1"})
    tool_msgs = [m for m in r["messages"] if m.type == "tool"]
    payload = json.loads(tool_msgs[0].content)
    assert "今天天气新闻" in payload[0]["title"]  # 来自 searchapi mock（非 stub_result）
