# 当前基线变更记录

> 本文件对应 `product/current/` 滚动工作基线。当前**无 git tag**，故本基线 = 阶段 0–9 的累积现状，变更明细统一在根 [CHANGELOG.md](../../CHANGELOG.md) 的「历史时期」banner 下（日期不确定）。

## 当前基线（= 历史时期 阶段 0–9 累积）

完整四分类明细（新增 / 变更 / 修复 / 移除，按阶段 9..0 降序）见 [../../CHANGELOG.md](../../CHANGELOG.md)。

要点（用户可见面的演进）：

- **一期 → 二期**：uniapp + Spring 单体 → 原生 Android + Spring Cloud 微服务（gateway/user/call/community/ai）。
- **三类身份 + 家庭/社区**：盲人/志愿者/家属，家庭审核，省/市/街道三级社区治理与求助。
- **实时通信**：视频求助 FIFO 匹配 + 家庭紧急求助（Netty → Spring WebSocket 演进）。
- **AI 大脑**：Spring AI Alibaba 多模型 + 工作流式工具路由 + 图片多轮上下文压缩（mqtt/RAG/自研 ReAct 试错后移除/弃用）。
- **分布式生产化**：gateway 负载均衡、多机通信、dev/prod 多环境、凭据占位符化、模块扁平化、Dubbo 端口迁出保留段。

## 首次打 tag 后

打 tag（计划 v1.0.0）时：① 本目录快照为 `product/v1.0.0/`；② 根 CHANGELOG 新增 `## v1.0.0 - <tag 日期>`（精确日期）；③ 此文件后续按版本记录用户可见变更。流程见 [CONTRIBUTING.md](../CONTRIBUTING.md) 第五节。
