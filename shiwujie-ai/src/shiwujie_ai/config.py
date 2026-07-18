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
