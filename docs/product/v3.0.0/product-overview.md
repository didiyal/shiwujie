# 产品概览（v3.0.0 · 进行中）

> 单体化改造版本。产品定位、三类身份、覆盖范围**继承 v2.0.0**（见 [../v2.0.0/product-overview.md](../v2.0.0/product-overview.md)，已封版 2026-07-11）。本版本**不新增用户可见业务能力**，焦点是工程架构的去微服务化精简。本文件属 `product/v3.0.0/`（当前工作版本）。版本分级模型见 [CONTRIBUTING.md](../../CONTRIBUTING.md) 第五节；当前版本指针 [../current.md](../current.md)。

## 本版本焦点：单体化改造

反思 v2.0.0 的 Spring Cloud 微服务对当前体量**过度设计**，v3.0.0 去微服务、按当前规模精简为单体应用（大方向见 [../../ROADMAP.md](../../ROADMAP.md) v3.0.0 段）：

- **架构目标**：合并 user / call / community / ai 为单体应用（保留模块化分包、统一 Spring Boot 版本），消除 SB2/SB3 版本割裂与鉴权逻辑多处重复。
- **用户可见契约**：原则上**保持不变**（API 路径 / WebSocket 信令 / FR / AC 继承 v2.0.0）。实际变更（如入口/端口收敛）随单体化设计滚动记录于本目录 [functional-requirements.md](functional-requirements.md) / [acceptance-criteria.md](acceptance-criteria.md) / [changelog.md](changelog.md)。
- **能力补全**：App 集成高德 SDK（替代 v2.0.0 的 URI 调起）。
- **安全加固**：v2.0.0 收尾遗留的 🔴 安全项（ai 默认用户后门、删改权限检查、WS 鉴权、密码哈希、弱密钥、前端 TLS）在单体化语境下统一修复。

## 非目标（沿用 v2.0.0）

见 [../v2.0.0/product-overview.md](../v2.0.0/product-overview.md#非目标当前明确不在范围内)，v3.0.0 不改变这些边界。因单体化而**不再适用**的工程项（分布式事务 Seata、网关统一鉴权、Knife4j 聚合 ai）见 [../../ROADMAP.md](../../ROADMAP.md) 删除线标注。
