"""FastAPI 服务壳测试 —— design 链路层（缝 A Python 侧）· **流式 ndjson 版（C2a-7）**。

TestClient（不起 uvicorn）验：
- /health。
- /ai/turn 流式：turn_start → [progress →] delta×N → turn_end 顺序；delta 拼接 = 末答。
- 多轮 issuing_turn 自推递增（turn_start.turn_end 携带）。
- progress 事件：scripted 模型发 tool_call → 对应进度事件（searching/recognizing_photo/routing）。
- 末答切块保序（2a 模拟 token delta 协议）。

FakeChatModel / scripted 零 token。证明 HTTP→graph.astream→ndjson 通路 + 进度事件派生。
"""

import json

from fastapi.testclient import TestClient
from langchain_core.messages import AIMessage

from shiwujie_ai.service.app import create_app


def _events(r) -> list:
    """ndjson 响应 → 事件 dict 列表（逐行 parse）。"""
    return [json.loads(line) for line in r.text.splitlines() if line.strip()]


# 默认 app（FakeChatModel 回声，零 token）。
client = TestClient(create_app())


def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"


def test_ai_turn_stream_shape():
    """纯问答（FakeChatModel 无 tool_call）：turn_start → delta×N → turn_end。"""
    r = client.post("/ai/turn", json={"thread_id": "smoke-1", "text": "你好"})
    assert r.status_code == 200
    evs = _events(r)
    assert evs[0]["type"] == "turn_start"
    assert evs[0]["issuing_turn"] == 1
    assert evs[-1]["type"] == "turn_end"
    assert evs[-1]["issuing_turn"] == 1
    # 中间全是 delta，无 progress（纯问答不调工具）。
    middle = evs[1:-1]
    assert middle, "应至少一个 delta"
    assert all(e["type"] == "delta" for e in middle)
    # delta 拼接 = 末答全文（含用户输入回声）。
    reply = "".join(e["text"] for e in middle)
    assert "你好" in reply


def test_ai_turn_multiturn_issuing_turn_increments():
    """同 thread 连两轮：issuing_turn 自推 1→2（turn_start/turn_end 携带；emergency 判据靠它）。"""
    tid = "smoke-2"
    r1 = client.post("/ai/turn", json={"thread_id": tid, "text": "第一轮"})
    r2 = client.post("/ai/turn", json={"thread_id": tid, "text": "第二轮"})
    assert _events(r1)[0]["issuing_turn"] == 1
    assert _events(r2)[0]["issuing_turn"] == 2
    reply2 = "".join(e["text"] for e in _events(r2) if e["type"] == "delta")
    assert "第二轮" in reply2


def test_ai_turn_no_progress_on_plain_qa():
    """纯问答不发 progress 事件（无工具）。"""
    r = client.post("/ai/turn", json={"thread_id": "smoke-3", "text": "闲聊"})
    assert not any(e["type"] == "progress" for e in _events(r))


# ───────────────────────── 进度事件（scripted 模型发 tool_call）─────────────────────────


class _ScriptChatModel:
    """按预设 AIMessage 序列响应的假模型（含 tool_call），零 token。"""

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


def _app_with_script(responses):
    return TestClient(create_app(model=_ScriptChatModel(responses)))


def test_progress_event_web_search():
    """web_search tool_call → progress: searching。"""
    c = _app_with_script([
        _tc("web_search", {"query": "新闻"}, "s1"),
        AIMessage(content="为您找到一些新闻。"),
    ])
    r = c.post("/ai/turn", json={"thread_id": "prog-1", "text": "今天有什么新闻"})
    evs = _events(r)
    prog = [e for e in evs if e["type"] == "progress"]
    assert prog and prog[0]["event"] == "searching"
    reply = "".join(e["text"] for e in evs if e["type"] == "delta")
    assert reply == "为您找到一些新闻。"


def test_progress_event_recognize_photo():
    """recognize_photo tool_call → progress: recognizing_photo。"""
    c = _app_with_script([
        _tc("recognize_photo", {"question": "前方"}, "r1"),
        AIMessage(content="前方有辆共享单车。"),
    ])
    r = c.post("/ai/turn", json={"thread_id": "prog-2", "text": "看看前面"})
    assert next(e for e in _events(r) if e["type"] == "progress")["event"] == "recognizing_photo"


def test_progress_event_routing_multistep():
    """导航多步：poi_search + route 连发 → 两个 routing 进度事件（同 event）。"""
    c = _app_with_script([
        _tc("gaode_poi_search", {"query": "医院"}, "p1"),
        _tc("gaode_route", {"destination": "医院", "mode": "walking"}, "r2"),
        AIMessage(content="已为你规划步行路线。"),
    ])
    r = c.post("/ai/turn", json={"thread_id": "prog-3", "text": "导我去最近医院"})
    routings = [e for e in _events(r) if e["type"] == "progress" and e["event"] == "routing"]
    assert len(routings) == 2  # poi_search + route 各一次
    reply = "".join(e["text"] for e in _events(r) if e["type"] == "delta")
    assert reply == "已为你规划步行路线。"


def test_progress_before_deltas():
    """进度事件在 delta 之前（UX：工具执行时先报进度，再出末答）。"""
    c = _app_with_script([
        _tc("get_weather", {"location": "北京"}, "w1"),
        AIMessage(content="北京今天晴。"),
    ])
    evs = _events(c.post("/ai/turn", json={"thread_id": "prog-4", "text": "北京天气"}))
    prog_idx = next(i for i, e in enumerate(evs) if e["type"] == "progress")
    first_delta_idx = next(i for i, e in enumerate(evs) if e["type"] == "delta")
    assert prog_idx < first_delta_idx
    assert evs[prog_idx]["event"] == "searching"  # get_weather → searching
