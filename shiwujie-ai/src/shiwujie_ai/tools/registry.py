"""工具注册中心 —— design ⑦。组装 graph 用的 BaseTool 列表（唯一装配点）。

chunk-2a：build_toolset() 返 16 个 stub 体可跑工具（native 6 + read_skill 1 + java_mcp 9），
让 graph 在零网络/零 token 下跑通控制流。

后续 chunk 在此替换（签名随之扩展）：
- chunk-2b：Java 9 工具改由真 MCP server 加载（MultiServerMCPClient.get_tools()，见
  spikes/mcp_client/connect.py 直连式模式），MCP 不可达回退本地 stub。届时加 mcp_url / allow_mcp 参数。
- C2a-4：native 6 体换真 mock-external-API（searchapi/高德REST/weather）/真 VLM。
- C2a-5：read_skill 体换真 SKILL.md loader。
"""

from . import ALL_TOOLS


def build_toolset():
    """组装 graph 工具集（chunk-2a：全 stub 体可跑）。

    返回新列表（调用方可安全增删，不污染 ALL_TOOLS）。
    """
    return list(ALL_TOOLS)
