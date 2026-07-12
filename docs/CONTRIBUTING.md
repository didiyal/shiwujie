# 文档撰写规范（本项目版）

> 本文件是**视无界（shiwujie）** 仓库 `docs/` 的权威撰写指南。规则裁自通用四层规范（CONTRIBUTING 母版），按本项目的「阶段 0–9 历史 + 详细随代码分层」实情改写。写 / 改文档前先读此文件；规则本体只存在这一处（根 `docs/CONTRIBUTING.md`），不在子项目里复制。

## 一、四层结构

| 层 | 文件 | 职责 | 粒度 |
|---|---|---|---|
| 路线图 | `docs/ROADMAP.md` | 定大方向、整体时间线 | 大版本 × 大方向 |
| 更新日志 | `docs/CHANGELOG.md` | 全部变更明细 | 每条变更 |
| 产品需求 | `docs/product/` | **用户可见契约** | 每版本 |
| 开发交付 | 各子项目 `docs/` + `docs/architecture/` + `docs/development/` | **技术实现** | 随代码就近 / 每版本 |

四层各司其职、互不重复：**ROADMAP 定方向 → CHANGELOG 记全部明细 → product 写用户能看见/能验收的契约 → development 写怎么实现**。

> 与母版的关键差异：本项目是**前后端 + Android/Web 多子项目**的工程（后端 v3.0.0 已单体化为 model 契约层 + bootstrap 唯一 app 两模块，但 backend / app / web 仍按子项目分库分 docs）。故 development 层**双维度**——按**组件**随代码就近分散在各子项目 `docs/`（`shiwujie-backend/docs/`、`shiwujie-frontend/app/docs/`、`shiwujie-frontend/web/docs/`）+ 根 `docs/architecture/`（跨切面概览）；按**版本**集中在根 `docs/development/vX.Y.Z/`（每版本三件套）。product / ROADMAP / CHANGELOG 仍**只集中在根 `docs/`** 一处——契约是全局的、实现按组件分散、交付按版本切片。

## 二、目录骨架

```text
Phase2/                              ← 仓库根（工作区恒设此层）
  README.md                          仓库入口（指向 docs + 子项目）
  CLAUDE.md                          Agent 工作向导（手写，tracked 随仓库走）；规则真值在 CONTRIBUTING，本文件为导航速查
  docs/                              ← 平台层：外部可见（概览 + 用户契约 + 规范 + 方向/明细）
    CONTRIBUTING.md                  ★ 本文件（规则真值，唯一一处）
    README.md                        文档中心首页：当前工作版本 + 入口 + 历史时期指针
    ROADMAP.md                       大方向（历史时期 rolled-up + 待实现）
    CHANGELOG.md                     变更明细（v3.0.0 进行中 + v2.1.0 + 历史时期 banner + 阶段 9..0）
    product/
      current.md                     → 当前工作版本（指针，指向进行中的 vX.Y.Z/）
      vX.Y.Z/                        每版本一个目录（4 文件同构）；工作直接写在进行中版本目录里，发布即冻结、历史版本目录保留不删
        product-overview.md
        functional-requirements.md
        acceptance-criteria.md
        changelog.md                 → 与根 CHANGELOG 同源
    architecture/                    跨切面概览（概念图/链路图/选型/分库设计/契约清单）
      overview.md / tech-stack.md / gateway-dubbo.md / auth.md / data-model.md
    development/                     ← 开发交付（按版本三件套，与按组件的子项目 docs/ 正交）
      current.md                     → 当前工作版本（指针，指向进行中的 vX.Y.Z/）
      vX.Y.Z/                        每版本一个目录（3 文件同构）
        task-breakdown.md            任务拆解（文件级改动 / 收尾任务）
        testing-strategy.md          测试策略（验证点 / 环境表 / 缺口）
        release-checklist.md         发布检查清单（含三处版本号同步）
  shiwujie-backend/docs/             ← 后端 development 细化（内部·详细·按组件）
    README.md / modules/*.md / known-issues.md / deployment.md
  shiwujie-frontend/
    app/docs/                        ← Android development 细化（按组件）
    web/docs/                        ← Web development 细化（按组件）
```

- **product 每版 4 文件**：overview（是什么）/ functional-requirements（`FR-<MODULE>-<NN>`）/ acceptance-criteria（`AC-<MODULE>-<NN>`）/ changelog。
- **development 双维度**：① 按**组件**随代码就近（子项目 `docs/`：每微服务 `modules/<svc>.md` + `known-issues.md` + `deployment.md`；根 `architecture/` 跨切面）；② 按**版本**集中在根 `development/vX.Y.Z/`（task-breakdown / testing-strategy / release-checklist 三件套）。两维度正交、不重复。
- **版本分级模型**：`current.md` 指针 + 每版本一个 `vX.Y.Z/` 目录。工作直接写在进行中版本的目录里；发布时冻结该目录、新建下一版、`current.md` 改指新版本；历史版本目录保留不删。`current.md` ×2（product + development）+ `docs/README.md` **三处版本号必须一致**。

## 三、各层撰写规则

### 1. ROADMAP.md

- **整体时间线**：`## 待实现`（真实前瞻特性）→ `## 历史时期`（阶段 0–9 rolled-up，日期不确定）。
- **历史时期**只写几条大方向（「微服务化」「AI 从零到能执行」「分布式生产化」…），**不展开**每阶段细节（细节在 CHANGELOG）。
- **待实现**只留真实前瞻特性，剔除 bug 修复 / 重构 / 实现细节（规范：这些**不入 ROADMAP**，下沉 CHANGELOG + known-issues）。🔴 安全洞（如权限检查被注释、默认用户后门）入待实现作必办。
- 格式：```` ```text ```` 围栏 + 裸 `[x]`/`[ ]`/`[?]`（无 `-` 前缀）。

### 2. CHANGELOG.md

- **降序**（最新在顶）。
- **进行中版本**：顶部 `## vX.Y.Z（进行中，未发布）`（当前 `v3.0.0`），打 tag 时改为 `## vX.Y.Z - YYYY-MM-DD`（精确日期，来自 tag）。
- **历史时期 banner**：`## 历史时期（阶段 0–9，约 2025-06 ~ 2026-07，日期不确定）`。阶段子条目 `### 阶段 N · 标题（约 区间）`，保留四分类（新增 / 变更 / 修复 / 移除）。
- 一行一变更，可带粗体小标题；技术细节（符号/路径）**允许在此层**（明细层）。

### 3. product/（用户可见契约）

- **可写**：对外 HTTP 路径（`/api/ai/ai/doChatByText`）、WebSocket 信令码（`type=5006`）、Dubbo 接口契约清单（接口名 + 方法名 + 提供者，**不含包路径**）、公开字段名、HTTP 状态码、业务状态码（`NOT_LOGIN`/`NO_AUTH`/`PARAMS_ERROR`）、路由表、数据契约（库/表/字段名 + 枚举值）、能力/方向的用户措辞、用户可验收的行为（`FR-*` / `AC-*`）。
- **禁止**：内部符号（`dispatch`/`ToolChoiceApp`/`CoordinationSocketHandler`/`MessageSerializer`…）、源码路径（`src/main/…`、`com/swj/…`、`file:line`、`.java:`）、启动命令（`java -jar`/`-DDUBBO_IP_TO_REGISTRY`/`mvn`）、测试产物（`pytest`/`N passed`/`grep` QA 命令）、配置 yml 片段。
- 四文件分工：overview = 项目是什么（用户视角：定位/三身份/覆盖范围/核心目标/非目标）；functional-requirements = 全部模块 `FR-<MODULE>-<NN>` 合并去重；acceptance-criteria = 全部 `AC-<MODULE>-<NN>` 合并去重；changelog = 该版本变更（与根 CHANGELOG 同源）。
- **状态标注**：已弃用/未启用/未实现的能力用状态列标 `❌ 已弃用` / `❌ 未启用` / `⚠ 部分`，不删除——诚实反映当前契约边界。

### 4. development/（技术实现）

双维度：

- **按组件·随代码就近**：
  - **子项目 `docs/modules/<svc>.md`**：模块定位（路径/端口/Dubbo 端口/框架/库）、目录与核心类、关键数据流（含 Mermaid）、配置要点、源码引用（`file:line` 允许且鼓励）。
  - **`docs/known-issues.md`** + **`docs/deployment.md`**：缺陷/技术债登记（🔴 安全洞同步进根 ROADMAP 待实现）+ 启动命令/部署坑/Dubbo 注册 IP/端口防火墙。
  - **跨切面**（路由图/调用图/鉴权链路/分库设计/选型表）放根 `docs/architecture/*.md`（概览级，含 Mermaid）。
- **按版本·集中三件套**（根 `docs/development/vX.Y.Z/`）：`task-breakdown.md`（该版本文件级改动 / 收尾任务）、`testing-strategy.md`（验证点 / 环境表 / 测试缺口）、`release-checklist.md`（逐项 `[ ]`，含三处版本号同步检查）。

> architecture 与 modules 的边界：architecture 写**跨多个服务的切面**（一张图/一张表覆盖全局）；modules 写**单服务内部**。二者都属 development 按组件维度，允许技术细节；`development/vX.Y.Z/` 属按版本维度，与按组件的文档正交、不重复。

## 四、内容边界对照表

| 内容 | product | development（architecture/modules/known-issues/版本三件套） |
|---|---|---|
| 用户可见行为 / 需求 / 验收（FR/AC） | ✓ | ✗（不重复） |
| HTTP 路径、WebSocket 信令码、HTTP 状态码、业务码 | ✓ | ✓（实现侧引用） |
| Dubbo 接口契约（接口名+方法+提供者） | ✓（清单） | ✓（消费侧数据流） |
| 库/表/字段名、枚举值（数据契约） | ✓ | ✓ |
| 内部符号（类名/方法名） | ✗ | ✓ |
| 源码路径、`file:line`、包名 | ✗ | ✓ |
| 启动命令、yml 片段、部署坑、端口防火墙 | ✗ | ✓ |
| bug / 重构 / 缺陷 / 技术债的实现细节 | ✗（只写用户影响） | ✓ |
| Mermaid 链路图 / 调用图 / 时序图 | ✗（用文字契约） | ✓ |

> **去重铁律**：FR/AC 只在 `product/<进行中版本>/` 出现一次。architecture 与 modules **不重复** FR/AC；若需引用，写「见 [product/v2.1.0/functional-requirements.md](product/v2.1.0/functional-requirements.md#fr-xxx)」。

## 五、版本号与状态规则

- **版本真值 = git tag**：tag 指向的 commit 即该版本起点；tag 之后的工作归下一版本。
- **版本分级模型（指针 + 版本目录）**：`product/current.md` 与 `development/current.md` 是指针，指向进行中的 `vX.Y.Z/` 目录；工作直接写在该版本目录里。发布时冻结该目录（保留不删）、新建下一版目录、把两个指针改指新版本。`current.md` ×2 + `docs/README.md` **三处版本号必须一致**。
- **现有 tag**：`v1.0`（一期·单体，独立根提交）、`v2.0.0`（二期开发后初步稳定版，2025-11-12）、`v2.1.0`（二期微服务封版，2026-07-11）；版本线 `v1.0`（一期）→ `v2.0.0`（二期初步稳定）→ `v2.1.0`（二期微服务封版）→ `v3.0.0`（单体化改造，进行中）。阶段 0–9 的历史**不回溯打 tag、不建 `product/阶段N/`**，统一并入 CHANGELOG 的「历史时期」banner（日期不确定标注）。这段历史的累积现状作为 `v2.1.0` 的起点内容——`product/v2.1.0/` 与 `development/v2.1.0/` 已封版（tag `v2.1.0`），当前工作版本 `v3.0.0`。
- **打 tag 流程**：① 冻结已存在的进行中 `product/<vX.Y.Z>/` 与 `development/<vX.Y.Z>/`；② CHANGELOG 的 `## vX.Y.Z（进行中，未发布）` 改为 `## vX.Y.Z - <tag 日期>`；③ `current.md` ×2 + README 指针与版本号改指下一版；④ ROADMAP 勾掉对应项。
- **状态标注**：进行中版本在 CHANGELOG / `current.md` / overview 标「进行中；尚未发布」。
- **前端无独立版本号**：前端随同一次 tag 一并发布，不在前端子项目里另设版本号。

## 六、一致性自检（改完文档后跑）

- **内容边界**（product 禁技术细节，期望零命中）：

  ```bash
  grep -rniE 'src/main|com/swj|\.java:|@DubboService|@DubboReference|java -jar|DUBBO_IP_TO_REGISTRY|mvn |\.yml' docs/product/
  ```

  > Dubbo **接口名**（`InnerSocket`/`InnerBlindService`…）允许出现在 product 的契约清单里；命中的应是接口名而非注解/路径。

- **链接有效性**：`docs/README.md`、各 product/architecture 文件、子项目 `docs/README.md` 内相对链接可达；`product/current/`、`development/current/` 目录**不应存在**（`current` 是 `.md` 指针文件，非目录）。
- **目录完整**：`product/current.md` 指针在 + 进行中 `product/v3.0.0/` 4 文件齐 + 封版 `product/v2.1.0/` 4 文件齐；`development/current.md` 指针在 + 进行中 `development/v3.0.0/` 三件套齐 + 封版 `development/v2.1.0/` 三件套齐；`shiwujie-backend/docs/modules/` 现况 = bootstrap + model-commonweb（+ gateway 历史标记）；frontend app/web 各有 docs。
- **去重**：`grep -rniE 'FR-[A-Z]+-[0-9]+|AC-[A-Z]+-[0-9]+' docs/ | grep -v 'docs/product/'` 期望**零命中**（FR/AC 不出现在 product 之外）。
- **三处版本号一致**：`product/current.md` + `development/current.md` + `docs/README.md` 写同一个当前版本号。
- **无信息丢失**：未开发路线全在 ROADMAP、缺陷全在 known-issues、历史全在 CHANGELOG、试错移除三处归位（CHANGELOG 移除 + development 残留 + product 非目标）。

## 七、流程约束

- **代码改 → 补对应文档**：任何代码变更须同步**对应层级**的文档，勿留陈旧——
  - **行为/特性变更** → CHANGELOG（必要时报 ROADMAP、刷 `product/<进行中版本>/` 契约）；
  - **内部实现变更**（核心类/数据流/配置/缺陷/技术债）→ 随代码就近的 development 文档：子项目 `modules/*.md` / `known-issues.md` / `deployment.md`、根 `architecture/*.md`、版本三件套 `development/vX.Y.Z/`；
  - **引用漂移**：改了某文件，就更新引用它的文档（`file:line`、路径、符号名漂移及时修）。
- **提交**：Conventional Commits（`docs: …` / `feat: …` / `fix: …`），**绝不带作者署名**（无 `Co-Authored-By` / `Generated with`）。
- **分支与合并**：常规单人开发直推 `master`；**禁止本地 `git merge`**（跨分支合并走远程 PR）；rebase 可用；**推送需用户明确同意**。
- **不动历史**：不回溯给阶段 0–9 打 tag、不建历史版本目录。

### 子项目内用 Agent 开发时必读

从**仓库根**（`Phase2/`）打开工作区，根 `CLAUDE.md`（tracked，Agent 工作向导）会自动载入并指向本规范。开发任意子项目前，Agent 必读：

1. `docs/CONTRIBUTING.md`——本规范（规则与内容边界）。
2. `docs/product/current.md`——当前版本入口；进入 `product/<进行中版本>/` 查该子项目实现的契约（FR/AC/HTTP 路径/WebSocket 码/Dubbo 接口/表）。
3. 相关 `docs/architecture/*.md`——跨切面契约（路由图/调用图/鉴权链路/分库）。

> 子项目 `docs/` 仅 development 细化（按组件），**不含契约**。**别把子项目隔离成独立 workspace**（否则根文档不自动出现）；工作区恒 = 仓库根。

### 阶段回卷仪式（development 分散、契约集中 → 有同步缝，需回卷）

- **行为变更** → 即时进 CHANGELOG（轻量，小改顺手）。
- **阶段结束 / 打 tag 前 → 覆盖全项目的 agent 回卷**：① 刷 `product/<进行中版本>/` 契约（若有用户可见变更）；② 写该阶段 CHANGELOG 条目；③ ROADMAP 勾项；④ 打 tag 时冻结当前 `product/vX.Y.Z/` + `development/vX.Y.Z/`、`current.md` ×2 指针改指下一版。
