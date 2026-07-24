"""长期偏好存储 + system prompt 注入测试 —— design 3.9（跨会话偏好，2a 框架）。

验：
- PreferenceStore：merge-with-latest（覆写非追加）/ 空 partial / None+空值跳过 / get 返副本隔离 /
  clear（单 blind / 全表）。
- format_preferences：空→""、核心字段朗读标签、未知字段前向兼容、未知枚举值原样。
- 注入 hook：build_system_prompt 接 store → 有偏好附短段、无偏好不附。
- graph 集成：agent 节点 system prompt 含偏好短段（wiring 通）。

零 token 零网络（in-process dict 回退）。偏好抽取留 2c，本块只验存储+注入框架。
"""

import pytest
from langchain_core.messages import AIMessage, HumanMessage

from shiwujie_ai.graph import build_graph, build_system_prompt
from shiwujie_ai.memory import (
    PreferenceStore,
    format_preferences,
    get_store,
    reset_store,
)
from shiwujie_ai.tools.registry import build_toolset


@pytest.fixture(autouse=True)
def _isolate_store():
    """每测前后清模块 store，免跨测污染。"""
    reset_store()
    yield
    reset_store()


# ───────────────────────── PreferenceStore ─────────────────────────


def test_merge_overwrites_not_appends():
    """merge-with-latest：偏好可变，同字段覆写非追加。"""
    s = PreferenceStore()
    s.merge("u1", {"nav_mode": "walking"})
    s.merge("u1", {"nav_mode": "transit"})  # 改了主意
    assert s.get("u1") == {"nav_mode": "transit"}


def test_merge_accumulates_distinct_fields():
    s = PreferenceStore()
    s.merge("u1", {"nav_mode": "walking"})
    s.merge("u1", {"communication_style": "concise"})
    assert s.get("u1") == {"nav_mode": "walking", "communication_style": "concise"}


def test_merge_empty_partial_noop():
    s = PreferenceStore()
    s.merge("u1", {"nav_mode": "walking"})
    assert s.merge("u1", {}) == {"nav_mode": "walking"}
    assert s.merge("u1", None) == {"nav_mode": "walking"}


def test_merge_skips_none_and_empty_values():
    """None / 空串值不污染（2c 抽取可能产出占位空值）。"""
    s = PreferenceStore()
    s.merge("u1", {"nav_mode": "walking", "communication_style": None, "frequent_apps": ""})
    assert s.get("u1") == {"nav_mode": "walking"}


def test_get_returns_copy_isolated():
    """get 返副本——外间改不动 store（防约定腐烂）。"""
    s = PreferenceStore()
    s.merge("u1", {"nav_mode": "walking"})
    out = s.get("u1")
    out["nav_mode"] = "HACK"
    assert s.get("u1") == {"nav_mode": "walking"}


def test_get_missing_returns_empty():
    assert PreferenceStore().get("nobody") == {}


def test_clear_by_blind_id():
    s = PreferenceStore()
    s.merge("u1", {"nav_mode": "walking"})
    s.merge("u2", {"nav_mode": "transit"})
    s.clear("u1")
    assert s.get("u1") == {}
    assert s.get("u2") == {"nav_mode": "transit"}  # 他用户不动


def test_clear_all():
    s = PreferenceStore()
    s.merge("u1", {"nav_mode": "walking"})
    s.clear()
    assert s.get("u1") == {}


# ───────────────────────── format_preferences ─────────────────────────


def test_format_empty_returns_empty():
    assert format_preferences({}) == ""
    assert format_preferences(None) == ""


def test_format_known_fields_labeled():
    out = format_preferences(
        {"communication_style": "concise", "nav_mode": "walking", "frequent_apps": "微信、高德"}
    )
    assert out.startswith("用户偏好：")
    assert out.endswith("。")
    assert "回答简洁" in out
    assert "导航偏好步行" in out
    assert "常用 APP 微信、高德" in out
    assert "；" in out  # 多字段分号分隔


def test_format_unknown_field_forward_compat():
    """2c 扩展字段（非核心三件套）原样尾附，不破 2a。"""
    out = format_preferences({"nav_mode": "walking", "diet": "素食"})
    assert "导航偏好步行" in out
    assert "diet: 素食" in out


def test_format_unknown_enum_value_passthrough():
    """未知枚举值（如 2c 抽出 walking 之外的值）原样展示，不报错。"""
    out = format_preferences({"nav_mode": "flying"})
    assert "导航偏好flying" in out


# ───────────────────────── system prompt 注入 hook ─────────────────────────


def test_injection_hook_with_prefs():
    """有偏好 → build_system_prompt 附偏好短段。"""
    get_store().merge("u-inj", {"communication_style": "concise", "nav_mode": "walking"})
    prompt = build_system_prompt({"blind_id": "u-inj"})
    assert "用户偏好：" in prompt
    assert "回答简洁" in prompt
    assert "导航偏好步行" in prompt


def test_injection_hook_no_prefs_no_segment():
    """无偏好 → system prompt 不含偏好段（baseline 不变）。"""
    prompt = build_system_prompt({"blind_id": "u-fresh"})
    assert "用户偏好：" not in prompt
    # 基底 prompt 关键护栏仍在。
    assert "emergency_help_prepare" in prompt
    assert "open_app" in prompt


def test_injection_hook_missing_blind_id_safe():
    """state 无 blind_id → 不注入（不崩）。"""
    prompt = build_system_prompt({})
    assert "用户偏好：" not in prompt


# ───────────────────────── graph 集成 ─────────────────────────


class _CaptureModel:
    """记录首条 system message 的假模型（验偏好真进了 agent 的 system prompt）。"""

    def __init__(self):
        self.seen_system = None

    def bind_tools(self, tools, **kwargs):
        return self

    def invoke(self, messages, **kwargs):
        if messages and getattr(messages[0], "type", None) == "system":
            self.seen_system = messages[0].content
        return AIMessage(content="好的。")


def test_graph_injects_preference_into_system_prompt():
    """agent 节点 → build_system_prompt → store 偏好真进 system prompt（wiring 通）。"""
    get_store().merge("u-graph", {"nav_mode": "walking", "communication_style": "concise"})
    model = _CaptureModel()
    graph = build_graph(model, build_toolset(), build_system_prompt)
    graph.invoke({"messages": [HumanMessage(content="你好")], "blind_id": "u-graph"})
    assert model.seen_system is not None
    assert "步行" in model.seen_system
    assert "简洁" in model.seen_system


def test_graph_no_preference_clean_system_prompt():
    """无偏好 → agent 的 system prompt 不含偏好段（与 baseline 一致）。"""
    model = _CaptureModel()
    graph = build_graph(model, build_toolset(), build_system_prompt)
    graph.invoke({"messages": [HumanMessage(content="你好")], "blind_id": "u-clean"})
    assert model.seen_system is not None
    assert "用户偏好：" not in model.seen_system
