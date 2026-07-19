#!/usr/bin/env bash
# chunk-2d：启动视无界双进程（Java + Python）Docker 编排。
# 用法：./scripts/start.sh [--build]
#   --build  强制重新构建镜像（改了源码后）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

ENV_FILE="config/.env"
if [ ! -f "$ENV_FILE" ]; then
  touch "$ENV_FILE"   # 空 .env → 容器走 yml/config.py committed 默认；prod 覆盖见 config/.env.example
  echo "[start] 已创建空 $ENV_FILE（可选覆盖见 config/.env.example）"
fi

COMPOSE=(docker compose -p shiwujie -f docker/docker-compose.yml)

BUILD_FLAG=()
if [ "${1:-}" = "--build" ]; then
  BUILD_FLAG=(--build)
  echo "[start] 强制重建镜像"
fi

echo "[start] 构建并启动（java:8100 公网 / python 内网）..."
"${COMPOSE[@]}" up -d "${BUILD_FLAG[@]}"

echo
echo "[start] 容器状态："
"${COMPOSE[@]}" ps
echo
echo "[start] 日志：./scripts/logs.sh    停止：./scripts/stop.sh"
