"""uvicorn 入口：`python -m shiwujie_ai`（仅内网 127.0.0.1:8500，绝不公网）。

chunk-2a 烟测（流式 ndjson 事件流，design ⑥）：
    uv --directory shiwujie-ai run python -m shiwujie_ai
    curl -N -X POST localhost:8500/ai/turn -H 'Content-Type: application/json' \
         -d '{"thread_id":"smoke","text":"你好"}'
    # 逐行 ndjson：turn_start → [progress →] delta×N → turn_end

模型 = FakeChatModel（零 token，末答切块模拟 delta）。chunk-2c 换真 qwen + stream_mode="messages"
真 token delta 时改 service.app 注入 llm.build_llm()。部署态（chunk-2d Docker）host 改容器内
0.0.0.0；本机烟测绑 127.0.0.1。
"""

import uvicorn


def main():
    uvicorn.run("shiwujie_ai.service.app:app", host="127.0.0.1", port=8500, log_level="info")


if __name__ == "__main__":
    main()
