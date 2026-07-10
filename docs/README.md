# 视无界（shiwujie）文档中心

> 平台层文档（Tier1）：**规范 + 用户可见契约 + 跨切面概览 + 路线/历史**。技术实现（development 细化）随代码就近，分别在 [shiwujie-backend/docs/](../shiwujie-backend/docs/) 与 [shiwujie-frontend/app/docs/](../shiwujie-frontend/app/docs/)、[shiwujie-frontend/web/docs/](../shiwujie-frontend/web/docs/)。规范真值在 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 当前工作版本（v3.0.0 · 进行中）

当前工作版本 `v3.0.0`（单体化改造·进行中；反思 v2.1.0 微服务过度设计，去微服务精简为单体）。前一版本 `v2.1.0`（二期微服务封版，tag `v2.1.0`，2026-07-11）；再前 `v2.0.0`（二期初步稳定，2025-11-12）。版本分级模型：`current.md` 指针 + 每版本一个 `vX.Y.Z/` 目录（工作直接写在进行中版本目录里，发布即冻结、目录保留不删）。打 tag 时冻结当前版本目录、`current.md` 改指下一版。

- **产品需求入口**（指针）：[product/current.md](product/current.md) → `product/v3.0.0/`（4 文件：overview / functional-requirements `FR-<MODULE>-<NN>` / acceptance-criteria `AC-<MODULE>-<NN>` / changelog）
- **开发交付入口**（指针）：[development/current.md](development/current.md) → `development/v3.0.0/`（三件套：task-breakdown / testing-strategy / release-checklist）

## 文档地图

| 想了解 | 进入 |
|---|---|
| 文档规范（四层结构 / 内容边界 / 自检 / 流程） | [CONTRIBUTING.md](CONTRIBUTING.md) |
| 项目是什么、用什么、怎么连 | [architecture/overview.md](architecture/overview.md) |
| 技术栈与 SB 2.7 / SB 3.4.5 版本割裂 | [architecture/tech-stack.md](architecture/tech-stack.md) |
| 网关路由 + Dubbo 接口契约 + 调用图 | [architecture/gateway-dubbo.md](architecture/gateway-dubbo.md) |
| JWT + Redis 单点鉴权链路 | [architecture/auth.md](architecture/auth.md) |
| 分库设计 + 表字典（数据契约） | [architecture/data-model.md](architecture/data-model.md) |
| 按版本的开发交付（任务 / 测试 / 发布清单） | [development/current.md](development/current.md) |
| 后端各微服务实现 + 缺陷 + 部署 | [../shiwujie-backend/docs/](../shiwujie-backend/docs/) |
| Android / Web 前端实现 + 缺陷 | [../shiwujie-frontend/app/docs/](../shiwujie-frontend/app/docs/) · [../shiwujie-frontend/web/docs/](../shiwujie-frontend/web/docs/) |

## 路线与历史

| 文档 | 内容 |
|---|---|
| [ROADMAP.md](ROADMAP.md) | 待实现（🔴 安全加固必办 / 能力补全 / 工程化）+ 历史时期 roll-up |
| [CHANGELOG.md](CHANGELOG.md) | v3.0.0（进行中）+ v2.1.0（2026-07-11）+ v2.0.0（2025-11-12）+ 历史时期 banner + 阶段 9..0 明细（新增 / 变更 / 修复 / 移除） |

> 阶段 0–9 属过去开发、不回溯分层，整合为「历史时期」一块（日期不确定，标注「约」）；其累积现状作为 v2.1.0（已封版）的起点。版本分级流程见 [CONTRIBUTING.md](CONTRIBUTING.md) 第五节。

## 分层一句话

- **product/** = 用户可见契约（能调什么接口、返回什么、状态码、表/字段/枚举）。**禁**源码路径 / 内部符号 / 启动命令 / `file:line`。
- **architecture/** = 跨切面概览（链路图 / 调用图 / 选型表 / 分库设计概念）。
- **development/** = 按版本的开发交付（三件套：任务 / 测试 / 发布）；按组件的实现细化在子项目 `docs/`。
- **子项目 docs/** = development 按组件细化（核心类 / 数据流 / 配置 / 启动命令 / 已知缺陷 / `file:line`）。
- **ROADMAP / CHANGELOG** = 方向与历史，根单中心。

> 子项目内用 Agent 开发时，从**仓库根**起步，必读 [CONTRIBUTING.md](CONTRIBUTING.md) + [product/current.md](product/current.md) + [development/current.md](development/current.md) + 相关 [architecture/](architecture/)；子项目 `docs/` 仅作实现参照。阶段结束执行回卷仪式（见 [CONTRIBUTING.md](CONTRIBUTING.md) 第七节）。
