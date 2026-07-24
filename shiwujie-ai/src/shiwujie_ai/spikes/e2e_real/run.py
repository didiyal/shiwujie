"""chunk-2c e2e 真验证：graph 直调真 qwen3.7-plus（parallel=False）端到端 5 场景。

绕 FastAPI/HTTP 省 token——graph.invoke 直调。外部 API mock（gaode/searchapi 默认 mock）、
Java-MCP stub（build_toolset 全 stub 体，唯 emergency prepare/confirm 是真体）、VLM 真
（SHIWUJIE_VLM_FORCE_REAL=1 + provisioned key）。

**前置**：config.py 内联 provisioned MaaS key（qwen3.7-plus / qwen3-vl-flash）。运行：
    cd shiwujie-ai
    SHIWUJIE_VLM_FORCE_REAL=1 uv run python -m shiwujie_ai.spikes.e2e_real.run

**5 场景**（每场景打印 input / 消息链 / tool_calls / 末答 / checkpoint 续轮）：
1. 纯问答（期望零工具调用）—— agent→END。
2. 单工具 FC（get_weather mock）—— agent→tools→agent→END round-trip。
3. navigation 多轮 checkpoint 续 —— T1 read_skill+poi→问交通（自然 END，HITL）；T2 步行→
   route→launch（checkpoint 载回 T1 历史续）。验真模型是否照 navigation skill 流程走。
4. emergency turn-bound —— T1 prepare→问确认（END）；T2 是→confirm 跨轮放行。验 design ⑬
   红队 Q18 硬修正 1：真模型走「跨轮 confirm」路径（同轮拒由 store 结构保证 + test_emergency 覆盖）。
5. VLM —— 直调 tools.vlm.recognize，真 qwen3-vl-flash（1 发，绕 graph 省 LLM 往返）。

**token 预算** ~15-25 发（ping 1 + 场景 1×1 + 2×2 + 3×~6 + 4×~4 + VLM×1；parallel=False 每轮
单 tool_call，多步流程走多轮 agent→tools→agent）。
"""

import json
import os
import sys

# Windows 控制台默认 GBK，emoji（✅/❌）+ 中文混排会 UnicodeEncodeError——强制 stdout UTF-8。
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

from langchain_core.messages import HumanMessage
from langgraph.checkpoint.memory import MemorySaver

from ... import config
from ...graph import build_graph, build_system_prompt
from ...llm import build_llm
from ...safety import emergency
from ...tools.registry import build_toolset
from ...tools.vlm import recognize as vlm_recognize

#: 默认 VLM 测试图（公网稳定示例图；env SHIWUJIE_E2E_IMAGE_URL 可覆盖）。
_DEFAULT_IMAGE_URL = "https://www.gstatic.com/webp/gallery/1.jpg"

_SEP = "═" * 70


# ───────────────────────── 打印 helper ─────────────────────────


def _banner(title: str) -> None:
    print(f"\n{_SEP}\n{title}\n{_SEP}")


def _tool_names(ai_msg) -> list:
    tcs = getattr(ai_msg, "tool_calls", None) or []
    return [tc["name"] for tc in tcs]


def _print_messages(label: str, messages) -> None:
    """打印消息链摘要：每条 [type] content 片段 + tool_calls / tool_call_id。"""
    print(f"── {label}（{len(messages)} 条）──")
    for m in messages:
        t = getattr(m, "type", "?")
        content = (getattr(m, "content", "") or "").replace("\n", " ")
        snippet = content[:140]
        tcs = _tool_names(m)
        tcid = getattr(m, "tool_call_id", None)
        name = getattr(m, "name", None)
        if tcs:
            extra = f"  tool_calls={tcs}"
        elif tcid:
            extra = f"  ← tool_result(name={name}, id={tcid}): {snippet[:80]}"
        else:
            extra = ""
        print(f"  [{t}] {snippet}{extra}")


def _final(result) -> str:
    return ((getattr(result["messages"][-1], "content", "") or "").strip())[:300]


def _all_ai_tool_calls(messages) -> list:
    """消息链里所有 AIMessage 的 tool_call 名（保序，含跨轮）。"""
    out = []
    for m in messages:
        if getattr(m, "type", "") == "ai":
            out.extend(_tool_names(m))
    return out


# ───────────────────────── ping ─────────────────────────


def _ping(model) -> bool:
    """1 发可达性 ping：provisioned key + qwen3.7-plus 活性。fail → abort。"""
    _banner("PING：qwen3.7-plus 可达性（provisioned MaaS key）")
    try:
        r = model.invoke([HumanMessage(content="只回两个字：可达")])
        print(f"  ✅ 模型回复：{(getattr(r, 'content', '') or '').strip()[:60]}")
        return True
    except Exception as e:  # encode-不抛精神，但 ping 失败应 abort
        print(f"  ❌ ping 失败：{type(e).__name__}: {e}")
        return False


# ───────────────────────── 5 场景 ─────────────────────────


def scenario_1_pure_chat(graph) -> None:
    _banner("场景 1：纯问答（期望零工具调用）")
    cfg = {"configurable": {"thread_id": "e2e-s1"}}
    text = "你好，请用一句话介绍你自己。"
    print(f"用户：{text}")
    r = graph.invoke({"messages": [HumanMessage(content=text)], "blind_id": "e2e-s1"}, cfg)
    _print_messages("消息链", r["messages"])
    no_tool = not _tool_names(r["messages"][-1])
    print(f"\n✅ 无 tool_calls（纯问答）: {no_tool}")
    print(f"末答：{_final(r)}")


def scenario_2_single_tool(graph) -> None:
    _banner("场景 2：单工具 FC（get_weather，外部 API mock）")
    cfg = {"configurable": {"thread_id": "e2e-s2"}}
    pos = {"lat": 30.5728, "lng": 104.0668, "address": "成都市天府广场"}
    text = "我现在这个地方天气怎么样？"
    print(f"用户：{text}（position={pos['address']}）")
    r = graph.invoke(
        {"messages": [HumanMessage(content=text)], "blind_id": "e2e-s2", "position": pos},
        cfg,
    )
    _print_messages("消息链", r["messages"])
    tools_called = _all_ai_tool_calls(r["messages"])
    print(f"\n工具调用：{tools_called}")
    print(f"末答：{_final(r)}")


def scenario_3_navigation_multiturn(graph) -> None:
    _banner("场景 3：navigation 多轮 checkpoint 续（read_skill+poi → 问交通 END；步行 → route → launch）")
    cfg = {"configurable": {"thread_id": "e2e-s3"}}
    pos = {"lat": 30.5728, "lng": 104.0668, "address": "成都市天府广场"}

    t1 = "帮我导航去最近的医院"
    print(f"[T1] 用户：{t1}")
    r1 = graph.invoke(
        {"messages": [HumanMessage(content=t1)], "blind_id": "e2e-s3", "position": pos}, cfg
    )
    _print_messages("T1 消息链", r1["messages"])
    t1_tools = _all_ai_tool_calls(r1["messages"])
    last1 = r1["messages"][-1]
    print(f"\nT1 工具调用：{t1_tools}")
    print(f"T1 自然停（末无 tool_calls = HITL 问交通）：{not _tool_names(last1)}")
    print(f"T1 末答：{_final(r1)}")

    before = len(graph.get_state(cfg).values.get("messages", []))
    t2 = "步行"
    print(f"\n[T2] 用户：{t2}（checkpoint 现有 {before} 条消息，应含 T1 skill body + poi 结果）")
    r2 = graph.invoke(
        {"messages": [HumanMessage(content=t2)], "blind_id": "e2e-s3", "position": pos}, cfg
    )
    _print_messages("T2 消息链（全量含 T1 历史）", r2["messages"])
    all_tools = _all_ai_tool_calls(r2["messages"])
    has_launch = "launch_navigation" in all_tools
    print(f"\n累计工具调用：{all_tools}")
    print(f"✅ 跨轮续到 launch_navigation：{has_launch}")
    print(f"T2 末答：{_final(r2)}")


def scenario_4_emergency(graph) -> None:
    _banner("场景 4：emergency turn-bound（prepare → 问确认 END；是 → confirm 跨轮放行）")
    emergency.reset_store(token_factory=lambda: "EMERG-E2E")
    cfg = {"configurable": {"thread_id": "e2e-s4"}}

    t1 = "我摔倒了，快通知我家属"
    print(f"[T1] 用户：{t1}（issuing_turn=1）")
    emergency.set_turn_context("e2e-s4", issuing_turn=1)
    r1 = graph.invoke(
        {"messages": [HumanMessage(content=t1)], "blind_id": "e2e-s4", "issuing_turn": 1}, cfg
    )
    _print_messages("T1 消息链", r1["messages"])
    t1_tools = _all_ai_tool_calls(r1["messages"])
    print(f"\nT1 工具调用：{t1_tools}（含 prepare：{'request_emergency_help_prepare' in t1_tools}）")
    print(f"T1 自然停（问确认）：{not _tool_names(r1['messages'][-1])}")
    print(f"T1 末答：{_final(r1)}")

    t2 = "是，立即通知他们"
    print(f"\n[T2] 用户：{t2}（issuing_turn=2，跨轮 → confirm 应放行）")
    emergency.set_turn_context("e2e-s4", issuing_turn=2)
    r2 = graph.invoke(
        {"messages": [HumanMessage(content=t2)], "blind_id": "e2e-s4", "issuing_turn": 2}, cfg
    )
    _print_messages("T2 消息链", r2["messages"])
    confirm_msgs = [
        m
        for m in r2["messages"]
        if getattr(m, "type", "") == "tool" and getattr(m, "name", "") == "request_emergency_help_confirm"
    ]
    status = None
    if confirm_msgs:
        try:
            status = json.loads(confirm_msgs[0].content).get("status")
        except Exception:
            status = "?parse-fail"
    print(f"\nconfirm 工具返回 status：{status}")
    print(f"✅ 跨轮 confirm 放行（status=ok）：{status == 'ok'}")
    print(f"T2 末答：{_final(r2)}")


def scenario_5_vlm() -> None:
    _banner("场景 5：VLM（真 qwen3-vl-flash，直调 recognize 绕 graph 省 LLM 往返）")
    if os.environ.get("SHIWUJIE_VLM_FORCE_REAL", "") != "1":
        print("  ⏭️  跳过：未设 SHIWUJIE_VLM_FORCE_REAL=1（mock 不验真 VLM 通路）")
        return
    img = os.environ.get("SHIWUJIE_E2E_IMAGE_URL", _DEFAULT_IMAGE_URL)
    print(f"图片 URL：{img}")
    print(f"VLM 模型：{config.VLM_MODEL}")
    try:
        desc = vlm_recognize(question="用不超过 80 字描述图片主要内容，侧重盲人关心的障碍物/文字/方位。", image=img)
        print(f"\n✅ VLM 描述：{desc[:400]}")
    except Exception as e:
        print(f"❌ VLM 调用失败：{type(e).__name__}: {e}")


# ───────────────────────── main ─────────────────────────


def main() -> None:
    _banner("chunk-2c e2e 真验证：qwen3.7-plus（parallel=False）+ MemorySaver")
    print(f"BASE_URL        = {config.BASE_URL}")
    print(f"PRIMARY_MODEL   = {config.PRIMARY_MODEL}")
    print(f"VLM_MODEL       = {config.VLM_MODEL}")
    print(f"VLM_FORCE_REAL  = {os.environ.get('SHIWUJIE_VLM_FORCE_REAL', '') == '1'}")
    print(f"GAODE_KEY       = {'<set>' if config.GAODE_KEY else '<empty → mock>'}")
    print(f"SEARCHAPI_KEY   = {'<set>' if config.SEARCHAPI_KEY else '<empty → mock>'}")

    model = build_llm(config.PRIMARY_MODEL, parallel=False)
    if not _ping(model):
        print("\n❌ ping 失败，abort（检查 provisioned key / 网络 / BASE_URL）。")
        sys.exit(1)

    graph = build_graph(
        model, build_toolset(), build_system_prompt, checkpointer=MemorySaver()
    )

    scenarios = [
        ("场景 1 纯问答", lambda: scenario_1_pure_chat(graph)),
        ("场景 2 单工具 FC", lambda: scenario_2_single_tool(graph)),
        ("场景 3 navigation 多轮", lambda: scenario_3_navigation_multiturn(graph)),
        ("场景 4 emergency turn-bound", lambda: scenario_4_emergency(graph)),
        ("场景 5 VLM", scenario_5_vlm),
    ]
    summary = []
    for name, fn in scenarios:
        ok, err = True, ""
        try:
            fn()
        except Exception as e:
            ok = False
            err = f"{type(e).__name__}: {e}"
            print(f"\n❌ {name} 抛异常：{err}")
        summary.append((name, ok, err))

    _banner("汇总")
    for name, ok, err in summary:
        print(f"{'✅' if ok else '❌'} {name}" + (f" — {err}" if err else ""))


if __name__ == "__main__":
    main()
