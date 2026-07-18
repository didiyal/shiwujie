# Python AI 服务部署

> Python 自建 ReAct loop 智能体服务的部署。development 层（允许启动命令 / 端口 / 配置）。**状态：设计敲定（Phase 1–4）· 实现待 Phase 5**——下列为本设计的目标部署形态，代码与 Dockerfile 尚未落地。Java 侧部署见 backend [deployment.md](../../shiwujie-backend/docs/deployment.md)；polyglot 双进程 compose 总图见根 [architecture/ai-rewrite.md](../../docs/architecture/ai-rewrite.md)。

> **铁律：Python 服务绝不公网发布端口。** App 只认 Java `:8100`（公网）；Python `:8500` 仅内网可达，Java 经 compose 服务名 `http://python:8500` 调。Python 不持用户 JWT，不直接面向客户端。

---

## 本地模式（uvicorn 直跑）

非 Docker 本地开发模式（与 Java `mvn install + java -jar` 对偶，Python 用 uvicorn 直跑）：

```bash
# 绑容器内网 / 127.0.0.1:8500，绝不 0.0.0.0 公网
uvicorn shiwujie_ai.app:app --host 127.0.0.1 --port 8500
```

- `--host 127.0.0.1`（或容器内网网卡）：**绝不** `0.0.0.0` 公网绑定。
- 本地模式仍需连宿主 Redis db=2（checkpoint `ai:ckpt:` 前缀 + 偏好 hash）+ MySQL 库 `shiwujie`（AiLogs 审计）。
- Java 本地（`java -jar` :8100）经 `http://127.0.0.1:8500` 调 Python。

## Docker 多阶段

```dockerfile
# 阶段 1：装依赖（pip / uv）
FROM python:3.12 AS builder
WORKDIR /app
# uv 或 pip 装依赖到 venv / --prefix
COPY pyproject.toml uv.lock* ./
RUN pip install uv && uv sync --frozen          # 或 pip install -r requirements.txt

# 阶段 2：瘦运行时
FROM python:3.12-slim
WORKDIR /app
COPY --from=builder /app/.venv ./.venv
COPY . .
ENV PATH="/app/.venv/bin:$PATH"
HEALTHCHECK --interval=30s --timeout=3s CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8500/health').status==200 else 1)"
# CMD = scripts/start.sh = exec uvicorn（本地与 Docker 同款启动脚本）
CMD ["sh", "scripts/start.sh"]
```

- `scripts/start.sh` 内容 = `exec uvicorn shiwujie_ai.app:app --host 0.0.0.0 --port 8500`（容器内 `0.0.0.0` 由 compose 不发布端口兜底，**公网不可达**）；本地模式同脚本但 `--host 127.0.0.1`（由环境变量切）。
- **healthcheck `/health`**：FastAPI 暴露 `/health` 端点，返回 200；compose 据此判活。
- 多阶段：builder 装依赖（含编译型依赖）、slim 运行时只拷 venv + 源码，镜像小。

> `scripts/`（start/stop/logs/export/import/clear.sh，bash）在仓库根，对齐参考仓 ctgu-agents；Python 与 Java 各一份 `start.sh`，本地与 Docker CMD 同款。详见根 [architecture/ai-rewrite.md](../../docs/architecture/ai-rewrite.md) 部署段。

## compose（与 Java 同 compose，java 公网 + python 内网）

`docker/docker-compose.yml` 两 service：

```yaml
services:
  java:
    build: ../shiwujie-backend
    ports:
      - "8100:8100"                 # ★ 公网发布（App 连此）
    extra_hosts:
      - "host-gateway"              # 连宿主 MySQL/Redis (47.112.114.139)
    restart: always
    init: true
  python:
    build: ../shiwujie-ai
    # ★ 不发布端口（仅内网，java 经 http://python:8500 调）
    extra_hosts:
      - "host-gateway"
    restart: always
    init: true
    healthcheck:
      test: ["CMD", "python", "-c", "import urllib.request;urllib.request.urlopen('http://127.0.0.1:8500/health')"]
```

要点：
- **java 发布 `8100:8100`（公网）**；**python 不发布端口**（仅 compose 内网，java 经服务名 `http://python:8500` 调）。
- 两 service `extra_hosts: host-gateway` 连宿主 MySQL / Redis（`47.112.114.139`）。
- `restart: always` + `init: true`（PID 1 僵尸进程兜底）。
- python 加 `/health` healthcheck。

## 共享外部依赖

| 依赖 | 地址 | 用途 |
|---|---|---|
| MySQL | `47.112.114.139:3306` 库 `shiwujie` | AiLogs 审计（追加只写）；业务表读写经缝 C MCP 委托 Java，Python **不直连业务表** |
| Redis | `47.112.114.139:6379` db=2 | 短期 checkpoint（key `ai:ckpt:{blind_id}`）+ 长期偏好 hash（按 blind_id） |

> `ai:ckpt:` 前缀刻意避撞 Java 侧 Redis key（spring-session / JWT `REDIS_SECRETKEY-*`）。Python 用 `ai:` 命名空间自洁。

## 配置（全 `${ENV:default}`，对齐项目约定）

配置全走环境变量 + 内联默认值（凭据留 env、default 硬编码在配置里，对齐项目「不搬 vault」约定；公网 IP 硬编码不抽变量）：

| 配置 | 环境变量 | default | 说明 |
|---|---|---|---|
| qwen 文本模型 | `QWEN_API_KEY` / `QWEN_BASE_URL` / `QWEN_MODEL` | （default 在 .env） | 经 `ChatOpenAI` 指 **DashScope compatible-mode**（OpenAI 兼容端点解耦 Alibaba 绑定，见 design.md ⑪ 理由 2） |
| 网络搜索 | `SEARCH_API_KEY` | （default 在 .env） | web_search 工具用 |
| 高德 | `GAODE_KEY` | （default 在 .env） | gaode_poi_search / gaode_route / get_weather |
| Java MCP url | `JAVA_MCP_URL` | `http://java:8100/mcp`（compose 服务名） | 缝 C MCP streamable HTTP |
| 内部鉴权 header | `X_INTERNAL_SECRET` | （default 在 .env） | Python→Java 内部调用鉴权（防外部直构造 MCP 调用） |
| 盲人标识 header | `X_BLIND_ID` | （Java 逐 turn 注入） | Java 鉴权后传 blind_id，Python 不信客户端自报 |

- `config/.env`（不入库）+ `config/.env.example`（入库作模板）。compose 用 `env_file: config/.env`。
- qwen 经 `ChatOpenAI`（langchain-openai）指 DashScope compatible-mode base-url——**复用止血期已验证的 OpenAI 兼容路径**（见 backend [known-issues](../../shiwujie-backend/docs/known-issues.md) ai #11），不碰 spring-ai-alibaba 的 `DashScopeChatModel`。

## 灰度（硬切换）

AI 重写灰度 = **硬切换**：后端镜像 + APK 同批发，旧 SSE 通道与新 WS AI-turn 不兼容（SSE↔WS 须版本配对）。不支持渐进灰度——App 与后端必须配对升级。详见 product（AI 通道契约变更）+ release-checklist。

## 非 Docker 本地模式仍可

本地开发不强求 Docker：Java `mvn install + java -jar`（:8100）+ Python `uvicorn`（127.0.0.1:8500）直跑，两进程本机自洽，连宿主 MySQL/Redis。与 Docker 模式仅隔离方式不同，配置同源（env）。
