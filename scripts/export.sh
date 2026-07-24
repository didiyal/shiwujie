#!/usr/bin/env bash
# chunk-2d：离线交付——把两镜像打包成 .tar（无 Docker 环境的目标机用 import.sh 灌入再 start.sh）。
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

OUT="docker/shiwujie-images.tar"
mkdir -p docker

echo "[export] 检查镜像存在..."
docker image inspect shiwujie-java:latest >/dev/null
docker image inspect shiwujie-python:latest >/dev/null

echo "[export] 打包 → $OUT（较大，请稍候）..."
docker save shiwujie-java:latest shiwujie-python:latest -o "$OUT"

# gzip 压缩可选；保留未压缩 .tar 以便 import.sh 直接 load（避免目标机缺 gzip）
SIZE=$(du -h "$OUT" | cut -f1)
echo "[export] 完成：$OUT ($SIZE)"
echo "[export] 拷贝 $OUT + 整个仓库到目标机 → ./scripts/import.sh → ./scripts/start.sh"
