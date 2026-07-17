# 路线图（ROADMAP）

> 大方向清单。`## 待实现`（当前工作版本 v3.0.0 单体化，v2.1.0 已于 2026-07-11 封版）→ `## 已完成`（按版本里程碑，**均为开发后抽取补档**——历史开发完成后再整理的文档，非预先规划）。bug / 重构 / 实现细节下沉 [CHANGELOG.md](CHANGELOG.md) + 各子项目 `known-issues.md`，本文件只列大方向。

## 待实现

### v3.0.0（单体化改造）

> 反思 v2.1.0 微服务对当前体量过度设计，去微服务、精简为单体（**已落地**，待 tag 封版）；并规划安全加固、能力补全与 AI 模块重写（polyglot：Java 业务单体 + Python LangGraph 智能体）。单体化 + 安全加固 + 能力补全**已落地**；AI 重写**设计已敲定（Phase 1-4 梳理完成，见 [architecture/ai-rewrite.md](architecture/ai-rewrite.md)），实现待 Phase 5**——本段全部 `[ ]` 均未勾，诚实反映尚未落地。

```text
[x] 单体重写：去 Spring Cloud / Dubbo，合并 user / call / community / ai 为单体应用（保留模块化分包，统一 Spring Boot 版本）——工程已落地，启动级 + WS/事务功能级验证通过，待 tag 仪式封版
[x] 安全加固：恢复 Helppost/Community 删改权限检查、密码加盐（MD5→BCrypt；`fix/v3.0.0-security-hardening`，2026-07-12）
[x] 能力补全：软件介绍主页；软件下载功能；社区页面集成到主页，Web页面美化（官网首页 + 全端响应式 + Android 下载 + 整体 UI redesign + APK 下载接口，分头开发合入，2026-07-12~14）
[ ] 高德适配：App 集成高德 SDK
[ ] AI 重写-设计：Phase 1-4 梳理（功能/技术/方案/整合）已敲定，见 [architecture/ai-rewrite.md](architecture/ai-rewrite.md)（polyglot Java 业务单体 + Python LangGraph 智能体两进程）
[ ] AI 重写-Python：LangGraph graph + 14 tool（6 Python-native + 8 Java-MCP）+ read_skill 元工具 + navigation skill + BM25 功能 KB + 两层记忆（短期 checkpoint / 长期偏好）
[ ] AI 重写-Java：WS 改造（ticket 鉴权 / 流式中继 / 并发安全）+ MCP server 8 工具（业务 4 + 信令 4）
[ ] AI 重写-删旧：删 Java AI 模块（工作流式 prompt-as-router / 自研 ChatMemory / 半残留 RAG / qwen 止血）；MyManus 自研 ReAct 骨架冻结保留作 Java-graph 回退起跑线
[ ] AI 重写-部署：两进程 Docker（Java 单体 + Python AI，根级 scripts/ + docker/ + config/ 编排）
[ ] AI 重写-前端：APK SSE→WS 全合一对话 + SocketData destination 载荷 + 4-button 重写
[ ] AI 重写-安全门：紧急求助确认门（prepare/confirm 双工具 + 同轮 token 拒绝 + App 显式确认面）+ update_profile 字段门（schema 硬卡 + 窄 DTO 单测）+ qwen FC spike 前置（≥90% 通过率 + MCP strict 校验 + tool-name 白名单两护栏）
```

## 已完成（过去时期，开发后抽取）

> v1.0 / v2.0.0 / v2.1.0 均为开发完成后**抽取补档**的整理性文档（v2.1.0 补精确封版日期 2026-07-11、v2.0.0 补 2025-11-12，阶段 0–9 日期仍为「约」）。明细见 [CHANGELOG.md](CHANGELOG.md) 历史时期；用户可见契约见 [product/](product/)。

### v2.1.0（二期微服务封版，阶段 0–9 累积 · 2026-07-11）

> 二期微服务封版（tag `v2.1.0`）。一期 `v1.0`、二期初步稳定 `v2.0.0`（2025-11-12）在先。阶段 0–9（约 2025-06 ~ 2026-07）累积现状作起点，主体能力已建成。未完成的收尾项（🔴 安全加固 / 能力补全 / 工程化）整体平移至 v3.0.0（单体化）。

```text
[x] 二期微服务化：Spring Cloud + Nacos + Dubbo，业务 SB2.7/Java17 + AI SB3.4.5/Java21
[x] 用户 / 家庭 / 社区：三类身份、家庭审核、省/市/街道三级社区治理与求助
[x] 实时通信：视频求助 FIFO 匹配 + 家庭紧急求助（Netty → Spring WebSocket 演进）
[x] AI 大脑：Spring AI Alibaba 多模型 + 工作流式工具路由 + 图片瘦身上下文工程（mqtt/RAG/自研 ReAct 试错后移除/弃用）
[x] 分布式生产化：gateway 负载均衡、多机 nacos+dubbo、dev/prod 多环境、凭据占位符化、模块扁平化、Dubbo 端口迁出保留段
[x] 修复 token 续期 key 漏身份前缀 bug（2026-07-10，滑动会话 90 天生效 + 删用户清 token 生效）
```

### v2.0.0（二期开发后初步稳定版 · 2025-11-12）

> 二期首个 semver 版本（tag `v2.0.0`，原中文名「二期开发后初步稳定版」）。阶段 7 前段、Spring AI Alibaba M6.1（M6 线）末尾的稳定里程碑（提交为开启 dubbo empty-protection，M6→1.0 重构在其后）。无独立版本目录——版本分级模型 2026-07 才落地，此 tag 为历史快照。

```text
[x] 二期开发后初步稳定版：阶段 7 前段、Spring AI Alibaba M6.1 时期的稳定里程碑（提交为开启 dubbo empty-protection，明细见 CHANGELOG 阶段 7）
```

### v1.0（一期单体）

> 一期封版（git tag `v1.0`，独立根提交）。作为二期微服务演进的对照基线保留在 git 历史，不再迭代。

```text
[x] 一期：单体平台封版（uniapp + Spring 单体 + 单库 4 表）
```
