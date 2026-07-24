#!/usr/bin/env bash
# chunk-2d：Java 后端本地运行模式（非 Docker）。
# Dockerfile 也可直接 CMD 此脚本——但后端镜像 ENTRYPOINT 用裸 java -jar 更直接，本脚本供本地 dev。
# 前置：先在仓库根 `mvn -f shiwujie-backend/pom.xml install -DskipTests` 产出 fat jar。
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR="$BACKEND_DIR/shiwujie-bootstrap/target/shiwujieBootstrap-0.0.1-SNAPSHOT.jar"

if [ ! -f "$JAR" ]; then
  echo "[start] 找不到 $JAR" >&2
  echo "[start] 先在仓库根执行：mvn -f shiwujie-backend/pom.xml install -DskipTests" >&2
  exit 1
fi

echo "[start] java -jar（单体 :8100，连宿主 MySQL/Redis 默认公网 IP）..."
exec java -jar "$JAR"
