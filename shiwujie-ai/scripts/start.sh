#!/usr/bin/env bash
# chunk-2d：Python AI 服务本地运行模式（非 Docker）。
# 前置：先在 shiwujie-ai/ 下 `uv sync`（装依赖到 .venv）。
# 本地默认绑 127.0.0.1:8500；Docker 经 SHIWUJIE_AI_HOST=0.0.0.0 覆盖（见 __main__.py）。
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
AI_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$AI_DIR"

if [ ! -d ".venv" ]; then
  echo "[start] 找不到 .venv；先执行：uv sync" >&2
  exit 1
fi

echo "[start] uv run python -m shiwujie_ai（FastAPI :8500，默认 127.0.0.1）..."
exec uv run python -m shiwujie_ai
