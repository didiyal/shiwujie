# v2.0.0 版本变更记录

> 本文件对应 `product/v2.0.0/`（二期·进行中）。v2.0.0 是**二期首个 semver 版本**（一期 `v1.0` tag 在前），二期尚无 semver tag；其起点 = 阶段 0–9 的累积现状，完整四分类明细（新增 / 变更 / 修复 / 移除）在根 [CHANGELOG.md](../../CHANGELOG.md) 的「历史时期」banner 下（日期不确定）。打 tag 时本段补精确日期，并按版本继续向前记录用户可见变更。

## v2.0.0（进行中，未发布）

v2.0.0 = 二期首个 semver 版本，内容起点为阶段 0–9 累积现状。用户可见面的演进要点：

- **一期 → 二期**：uniapp + Spring 单体 → 原生 Android + Spring Cloud 微服务（gateway/user/call/community/ai）。
- **三类身份 + 家庭/社区**：盲人/志愿者/家属，家庭审核，省/市/街道三级社区治理与求助。
- **实时通信**：视频求助 FIFO 匹配 + 家庭紧急求助（Netty → Spring WebSocket 演进）。
- **AI 大脑**：Spring AI Alibaba 多模型 + 工作流式工具路由 + 图片多轮上下文压缩（mqtt/RAG/自研 ReAct 试错后移除/弃用）。
- **分布式生产化**：gateway 负载均衡、多机通信、dev/prod 多环境、凭据占位符化、模块扁平化、Dubbo 端口迁出保留段。

> 打 tag 流程见 [CONTRIBUTING.md](../../CONTRIBUTING.md) 第五节；当前版本指针见 [../current.md](../current.md)。
