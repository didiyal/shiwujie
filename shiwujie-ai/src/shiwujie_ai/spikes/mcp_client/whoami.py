"""Spike 2b-3：验 X-Blind-Id header 经 MCP transport context 透传到 @Tool（缝 C blind_id 流）。

Python 端带 header 连 Java MCP server，调 SpikeMcpTools.whoami(ToolContext) → 期望回显 blind_id。

机制链路（调研落锤）：
  langchain-mcp-adapters 0.3.0 headers（httpx client-level，每次 JSON-RPC POST 带）
  → Java McpTransportContextExtractor（McpTransportConfig）抓 X-Blind-Id
  → McpTransportContext → reactor context 透传
  → Spring AI ToolContext（McpToolUtils.getMcpExchange）
  → BlindMcpContext.blindId(toolContext)

期望 whoami() 返回含 blind_id=123。FREE：whoami 不经 LLM，纯 RPC 验管道。
端点 / 默认 secret 沿用 config（与 connect.py 同源）。
"""

import asyncio

from langchain_mcp_adapters.client import MultiServerMCPClient

from ... import config

# spike 测试值：blind_id 任意（whoami 回显）；secret 与 Java yml 默认一致（Java 当前不校验，仅验透传）。
TEST_BLIND_ID = "123"
TEST_SECRET = "dev-internal-secret"


async def run() -> dict:
    """带 header 连 Java MCP server 调 whoami，返回结论 dict。"""
    report: dict = {
        "url": config.JAVA_MCP_URL,
        "expected_blind_id": TEST_BLIND_ID,
        "tool_count": None,
        "whoami": None,
        "blind_id_seen": None,
        "error": None,
    }
    client = MultiServerMCPClient(
        {
            "shiwujie": {
                "url": config.JAVA_MCP_URL,
                "transport": "http",
                "headers": {
                    "X-Blind-Id": TEST_BLIND_ID,
                    "X-Internal-Secret": TEST_SECRET,
                },
            }
        }
    )
    try:
        tools = await client.get_tools()
        names = [t.name for t in tools]
        report["tool_count"] = len(names)
        print(f"[whoami-spike] get_tools() → {len(names)}: {names}")

        whoami = next((t for t in tools if t.name == "whoami"), None)
        if whoami is None:
            report["error"] = "whoami tool 未暴露（Java 端 SpikeMcpTools.whoami 未注册？）"
            print(f"[whoami-spike][ERR] {report['error']}")
            return report

        result = await whoami.ainvoke({})
        report["whoami"] = str(result)
        # whoami 返回 {"blind_id":<num>,...}，校验回显的 blind_id 是否 == 传入
        report["blind_id_seen"] = TEST_BLIND_ID in str(result)
        print(f"[whoami-spike] whoami() -> {result}")
        verdict = "OK" if report["blind_id_seen"] else "FAIL"
        print(f"[whoami-spike] transport {verdict} (expect blind_id contains {TEST_BLIND_ID})")
    except Exception as e:  # noqa: BLE001 — spike：任何失败都落 report
        report["error"] = f"{type(e).__name__}: {e}"
        print(f"[whoami-spike][ERR] {report['error']}")
    return report


if __name__ == "__main__":
    asyncio.run(run())
