# v2.0.0 任务拆解

> 本文件属 `development/v2.0.0/`（二期·进行中）。**交付范围**（已建成能力，按模块指向各组件详细文档，不在此重复 `file:line`）+ **收尾任务**（打 tag 前须完成的 file-level 任务，此处允许源码路径/符号，属 development 层）。版本指针 [../current.md](../current.md)。

## v2.0.0 交付范围（已建成）

### 后端（shiwujie-backend）

| 模块 | 已建成能力 | 详细文档 |
|---|---|---|
| gateway | 端口 8100，Spring Cloud Gateway 路由 + LoadBalancer 轮询，**不做鉴权**（下沉各服务） | [modules/gateway.md](../../../shiwujie-backend/docs/modules/gateway.md) |
| user | 三类身份（盲人/志愿者/员工）+ 家庭；端口 8200；Dubbo 21200；提供 Blind/Volunteer/Family Inner 服务 | [modules/user.md](../../../shiwujie-backend/docs/modules/user.md) |
| call | 视频求助 FIFO 匹配 + 紧急求助；端口 8300；Dubbo 21300；Spring WebSocket `/ws/call`；提供 Socket Inner 服务 | [modules/call.md](../../../shiwujie-backend/docs/modules/call.md) |
| community | 省/市/街道三级社区 + 审核 + 管理者 + 求助帖 + 活动 + 报名；端口 8400；Dubbo 21400 | [modules/community.md](../../../shiwujie-backend/docs/modules/community.md) |
| ai | Spring AI Alibaba 多模型 + 工作流式工具路由（case 1–9）+ 图片瘦身上下文工程；端口 8500；Dubbo 21500；**纯消费方** | [modules/ai.md](../../../shiwujie-backend/docs/modules/ai.md) |
| model | 契约层（Dubbo Inner 接口 + VO/Request），全模块可依赖 | [modules/model-commonweb.md](../../../shiwujie-backend/docs/modules/model-commonweb.md) |
| common-web | 公共层（LoginCheckInterceptor / JwtUtil / RedissonConfig 等），仅 SB2 模块依赖 | [modules/model-commonweb.md](../../../shiwujie-backend/docs/modules/model-commonweb.md) |

跨切面（路由表 / Dubbo 契约 / 鉴权链路 / 分库）见 [architecture/](../../architecture/)。

### 前端（shiwujie-frontend）

| 端 | 已建成能力 | 详细文档 |
|---|---|---|
| Android App | 视障/志愿者双端（Java + ViewBinding）；AI 多轮对话 + 悬浮球；视频/紧急求助；拍照识别；高德 URI 导航 | [app/docs/android.md](../../../shiwujie-frontend/app/docs/android.md) |
| Web 管理后台 | Vue3 + AntdV4；社区/活动/求助/用户管理；社区入驻审核 | [web/docs/vue-admin.md](../../../shiwujie-frontend/web/docs/vue-admin.md) |

> 阶段 0–9 的演进明细见 [CHANGELOG.md](../../CHANGELOG.md) 历史时期；用户可见契约（FR/AC）见 [product/v2.0.0/](../../product/v2.0.0/)。

## v2.0.0 收尾任务（打 tag 前须完成）

> 与 [ROADMAP.md](../../ROADMAP.md) 待实现段、[known-issues.md](../../../shiwujie-backend/docs/known-issues.md) 三处归位不重复——此处给 file-level 定位。

### 🔴 安全加固（必办）

- [ ] **关闭 ai 默认用户兜底**：`shiwujie-ai` 的 `LoginCheckInterceptor`（无 token 即注入默认盲人，生产白嫖 AI / 消耗 DashScope）。机制见 [auth.md](../../architecture/auth.md)。
- [ ] **恢复 Helppost / Community 删除/更新权限检查**：community 模块删除/更新前的权限校验代码被注释（任意登录用户可删/改任意帖/社区）。
- [ ] **`/ws/call` 与社区/家庭审核补鉴权**：WS 当前绕过 JWT；加入社区/家庭审核未校验是否 creator。
- [ ] **密码哈希升级 + 密钥环境变量**：MD5 无盐 → BCrypt/Argon2；`TOKEN_SECRETKEY` 走环境变量（当前硬编码弱密钥）。位置 user 模块登录/注册。
- [ ] **前端 TLS + 去硬编码密钥**：App/Web 明文 HTTP/WS → TLS；anyRTC appId / 讯飞 appid / 高德 key / token 移出硬编码；release 关闭调试日志。
- [ ] **修复续期 key 拼接 bug**：滑动会话续期漏 `-blind-`/`-volunteer-` 前缀，token 静默失效。位置 common-web Redis 操作。

### 能力补全

- [ ] **接入真实避障模型**：App `ObstacleDetectionManager` 当前用模拟数据，避障服务内网不可达。
- [ ] **App 集成高德 SDK**：`NavigationManager` 当前仅 URI 调起高德（未集成 SDK）。
- [ ] **Web 统计模块落地**：Dashboard / CommunityStats / ActivityStats 全占位（`<a-empty/>`，无图表库）；引入 echarts/antv + 接 `activityApi.getActivityStats`。
- [ ] **Web 多社区切换**：当前只操作 `userCommunities[0]`。
- [ ] **Activitysign 补签到/签退接口** + 校验人数上限 / 活动状态。

### 工程化

- [ ] **分布式事务**：跨库写当前靠 `synchronized` + 级联 update（Seata）。
- [ ] **网关统一鉴权**：4 处 `LoginCheckInterceptor` 重复，上移 gateway。
- [ ] **Knife4j 聚合 ai**：SB2 v2 api-docs 与 SB3 OpenAPI3 不兼容。
- [ ] **压测 + AiLogs 索引调优**：当前无压测；AiLogs 当对象存储用，数据增长隐患。
- [ ] **Docker 化部署**。
