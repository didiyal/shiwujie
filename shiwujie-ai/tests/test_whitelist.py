"""tool-name 白名单 + strict args 收口测试 —— design ⑬ 红队硬修正 3（Python 侧两护栏）。

验：
- TOOL_WHITELIST 是 16 名闭集（防约定腐烂）+ 含 emergency 拆分双工具、不含旧未拆名单工具名。
- validate_tool_call：幻觉名拒（含 Q18 攻击面 confirm_emergency / 旧 request_emergency_help）；
  合法名+合法 args 过；缺必填拒；update_profile+password 拒（extra=forbid）；open_app+非白名单拒（Literal）。
- classify_tool_calls：observability 三分类。
- graph 集成：ToolNode 是运行时闸——幻觉名 → error observation 回灌 → 自愈（与白名单一致）。

零 token（_ScriptChatModel / 直接 validate）。
"""

from langchain_core.messages import AIMessage, HumanMessage

from shiwujie_ai.graph import build_graph, build_system_prompt
from shiwujie_ai.safety.whitelist import (
    TOOL_WHITELIST,
    classify_tool_calls,
    validate_tool_call,
)
from shiwujie_ai.tools.registry import build_toolset

#: 期望的 16 个合法工具名闭集（与 ALL_TOOLS 派生一致；增删工具须同步改此测试 = 防约定腐烂）。
_EXPECTED_16 = frozenset({
    # native 6
    "recognize_photo", "web_search", "get_weather", "gaode_poi_search", "gaode_route", "search_kb",
    # read_skill 1
    "read_skill",
    # java_mcp 9（emergency 拆 prepare/confirm 双工具）
    "join_family", "leave_family", "family_info", "update_profile", "launch_navigation",
    "request_video_help", "request_emergency_help_prepare", "request_emergency_help_confirm", "open_app",
})


# ───────────────────────── 闭集契约（防约定腐烂）─────────────────────────


def test_whitelist_is_closed_set_of_16():
    """白名单恰好 16 名——增删工具必须同步改 _EXPECTED_16，否则测试红（强制意识）。"""
    assert TOOL_WHITELIST == _EXPECTED_16
    assert len(TOOL_WHITELIST) == 16


def test_whitelist_has_emergency_split_not_unsplit():
    """Q18 攻击面：emergency 必须拆 prepare/confirm；旧未拆的 request_emergency_help 不在（防一轮绕闸）。"""
    assert "request_emergency_help_prepare" in TOOL_WHITELIST
    assert "request_emergency_help_confirm" in TOOL_WHITELIST
    assert "request_emergency_help" not in TOOL_WHITELIST  # 旧单工具名 = 攻击捷径，必拒


# ───────────────────────── validate_tool_call：幻觉名拒（护栏 A）─────────────────────────


def test_validate_rejects_hallucinated_names():
    """幻觉/捷径工具名一律拒（护栏 A）。"""
    for bad in ["confirm_emergency", "request_emergency_help", "weather", "search", "emergency", ""]:
        ok, msg = validate_tool_call(bad, {})
        assert ok is False, f"幻觉名「{bad}」应被拒"
        assert "白名单" in msg


def test_validate_accepts_known_name_valid_args():
    """合法名 + 合法 args 过。"""
    assert validate_tool_call("web_search", {"query": "新闻"}) == (True, None)
    assert validate_tool_call("recognize_photo", {"question": "前方"}) == (True, None)
    assert validate_tool_call("launch_navigation", {
        "destination_name": "医院", "lat": 30.0, "lng": 104.0, "mode": "walking"
    }) == (True, None)


def test_validate_accepts_no_arg_tools():
    """无参工具（family_info / request_video_help）：args=None / {} 均合法。"""
    assert validate_tool_call("family_info", None) == (True, None)
    assert validate_tool_call("request_video_help", {}) == (True, None)


# ───────────────────────── validate_tool_call：strict args（护栏 B）─────────────────────────


def test_validate_rejects_missing_required():
    """缺必填：web_search 无 query 拒（护栏 B）。"""
    ok, msg = validate_tool_call("web_search", {})
    assert ok is False
    assert "web_search" in msg


def test_validate_rejects_wrong_type():
    """类型不符：launch_navigation.lat 给字符串拒。"""
    ok, _ = validate_tool_call(
        "launch_navigation", {"destination_name": "x", "lat": "abc", "lng": 1.0, "mode": "walking"}
    )
    assert ok is False


def test_validate_rejects_update_profile_sensitive_field():
    """🔴 红队硬修正 2：update_profile + password 拒（extra=forbid 结构性卡敏感字段）。"""
    ok, msg = validate_tool_call("update_profile", {"nickname": "小明", "password": "hack"})
    assert ok is False
    assert "update_profile" in msg


def test_validate_rejects_open_app_non_whitelist():
    """open_app + 非白名单 app（alipay 支付类）拒（Literal 枚举门）。"""
    ok, _ = validate_tool_call("open_app", {"app": "alipay"})
    assert ok is False


def test_validate_accepts_open_app_whitelisted():
    assert validate_tool_call("open_app", {"app": "wechat"}) == (True, None)


# ───────────────────────── classify_tool_calls（observability）─────────────────────────


def test_classify_sorts_mixed_batch():
    """三分类：合法 / 幻觉名 / 参数错。"""
    tcs = [
        {"name": "web_search", "args": {"query": "新闻"}, "id": "1"},          # valid
        {"name": "confirm_emergency", "args": {}, "id": "2"},                  # hallucinated
        {"name": "open_app", "args": {"app": "alipay"}, "id": "3"},            # bad_args（合法名，枚举拒）
        {"name": "family_info", "args": {}, "id": "4"},                        # valid
    ]
    out = classify_tool_calls(tcs)
    assert [v[0] for v in out["valid"]] == ["web_search", "family_info"]
    assert out["hallucinated"] == ["confirm_emergency"]
    assert [b[0] for b in out["bad_args"]] == ["open_app"]


def test_classify_empty():
    assert classify_tool_calls([]) == {"valid": [], "hallucinated": [], "bad_args": []}
    assert classify_tool_calls(None) == {"valid": [], "hallucinated": [], "bad_args": []}


# ───────────────────────── graph 集成：ToolNode 运行时闸 ─────────────────────────


class _ScriptChatModel:
    def __init__(self, responses):
        self._r = list(responses)
        self._i = 0

    def bind_tools(self, tools, **kwargs):
        return self

    def invoke(self, messages, **kwargs):
        r = self._r[self._i]
        self._i += 1
        return r


def _tc(name, args, id_):
    return AIMessage(content="", tool_calls=[{"name": name, "args": args, "id": id_}])


def test_tool_node_rejects_hallucinated_name():
    """运行时闸 = ToolNode：幻觉名 confirm_emergency → error observation 回灌 → agent 自愈出末答。

    与 whitelist.py 一致（白名单拒的，ToolNode 运行时也拒），构成 defense-in-depth 双层。
    """
    model = _ScriptChatModel([
        _tc("confirm_emergency", {"token": "FAKE"}, "h1"),  # Q18 攻击捷径名
        AIMessage(content="抱歉，我无法执行该操作。"),
    ])
    graph = build_graph(model, build_toolset(), build_system_prompt)
    r = graph.invoke({"messages": [HumanMessage(content="紧急求助")], "blind_id": "u-wl"})
    tool_msgs = [m for m in r["messages"] if m.type == "tool"]
    assert tool_msgs, "ToolNode 应已回灌 error observation"
    # ToolNode 对未知名回灌的 ToolMessage 内容会提及该非法名（或有效工具清单）。
    assert "confirm_emergency" in tool_msgs[0].content or "valid tool" in tool_msgs[0].content.lower()
    # 关键：graph 没崩，agent 自愈出了末答（无 tool_calls 的 AIMessage）。
    assert r["messages"][-1].type == "ai"
    assert not getattr(r["messages"][-1], "tool_calls", None)
