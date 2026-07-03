# 视无界（shiwujie）文档中心

> 平台层文档（Tier1）：**规范 + 用户可见契约 + 跨切面概览 + 路线/历史**。技术实现（development 细化）随代码就近，分别在 [shiwujie-backend/docs/](../shiwujie-backend/docs/) 与 [shiwujie-frontend/app/docs/](../shiwujie-frontend/app/docs/)、[shiwujie-frontend/web/docs/](../shiwujie-frontend/web/docs/)。规范真值在 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 当前基线（product/current）

滚动工作基线，反映阶段 0–9 累积现状；首次打 tag（计划 v1.0.0）后整体快照为 `product/v1.0.0/`。

| 文档 | 内容 |
|---|---|
| [product/current/product-overview.md](product/current/product-overview.md) | 定位 / 三身份 / 覆盖范围 / 核心目标 / 非目标 |
| [product/current/functional-requirements.md](product/current/functional-requirements.md) | 全模块功能需求 `FR-<MODULE>-<NN>`（含状态列，如实标注已弃用/未启用） |
| [product/current/acceptance-criteria.md](product/current/acceptance-criteria.md) | 全模块验收标准 `AC-<MODULE>-<NN>` |
| [product/current/changelog.md](product/current/changelog.md) | 指向根 CHANGELOG 历史时期 |

## 文档地图

| 想了解 | 进入 |
|---|---|
| 文档规范（四层结构 / 内容边界 / 自检 / 流程） | [CONTRIBUTING.md](CONTRIBUTING.md) |
| 项目是什么、用什么、怎么连 | [architecture/overview.md](architecture/overview.md) |
| 技术栈与 SB 2.7 / SB 3.4.5 版本割裂 | [architecture/tech-stack.md](architecture/tech-stack.md) |
| 网关路由 + Dubbo 接口契约 + 调用图 | [architecture/gateway-dubbo.md](architecture/gateway-dubbo.md) |
| JWT + Redis 单点鉴权链路 | [architecture/auth.md](architecture/auth.md) |
| 分库设计 + 表字典（数据契约） | [architecture/data-model.md](architecture/data-model.md) |
| 后端各微服务实现 + 缺陷 + 部署 | [../shiwujie-backend/docs/](../shiwujie-backend/docs/) |
| Android / Web 前端实现 + 缺陷 | [../shiwujie-frontend/app/docs/](../shiwujie-frontend/app/docs/) · [../shiwujie-frontend/web/docs/](../shiwujie-frontend/web/docs/) |

## 路线与历史

| 文档 | 内容 |
|---|---|
| [ROADMAP.md](ROADMAP.md) | 待实现（🔴 安全加固必办 / 能力补全 / 工程化）+ 历史时期 roll-up |
| [CHANGELOG.md](CHANGELOG.md) | 历史时期 banner + 阶段 9..0 明细（新增 / 变更 / 修复 / 移除） |

> 阶段 0–9 属过去开发、不回溯分层，整合为「历史时期」一块（日期不确定，标注「约」）。后续打 tag 走 [CONTRIBUTING.md](CONTRIBUTING.md) 第五节的前瞻流程。

## 分层一句话

- **product/** = 用户可见契约（能调什么接口、返回什么、状态码、表/字段/枚举）。**禁**源码路径 / 内部符号 / 启动命令 / `file:line`。
- **architecture/** = 跨切面概览（链路图 / 调用图 / 选型表 / 分库设计概念）。
- **子项目 docs/** = development 细化（核心类 / 数据流 / 配置 / 启动命令 / 已知缺陷 / `file:line`）。
- **ROADMAP / CHANGELOG** = 方向与历史，根单中心。

> 子项目内用 Agent 开发时，从**仓库根**起步，必读 [CONTRIBUTING.md](CONTRIBUTING.md) + [product/current/](product/current/) + 相关 [architecture/](architecture/)；子项目 `docs/` 仅作实现参照。阶段结束执行回卷仪式（见 [CONTRIBUTING.md](CONTRIBUTING.md) 第七节）。
