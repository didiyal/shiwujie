"""Spike 2b-3b：验业务 MCP 工具真身端到端接线（缝 C Java 侧 service 调用）。

带 X-Blind-Id header 连 Java MCP server，验证：
1. tools/list 暴露 8 工具（whoami 已随 2b-3a 验完移除；业务 4 真身 + 信令 4 桩）
2. family_info 经 BlindMcpContext.blindId 透传 → BlindService.getById → 返回真实 service 结果
   （blind_id=123 假 id，库里应无 → 期望「未找到当前用户」，验透传 + service 接线 + encode-不抛，
   且只读不写库无副作用）

FREE：纯 RPC + DB 查询，不烧 LLM（不跑 graph / qwen）。端点 / secret 沿用 config（与 whoami.py 同源）。
"""

import asyncio

from langchain_mcp_adapters.client import MultiServerMCPClient

from ... import config

# blind_id=123 假 id（与 whoami spike 同），库里无此盲人 → getById 返 null → 「未找到当前用户」
TEST_BLIND_ID = "123"
TEST_SECRET = "dev-internal-secret"


async def run() -> dict:
    report: dict = {
        "url": config.JAVA_MCP_URL,
        "tool_count": None,
        "tool_names": None,
        "whoami_removed": None,
        "family_info": None,
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
        names = sorted(t.name for t in tools)
        report["tool_count"] = len(names)
        report["tool_names"] = names
        # whoami 应已移除（生产 LLM 不应见调试工具）
        report["whoami_removed"] = "whoami" not in names
        print(f"[biz-spike] tools({len(names)}): {names}")
        if not report["whoami_removed"]:
            print("[biz-spike][WARN] whoami 仍在（2b-3a 移除未生效？）")

        # family_info（只读最安全）：blind_id=123 → getById null → 「未找到当前用户」
        fi = next((t for t in tools if t.name == "family_info"), None)
        if fi is None:
            report["error"] = "family_info 未暴露"
            print(f"[biz-spike][ERR] {report['error']}")
            return report
        result = await fi.ainvoke({})
        report["family_info"] = str(result)
        print(f"[biz-spike] family_info() -> {result}")

        verdict = "OK" if "未找到当前用户" in str(result) else "CHECK"
        print(f"[biz-spike] service 接线 {verdict}（期望 blind_id={TEST_BLIND_ID} 不存在 → 未找到当前用户）")
    except Exception as e:  # noqa: BLE001 — spike：任何失败都落 report
        report["error"] = f"{type(e).__name__}: {e}"
        print(f"[biz-spike][ERR] {report['error']}")
    return report


if __name__ == "__main__":
    asyncio.run(run())
