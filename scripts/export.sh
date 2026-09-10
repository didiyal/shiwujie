#!/usr/bin/env bash
# 离线交付——把镜像打包成 .tar（无 Docker 环境的目标机用 import.sh 灌入再 start.sh）。
# AI 回退后 Python 默认不构建：java 镜像必选；python 镜像存在才打包（AI 重启后自动重新纳入）。
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

OUT="docker/shiwujie-images.tar"
mkdir -p docker

echo "[export] 检查镜像存在..."
docker image inspect shiwujie-java:latest >/dev/null

IMAGES=(shiwujie-java:latest)
if docker image inspect shiwujie-python:latest >/dev/null 2>&1; then
  IMAGES+=(shiwujie-python:latest)
  echo "[export] python 镜像存在，一并打包"
else
  echo "[export] python 镜像不存在（AI 回退后默认不构建），跳过"
fi

echo "[export] 打包 → $OUT（较大，请稍候）..."
docker save "${IMAGES[@]}" -o "$OUT"

# gzip 压缩可选；保留未压缩 .tar 以便 import.sh 直接 load（避免目标机缺 gzip）
SIZE=$(du -h "$OUT" | cut -f1)
echo "[export] 完成：$OUT ($SIZE)"
echo "[export] 拷贝 $OUT + 整个仓库到目标机 → ./scripts/import.sh → ./scripts/start.sh"
