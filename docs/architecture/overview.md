# 架构总览

> 跨切面的结构地图：项目是什么 / 用什么 / 怎么连。**项目定位、覆盖范围、核心目标、非目标**属用户可见面，见 [product/v2.1.0/product-overview.md](../product/v2.1.0/product-overview.md)；**迭代历程（两期演进、能力-时间对照）**见 [../CHANGELOG.md](../CHANGELOG.md)。本篇只留结构本身。

## 一句话

**视无界（shiwujie）** 是面向视障人士的无障碍服务平台，以 Spring Cloud 微服务后端 + 原生 Android 客户端 + Vue3 管理后台落地「AI 助手 + 远程人工协助 + 社区互助」三条路径。

## 架构文档地图

| 想了解 | 进入 |
|---|---|
| 技术栈与 SB 2.7 / SB 3.4.5 版本割裂根因 | [tech-stack.md](tech-stack.md) |
| 网关路由表 + Dubbo 接口契约 + RPC 调用图/时序图 | [gateway-dubbo.md](gateway-dubbo.md) |
| JWT + Redis 单点鉴权链路图 | [auth.md](auth.md) |
| 分库设计 + 表字典（数据契约）+ 共享缓存 | [data-model.md](data-model.md) |

> 用户可见契约（FR / AC / 端点 / 状态码）在 [product/current.md](../product/current.md)；各微服务与前端的技术实现（核心类 / 数据流 / 配置 / 已知缺陷）随代码就近，见 [../../shiwujie-backend/docs/](../../shiwujie-backend/docs/) 与 [../../shiwujie-frontend/app/docs/](../../shiwujie-frontend/app/docs/)、[../../shiwujie-frontend/web/docs/](../../shiwujie-frontend/web/docs/)。

## 结构关键事实

- **后端微服务**：gateway（8100，仅路由+LB）+ user（8200）/ call（8300）/ community（8400）/ ai（8500），Dubbo provider 端口 21200–21500。完整路由表与调用图见 [gateway-dubbo.md](gateway-dubbo.md)。
- **版本双栈**：业务模块 Spring Boot 2.7 / Java 17；ai 模块 Spring Boot 3.4.5 / Java 21（Spring AI 强制）。逼出 model（契约层，全模块可依赖）/ common-web（公共层，仅 SB2 可依赖）两层。见 [tech-stack.md](tech-stack.md)。
- **分库**：user / call / community / ai 各一独立库，共享 Redis db=2；跨库访问走 Dubbo RPC，不走 JOIN。见 [data-model.md](data-model.md)。
- **鉴权**：JWT + Redis 单点，网关不鉴权、各业务服务各复制一份拦截器。见 [auth.md](auth.md)。
- **AI 推送唯一落地点**：ai 经 Dubbo 调 call 的 `InnerSocket`，再由 call 经 WebSocket 推前端（5xxx 信令）。
- **dev/prod 拓扑差异**：dev 期 Nacos 走本机、MySQL/Redis 连服务器；prod 期全部连服务器。生产 Dubbo 注册 IP 坑见 [../../shiwujie-backend/docs/deployment.md](../../shiwujie-backend/docs/deployment.md)。
- **诚实缺口**：无压测、无 Docker 化、无索引调优、统计页未实现（同步进 [../ROADMAP.md](../ROADMAP.md)）。
