# 路线图（ROADMAP）

> 大方向清单。`## 待实现`（下一里程碑 v3.0.0 单体化）→ `## 已完成`（按版本里程碑，**均为开发后抽取补档**——历史开发完成后再整理的文档，非预先规划）。bug / 重构 / 实现细节下沉 [CHANGELOG.md](CHANGELOG.md) + 各子项目 `known-issues.md`，本文件只列大方向。

## 待实现

### v3.0.0（单体化改造）

> 反思 v2.0.0 微服务对当前体量过度设计，去微服务、按当前规模精简为单体。下列项随单体化落地或因单体化而废弃。

```text
[ ] 单体重写：去 Spring Cloud / Dubbo，合并 user / call / community / ai 为单体应用（保留模块化分包，统一 Spring Boot 版本）
[ ] 安全加固：关闭 ai 默认用户兜底、恢复 Helppost/Community 删改权限检查、/ws/call 与社区/家庭审核补鉴权、密码 MD5→BCrypt/Argon2、TOKEN_SECRETKEY 走环境变量、前端 TLS + 移除硬编码 SDK Key
[ ] 能力补全：App 集成高德 SDK
[ ] Docker 化部署
[ ] 压力测试 + 性能基线 + AiLogs 索引调优
~~引入分布式事务（Seata）~~（单体化后单库，无需跨库事务）
~~网关统一鉴权~~（单体化后单拦截器，4 处 LoginCheckInterceptor 重复自然消除）
~~Knife4j 聚合 ai 服务~~（单体化后统一 Spring Boot 版本，无 SB2/SB3 文档协议割裂）
```

> 🔴 安全加固项的代码定位与机制见 [shiwujie-backend/docs/known-issues.md](../shiwujie-backend/docs/known-issues.md) 与 [architecture/auth.md](architecture/auth.md)；此处登记为 v3.0.0 方向。删除线项因单体化不再适用，自原 v2.0.0 待办移除。

## 已完成（过去时期，开发后抽取）

> v1.0 / v2.0.0 均为开发完成后**抽取补档**的整理性文档，日期多为「约」。明细见 [CHANGELOG.md](CHANGELOG.md) 历史时期；用户可见契约见 [product/](product/)。

### v2.0.0（二期微服务，阶段 0–9 累积）

> 二期首个 semver 版本（一期 `v1.0` tag 在前）。阶段 0–9（约 2025-06 ~ 2026-07）累积现状作起点；**主体能力已建成，版本封版仍在收尾、尚未打 tag**（进度跟踪见 [CHANGELOG.md](CHANGELOG.md) + [development/current.md](development/current.md)）。打 tag 时补精确日期。

```text
[x] 二期微服务化：Spring Cloud + Nacos + Dubbo，业务 SB2.7/Java17 + AI SB3.4.5/Java21
[x] 用户 / 家庭 / 社区：三类身份、家庭审核、省/市/街道三级社区治理与求助
[x] 实时通信：视频求助 FIFO 匹配 + 家庭紧急求助（Netty → Spring WebSocket 演进）
[x] AI 大脑：Spring AI Alibaba 多模型 + 工作流式工具路由 + 图片瘦身上下文工程（mqtt/RAG/自研 ReAct 试错后移除/弃用）
[x] 分布式生产化：gateway 负载均衡、多机 nacos+dubbo、dev/prod 多环境、凭据占位符化、模块扁平化、Dubbo 端口迁出保留段
[x] 修复 token 续期 key 漏身份前缀 bug（2026-07-10，滑动会话 90 天生效 + 删用户清 token 生效）
```

### v1.0（一期单体）

> 一期封版（git tag `v1.0`，独立根提交）。作为二期微服务演进的对照基线保留在 git 历史，不再迭代。

```text
[x] 一期：单体平台封版（uniapp + Spring 单体 + 单库 4 表）
```
