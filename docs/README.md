# 视无界（shiwujie）技术文档

> 前后端分离的系统性技术文档。覆盖 Spring Cloud 微服务后端（gateway/user/call/community/ai）、原生 Android 客户端、Vue3 管理后台。
>
> 迭代历程取自 git 提交历史（[CHANGELOG.md](CHANGELOG.md)），**不设版本标签**。

## 快速导航

| 想了解 | 进入 |
|---|---|
| 项目是什么、做什么、不做什么 | [architecture/overview.md](architecture/overview.md) |
| 技术栈与版本割裂根因 | [architecture/tech-stack.md](architecture/tech-stack.md) |
| 网关路由 + Dubbo 调用图 + 接口契约 | [architecture/gateway-dubbo.md](architecture/gateway-dubbo.md) |
| 鉴权链路与风险点 | [architecture/auth.md](architecture/auth.md) |
| 分库设计与表字典 | [architecture/data-model.md](architecture/data-model.md) |
| 迭代历程（新增/变更/修复/移除） | [CHANGELOG.md](CHANGELOG.md) |
| 已完成 / 规划中 / 待评估 | [ROADMAP.md](ROADMAP.md) |

## 目录结构

```text
docs/
├── README.md              ← 本文件（索引）
├── CHANGELOG.md           # 迭代历程（按 git 阶段，无标签）
├── ROADMAP.md             # 复选框清单
├── architecture/          # 跨前后端共享
│   ├── overview.md        # 定位 + 覆盖范围 + 核心目标 + 非目标
│   ├── tech-stack.md      # 技术栈（含 SB2.7 与 SB3.4.5 割裂说明）
│   ├── gateway-dubbo.md   # 网关路由表 + Dubbo 调用图 + 接口契约
│   ├── auth.md            # JWT+Redis 单点鉴权 + 风险点
│   └── data-model.md      # 分库设计 + 表字典 + 契约清单
├── backend/               # 各微服务逐个详述（含 FR/AC 章节）
│   ├── gateway.md
│   ├── model-commonweb.md
│   ├── user.md
│   ├── call.md
│   ├── community.md
│   └── ai.md              # ★ AI 模块（最深，含试错-移除能力记录）
└── frontend/
    ├── app-android.md     # 原生 Android（角色包/网络/SDK/悬浮窗）
    └── web-vue.md         # Vue3 管理后台
```

## 各模块 FR/AC 索引

每个 backend/frontend 文档内嵌 `## 功能需求 (FR-XX-NN)` 与 `## 验收标准 (AC-XX-NN)`，编号方案 `FR-<MODULE>-<NN>`，MODULE 取值：

| MODULE | 文档 |
|---|---|
| `GATEWAY` / `AUTH` / `MODEL` | [architecture/gateway-dubbo.md](architecture/gateway-dubbo.md) / [auth.md](architecture/auth.md) |
| `USER` | [backend/user.md](backend/user.md) |
| `CALL` | [backend/call.md](backend/call.md) |
| `COMMUNITY` | [backend/community.md](backend/community.md) |
| `AI` | [backend/ai.md](backend/ai.md) |
| `APP` | [frontend/app-android.md](frontend/app-android.md) |
| `WEB` | [frontend/web-vue.md](frontend/web-vue.md) |

## 历史一句话速查

| 阶段 | 时间 | 一句话 | 代表提交 |
|---|---|---|---|
| 0 一期封版 | 2025-06 | uniapp + Spring 单体 + 4 表 | `423c0fa` |
| 1 微服务脚手架 | 2025-07 | 多模块 + nacos + JWT/Redis 骨架 | `a650f92` |
| 2 用户与家庭 | 2025-07~08 | 三类账号 + 家庭审核 + common 工具 | `61f5102` |
| 3 视频通话/紧急求助 | 2025-07~08 | call 模块（早期 Netty socket） | `d96a5aa` |
| 4 社区治理 | 2025-07~08 | 六大主体 | `b5a072e` |
| 5 AI 从零到能用 | 2025-08 | model 独立 + Spring AI + 工具/流式/TTS | `d9a6005` |
| 6 AI 能执行 | 2025-08 | 避障/导航/跳转/性能 +50% | `831b7e0` |
| 7 引擎升级 | 2025-10~11 | M6→1.0 + 工作流（mqtt/RAG/ReAct 试错后移除） | `07459f6` |
| 8 分布式生产化 | 2026-01 | Netty→Spring WS + gateway 负载 + 多机 | `3f88ea7` |
| 9 工程化收尾 | 2026-07 | call 路由对齐 + 多环境 + 凭据占位符 + 后端扁平化 + 仓库卫生 | `2e5573a` |

> 详细四分类见 [CHANGELOG.md](CHANGELOG.md)。

## 关键工程事实（速览）

- **SB 2.7 + SB 3.4.5 双栈**：Spring AI 强制 SB3+/Java21，业务模块仍 SB2.7/Java17 → 逼出 model / common-web 两层（[tech-stack.md](architecture/tech-stack.md)）。
- **Dubbo 单一契约源**：10 个 `Inner*Service` 接口集中在 shiwujie-model，提供者 `@DubboService`、消费者 `@DubboReference`（[gateway-dubbo.md](architecture/gateway-dubbo.md)）。
- **AI 推送唯一落地点**：ai 模块通过 Dubbo 调 call 的 `InnerSocket`，再由 call 经 WebSocket 推前端（5xxx 信令）。
- **三个试错-移除能力**（如实记录）：mqtt 硬件通道（取消，pom 残留）、自研 ReAct Agent（弃用，`@Component` 注释）、RAG（半残留，未注入）。
- **dev/prod 拓扑差异**：dev 期 Nacos（发现 + Dubbo 注册中心）走**本机**、仅 MySQL/Redis 连服务器，整套服务本机自洽；prod 期全部连服务器。`nacos.address` 占位符由 profile 覆盖（详见 [tech-stack.md](architecture/tech-stack.md)）。
- **Dubbo 注册 IP 坑（prod/多机场景）**：`spring.cloud.nacos.discovery.ip` 只解决网关 `lb://` 发现；**Dubbo 独立注册，须启动命令加 `-DDUBBO_IP_TO_REGISTRY`**（详见 [gateway-dubbo.md](architecture/gateway-dubbo.md)）。纯本机 dev（Nacos 走本机）不触发。
- **已知高危项**：续期 key 拼接 bug（滑动会话失效）、sessionMap HashMap 并发、AI 默认用户后门、多处权限检查被注释（详见各文档「已知问题」与 [auth.md](architecture/auth.md)）。
- **诚实缺口**：无压测、无 Docker、无索引调优、统计页未实现。

## 文档约定

- 纯 markdown，中文为主、代码标识符保留英文；表格 + bullet 优先。
- 调用图/时序图用 **Mermaid**。
- 编号追溯：`FR-<MODULE>-<NN>`（需求）↔ `AC-<MODULE>-<NN>`（验收）。
- CHANGELOG 四分类：新增 / 变更 / 修复 / 移除（[Keep a Changelog](https://keepachangelog.com/) 风格）。
- 文档由「分子 Agent 审计 + 主循环综合」生成，事实均有代码 `file:line` 支撑。
