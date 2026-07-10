# v2.0.0 测试策略

> 本文件属 `development/v2.0.0/`（二期·已封版 2026-07-11）。**测试现状** + **验证点**（按模块，交叉引用 [product/v2.0.0/acceptance-criteria.md](../../product/v2.0.0/acceptance-criteria.md) 的 `AC-*`）+ **环境表** + **已知缺口**。版本指针 [../current.md](../current.md)。

## 测试现状

- **后端**：仅 `contextLoads`（Spring Boot 启动冒烟），无业务单元测试 / 集成测试 / 端到端。
  - **启用真实容器**：`contextLoads` 用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + 真实 Nacos/MySQL/Redis 连接（见阶段 9 提交 `d832152`），验证上下文能起。
  - **JDK21 + nacos-client 坑**：SB2.7 模块（gateway/user/call/community/common-web）的 `contextLoads` 在 JDK21 上因旧 `nacos-client` 失败——CLI 跑测试切 **JDK17**；ai 模块（SB3.4.5/Java21）跑 JDK21。
  - **Dubbo 端口坑**：provider 端口原 502xx 落入 Windows Hyper-V/WSL2 动态保留段，bind 抛 `Address already in use` 而 `netstat` 查无——迁至 **21200–21500**（阶段 9 `0bdbbc5`）。
- **前端**：Android 无自动化测试（手动）；Web 无测试。
- **联调**：靠手动跑 App + Web + 全后端，无自动化回归。

## 验证点（按模块，交叉引用 AC）

> 用户可验收行为见 [product/v2.0.0/acceptance-criteria.md](../../product/v2.0.0/acceptance-criteria.md)（`AC-<MODULE>-<NN>`）。下表只列「验证这条 AC 用什么手段」。

| 模块 | 关键 AC | 验证手段 | 现状 |
|---|---|---|---|
| user | AC-USER 登录/注册/家庭 | 手动：App 选身份登录 + Web 社区登录 | 手动覆盖 |
| call | AC-CALL 视频求助 FIFO / 紧急求助 | 手动：盲人发起 → 志愿者接听 → 挂断回放 | 手动覆盖 |
| community | AC-COMMUNITY 社区审核 / 活动 / 求助 | 手动：Web 审核流 + App 活动报名 | 手动覆盖 |
| ai | AC-AI 多轮对话 / 工具路由 / 图片识别 | 手动：App AiFragment 文本 + 拍照；核对 WS 5001–5006 推送 | 手动覆盖 |
| gateway | AC-GATEWAY 路由可达 | `contextLoads` + 手动 curl 各 `/api/*` | 半自动 |

## 环境表

| 维度 | 后端业务模块 | ai 模块 | 前端 |
|---|---|---|---|
| JDK | **17** | **21** | n/a |
| Spring Boot | 2.7 | 3.4.5 | n/a |
| MySQL | shiwujieuser / shiwujiecall / shiwujiecommunity / shiwujieai（4 库） | shiwujieai | n/a |
| Redis | db=2（全模块共享） | db=2 | n/a |
| 注册中心 | Nacos | Nacos | n/a |
| Dubbo provider 端口 | 21200 / 21300 / 21400 | 21500 | n/a |
| 公网入口 | 47.112.114.139:8100（硬编码） | 同 | vite proxy `/api` → 8100 |

> 凭据留 yml 用 `${ENV:default}` 占位符；公网 IP 硬编码不抽环境变量（项目约定，见 [CONTRIBUTING.md](../../CONTRIBUTING.md)）。

## 已知缺口

- **无业务测试**：后端无 service/controller 单测，无集成测试。
- **无自动化回归**：前端无测试，全靠手动。
- **无压测**：性能基线缺失（见 [task-breakdown.md](task-breakdown.md) 工程化段）。
- **无 CI**：测试只在本地手动跑（且受 JDK17/21 + Dubbo 端口坑约束）。
