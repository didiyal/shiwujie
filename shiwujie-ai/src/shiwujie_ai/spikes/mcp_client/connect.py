"""Spike-2 Python 端：连 Java MCP server（缝 C）验管道 + #466 teardown。

跑三件事并打结构化报告：
1. get_tools() 拿到 N 个工具（Java SpikeMcpTools 暴露的 8 个）。
2. round-trip：调一个无参工具拿桩结果（自动试 ainvoke({})，首成功即止——无参工具 succeed，
   需参工具抛错跳过，自动适配 Spring AI camelCase 方法名）。
3. #466：streamable-HTTP teardown 噪声（langchain-mcp-adapters 历史坑）——0.3.0 禁了
   MultiServerMCPClient 的 async-with（__aexit__ 抛 NotImplementedError），改直接式
   client.get_tools()（内部自管连接）。teardown 噪声会在 asyncio loop 关停时冒出，
   本脚本由 __main__ 的 asyncio.run 外层捕获归类（run() 体内捕连接/取工具错，外层捕
   loop 关停 teardown 错）——按 design，逐 turn 用法不受非阻塞 teardown 影响。

端点来源 config.JAVA_MCP_URL（env SHIWUJIE_JAVA_MCP_URL 覆盖，默认 http://localhost:8100/mcp）。
"""

from langchain_mcp_adapters.client import MultiServerMCPClient

from ... import config


async def run() -> dict:
    """连 Java MCP server 跑一轮验证，返回结论 dict（不含 teardown 探测——交 __main__ 外层）。"""
    report: dict = {
        "url": config.JAVA_MCP_URL,
        "tool_count": None,
        "tool_names": [],
        "roundtrip": None,
        "phase_error": None,
    }
    client = MultiServerMCPClient(
        {"shiwujie": {"url": config.JAVA_MCP_URL, "transport": "http"}}
    )
    phase = "get_tools"
    try:
        tools = await client.get_tools()
        report["tool_count"] = len(tools)
        report["tool_names"] = [t.name for t in tools]
        print(f"[mcp] get_tools() → {len(tools)}: {report['tool_names']}")

        phase = "roundtrip"
        # 无参工具（familyInfo/leaveFamily/requestVideoHelp）ainvoke({}) succeed；
        # 需参工具抛错→跳过。首成功即 round-trip 通。
        for t in tools:
            try:
                result = await t.ainvoke({})
                report["roundtrip"] = {"tool": t.name, "result": str(result)}
                print(f"[mcp] round-trip {t.name}() → {result}")
                break
            except Exception:  # noqa: BLE001 — 需参工具正常抛，试下一个
                continue
        if report["roundtrip"] is None:
            report["roundtrip"] = {"skipped": "no no-arg tool succeeded"}
    except Exception as e:  # noqa: BLE001 — 连接/取工具/round-trip 失败
        report["phase_error"] = f"{phase}: {type(e).__name__}: {e}"
        print(f"[mcp][ERR] {phase}: {type(e).__name__}: {e}")
    return report
