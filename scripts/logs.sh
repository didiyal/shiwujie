#!/usr/bin/env bash
# 跟随运行中容器日志（AI 回退后默认仅 java；--with-ai 启动后才含 python）。
# 用法：./scripts/logs.sh [java|python]   （可选只跟一个服务）
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"
COMPOSE=(docker compose -p shiwujie -f docker/docker-compose.yml)

if [ "${1:-}" = "java" ] || [ "${1:-}" = "python" ]; then
  echo "[logs] 只跟 $1"
  "${COMPOSE[@]}" logs -f --tail=200 "$1"
else
  echo "[logs] 跟全部（Ctrl+C 退出；只读，不停容器）"
  "${COMPOSE[@]}" logs -f --tail=100
fi
