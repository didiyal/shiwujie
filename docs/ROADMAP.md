# 路线图（ROADMAP）

> 大方向清单。`## 待实现`（真实前瞻特性，按规范剔除 bug/重构/细节——那些下沉 [CHANGELOG.md](CHANGELOG.md) + 各子项目 `known-issues.md`）→ `## 历史时期`（阶段 0–9 rolled-up，日期不确定，细节在 CHANGELOG）。

## 待实现

### 安全加固（🔴 必办）

```text
[ ] 关闭 ai 默认用户兜底（生产后门：无 token 即白嫖 AI / 消耗 DashScope）
[ ] 修复 Helppost / Community 删除更新权限检查被注释（任意登录用户可删/改任意帖/社区）
[ ] /ws/call 与社区/家庭审核补鉴权（当前 WS 绕过 JWT、审核未校验是否 creator）
[ ] 密码 MD5 无盐 → BCrypt/Argon2；TOKEN_SECRETKEY 走环境变量（当前硬编码弱密钥）
[ ] 前端明文 HTTP/WS → TLS；移除硬编码 SDK Key / token；release 关闭调试日志
[ ] 修复续期 key 拼接 bug（漏 -blind-/-volunteer- 前缀，滑动会话静默失效）
```

> 以上 🔴 项的代码定位与机制见 [shiwujie-backend/docs/known-issues.md](../shiwujie-backend/docs/known-issues.md) 与 [architecture/auth.md](architecture/auth.md)；此处只登记为必办方向。

### 能力补全

```text
[ ] 接入真实避障模型（App 当前用模拟数据，避障服务内网不可达）
[ ] App NavigationManager 集成高德 SDK（当前仅 URI 调起）
[ ] Web 统计模块落地（Dashboard / 社区 / 活动统计当前全占位）+ 引入图表库
[ ] Web 多社区切换（当前只能操作 userCommunities[0]）
[ ] Activitysign 补签到/签退接口 + 校验人数上限 / 活动状态
```

### 工程化

```text
[ ] 引入分布式事务（Seata，当前跨库写靠 synchronized + 级联 update）
[ ] 网关统一鉴权（当前下沉各服务，4 处 LoginCheckInterceptor 重复）
[ ] Knife4j 聚合 ai 服务（SB2/SB3 文档协议不兼容）
[ ] 压力测试 + 性能基线 + AiLogs 索引调优（当前无压测）
[ ] Docker 化部署
[?] 二期单体重写（去微服务、按当前体量精简；反思 v2.0.0 微服务过度设计）—— 考虑中，编号待立项（v2.x 或 v3）
```

## 历史时期（阶段 0–9，约 2025-06 ~ 2026-07，日期不确定）

> 过去开发按能力域聚类为阶段 0–9，**不回溯打 tag、不建版本目录**，统一并入 [CHANGELOG.md](CHANGELOG.md) 的历史时期明细。此处只列大方向。

```text
[x] 一期：单体平台封版（uniapp + Spring 单体 + 单库 4 表）
[x] 二期微服务化：Spring Cloud + Nacos + Dubbo，业务 SB2.7/Java17 + AI SB3.4.5/Java21
[x] 用户 / 家庭 / 社区：三类身份、家庭审核、省/市/街道三级社区治理与求助
[x] 实时通信：视频求助 FIFO 匹配 + 家庭紧急求助（Netty → Spring WebSocket 演进）
[x] AI 大脑：Spring AI Alibaba 多模型 + 工作流式工具路由 + 图片瘦身上下文工程（mqtt/RAG/自研 ReAct 试错后移除/弃用）
[x] 分布式生产化：gateway 负载均衡、多机 nacos+dubbo、dev/prod 多环境、凭据占位符化、模块扁平化、Dubbo 端口迁出保留段
```

> v2.0.0（进行中）：`docs/product/v2.0.0/` 与 `docs/development/v2.0.0/` 三件套已就位、随收尾工作滚动；打 tag 时冻结并补精确日期（流程见 [CONTRIBUTING.md](CONTRIBUTING.md) 第五节），并在本文件历史时期之上新增 `## v2.0.0` 条目。
