# v3.0.0 版本变更记录

> 本文件对应 `product/v3.0.0/`（单体化改造·进行中）。完整四分类明细在根 [CHANGELOG.md](../../CHANGELOG.md) `## v3.0.0（进行中，未发布）`。打 tag 时补精确日期。前版本 v2.1.0 已封版（tag `v2.1.0`，2026-07-11），其变更记录见 [../v2.1.0/changelog.md](../v2.1.0/changelog.md)。

## v3.0.0（进行中，未发布）

v3.0.0 = 单体化改造版本（v2.1.0 微服务 → 单体）。**用户可见面零变更**（契约继承 v2.1.0，启动级回归通过；功能级 WS 往返 / ai SSE / 事务 / token 待 App/Web 联调）。变更以工程架构为主，不涉及用户可见行为：

- 对外 HTTP 路径 `/api/{user,call,community,ai}/**`、WebSocket `/api/ws/call`（12 信令码 `-1/0/1/2/3/4/5001~5006`）、HTTP/业务状态码、返回字段名——**全部不变**，前端 App/Web 零改动即可对接新单体。
- 工程架构变更（合并单体 / 统一技术栈 / 合库 / 跨服务调用改本地）明细见根 [CHANGELOG.md](../../CHANGELOG.md) `## v3.0.0`「行为变更明细」段。

> 打 tag 流程见 [CONTRIBUTING.md](../../CONTRIBUTING.md) 第五节；当前版本指针见 [../current.md](../current.md)。
