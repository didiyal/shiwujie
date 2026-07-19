"""高德 REST wrapper 测试 —— design ⑦（自建 wrapper，出参剪裁朗读友好）。

验：
- 解析：高德原生响应 → 归一化 raw（poi/route walking+transit/weather），喂 fixture 无网络。
- 剪裁：raw → 朗读友好 JSON（POI 省冗余、route 距离/时间/转向、weather 含建议）。
- 格式化：米↔公里、秒↔分钟、天气建议启发式。
- mock fallback：2a 默认 mock（确定性、与旧 stub "500米" 兼容不破 nav 测试）。
- encode-不抛：real 路径异常回退 mock（_use_real 注入假异常）。
- graph 集成：3 工具体（poi/route/weather）stub→真升级，round-trip 跑通。

零网络零 token（_use_real 默认 False，全程 mock）。
"""

import json

from langchain_core.messages import AIMessage, HumanMessage

from shiwujie_ai.graph import build_graph, build_system_prompt
from shiwujie_ai.tools import gaode
from shiwujie_ai.tools.registry import build_toolset


# ───────────────────────── 格式化 ─────────────────────────


def test_fmt_distance_meters_and_km():
    assert gaode._fmt_distance(500) == "500米"
    assert gaode._fmt_distance(800) == "800米"
    assert gaode._fmt_distance(1500) == "1.5公里"
    assert gaode._fmt_distance(1000) == "1.0公里"


def test_fmt_duration():
    assert gaode._fmt_duration(600) == "10分钟"  # 600s
    assert gaode._fmt_duration(60) == "1分钟"
    assert gaode._fmt_duration(5400) == "1小时30分钟"  # 90min


def test_weather_suggestion_heuristics():
    assert "带伞" in gaode._weather_suggestion("小雨", 20)
    assert "防滑" in gaode._weather_suggestion("小雪", -2)
    assert "保暖" in gaode._weather_suggestion("晴", 0)  # 冷
    assert "防暑" in gaode._weather_suggestion("晴", 35)  # 热
    assert "适合外出" in gaode._weather_suggestion("晴", 22)


# ───────────────────────── 解析高德响应（fixture）─────────────────────────


def test_parse_pois_around_has_distance():
    """高德 /place/around：location=lng,lat（经度在前），distance 米。"""
    raw = {
        "pois": [
            {"name": "市一医院", "address": "人民路1号", "location": "104.0668,30.5728", "distance": "500"},
            {"name": "中医院", "address": "解放路2号", "location": "104.0700,30.5800", "distance": "1200"},
        ]
    }
    pois = gaode._parse_pois(raw)
    assert pois[0] == {"name": "市一医院", "address": "人民路1号", "lng": 104.0668, "lat": 30.5728, "distance_m": 500}
    assert pois[1]["distance_m"] == 1200


def test_parse_pois_text_no_distance():
    """高德 /place/text：无 distance 字段 → distance_m None（trim 省略）。"""
    raw = {"pois": [{"name": "天安门", "address": "北京", "location": "116.397,39.908"}]}
    pois = gaode._parse_pois(raw)
    assert pois[0]["distance_m"] is None


def test_parse_route_walking():
    raw = {"route": {"paths": [{"distance": "800", "duration": "600", "steps": [
        {"instruction": "向东步行200米"}, {"instruction": "右转进入人民路"}]}]}}
    r = gaode._parse_route(raw, "walking")
    assert r["distance_m"] == 800
    assert r["duration_s"] == 600
    assert r["steps"] == ["向东步行200米", "右转进入人民路"]


def test_parse_route_transit_segments():
    raw = {"route": {"transits": [{"distance": "5000", "duration": "1200",
        "segments": [{"bus": {"buslines": [{"name": "1路公交"}]}}]}]}}
    r = gaode._parse_route(raw, "transit")
    assert r["distance_m"] == 5000
    assert r["duration_s"] == 1200
    assert r["steps"] == ["乘1路公交"]


def test_parse_route_empty():
    assert gaode._parse_route({}, "walking") == {"distance_m": 0, "duration_s": 0, "steps": []}
    assert gaode._parse_route({}, "transit") == {"distance_m": 0, "duration_s": 0, "steps": []}


def test_parse_weather():
    raw = {"lives": [{"weather": "晴", "temperature": "22", "winddirection": "东北", "windpower": "≤3"}]}
    w = gaode._parse_weather(raw)
    assert w["weather"] == "晴"
    assert w["temperature_c"] == "22"


def test_parse_weather_empty():
    w = gaode._parse_weather({})
    assert w["weather"] == "未知"


# ───────────────────────── 剪裁 ─────────────────────────


def test_trim_pois_keeps_latlng_omits_null_distance():
    pois = [{"name": "医院", "address": "人民路", "lng": 104.0, "lat": 30.5, "distance_m": None}]
    out = json.loads(gaode._trim_pois(pois))
    assert out[0]["name"] == "医院"
    assert out[0]["lat"] == 30.5  # lat/lng 保留（launch_navigation 要）
    assert "distance" not in out[0]  # text 搜无距离 → 省略


def test_trim_pois_topk_limits():
    pois = [{"name": f"地点{i}", "address": "", "lng": 0.0, "lat": 0.0, "distance_m": i * 100} for i in range(10)]
    out = json.loads(gaode._trim_pois(pois, k=3))
    assert len(out) == 3


def test_trim_route_format():
    r = {"distance_m": 800, "duration_s": 600, "steps": ["向东步行200米", "右转", "直行"]}
    out = json.loads(gaode._trim_route(r))
    assert out["distance"] == "800米"
    assert out["duration"] == "10分钟"
    assert "向东步行200米" in out["summary"]


def test_trim_weather_format():
    w = {"weather": "晴", "temperature_c": "22"}
    out = json.loads(gaode._trim_weather(w))
    assert out["weather"] == "晴"
    assert out["temperature"] == "22度"
    assert "适合外出" in out["suggestion"]


# ───────────────────────── mock fallback（2a 默认）─────────────────────────


def test_poi_search_mock_compat_500m():
    """2a mock 返单候选带 500米——与旧 stub 兼容（test_navigation 断言 "500米" 不破）。"""
    out = json.loads(gaode.poi_search("医院"))
    assert isinstance(out, list)
    assert out[0]["distance"] == "500米"
    assert "lat" in out[0] and "lng" in out[0]


def test_route_search_mock():
    out = json.loads(gaode.route(origin=None, destination="医院", mode="walking"))
    assert out["distance"] == "800米"
    assert out["duration"] == "10分钟"
    assert "步行" in out["summary"]


def test_weather_search_mock():
    out = json.loads(gaode.weather(city="北京"))
    assert out["weather"] == "晴"
    assert out["temperature"] == "22度"


def test_use_real_default_false():
    """2a 默认 mock：GAODE_KEY 空 → _use_real() False（不触网络）。"""
    assert gaode._use_real() is False


# ───────────────────────── encode-不抛：real 异常回退 mock ─────────────────────────


def test_poi_search_real_exception_falls_back_to_mock(monkeypatch):
    """real 路径抛异常（网络/解析）→ 回退 mock（design ⑫ encode-不抛，不杀 graph）。"""
    monkeypatch.setattr(gaode, "_use_real", lambda: True)

    def _boom(*a, **kw):
        raise RuntimeError("模拟网络故障")

    monkeypatch.setattr(gaode, "_fetch_pois_real", _boom)
    out = json.loads(gaode.poi_search("医院"))
    assert out[0]["distance"] == "500米"  # mock 兜底


# ───────────────────────── graph 集成：3 工具体 stub→真 ─────────────────────────


class _ScriptChatModel:
    """固定脚本模型：按序返 AIMessage（带 tool_calls 或纯文本）。"""

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


def test_graph_weather_tool_roundtrip():
    """get_weather 体已接高德 wrapper：tool_call → ToolNode 调真体 → 终答含天气。"""
    model = _ScriptChatModel([
        _tc("get_weather", {"location": "北京"}, "w1"),
        AIMessage(content="北京今天晴，22 度，适合外出。"),
    ])
    graph = build_graph(model, build_toolset(), build_system_prompt)
    r = graph.invoke({"messages": [HumanMessage(content="北京天气？")], "blind_id": "u1"})
    tool_msgs = [m for m in r["messages"] if m.type == "tool"]
    assert len(tool_msgs) == 1
    payload = json.loads(tool_msgs[0].content)
    assert payload["weather"] == "晴"  # 来自 gaode.weather（非 stub_result）


def test_graph_poi_tool_roundtrip():
    """gaode_poi_search 体接 wrapper：返回含 lat/lng（launch_navigation 要）。"""
    model = _ScriptChatModel([
        _tc("gaode_poi_search", {"query": "医院"}, "p1"),
        AIMessage(content="找到附近医院。"),
    ])
    graph = build_graph(model, build_toolset(), build_system_prompt)
    r = graph.invoke({"messages": [HumanMessage(content="附近医院")], "blind_id": "u1"})
    tool_msgs = [m for m in r["messages"] if m.type == "tool"]
    payload = json.loads(tool_msgs[0].content)
    assert payload[0]["distance"] == "500米"
    assert "lat" in payload[0]


def test_graph_route_tool_roundtrip():
    """gaode_route 体接 wrapper：返回距离+时间+转向摘要。"""
    model = _ScriptChatModel([
        _tc("gaode_route", {"destination": "医院", "mode": "walking"}, "g1"),
        AIMessage(content="步行约 800 米。"),
    ])
    graph = build_graph(model, build_toolset(), build_system_prompt)
    r = graph.invoke({"messages": [HumanMessage(content="走路去医院")], "blind_id": "u1"})
    tool_msgs = [m for m in r["messages"] if m.type == "tool"]
    payload = json.loads(tool_msgs[0].content)
    assert payload["distance"] == "800米"
    assert payload["duration"] == "10分钟"
