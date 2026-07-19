# syntax=docker/dockerfile:1
# chunk-2d：Python AI 服务多阶段构建（uv 装依赖到 .venv → python:3.12-slim 运行）。
# build context = shiwujie-ai/（compose 配 context: ../shiwujie-ai）。
# 服务 = FastAPI /ai/turn（chunk-2a FakeChatModel，零外部依赖即可起 /health；chunk-2c 换真 qwen）。
# ⚠️ 绑定 host：容器须 SHIWUJIE_AI_HOST=0.0.0.0（compose 已设），否则 java 容器经服务名连不上。

# ── stage 1: build .venv（仅装依赖，不装项目包 → stage2 用 PYTHONPATH 找 src，免 editable 路径漂移）──
FROM python:3.12-slim AS build
WORKDIR /workspace
RUN pip install --no-cache-dir uv
COPY pyproject.toml uv.lock ./
# --frozen：严格按 uv.lock（revision 3）；--no-dev：不装 pytest；--no-install-project：只装第三方依赖
RUN uv sync --frozen --no-dev --no-install-project

# ── stage 2: runtime（slim，不带 uv/构建缓存）──
FROM python:3.12-slim
WORKDIR /app
COPY --from=build /workspace/.venv /app/.venv
COPY src/ /app/src
ENV PATH="/app/.venv/bin:${PATH}" \
    PYTHONPATH="/app/src" \
    PYTHONUNBUFFERED=1
# 8500 = FastAPI /ai/turn + /health（仅 compose 内网，不公网发布，见 docker-compose.yml）
EXPOSE 8500
ENTRYPOINT ["python", "-m", "shiwujie_ai"]
