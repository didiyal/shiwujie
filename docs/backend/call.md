# call 模块

> WebSocket 信令中枢 + 视频求助/紧急求助业务；Dubbo 提供 `InnerSocket`，是 **AI→前端实时推送的唯一落地点**。

## 模块定位

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-call/` |
| 端口 | 8300，context-path `/api` |
| Dubbo 端口 | 50300 |
| 框架 | SB 2.7.0 + Java 17 |
| MySQL | `shiwujiecall`（Videohelp/Urgenthelp） |
| 启动类 | `CallApplication`（`@EnableDubbo`） |
| 角色 | Dubbo **提供者**（InnerSocket，被 ai 消费）+ **消费者**（InnerVolunteer/InnerBlindService） |

## 目录与核心类

```
src/main/java/com/swj/shiwujie/
├── CallApplication.java
├── config/{WebSocketConfig, WebConfig, SwaggerConfig}.java
├── constants/CallConstant.java            # VOLUNTEER_QUEUE_REDIS_KEY
├── interceptor/LoginCheckInterceptor.java
├── socket/
│   ├── CoordinationSocketHandler.java     # @ServerEndpoint("/ws/call") 核心
│   └── inner/InnerSocketImpl.java         # @DubboService
├── controller/{Videohelp, Urgenthelp}Controller.java
├── service/{Videohelp, Urgenthelp}Service(+impl).java
└── mapper/{Videohelp, Urgenthelp}Mapper.java
```

> `WebSocketConfig` 仅注册 `ServerEndpointExporter`，用 **javax.websocket**（`@ServerEndpoint`）。这是阶段 8 从 Netty 迁移到 Spring WebSocket 的成果。

## WebSocket 消息协议

`CoordinationSocketHandler`（`@ServerEndpoint("/ws/call")`）维护 `sessionMap<phone, Session>` 与 `sessionPhoneMap<Session, phone>`。消息类型：

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

## 接口与 Dubbo 契约

**提供者** `InnerSocketImpl`（`@DubboService`）6 个方法：`noticeTakePhoto`(5001) / `noticeVideoHelp`(5002) / `noticeUrgentHelp`(5003) / `noticeJumpSoftware`(5004) / `noticeJumpToUserUpdate`(5005) / `noticeNavigation`(5006)。

**消费者**（→ user）：`InnerVolunteerService`（getById / getListByFamilyId）、`InnerBlindService`（getById / getByPhone）。

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

- 端口 8300 / context-path `/api`；Dubbo 50300；MySQL `shiwujiecall`；Redis db=2。
- **MyBatis-Plus 驼峰映射关闭**（`map-underscore-to-camel-case: false`），SQL 与实体都用 snake_case（与其他模块不一致）。
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

## 功能需求（FR-CALL）

| ID | 需求 |
|---|---|
| FR-CALL-01 | 志愿者加入/退出匹配队列（Redis 持久 + 30s TTL） |
| FR-CALL-02 | 视障匹配从队列取最早志愿者（FIFO） |
| FR-CALL-03 | 匹配成功 WS 通知志愿者(type1)，就绪后转告视障(type2) |
| FR-CALL-04 | 紧急求助须已加入家庭，群发家庭内所有家属 |
| FR-CALL-05 | 求助状态机：WAITING→HELPING→END_HELP/FALL |
| FR-CALL-06 | 挂断记录 end_time + duration(分钟) |
| FR-CALL-07 | 心跳保活（type -1） |
| FR-CALL-08 | 连接绑定（type 0）以手机号为 key |
| FR-CALL-09 | 提供 Dubbo InnerSocket，供 ai 触发 6 类前端推送 |
| FR-CALL-10 | 同一用户重复匹配/求助拦截 |

## 验收标准（AC-CALL）

| ID | 验收点 |
|---|---|
| AC-CALL-01 | 志愿者重复加入返回"您已经在匹配中了" |
| AC-CALL-02 | 无空闲志愿者返回"没有空闲的志愿者" |
| AC-CALL-03 | 匹配成功志愿者端收到 type1（含 blindPhone/channelId） |
| AC-CALL-04 | 未加入家庭发起紧急求助返回"您没有加入家庭,无法紧急求助" |
| AC-CALL-05 | 心跳超时/断连后 sessionMap 清理对应手机号 |
| AC-CALL-06 | 挂断 duration=round((end_time-response_time)/MINUTE) |
| AC-CALL-07 | ai 启动后 Dubbo 发现 InnerSocket 提供方 |
| AC-CALL-08 | dev/prod profile 切换 nacos 注册 IP |
| AC-CALL-09 | `/call/*` 接口需有效 Bearer token |

## 已知问题

1. **疑点 #4 定论：netty-all 为冗余残留依赖**。`pom.xml:84-89` 仍声明 `netty-all:4.1.50.Final`，但 call 源码**零 Netty 引用**（阶段 8 已迁 Spring WebSocket）。可安全删除，当前仅膨胀 jar（~4MB），无运行期副作用。
2. **🔴 sessionMap 并发隐患（高危）**：`sessionMap`/`sessionPhoneMap` 是普通 `HashMap`，javax.websocket 每连接独立线程触发 onOpen/onMessage/onClose/onError → 并发 put 可致链表环/数据丢失；复合操作 TOCTOU。应改 `ConcurrentHashMap`。
3. **🟠 5xxx 失败分支不抛错**：用户不在线时 noticeXxx 仅打 log，返回值被丢弃 → ai 侧仍收到"成功"，前端实际未收到。应抛 `BusinessException` 透传给 ai 重新生成回复。
4. **🟠 /ws/call 未在鉴权白名单**：见 [`../architecture/auth.md`](../architecture/auth.md) 风险 #8——WS 绕过 JWT，任何人可冒名 bind。
5. **`removeVolunteerFromVideohelp` 潜在 NPE**：Redis 无队列 key 时 `queue` 为 null，下一行 `queue.contains` 直接 NPE。
6. **Redis 队列 30s TTL 与匹配窗口错配**：志愿者挂机 30s 后队列过期，需确认产品是否接受。
7. **逻辑删除字段命名**：实体属性是否为 `isDelete` 需与 model 实体核对（call 关闭驼峰映射，可能影响逻辑删除生效）。
