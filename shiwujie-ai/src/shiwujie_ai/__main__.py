"""uvicorn 入口：`python -m shiwujie_ai`（仅内网 8500，绝不直接公网）。

chunk-2a 烟测（流式 ndjson 事件流，design ⑥）：
    uv --directory shiwujie-ai run python -m shiwujie_ai
    curl -N -X POST localhost:8500/ai/turn -H 'Content-Type: application/json' \
         -d '{"thread_id":"smoke","text":"你好"}'
    # 逐行 ndjson：turn_start → [progress →] delta×N → turn_end

模型：`SHIWUJIE_AI_REAL=1` 切真 `build_llm(PRIMARY_MODEL, parallel=False)`（qwen3.7-plus，chunk-2c 端到端验通 ✅，
5 场景 + HTTP 流式全绿）；默认 FakeChatModel（零 token，保烟测 + 测试套件）。真 qwen 跑法：
    SHIWUJIE_AI_REAL=1 uv --directory shiwujie-ai run python -m shiwujie_ai
真 token delta（stream_mode="messages"）推迟下一轮——现 delta 仍是 chunk_reply 模拟切块（CHUNK_SIZE=4）。

绑定 host（chunk-2d Docker）：
- 本机烟测默认 127.0.0.1（不暴露公网）。
- Docker 部署须绑 0.0.0.0（否则 java 容器经服务名 http://python:8500 连不上）——compose 设
  SHIWUJIE_AI_HOST=0.0.0.0；**Python 仍不发布端口**（仅 compose 内网，不经公网，design 缝 A 安全边界）。
"""

import os

import uvicorn


def main():
    host = os.environ.get("SHIWUJIE_AI_HOST", "127.0.0.1")
    uvicorn.run("shiwujie_ai.service.app:app", host=host, port=8500, log_level="info")


if __name__ == "__main__":
    main()
