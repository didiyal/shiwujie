# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Agent 工作向导（手写，对标 `/init` 产物，**tracked 随仓库走**）。文档规范真值在 [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md)——本文件只做导航与约定速查，规则以 CONTRIBUTING 为准。

## 仓库是什么

面向视障人士的无障碍服务平台：**单体后端（v3.0.0 单体化，原 Spring Cloud 微服务已合并为 model 契约层 + bootstrap 唯一 app 两模块）+ 原生 Android 客户端 + Vue3 管理后台**，落地「AI 助手 + 远程人工协助 + 社区互助」。阶段 0–9 累积现状已于 `v2.1.0`（二期微服务封版，2026-07-11）封版，当前工作版本 `v3.0.0`（单体化改造·进行中）。版本线：`v1.0`（一期·单体）→ `v2.0.0`（二期初步稳定，2025-11-12，Spring AI M6.1）→ `v2.1.0`（二期微服务封版，2026-07-11）→ `v3.0.0`（单体化·进行中）。

## 构建 / 测试 / 运行

**后端**（Maven 多模块；`shiwujie-backend/pom.xml` 父 pom 聚合 **2 模块**：`shiwujie-model`（契约层 jar，无 Spring）+ `shiwujie-bootstrap`（唯一可执行单体 app，SB 3.4.5/Java21，repackage）。v3.0.0 阶段2.8 把原 common-web + user/call/community/ai 五模块并入 bootstrap，gateway 已删）：

```bash
# 构建 2 模块 reactor（model 契约层 + bootstrap 单体 app）
mvn -f shiwujie-backend/pom.xml install -DskipTests

# 测试（当前后端 0 测试类，surefire 跑空）
mvn -f shiwujie-backend/pom.xml test

# 运行：单 jar，仅需 MySQL（库 shiwujie）+ Redis（db=2）；无 Nacos/Dubbo
java -jar shiwujie-backend/shiwujie-bootstrap/target/shiwujieBootstrap-0.0.1-SNAPSHOT.jar
# 期望：Started ShiwujieBootstrapApplication ... on port(s): 8100
```

> v3.0.0 单体化后统一 Spring Boot **3.4.5** / Java **21**（SB 2.7/SB 3.4.5 双栈根因消除）；「JDK21 + 旧 nacos-client 的 contextLoads 坑」随 Nacos 删除已失效。

**前端 Web 管理后台**（`shiwujie-frontend/web/`，Vite + Vue3 + Ant Design Vue）：

```bash
cd shiwujie-frontend/web
npm install
npm run dev        # vite dev server（端口 9090；npm run dev:clean 先 kill 9090 再起）
npm run build      # 生产构建
npm run lint       # eslint --fix
```

**前端 Android 客户端**（`shiwujie-frontend/app/shiwujie/`，Java + ViewBinding，Gradle Kotlin DSL）：

```bash
cd shiwujie-frontend/app/shiwujie
./gradlew assembleDebug                # debug 构建
./gradlew :app:testDebugUnitTest       # 单元测试
```

## 顶层结构

```
Phase2/
├── docs/                      # ★ 平台层文档（规范/契约/概览/路线/历史）——先读这里
├── shiwujie-backend/          # 后端单体：model（契约层）+ bootstrap（唯一 app，含原 user/call/community/ai/common-web）
├── shiwujie-frontend/
│   ├── app/                   # Android 原生（Java + ViewBinding）
│   └── web/                   # Vue3 管理后台
└── README.md                  # 仓库入口
```

## 写文档 / 改代码前，先读

1. [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) —— 四层文档规范（product / architecture / development / ROADMAP+CHANGELOG）、版本分级模型、内容边界对照表、一致性自检、流程约束。**必读**。
2. [`docs/product/current.md`](docs/product/current.md) —— 当前产品需求版本指针（→ `product/v3.0.0/`）：用户可见契约（FR/AC/端点/状态码/表字段）。
3. [`docs/development/current.md`](docs/development/current.md) —— 当前开发交付版本指针（→ `development/v3.0.0/` 三件套：任务/测试/发布）。
4. 相关 [`docs/architecture/*.md`](docs/architecture/) —— 跨切面（路由/调用图/鉴权/分库/选型）。

> **子项目内用 Agent 开发时，工作区恒 = 仓库根**（别把子项目隔离成独立 workspace，否则根 docs 不自动出现）。子项目 `shiwujie-backend/docs/`、`shiwujie-frontend/{app,web}/docs/` 是 development **按组件**细化，根 `docs/development/vX.Y.Z/` 是 **按版本**三件套，二者正交，**都不含契约**（契约在 product）。

## 文档分层（一句话，详见 CONTRIBUTING）

| 层 | 位置 | 写什么 | 禁什么 |
|---|---|---|---|
| product | `docs/product/current.md`（指针）→ `vX.Y.Z/` | 用户可见契约 | 源码路径/内部符号/启动命令/`file:line` |
| architecture | `docs/architecture/` | 跨切面概览（图/选型/分库概念） | 单模块实现细节 |
| development | 子项目 `docs/`（按组件）+ `docs/development/vX.Y.Z/`（按版本三件套） | 技术实现（核心类/数据流/配置/缺陷） | 用户契约（去 product 查） |
| ROADMAP/CHANGELOG | `docs/` | 方向与历史（根单中心） | |

## 工作约定（务必遵守）

- **禁止本地 `git merge`**：合并走远程 PR；推送允许；日常单人开发可直接提交 master。
- **提交格式**：Conventional Commits；**不带署名**（无 `Co-Authored-By` / `Generated with`）。
- **凭据**：留在 yml 里，用 `${ENV:default}` 占位符 + 内联默认值；**不**搬去 vault/Nacos-config。
- **公网 IP `47.112.114.139`**：硬编码，**不**抽环境变量、**不** gitignore prod 配置。
- ~~**Dubbo provider 端口 21200–21500**~~：v3.0.0 删 Dubbo 后失效（历史：避让 Windows Hyper-V/WSL2 动态保留段，原 502xx 会 bind 抛错）。
- ~~**JDK21 + nacos-client 坑**~~：v3.0.0 删 Nacos 后失效（历史：SB2.7 模块 JDK21 跑 contextLoads 因旧 nacos-client 失败需切 JDK17，compile 在 JDK21 OK）。
- **版本分级模型**：`current` 是 `.md` 指针文件（**非目录**），工作直接写在进行中的 `vX.Y.Z/` 目录里（当前 `v3.0.0/`）。阶段 0–9 不回溯分层/打 tag，整合为 CHANGELOG 一块「历史时期」（日期不确定），其累积现状作 `v2.1.0`（已封版）起点。打 tag 时冻结当前版本目录、`current.md` 改指下一版、补精确日期。三处版本号一致：`product/current.md` + `development/current.md` + `docs/README.md`。
- **代码改 → 补对应文档**：改代码同步更新**对应层级**文档——行为/特性变更进 CHANGELOG（必要时刷 `product/<进行中版本>/` 契约、报 ROADMAP）；内部实现变更（核心类/数据流/配置/缺陷）更新随代码就近的 development 文档（子项目 `modules`/`known-issues`/`deployment`、根 `architecture`、版本三件套 `development/vX.Y.Z/`）；引用的 `file:line` 漂移及时修。规则真值见 [CONTRIBUTING 第七节](docs/CONTRIBUTING.md)。
- **回卷仪式**：行为变更进 CHANGELOG；阶段结束/tag 前做覆盖全项目的 product 契约刷新 + CHANGELOG + ROADMAP 勾项。

## 内容边界自检（CONTRIBUTING 第六节，本项目版）

`grep -rniE 'src/main|com/swj|\.java:|@DubboService|@DubboReference|java -jar|DUBBO_IP_TO_REGISTRY|mvn |\.yml' docs/product/` 期望零命中。

## 技术栈现状（v3.0.0 单体化后）

后端统一 Spring Boot **3.4.5** / Java **21**（Spring AI 强制 SB3/Java21）。v2.1.0 的 SB 2.7 / SB 3.4.5 双栈割裂已消除——原 `shiwujie-model`（契约层）保留，`shiwujie-common-web`（公共层）随阶段2.8 并入 `shiwujie-bootstrap`，现后端 = **model 契约层 + bootstrap 唯一 app 两模块**。详见 [`docs/architecture/tech-stack.md`](docs/architecture/tech-stack.md)。
