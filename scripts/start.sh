#!/usr/bin/env bash
# 启动视无界 Docker 编排（AI 回退后默认仅 Java；Python AI 挂 profiles: ["ai"] 按需启用）。
# 用法：./scripts/start.sh [--build] [--with-ai]
#   --build    强制重新构建镜像（改了源码后）
#   --with-ai  额外启动 Python AI 服务（AI 重写重启时用；默认不启动，见 docker/docker-compose.yml 头注）
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
PROFILE_FLAG=()
for arg in "$@"; do
  case "$arg" in
    --build)
      BUILD_FLAG=(--build)
      echo "[start] 强制重建镜像"
      ;;
    --with-ai)
      PROFILE_FLAG=(--profile ai)
      echo "[start] 附带启动 Python AI 服务（--profile ai）"
      ;;
    *)
      echo "[start] 未知参数：$arg（支持 --build / --with-ai）" >&2
      exit 1
      ;;
  esac
done

if [ -n "$PROFILE_FLAG" ]; then
  echo "[start] 构建并启动（java:8100 公网 / python 内网）..."
else
  echo "[start] 构建并启动（仅 java:8100 公网；Python AI 默认不启动，加 --with-ai 启用）..."
fi
"${COMPOSE[@]}" "${PROFILE_FLAG[@]}" up -d "${BUILD_FLAG[@]}"

echo
echo "[start] 容器状态："
"${COMPOSE[@]}" "${PROFILE_FLAG[@]}" ps
echo
echo "[start] 日志：./scripts/logs.sh    停止：./scripts/stop.sh"
