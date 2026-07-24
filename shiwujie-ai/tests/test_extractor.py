"""偏好抽取测试 —— design 3.9 chunk-2c 后台隐式抽取。

验：
- has_preference_signal：关键词命中 → True；无信号（闲聊）→ False（省 LLM）。
- extract_preferences：模型返结构化 → 去空值 dict；异常 → {}；全 null → {}。
- _extract_and_merge：有信号抽+merge 入库；无信号跳过（store 不变）；异常吞掉不抛。

零 token / 零网络（脚本化假 structured runnable）。
"""

import asyncio

import pytest
from langchain_core.messages import AIMessage, HumanMessage

from shiwujie_ai.memory import get_store, reset_store
from shiwujie_ai.memory.extractor import (
    UserPreferences,
    extract_preferences,
    has_preference_signal,
)
from shiwujie_ai.service.app import _extract_and_merge


@pytest.fixture(autouse=True)
def _isolate_store():
    reset_store()
    yield
    reset_store()


# ───────────────────────── 脚本化假 structured 模型 ─────────────────────────


class _FakeStructured:
    """假 with_structured_output 返回的 runnable：ainvoke 返 ctor 给的结果或抛异常。"""

    def __init__(self, result):
        self._result = result  # UserPreferences 实例 或 Exception

    async def ainvoke(self, messages, **kwargs):
        if isinstance(self._result, Exception):
            raise self._result
        return self._result


class _FakeModel:
    """假模型：with_structured_output 返 _FakeStructured。"""

    def __init__(self, structured_result):
        self._structured = _FakeStructured(structured_result)

    def with_structured_output(self, schema, **kwargs):
        return self._structured


# ───────────────────────── has_preference_signal ─────────────────────────


def test_signal_true_on_style_keyword():
    msgs = [HumanMessage(content="回答简洁一点")]
    assert has_preference_signal(msgs) is True


def test_signal_true_on_nav_keyword():
    msgs = [HumanMessage(content="帮我导去医院"), AIMessage(content="好的")]
    # 助手没说偏好，但用户下一条命中
    msgs.append(HumanMessage(content="我喜欢坐地铁"))
    assert has_preference_signal(msgs) is True


def test_signal_false_on_plain_chat():
    """闲聊无偏好信号 → False（省 LLM）。"""
    msgs = [HumanMessage(content="今天天气怎么样"), AIMessage(content="晴天")]
    assert has_preference_signal(msgs) is False


def test_signal_false_empty():
    assert has_preference_signal([]) is False


# ───────────────────────── extract_preferences ─────────────────────────


def _run(coro):
    return asyncio.run(coro)


def test_extract_returns_non_empty_fields():
    """模型返三字段 → dict 保留非空值。"""
    model = _FakeModel(
        UserPreferences(
            communication_style="concise", nav_mode="transit", frequent_apps="微信、高德"
        )
    )
    out = _run(extract_preferences([HumanMessage(content="我喜欢坐地铁，回答简洁")], model))
    assert out == {
        "communication_style": "concise",
        "nav_mode": "transit",
        "frequent_apps": "微信、高德",
    }


def test_extract_strips_null_fields():
    """全 null 字段被去掉 → 空 dict（service 不 merge）。"""
    model = _FakeModel(UserPreferences(communication_style=None, nav_mode=None, frequent_apps=None))
    out = _run(extract_preferences([HumanMessage(content="闲聊")], model))
    assert out == {}


def test_extract_partial_fields():
    """只抽出部分字段 → dict 只含该部分。"""
    model = _FakeModel(UserPreferences(communication_style="detailed", nav_mode=None, frequent_apps=None))
    out = _run(extract_preferences([HumanMessage(content="说详细点")], model))
    assert out == {"communication_style": "detailed"}


def test_extract_exception_returns_empty():
    """模型抛异常 → {}（design 3.9 失败无害，不抛给 caller）。"""
    model = _FakeModel(RuntimeError("模拟抽取模型炸了"))
    out = _run(extract_preferences([HumanMessage(content="我喜欢步行")], model))
    assert out == {}


# ───────────────────────── _extract_and_merge（service 后台 fire 体）─────────────────────────


def test_extract_and_merge_with_signal_stores_prefs():
    """有信号 + 模型抽到偏好 → merge 入库（store 有值）。"""
    model = _FakeModel(UserPreferences(nav_mode="walking"))
    msgs = [HumanMessage(content="我喜欢步行回家")]
    _run(_extract_and_merge("u-extr", msgs, model))
    assert get_store().get("u-extr") == {"nav_mode": "walking"}


def test_extract_and_merge_no_signal_skips():
    """无信号 → 跳过抽取（不调模型、不入库）。"""
    model = _FakeModel(UserPreferences(nav_mode="walking"))  # 即使模型能抽，也不该被调
    msgs = [HumanMessage(content="今天天气怎么样")]
    _run(_extract_and_merge("u-nosig", msgs, model))
    assert get_store().get("u-nosig") == {}


def test_extract_and_merge_exception_does_not_raise():
    """模型异常 → _extract_and_merge 吞掉不抛（fire-and-forget 安全）。"""
    model = _FakeModel(RuntimeError("炸"))
    msgs = [HumanMessage(content="我喜欢步行")]
    # 不应抛
    _run(_extract_and_merge("u-exc", msgs, model))
    assert get_store().get("u-exc") == {}  # 失败不入库


def test_extract_and_merge_empty_result_no_store():
    """模型抽到全 null → 空 dict → 不 merge（store 不污染空字段）。"""
    model = _FakeModel(UserPreferences(communication_style=None, nav_mode=None, frequent_apps=None))
    msgs = [HumanMessage(content="我喜欢步行")]  # 有信号但模型没抽到
    _run(_extract_and_merge("u-empty", msgs, model))
    assert get_store().get("u-empty") == {}
