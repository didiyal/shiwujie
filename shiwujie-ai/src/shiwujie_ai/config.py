"""模型来源配置（env 覆盖 + 内联默认）。

⚠️ Python base_url 必须带 /v1：openai SDK 只拼 /chat/completions。
   （与 Java 侧不同——Spring AI 自拼 /v1/chat/completions，故 yml base-url 不带 /v1。）

默认百炼 provisioned MaaS 端点（用户私有部署子域 llm-8oompsig0r5hox8l）：
- 主对话/FC 模型 = qwen3.7-plus（多模态智能体，文本 FC + 图像理解；2026-07-18 选型锁定）。
- 拍照识别 VLM = qwen3-vl-flash（保留旧 ImageApp 用的 vl-flash；拍照按钮直连绕 loop 用）。
切其他端点/模型：设 env SHIWUJIE_AI_BASE_URL / API_KEY / MODEL / VLM_MODEL。
"""

import os

BASE_URL = os.environ.get(
    "SHIWUJIE_AI_BASE_URL",
    "https://llm-8oompsig0r5hox8l.cn-beijing.maas.aliyuncs.com/compatible-mode/v1",
)
API_KEY = os.environ.get(
    "SHIWUJIE_AI_API_KEY",
    "sk-ws-H.EDXRRIY.J6km.MEYCIQDyHREnYUEDiOwToFfUhCLQ3ubj3r_9vbclqwtXp5rBhQIhAJUrBz50o1fflzxQopP3uzJvMvj1Jj7pD0PgcNHNbQAH",
)
PRIMARY_MODEL = os.environ.get("SHIWUJIE_AI_MODEL", "qwen3.7-plus")
VLM_MODEL = os.environ.get("SHIWUJIE_AI_VLM_MODEL", "qwen3-vl-flash")
#: Java 单体 MCP server 端点（缝 C：spring-ai-starter-mcp-server-webmvc streamable HTTP，暴露 8 工具）。
#: Spike-2 连本端点验 get_tools() round-trip + #466 teardown。
JAVA_MCP_URL = os.environ.get("SHIWUJIE_JAVA_MCP_URL", "http://localhost:8100/mcp")
#: 高德 web 服务 key（design ⑦ 自建 REST wrapper：poi/route/weather）。空 → 走 mock（chunk-2a FREE，
#: 零网络零 key）；chunk-2c 填真 key + SHIWUJIE_GAODE_FORCE_REAL=1 连 /place/text 等真端点。
#: （凭据留 env 内联默认，沿用 shiwujie-config-convention；真 key 拿到后 inline 到此默认。）
GAODE_KEY = os.environ.get("SHIWUJIE_GAODE_KEY", "")
#: searchapi.io key（web_search 工具，engine=baidu）。空 → mock（2a FREE）；2c 填真 key + FORCE_REAL=1。
SEARCHAPI_KEY = os.environ.get("SHIWUJIE_SEARCHAPI_KEY", "")
