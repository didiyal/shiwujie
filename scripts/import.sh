#!/usr/bin/env bash
# chunk-2d：离线交付目标机侧——从 .tar 灌入两镜像（不构建，需先 export.sh 出包）。
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

SRC="docker/shiwujie-images.tar"
if [ ! -f "$SRC" ]; then
  echo "[import] 找不到 $SRC；先在构建机执行 ./scripts/export.sh" >&2
  exit 1
fi

echo "[import] 从 $SRC 灌入镜像..."
docker load -i "$SRC"

echo "[import] 完成。下一步：./scripts/start.sh（镜像已就位，不会触发构建）"
