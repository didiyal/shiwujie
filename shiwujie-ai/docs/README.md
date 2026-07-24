# Python AI 服务文档（development 细化）

> 视无界盲人 AI 助手的「**计算大脑**」——polyglot 双进程架构的 Python 侧：Java 业务单体背后的 **LangGraph 智能体服务**。本目录是 AI 重写技术设计的权威落地（development 层，允许全部技术词：LangGraph / Python / MCP / checkpoint / function-calling / schema / spring-ai-alibaba-graph …）。**不含用户契约**——FR-AI / AC-AI / HTTP 路径 / WebSocket 信令码在根 [product/current.md](../../docs/product/current.md)；polyglot 总图与两条缝（缝 A 对话流 / 缝 C Java 能力）在根 [architecture/ai-rewrite.md](../../docs/architecture/ai-rewrite.md)；规范见 [docs/CONTRIBUTING.md](../../docs/CONTRIBUTING.md)。

> **状态**：本文为「**设计敲定（Phase 1–4）· 实现待 Phase 5**」——AI 重写设计已完成梳理与红队评审，代码尚未落地（v3.0.0 进行中、未发布）。文中所有节点 / 工具 / 状态机 / 部署细节均为已敲定设计，非已运行实现。打 tag 前**不**勾 [ROADMAP](../../docs/ROADMAP.md) 的实现项。

## 这是什么

把现有 Java AI 模块（工作流式 prompt-as-router + 自研 ChatMemory + 弃用的自研 ReAct 雏形 + 半残留 RAG + qwen 止血）**整体替换**为 Python LangGraph 智能体；Java 业务单体保留，降级为 polyglot 架构的网关 / MCP server。重写范围**仅 AI 对话与工具执行链路**；业务契约（user / call / community）零变更。

诚实选型理由见 [design.md](design.md#诚实选型理由)——**不是因为「Java 做不到 graph/checkpoint/interrupt」**（红队已证伪：spring-ai-alibaba-graph 1.0 GA 在本项目已用的 alibaba-bom 1.0.0.2 内确有这些原语），而是三条务实理由 + 一条反转 gate（备选 B-prime）。硬修正（紧急求助确认门、update_profile 字段门、qwen FC spike、自研 ReAct 冻结）全列于 [design.md](design.md) 与 [known-issues.md](known-issues.md)。

## 目录

| 文档 | 内容 |
|---|---|
| [design.md](design.md) | ★ graph 设计圣经：State schema / 节点 / 边 / HITL / checkpointer / 动态反馈 / 14 工具清单 / navigation skill / BM25 KB / 两层记忆 / 诚实选型 / Pi 金标准 / 硬修正全列 |
| [known-issues.md](known-issues.md) | 依赖风险（spring-ai MCP server BOM 冲突 / langchain-mcp-adapters open bug）+ 紧急门并行调用洞 + qwen FC 可靠性 + Java-graph B-prime 前置 spike + MCP vs REST-wrap fallback |
| [deployment.md](deployment.md) | 本地 uvicorn（**绝不公网**）/ Docker 多阶段（pip/uv → python:3.12-slim）/ healthcheck / 连宿主 Redis db=2 + MySQL / 配置全 `${ENV:default}` / 与 Java 同 compose（java 公网 + python 内网） |
| [fallback.md](fallback.md) | MCP 翻车降级方案：缝 C 改 Python 8 个薄 REST wrapper（直调 Java REST 端点），toolset 对 LLM 无差异；代价与回 MCP 条件 |

> 工具（14 + read_skill）/ 技能（1）/ KB 的**清单与来源**分散在 design.md 第 ⑦⑧⑨ 段（设计权威）；本 README 不重复清单，只指路。两层记忆细节在 design.md 第 ⑩ 段。

## 在架构中的位置

```text
                  App（盲人/志愿者）
                       │  缝 A：WS 全合一（文本/语音/位置/图片/流式回/5001-6 信令/主动推送）
                       ▼
        Java 业务单体（网关 + MCP server）:8100（公网）
          ├─ WS 终结点 + JWT/Redis 鉴权 + ticket
          ├─ 业务真相源（user/call/community）
          └─ MCP server（8 工具）─── 缝 C：MCP streamable HTTP ───┐
                       │  缝 A 内部逐 turn HTTP（FastAPI StreamingResponse）│
                       ▼                                          ▼
                  Python LangGraph 智能体:8500（仅内网，绝不公网）
                  ├─ agent loop（原生 function-calling）
                  ├─ 14 工具（6 native + read_skill + 8 MCP）
                  ├─ 两层记忆（短期 checkpoint + 长期偏好）
                  └─ BM25 功能 KB
```

> 缝 B（原「信令中继」）已并入缝 C：信令类能力（launch_navigation / request_video_help / request_emergency_help / open_app）作为 MCP 工具由 Python 调 Java，Python 零信令代码。完整 polyglot 缝图、两条缝定义、共享状态（Redis db=2 + MySQL）见根 [architecture/ai-rewrite.md](../../docs/architecture/ai-rewrite.md)。

## 共享状态（与 Java 同源）

- **Redis db=2**：短期记忆 = LangGraph checkpoint（key `ai:ckpt:{blind_id}`，`ai:ckpt:` 前缀避撞 spring-session / JWT key）；长期偏好 = Redis hash（按 blind_id）。Python **不持用户 JWT**——Java 鉴权后内部逐 turn 传 `blind_id`。
- **MySQL 库 `shiwujie`**：AiLogs 表降级为**追加只写审计 / 可观测日志**（不再当记忆读）；图片 offload 去留待 Phase 5。业务表读写经缝 C MCP 工具委托 Java（Python 零业务真相）。

## 相关文档（跳出本目录）

- **用户契约**：[../../docs/product/current.md](../../docs/product/current.md) → FR-AI / AC-AI / `/api/ws/call` 新 AI-turn 消息类型 / 6 信令码（5006 多可选 destination 载荷）。
- **架构总图**：[../../docs/architecture/ai-rewrite.md](../../docs/architecture/ai-rewrite.md)（polyglot 总图、两缝定义、诚实理由架构版、反转 gate）。
- **Java 侧（缝 A 网关 + 缝 C MCP server）**：[../../shiwujie-backend/docs/](../../shiwujie-backend/docs/)——WS 必修改造（ticket 鉴权 / getAsyncRemote 流式中继 / ConcurrentHashMap / 删 AI 拦截器 dev 后门 [known-issues #1](../../shiwujie-backend/docs/known-issues.md) / 修续期 key bug / 8 MCP 工具实现）。
- **App 侧（缝 A 客户端 + 确认门）**：[../../shiwujie-frontend/app/docs/](../../shiwujie-frontend/app/docs/)——AI-turn WS 消息消费、紧急求助 App 显式确认面（非-MCP HTTP 消费 token，第三道门）。
- **规范**：[../../docs/CONTRIBUTING.md](../../docs/CONTRIBUTING.md)。
