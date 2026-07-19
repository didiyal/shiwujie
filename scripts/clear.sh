#!/usr/bin/env bash
# chunk-2d：彻底清理——停容器 + 删容器/网络 + 删两镜像 + 删命名卷。
# ⚠️ 不删宿主 MySQL/Redis 数据（那是外部服务，不归本编排管）。
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

echo "[clear] 即将删除："
echo "  - 容器/网络：shiwujie-java / shiwujie-python / 网络 shiwujie_default"
echo "  - 镜像：shiwujie-java:latest / shiwujie-python:latest"
echo "  - 命名卷（本编排当前未声明，若有则一并 -v 删）"
echo "  ⚠️  保留：宿主 MySQL/Redis 数据（外部服务）"
echo
read -r -p "[clear] 确认彻底清理？输入 yes 继续：" ANS
if [ "$ANS" != "yes" ]; then
  echo "[clear] 已取消。"
  exit 0
fi

echo "[clear] 停止并删容器/网络/卷..."
docker compose -p shiwujie -f docker/docker-compose.yml down -v --rmi local 2>/dev/null || \
  docker compose -p shiwujie -f docker/docker-compose.yml down -v

echo "[clear] 兜底删两镜像（若 down --rmi local 没删干净）..."
docker rmi shiwujie-java:latest shiwujie-python:latest 2>/dev/null || true

echo "[clear] 完成。重启：./scripts/start.sh --build"
