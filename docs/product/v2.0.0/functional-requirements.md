# 功能需求（v2.0.0 · 进行中）

> 全部模块的功能需求合并去重，编号 `FR-<MODULE>-<NN>`。`✅` 已实现 / `⚠` 部分（见备注）/ `❌` 已弃用·未启用·未实现。本文件属 `product/v2.0.0/`（进行中）；技术实现（核心类/数据流/源码引用）见各子项目 `docs/modules/`，**不在此重复**。状态列如实反映当前契约边界（含试错移除能力）。

## 网关与共享层

### FR-GATEWAY（网关）

| ID | 需求 | 状态 |
|---|---|---|
| FR-GATEWAY-01 | 基于 Nacos 服务发现对 user/call/community/ai 做路径前缀路由（`/api/user/**`、`/api/call/**`、`/api/community/**`、`/api/ai/**`） | ✅ |
| FR-GATEWAY-02 | 支持 WebSocket 与 SockJS 双形态路由（`/api/ws/call`） | ✅ |
| FR-GATEWAY-03 | 聚合各服务接口文档（当前未聚合 ai） | ⚠ |
| FR-GATEWAY-04 | 提供轮询负载均衡 | ✅ |
| FR-GATEWAY-05 | 多机部署时正确注册对外可达 IP（dev/prod 环境） | ✅ |

### FR-MODEL（契约层 / 公共层）

| ID | 需求 | 状态 |
|---|---|---|
| FR-MODEL-01 | 所有跨服务 RPC 接口集中在契约层定义（`Inner*Service`） | ✅ |
| FR-MODEL-02 | domain / enums / request / VO 在契约层统一维护作为数据契约 | ✅ |
| FR-MODEL-03 | 分页请求提供统一基类 | ✅ |
| FR-MODEL-04 | 统一返回结构与统一异常转换 | ✅ |

## 鉴权（跨服务）

### FR-AUTH

| ID | 需求 | 状态 |
|---|---|---|
| FR-AUTH-01 | 登录成功后生成 HS256 JWT 并写 Redis（TTL=90 天） | ✅ |
| FR-AUTH-02 | 受保护服务校验 Bearer token：JWT 签名 + Redis token 比对，**忽略 JWT 自身过期、以 Redis TTL 为准** | ✅ |
| FR-AUTH-03 | 放行 OPTIONS 预检与登录/注册类 URL | ✅ |
| FR-AUTH-04 | 校验通过后注入用户身份到请求 | ✅ |
| FR-AUTH-05 | 每次合法请求对 Redis token key 续期（设计意图=滑动会话） | ❌（续期 key 拼接 bug，滑动会话静默失效） |
| FR-AUTH-06 | 注销删除 Redis 对应 token key | ✅ |

> ⚠ 鉴权链路存在多个已知风险（JWT 过期校验关闭、弱密钥硬编码、MD5 无盐、ai 默认用户后门、WS 绕过鉴权等），方向登记见 [ROADMAP.md](../../ROADMAP.md) 安全加固，机制定位见 [architecture/auth.md](../../architecture/auth.md) 与 [shiwujie-backend/docs/known-issues.md](../../../shiwujie-backend/docs/known-issues.md)。

## 用户与家庭（user 模块）

### FR-USER

| ID | 需求 | 状态 |
|---|---|---|
| FR-USER-01 | 视障者密码登录（未注册自动注册） | ✅ |
| FR-USER-02 | 视障者一键登录（无密码） | ✅ |
| FR-USER-03 | 志愿者密码登录（未注册自动注册） | ✅ |
| FR-USER-04 | 志愿者一键登录（无密码） | ✅ |
| FR-USER-05 | 跨表手机号唯一（盲人/志愿者互斥） | ✅ |
| FR-USER-06 | 登录签发 JWT + Redis 单点（同账号他处登录旧 token 失效） | ✅ |
| FR-USER-07 | 注销清 Redis | ✅ |
| FR-USER-08 | 修改密码 | ✅ |
| FR-USER-09 | 修改手机号 | ✅ |
| FR-USER-10 | 资料更新（身份证/残疾证 MD5 存储） | ✅ |
| FR-USER-11 | 资料脱敏返回（证件仅回布尔） | ✅ |
| FR-USER-12 | 创建家庭（仅志愿者） | ✅ |
| FR-USER-13 | 申请加入家庭 | ✅ |
| FR-USER-14 | 家主审核加入（通过/拒绝） | ✅ |
| FR-USER-15 | 查家庭列表 | ✅ |
| FR-USER-16 | 踢人退出家庭 | ✅ |
| FR-USER-17 | 删家庭（级联清成员） | ✅ |
| FR-USER-18 | 删志愿者（级联删家庭、清社区） | ✅ |
| FR-USER-19 | 加入社区 | ✅ |
| FR-USER-20 | 踢出社区（需管理员权限） | ✅ |
| FR-USER-21 | 分页查社区成员 | ✅ |
| FR-USER-22 | Inner 服务供 ai/call/community 调用 | ✅ |

## 实时通信与求助（call 模块）

### FR-CALL

> WebSocket 信令码：`-1` 心跳 / `0` 绑定 / `1` 匹配通知志愿者 / `2` 志愿者就绪转告视障 / `3` 紧急求助通知家属 / `4` 取消通知 / `5001~5006` AI 触发的视障端跳转（拍照/视频求助/紧急求助/跳应用/导航）。

| ID | 需求 | 状态 |
|---|---|---|
| FR-CALL-01 | 志愿者加入/退出匹配队列（持久 + 30s TTL） | ✅ |
| FR-CALL-02 | 视障匹配从队列取最早志愿者（FIFO） | ✅ |
| FR-CALL-03 | 匹配成功通知志愿者（type1），就绪后转告视障（type2） | ✅ |
| FR-CALL-04 | 紧急求助须已加入家庭，群发家庭内所有家属（type3） | ✅ |
| FR-CALL-05 | 求助状态机：WAITING→HELPING→END_HELP/FALL | ✅ |
| FR-CALL-06 | 挂断记录结束时间 + 时长（分钟） | ✅ |
| FR-CALL-07 | 心跳保活（type -1） | ✅ |
| FR-CALL-08 | 连接绑定（type 0）以手机号为 key | ✅ |
| FR-CALL-09 | 提供 Dubbo `InnerSocket`，供 ai 触发 6 类前端推送（5001~5006） | ✅ |
| FR-CALL-10 | 同一用户重复匹配/求助拦截 | ✅ |

## 社区治理（community 模块）

### FR-COMMUNITY

| ID | 需求 | 状态 |
|---|---|---|
| FR-COMMUNITY-01 | 注册人入驻社区，自动建省/市级默认社区 | ✅（无分布式事务） |
| FR-COMMUNITY-02 | 社区管理员登录 | ✅ |
| FR-COMMUNITY-03 | 修改/删除社区（联动清成员 + 删管理记录） | ✅ |
| FR-COMMUNITY-04 | 分页查子社区 | ✅ |
| FR-COMMUNITY-05 | 管理员增改删 + 员工分页 | ✅ |
| FR-COMMUNITY-06 | 成员加入审核（通过回写 communityId） | ✅（无事务） |
| FR-COMMUNITY-07 | 视障发求助帖（须隶属社区） | ✅ |
| FR-COMMUNITY-08 | 求助帖分页/删除/更新 | ⚠（权限检查被注释） |
| FR-COMMUNITY-09 | 活动 CRUD | ✅ |
| FR-COMMUNITY-10 | 活动报名（防重复） | ⚠（未校验人数上限/活动状态） |

## AI 助手（ai 模块）

### FR-AI

> 状态列如实标注已弃用/未启用/未实现能力（试错-移除记录见 [CHANGELOG.md](../../CHANGELOG.md) 阶段 7；残留代码状态见 [shiwujie-backend/docs/modules/ai.md](../../../shiwujie-backend/docs/modules/ai.md)）。

| ID | 需求 | 状态 |
|---|---|---|
| FR-AI-01 | 文本多轮对话（流式 SSE） | ✅ |
| FR-AI-02 | 图片识别（多模态流式） | ✅ |
| FR-AI-03 | 多轮对话记忆（跨会话持久化） | ✅ |
| FR-AI-04 | 工具路由（9 类业务工具，约定 JSON 解析） | ✅ |
| FR-AI-05 | 拍照识别 / 图片追问 | ✅ |
| FR-AI-06 | 视频求助 / 紧急求助（前端跳转） | ✅ |
| FR-AI-07 | 家庭加入/退出/查询 | ✅ |
| FR-AI-08 | 高德导航 / 跳转他应用 | ✅ |
| FR-AI-09 | 图片多轮上下文压缩（降低多图场景首字延迟） | ✅ |
| FR-AI-10 | Web 搜索（联网检索 + 摘要） | ✅ |
| FR-AI-11 | 登录鉴权（JWT + Redis） | ⚠（带默认用户兜底，生产后门） |
| FR-AI-12 | AI 操作日志 | ✅ |
| FR-AI-13 | RAG 知识库增强 | ❌ 已弃用（半残留、未启用） |
| FR-AI-14 | 自研 ReAct Agent | ❌ 未启用（保留代码） |
| FR-AI-15 | MQTT / IoT 硬件接入 | ❌ 已取消（依赖残留） |
| FR-AI-16 | community 求助帖工具 | ❌ 未实现（仅提示词残留） |

## Android 客户端（App）

### FR-APP

| ID | 需求 | 状态 |
|---|---|---|
| FR-APP-01 | 双角色选择与一键/密码登录 | ✅ |
| FR-APP-02 | 登录态持久化 | ✅ |
| FR-APP-03 | 盲人端 AI 多轮对话（流式 + 流式 TTS 播报） | ✅ |
| FR-APP-04 | 盲人端 AI 悬浮球（任意界面唤起） | ✅ |
| FR-APP-05 | 视频求助发起/接听（双向音视频） | ✅ |
| FR-APP-06 | 紧急求助（家属通知 + 来电悬浮窗 + 响铃） | ✅ |
| FR-APP-07 | WebSocket 信令处理（-1/0/1/2/3/4/5xxx） | ✅ |
| FR-APP-08 | 社区（求助帖/活动/报名） | ✅ |
| FR-APP-09 | 家庭（加入/退出/审核） | ✅ |
| FR-APP-10 | 图片识别（多模态） | ✅ |
| FR-APP-11 | 避障检测 | ⚠（模拟数据，模型未接） |
| FR-APP-12 | 高德导航（URI 调起） | ✅ |
| FR-APP-13 | 跳转他应用 | ✅ |
| FR-APP-14 | 讯飞 TTS / ASR | ✅ |
| FR-APP-15 | 资料编辑 | ✅ |
| FR-APP-16 | 志愿者消息 | ⚠（全 mock 数据） |
| FR-APP-17 | 个人中心 | ✅ |
| FR-APP-18 | 权限管理 | ✅ |

## Web 管理后台

### FR-WEB

| ID | 需求 | 状态 |
|---|---|---|
| FR-WEB-01 | 社区管理员密码登录 | ✅ |
| FR-WEB-02 | 社区入驻注册（自动登录） | ✅ |
| FR-WEB-03 | 登录态持久化 + 路由守卫 | ✅ |
| FR-WEB-04 | 社区列表（子社区树/分页/删除） | ✅（子社区分页有 bug） |
| FR-WEB-05 | 社区信息编辑 | ✅ |
| FR-WEB-06 | 加入申请审核 | ✅ |
| FR-WEB-07 | 活动 CRUD | ✅ |
| FR-WEB-08 | 活动报名管理 + 审核 | ✅ |
| FR-WEB-09 | 求助帖列表 + 详情 | ✅（只读） |
| FR-WEB-10 | 视障列表 + 踢出 | ✅（添加/编辑"开发中"） |
| FR-WEB-11 | 志愿者列表 + 设为管理员 + 踢出 | ✅ |
| FR-WEB-12 | 员工列表 + 角色更新 | ✅ |
| FR-WEB-13 | 大数字（19 位雪花 ID）精度保护 | ✅（多层防护） |
| FR-WEB-14 | 仪表板统计概览 | ❌ 占位 |
| FR-WEB-15 | 社区统计图表 | ❌ 占位 |
| FR-WEB-16 | 活动统计图表 | ❌ 占位（后端接口已就绪） |

## 数据契约（补充）

跨服务数据访问不走 JOIN，统一经 Dubbo `Inner*Service`。各库独立、共享缓存 db=2。表字典与字段枚举见 [architecture/data-model.md](../../architecture/data-model.md)（数据契约属用户可见面，列入 product 边界；分库设计的实现取舍见同文与 [shiwujie-backend/docs/modules/](../../../shiwujie-backend/docs/)）。
