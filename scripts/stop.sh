#!/usr/bin/env bash
# chunk-2d：停止双进程（保留容器/网络/卷）。
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"
docker compose -p shiwujie -f docker/docker-compose.yml down
echo "[stop] 已停止（镜像/卷保留；彻底清理：./scripts/clear.sh）"
