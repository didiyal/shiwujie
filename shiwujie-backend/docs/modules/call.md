# call 模块

> WebSocket 信令中枢 + 视频求助/紧急求助业务；提供 `InnerSocket`，是 **AI→前端实时推送的唯一落地点**。本文为 development 细化；用户可见契约（FR-CALL / AC-CALL）见 [product/current.md](../../../docs/product/current.md)。

> ⚠️ **v3.0.0 单体化后**：call 不再是独立服务（无 8300 端口 / 无 `CallApplication` / 无 Dubbo），并入 `shiwujie-bootstrap` 单进程，库 `shiwujie`，`Inner*` 改同进程 Bean 注入。「模块定位」表已更新；正文数据流里的「Dubbo innerXxx」即 v2.1.0 远程调用，v3.0.0 已是同进程方法调用。当前态见 [tech-stack](../../../docs/architecture/tech-stack.md) / [deployment](../deployment.md) as-built。

## 模块定位

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-call/` |
| 端口 | （v3.0.0 单体，无独立端口；对外经 bootstrap:8100，前缀 `/api/call`） |
| 框架 | SB 3.4.5 + Java 21（v3.0.0 统一） |
| MySQL | `shiwujie`（Videohelp/Urgenthelp，v3.0.0 合库） |
| 启动类 | （无；v3.0.0 删 `CallApplication`，由 `shiwujie-bootstrap` 统一入口） |
| 角色 | 本地 Bean **提供者**（`InnerSocketImpl`，`@Service`，被 ai 同进程消费）+ **消费者**（`@Resource` 注入 InnerVolunteer/InnerBlindService） |

## 目录与核心类

```
src/main/java/com/swj/shiwujie/
├── config/WebSocketConfig.java            # （v3.0.0 删 CallApplication/SwaggerConfig；WebConfig+拦截器收敛 common-web）
├── constants/CallConstant.java            # VOLUNTEER_QUEUE_REDIS_KEY
├── socket/
│   ├── CoordinationSocketHandler.java     # @ServerEndpoint("/api/ws/call") 核心（v3.0.0 路径内化）
│   └── inner/InnerSocketImpl.java         # @Service（v2.1.0 为 @DubboService）
├── controller/{Videohelp, Urgenthelp}Controller.java
├── service/{Videohelp, Urgenthelp}Service(+impl).java
└── mapper/{Videohelp, Urgenthelp}Mapper.java
```

> `WebSocketConfig` 仅注册 `ServerEndpointExporter`，用 **jakarta.websocket**（v3.0.0 阶段1.2 由 `javax.websocket` 迁移）。这是阶段 8 从 Netty 迁移到 Spring WebSocket、v3.0.0 jakarta 化的成果。

## WebSocket 消息协议

`CoordinationSocketHandler`（`@ServerEndpoint("/api/ws/call")`，v3.0.0 阶段2.4 路径内化）维护 `sessionMap<phone, Session>` 与 `sessionPhoneMap<Session, phone>`。消息类型：

| type | 方向 | 含义 |
|---|---|---|
| -1 | 双向 | ping/pong 心跳保活 |
| 0 | 客户端→服务端 | bind：以手机号为 key 绑定连接 |
| 1 | 服务端→志愿者 | match：匹配成功，通知志愿者初始化 |
| 2 | 双向 | init：志愿者就绪，转告视障端 |
| 3 | 服务端→家属 | urgent：紧急求助通知（群发家庭） |
| 4 | 服务端→家属 | urgent 取消通知 |
| 5001 | 服务端→视障 | 拍照识别（AI 触发） |
| 5002 | 服务端→视障 | 视频求助跳转（AI 触发） |
| 5003 | 服务端→视障 | 紧急求助跳转（AI 触发） |
| 5004 | 服务端→视障 | 跳转他应用（AI 触发） |
| 5005 | 服务端→视障 | 跳转用户编辑（**孤儿方法，无生产触发**） |
| 5006 | 服务端→视障 | 高德导航（AI 触发） |

> 5xxx 推送由 ai 模块通过 Dubbo 调 `InnerSocket` 触发。**5005** `noticeJumpToUserUpdate` 接口与实现存在，但 ai 侧 `FrontendTools` 无对应 `@Tool`，全局无生产端调用——疑似遗留或预留。

## 接口与本地契约（v2.1.0 Dubbo → v3.0.0 同进程 Bean）

**提供者** `InnerSocketImpl`（`@Service`，v2.1.0 为 `@DubboService`）6 个方法：`noticeTakePhoto`(5001) / `noticeVideoHelp`(5002) / `noticeUrgentHelp`(5003) / `noticeJumpSoftware`(5004) / `noticeJumpToUserUpdate`(5005) / `noticeNavigation`(5006)。

**消费者**（→ user，`@Resource` 注入）：`InnerVolunteerService`（getById / getListByFamilyId）、`InnerBlindService`（getById / getByPhone）。

**REST 接口**（均 `/api/call/*`）：

| 路径 | 作用 |
|---|---|
| `GET /call/videohelp/volunteer/add` | 志愿者加入匹配队列 |
| `DELETE /call/videohelp/volunteer/delete` | 退出匹配 |
| `GET /call/videohelp/blind/join` | 视障加入匹配（触发匹配 + WS type1） |
| `POST /call/videohelp/join` | 上传视频回放路径 |
| `DELETE /call/videohelp/delete/leave` | 挂断（写 end_time/duration） |
| `GET /call/urgenthelp/blind/add` | 发起紧急求助（WS type3 群发家属） |
| `DELETE /call/urgenthelp/blind/delete` | 取消（WS type4） |
| `POST /call/urgenthelp/join` | 上传回放 |
| `GET /call/urgenthelp/volunteer/join` | 家属加入 |

## 配置要点

- v3.0.0 单体经 bootstrap:8100 对外，前缀 `/api/call`（无独立 8300 端口）；MySQL 库 `shiwujie`（合库）；Redis db=2。
- **MyBatis-Plus 驼峰映射统一 `true`**（v3.0.0 阶段2.6，原 call 单独 `false` / snake_case 实体已改 camelCase）。
- CORS 全开（`http://*:*` 等 + allowCredentials=true）。
- 志愿者匹配队列 Redis TTL=30 秒。

## 关键数据流

### 视频求助匹配

```
视障 GET /call/videohelp/blind/join
  → VideohelpServiceImpl.joinVideohelp
      ├─ Redis 取 VOLUNTEER_QUEUE_REDIS 队列 → poll() 出队最早志愿者(FIFO)
      ├─ DB update Videohelp(status=HELPING)
      ├─ Dubbo innerVolunteerService.getById → 取手机号
      └─ coordinationSocketHandler.matchSuccess ── WS type1 ──→ 志愿者端
志愿者就绪 → 回发 WS type2 → handler.onMessage case2 ── WS type2 ──→ 视障端
挂断 → DELETE /call/videohelp/delete/leave → 写 end_time/duration(MINUTE)/status=END_HELP
```

### 紧急求助（家庭内）

```
视障 GET /call/urgenthelp/blind/add
  → UrgenthelpServiceImpl.createUrgenthelp
      ├─ 查重(WAITING/HELPING)
      ├─ Dubbo innerBlindService.getById → familyId + phone
      ├─ 校验已加入家庭
      ├─ DB insert(status=WAITING)
      ├─ Dubbo innerVolunteerService.getListByFamilyId(familyId)
      └─ urgenthelpToFamily ── 遍历在线家属 WS type3 ──→ 家属端
家属点击 GET /call/urgenthelp/volunteer/join?blindPhone= → DB update(status=HELPING)
```

### 5xxx 推送（AI→前端，跨模块）

```
ai 模块 LLM 决策调用 @Tool（FrontendTools.noticeXxx）
  → Dubbo InnerSocketImpl.noticeXxx ──→ call
  → CoordinationSocketHandler.sessionMap.get(blindPhone)
  → sendMessage ── WS {code:0, message, socketData{requestType:5xxx}} ──→ 视障前端
```

> call 模块缺陷（netty-all 冗余残留、🔴 sessionMap 并发隐患、5xxx 失败分支不抛错、/ws/call 未在鉴权白名单、removeVolunteerFromVideohelp 潜在 NPE、Redis 队列 TTL 错配、逻辑删除字段命名）见 [`../known-issues.md`](../known-issues.md)。
