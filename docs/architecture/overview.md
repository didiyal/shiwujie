# 架构总览

> 跨切面的结构地图：项目是什么 / 用什么 / 怎么连。**项目定位、覆盖范围、核心目标、非目标**属用户可见面，见 [product/v2.1.0/product-overview.md](../product/v2.1.0/product-overview.md)；**迭代历程（两期演进、能力-时间对照）**见 [../CHANGELOG.md](../CHANGELOG.md)。本篇只留结构本身。

## 一句话

**视无界（shiwujie）** 是面向视障人士的无障碍服务平台，以单体后端（v3.0.0：原 Spring Cloud 微服务已合并为 model 契约层 + bootstrap 唯一 app）+ 原生 Android 客户端 + Vue3 管理后台落地「AI 助手 + 远程人工协助 + 社区互助」三条路径。

## 架构文档地图

| 想了解 | 进入 |
|---|---|
| 技术栈与版本割裂消除（v3.0.0 统一 SB 3.4.5/Java21） | [tech-stack.md](tech-stack.md) |
| 网关/Dubbo 历史 + v3.0.0 本地调用契约 + 调用图/时序图 | [gateway-dubbo.md](gateway-dubbo.md) |
| JWT + Redis 单点鉴权链路图 | [auth.md](auth.md) |
| 分库设计 + 表字典（数据契约）+ 共享缓存 | [data-model.md](data-model.md) |

> 用户可见契约（FR / AC / 端点 / 状态码）在 [product/current.md](../product/current.md)；各微服务与前端的技术实现（核心类 / 数据流 / 配置 / 已知缺陷）随代码就近，见 [../../shiwujie-backend/docs/](../../shiwujie-backend/docs/) 与 [../../shiwujie-frontend/app/docs/](../../shiwujie-frontend/app/docs/)、[../../shiwujie-frontend/web/docs/](../../shiwujie-frontend/web/docs/)。

## 结构关键事实

- **后端单体**（v3.0.0）：唯一 app `bootstrap`（端口 8100，复用原 gateway），聚合原 user/call/community/ai/common-web 全部代码；`model` 为独立契约层 jar。原 7 模块已收敛为 2 模块，完整路由表与调用图见 [gateway-dubbo.md](gateway-dubbo.md)。
- **技术栈统一**：Spring Boot 3.4.5 / Java 21（Spring AI 强制 SB3/Java21）。v2.1.0 的 SB 2.7/SB 3.4.5 双栈割裂已消除，common-web 随阶段2.8 并入 bootstrap。见 [tech-stack.md](tech-stack.md)。
- **单库**：原 user/call/community/ai 四库合并为单库 `shiwujie`（共享 Redis db=2）；原跨库 Dubbo RPC 改同进程 Bean 注入。见 [data-model.md](data-model.md)。
- **鉴权**：JWT + Redis 单点；v2.1.0 的 4 份 `LoginCheckInterceptor` 重复已收敛为 1（common-web，现处 bootstrap）+ ai 的 `AiLoginCheckInterceptor`。见 [auth.md](auth.md)。
- **AI 推送**：ai 同进程调 call 的 `InnerSocket`，再经 WebSocket 推前端（5xxx 信令）。
- **部署**：单 jar + MySQL（库 shiwujie）+ Redis（db=2），无 Nacos/Dubbo；dev/prod profile 随 Nacos 移除暂无覆盖项。见 [../../shiwujie-backend/docs/deployment.md](../../shiwujie-backend/docs/deployment.md)。
- **诚实缺口**：无压测、无 Docker 化、无索引调优、统计页未实现（同步进 [../ROADMAP.md](../ROADMAP.md)）。
