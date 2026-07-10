# v3.0.0 测试策略

> 本文件属 `development/v3.0.0/`（单体化改造·进行中）。测试基线**继承 v2.1.0**（见 [../v2.1.0/testing-strategy.md](../v2.1.0/testing-strategy.md)，已封版 2026-07-11），本版本补充单体化特有的验证点。版本指针 [../current.md](../current.md)。

## 继承基线

v2.1.0 的测试现状 / 环境表 / 验证点 / 缺口继续适用，见 [../v2.1.0/testing-strategy.md](../v2.1.0/testing-strategy.md)。

## 契约保护铁律（v3.0.0 单体化硬约束）

单体化是工程架构内部重构，**对外行为零变更**。下列契约在阶段 1/2 完成后必须逐一回归通过，前端 App/Web 不改即可对接：

- **HTTP 路径不变**：`/api/user/**`（blind / volunteer / family / familyJoinReview）、`/api/call/**`（videohelp / urgenthelp）、`/api/community/**`（community / communitymanager / communityjoinreview / activity / activitysign / helppost）、`/api/ai/**`（doChatByText / doChatByImage / NewApp，SSE）。
- **WebSocket `/api/ws/call` 不变**：12 信令码 `-1`(心跳) / `0`(bind) / `1`(匹配成功) / `2`(就绪) / `3`(紧急通知) / `4`(取消) / `5001~5006`(AI 联动：拍照 / 视频求助 / 紧急求助 / 跳应用 / 跳编辑 / 导航) + `SocketData`(requestType / blindPhone / volunteerPhone / channelId) / `SocketVO`(code / message / socketData) 消息格式原样。
- **状态码 / 字段名不变**：HTTP 状态码、业务码（`NOT_LOGIN` / `NO_AUTH` / `PARAMS_ERROR`）、`BaseResponse` 字段名。

## v3.0.0 验证点（随两阶段落地滚动补充）

**阶段 1（升 SB 3.4.5/Java21，仍微服务）**：

- 每模块 `mvn compile`（JDK21）reactor 全绿；`contextLoads` 全绿（新 nacos-client 2023.0.1.0 修好 JDK21 坑，不再切 JDK17）。
- jakarta 迁移回归：登录 → 受保护接口（拦截器 `jakarta.servlet` + Redis token 比对 + 滑动续期）跑通。
- call WebSocket：`jakarta.websocket` import 后 `@ServerEndpoint("/ws/call")` 连接 + 信令往返正常（生产 Tomcat10 容器；MOCK 环境 `ServerEndpointExporter` 行为单独确认）。
- knife4j openapi3：文档页可访问，83 处注解映射正确。

**阶段 2（合并单体 + 合库）**：

- 单端口入口回归：原经 gateway 分发到 4 服务的链路，合并为单体统一入口后功能不变（按上述 HTTP 路径契约逐条回归）。
- 跨模块调用改本地调用回归：原 Dubbo Inner 调用改为同进程方法调用，幂等性与异常透传一致。
- 事务回归：删志愿者级联清家庭/社区、删社区清成员——单 `@Transactional` 一致性（中途异常无脏数据残留，原先跨库 + synchronized 无保证）。
- 鉴权回归：4 处 `LoginCheckInterceptor` 收敛为 common-web 1 处后，鉴权行为（含 v2.1.0 修复的 token 滑动会话续期 + 删用户清 token）不回归。
- 合库回归：13 表数据完整导入 `shiwujie`；call 实体 snake→camel 后查询/写入字段映射正确（DB 列名未变）。
- AI 回归：多模型对话 + 工具路由 + 流式 SSE + ChatMemory（Redis + MySQL 双写）正常。
