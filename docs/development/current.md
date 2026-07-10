# 当前开发交付版本

当前工作版本：`v3.0.0`（单体化改造·进行中）。已发布版本：`v2.0.0`（tag `v2.0.0`，2026-07-11）。

## 三件套入口

- [任务拆解](v3.0.0/task-breakdown.md)（单体化交付范围 + 任务）
- [测试策略](v3.0.0/testing-strategy.md)（测试现状 + 验证点 + 环境表 + 缺口）
- [发布检查清单](v3.0.0/release-checklist.md)（逐项核对）

## 正交说明：development 是双维度

shiwujie 是前后端 + 微服务多子项目工程，development 层不集中在一处，而是两个正交维度：

| 维度 | 位置 | 组织方式 | 内容 |
|---|---|---|---|
| **按组件** | 各子项目 `docs/`（[backend](../../shiwujie-backend/docs/) / [app](../../shiwujie-frontend/app/docs/) / [web](../../shiwujie-frontend/web/docs/)）+ 根 [architecture/](../architecture/) | 随代码就近、不分版本 | 核心类 / 数据流 / 配置 / 缺陷 / `file:line` |
| **按版本** | 本目录 `development/vX.Y.Z/`（三件套） | 每版本一份 | 交付范围 / 测试策略 / 发布检查 |

两维度不重复：按组件的文档回答「这个模块怎么实现」；按版本的三件套回答「这个版本交付什么、怎么验证、怎么发布」。这与参考仓 ctgu-models 的单集中模型不同，是本项目分散现实的合理适配。详见 [CONTRIBUTING.md](../CONTRIBUTING.md) 第三节.4。

> 版本分级模型（`current.md` 指针 + 每版本一个 `vX.Y.Z/` 目录；发布冻结、新建下一版；历史版本目录保留不删）见 [CONTRIBUTING.md](../CONTRIBUTING.md) 第五节。三处版本号一致：本指针 + [product/current.md](../product/current.md) + [README.md](../README.md)。前一版本 v2.0.0 已封版（目录保留不删），其三件套见 [v2.0.0/](v2.0.0/)。
