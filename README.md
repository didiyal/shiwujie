# 视无界 v1.0（项目一期·单体架构版本）

> **一句话定位**：面向视障人士的辅助平台 —— 盲人用户可通过 App 一键连线志愿者视频求助、向家属发起紧急求助、使用「智能 AI」语音视觉助手；志愿者与家属可在 Web 控制中心和 App 端协同服务。

> **关于本文档**：本 README 是在项目进入二期（微服务架构）阶段后，为 v1.0 历史快照补写的文档，用于说明一期的技术栈、架构与功能边界，并点明其与二期的差异。v1.0 是早期快速开发版本，技术栈与二期完全不同，请勿与二期代码混用。

---

## 一、项目背景

「视无界」是一个面向视障人群的综合性辅助平台，核心场景包括：

- **盲人 ↔ 志愿者视频连线**：盲人发起呼叫，系统从在线志愿者队列（Redis 实现）中匹配空闲志愿者，建立音视频通话。
- **盲人 ↔ 家属紧急求助**：盲人通过 WebSocket 向所在「家庭」的在线家属推送求助通知，家属接听后建立通话。
- **智能 AI 助手**：盲人端通过摄像头/语音与 AI（通义千问 Qwen2.5-VL 视觉大模型）对话，获取场景识别与语音播报。
- **家庭管理**：盲人可创建/加入家庭，家属之间组成互助小组。
- **志愿者体系 / Web 控制中心**：志愿者可通过 Web 端登录、查看社区与活动。

v1.0（一期）是上述功能的**第一个可用版本**，以单体后端 + uniapp 客户端的形式快速落地。

---

## 二、v1.0 与二期（master）的对比

> 二期是当前主干，已重构为 Spring Cloud 微服务架构；v1.0 是其前身。

| 维度 | v1.0（本项目，标签 `v1.0`） | 二期（master / tag `二期开发后初步稳定版`） |
| --- | --- | --- |
| **后端架构** | 单体（single module） | 微服务（多模块 Spring Cloud） |
| **Spring Boot** | 2.7.0 | 2.7.x（业务）+ 3.4.5（AI 模块） |
| **Java 版本** | Java 17 | Java 17 |
| **服务治理** | 无注册中心、无网关 | Nacos（注册/配置）+ Spring Cloud Gateway（负载均衡） |
| **RPC/通信** | 无 Dubbo；进程内调用 | Dubbo RPC |
| **实时通信** | **Netty 原生 WebSocket**（独立端口 8082） | Spring WebSocket（call 模块）+ MQTT |
| **AI 实现** | **外置独立 AI 服务器**（`http://47.92.170.90:8080`，Qwen2.5-VL），源码不在本仓库 | 内建 `shiwujie-ai` 模块（Spring AI + DashScope） |
| **客户端** | uniapp（Vue3，nvue 原生渲染页面）+ Web 门户（Vue3 + Ant Design Vue） | 同类客户端，已演进 |
| **数据库** | 单库 `shiwujie`（MySQL），4 张核心表 | 多服务各自库 / 共享库 |
| **构建产物** | 单个 jar + 一个 apk + 一个 Web 站点 | 多个微服务镜像 |
| **定位** | 早期快速开发版本，功能集中、便于部署 | 工程化、可扩展的生产架构 |

一句话：**v1.0 是“能跑起来”的早期版本，二期是“能横向扩展”的工程化版本。**

---

## 三、技术栈清单

### 后端（`shiwujie-backend/shiwujie-单一架构版本/`）

| 类别 | 技术 / 依赖 | 版本 |
| --- | --- | --- |
| 语言 | Java | 17 |
| 框架 | Spring Boot | 2.7.0 |
| Web | spring-boot-starter-web | 随父 |
| ORM | MyBatis Spring Boot Starter | 2.2.2 |
| ORM 增强 | MyBatis-Plus | 3.5.1（含逻辑删除） |
| 数据库 | MySQL Connector/J | 8.0.29（runtime） |
| 连接池 | HikariCP | 随 Spring Boot |
| 缓存/会话 | spring-boot-starter-data-redis | 2.6.4 |
| 分布式会话 | spring-session-data-redis | 2.6.3 |
| WebSocket（信令） | **Netty** | 4.1.42.Final（+ macOS DNS native） |
| HTTP 客户端 | OkHttp / Java-WebSocket | 4.9.3 / 1.5.3 |
| 鉴权 | JWT (jjwt) | 0.11.2 |
| 工具库 | Hutool-all | 5.8.25（pom 中另有一处 5.2.5 旧版本声明） |
| JSON | Gson 2.9.0 + Alibaba FastJSON 1.2.76 | — |
| API 文档 | Knife4j (Swagger) | 2.0.7 |
| AI SDK | Alibaba DashScope SDK | 2.15.1（⚠️ 仅声明依赖，源码中**未被调用**，见下文“已知局限”） |
| 其他 | Lombok / commons-lang3 3.12.0 / devtools | — |

### uniapp 客户端（`shiwujie-frontend/shiwujie-client-develop 2 (1)/`）

| 类别 | 技术 | 说明 |
| --- | --- | --- |
| 框架 | uni-app（HBuilderX 工程，`vueVersion: "3"`） | 编译目标：Android App（5+ App） |
| 页面 | `.vue` + `.nvue` 混用 | nvue 用于原生渲染的摄像头/视频/AI 页 |
| 音视频 | **anyRTC 音视频 SDK 插件** `AR-RtcModule`（云端插件 pid=3661） | 用于盲人↔志愿者、盲人↔家属实时通话 |
| 悬浮窗 | `Ba-FloatWinWeb` 插件（pid=14471） | 通话/求助/挂断/翻转摄像头/跳 AI 的全局悬浮控制 |
| 地图 | 高德地图 SDK（amap）+ `js_sdk/fx-openMap` | 目的地搜索与调起原生地图导航 |
| 登录 | OAuth(univerify) / uniCloud-aliyun `getPhoneNumber` 云函数 | 手机号一键登录 |
| 定位 | Geolocation（系统） | — |
| App 模块 | Camera / LivePusher / Geolocation / OAuth | manifest.json 中声明 |
| AI | 调用**外置 AI 服务** `http://47.92.170.90:8080` | Qwen2.5-VL-72b，见 `utils/requestAI.js` |
| 后端联调 | 各 `api/*.js` 硬编码 `http://43.139.38.62:8081` | 无统一 baseUrl 变量 |

### Web 前端（`shiwujie-frontend/shiwujie-web/`）

| 类别 | 技术 / 依赖 | 版本 |
| --- | --- | --- |
| 框架 | Vue | ^3.2.13 |
| 语言 | TypeScript | ~4.5.5 |
| 构建 | @vue/cli-service | ~5.0.0 |
| UI | Ant Design Vue | ^4.2.6 |
| 路由 | vue-router | ^4.0.3 |
| 状态 | Pinia + pinia-plugin-persistedstate | ^3.0.1 / ^4.2.0 |
| HTTP | axios | ^1.8.4 |
| 工具 | lodash-es ^4.17.21 | — |
| 样式 | Less / less-loader | ^4.2.2 / ^12.2.0 |

定位：**对外门户站 + 志愿者“控制中心”**。首页展示项目介绍并提供 App 下载入口（`http://<host>:8081/app/download`），登录后进入志愿者社区/活动管理后台。

---

## 四、目录结构

```
v1.0-snapshot/
├── shiwujie-backend/
│   └── shiwujie-单一架构版本/                 # 单体后端（Maven）
│       ├── pom.xml
│       ├── mvnw / mvnw.cmd                     # Maven Wrapper
│       ├── generate-certificate.sh             # Netty WSS 自签证书生成脚本
│       ├── sql/sql_table.sql                   # 建库建表脚本（4 张表）
│       └── src/main/
│           ├── java/com/swj/shiwujie/
│           │   ├── ShiwujieApplication.java    # 启动类
│           │   ├── controller/                 # UserController / VideoController /
│           │   │                               #   CallHelpController / FamilyController /
│           │   │                               #   ApkDownloadController / TestController
│           │   ├── service/ + service/impl/    # 业务层（User/Video/CallHelp/Family）
│           │   ├── mapper/                     # MyBatis-Plus Mapper（+ resources/mapper/*.xml）
│           │   ├── model/                      # domain / VO / request / enums
│           │   ├── netty/                      # Netty WebSocket 服务端（信令/求助推送）
│           │   ├── config/                     # WebConfig / NettyConfig / Redis / Swagger
│           │   ├── interceptor/                # LoginCheckInterceptor（JWT + Redis）
│           │   ├── constants/                  # Netty / User / Video / CallHelp 常量
│           │   ├── exception/                  # 全局异常 + BusinessException
│           │   ├── common/                     # BaseResponse / ErrorCode
│           │   └── utils/                      # JWT / Redis / Snowflake / Result / VideoQueue
│           └── resources/
│               ├── application.yml             # 主配置（默认 active: dev）
│               ├── application-dev.yml         # 开发环境（连云上 MySQL/Redis）
│               ├── application-prod.yml        # 生产环境（本地 MySQL/Redis）
│               ├── mapper/*.xml
│               ├── apks/shiwujie.apk           # 内嵌 App 安装包（供 /app/download）
│               └── static/test.html
│
└── shiwujie-frontend/
    ├── shiwujie-client-develop 2 (1)/
    │   └── shiwujie-client-develop 2/          # uniapp 客户端（HBuilderX 工程）
    │       ├── manifest.json                   # App 配置（anyRTC/高德/悬浮窗/权限）
    │       ├── pages.json                      # 页面路由 + 自定义 tabBar
    │       ├── App.vue                         # 全局：WebSocket 连接 + 悬浮窗事件分发
    │       ├── pages/                          # 业务页面（见下文“前端说明”）
    │       ├── api/                            # 后端接口封装（login/video/family/callHelp/user）
    │       ├── utils/                          # requestAI.js / storage / socket / until
    │       ├── components/                     # customTabbar 等组件
    │       ├── js_sdk/fx-openMap/              # 地图调起 SDK
    │       ├── nativeplugins/                  # CW-DetectionModule 原生插件
    │       └── uniCloud-aliyun/                # getPhoneNumber 云函数
    │
    └── shiwujie-web/                           # Web 门户（Vue3 + TS + Ant Design Vue）
        ├── package.json / vue.config.js / tsconfig.json
        └── src/
            ├── main.ts / App.vue
            ├── router/index.ts                 # 路由（首页/登录/志愿者后台）
            ├── layouts/BasicLayout.vue
            ├── components/GlobalHeader.vue
            ├── views/Home/HomePage.vue         # 门户首页 + 下载入口
            ├── views/login/RegisterAndLogin.vue
            ├── views/volunteer/                # 社区 / 活动 / 活动详情
            └── stores/                         # Pinia: community / activity
```

> 提示：仓库根目录下的 `.idea/`、`testgit.txt` 等为历史噪声文件，与业务无关。

---

## 五、后端架构与功能详解

### 5.1 启动与端口

- **启动类**：`com.swj.shiwujie.ShiwujieApplication`（标准 `@SpringBootApplication`）。
- **HTTP 服务端口**：`8081`（`application-dev/prod.yml` 中 `server.port`）。
- **WebSocket（Netty）端口**：`8082`，路径 `/shiwujie/websocket/{userId}`（见 `NettyConstants`）。Netty 由 `NettyServer` 在 `@PostConstruct` 中以独立线程启动，**不经过 Spring MVC**。
- **Context-path**：`/`。
- **Profile**：默认 `dev`（连云上 `43.139.38.62` 的 MySQL/Redis）；`prod` 改连本地。

### 5.2 数据存储

- **MySQL**：库 `shiwujie`，4 张核心表（`sql/sql_table.sql`）：
  - `user`：用户表（含盲人/志愿者角色、残疾人证、通话状态 `callStatus`、当前频道 `callChannel` 等）。
  - `family`：家庭表（含家主 `userId`、成员 `addId` JSON 数组、待加入 `postId`、唯一 `familyAccount`）。
  - `video`：志愿者视频通话频道表（`channel`/`blindUid`/`volunteerUid`/状态/起止时间）。
  - `callHelp`：家属紧急求助通话频道表（多协助人字段，预留多人）。
  - 均使用 MyBatis-Plus 逻辑删除（`isDelete`，0 存在 / 1 删除）。
- **Redis**（`43.139.38.62:6379`，db=1）：
  - 登录态：`LOGIN_USER_KEY + userId` / `LOGIN_USER_TOKEN + userId`，TTL 365 天，拦截器命中时续期 12 小时。
  - 视频匹配队列：`VIDEO_QUEUE`（`LinkedList` 序列化），存放空闲志愿者的 `channel`，TTL 120h。

### 5.3 鉴权机制

- `LoginCheckInterceptor`（`WebConfig` 注册，拦截 `/**`）：
  - 放行 `OPTIONS`、含 `LoginAndRegister`/`download`/`test` 的 URL。
  - 取 `Authorization: Bearer <jwt>` → `JWTUtils.extractUserId` → 校验 Redis 中是否存在 → 续期 → 将 `loginUserId` 注入请求属性。
- 密码：`md5(SALT + 明文)`；账号用**雪花算法**生成 8 位（`SnowflakeIdGenerator`）。

### 5.4 主要业务模块

#### A. 用户（`UserController` → `/shiwujie/user`）
- 手机号+密码登录注册（`/LoginAndRegister`）：不存在则自动注册。
- 手机号一键登录注册（`/LoginAndRegisterQuickly`）：免密，直接按手机号建号。
- 当前用户信息查看/修改/注销（`/mine/*`）、退出登录、密码校验。
- **残疾人证绑定**（`/certificate/add`）：校验 20 位纯数字后写入，对外脱敏为 `"true"/"false"`。
- JWT 校验（`/test/jwt`）。

#### B. 视频通话频道（`VideoController` → `/shiwujie/channel`）
核心是**盲人 ↔ 志愿者**的匹配与通话生命周期管理（`VideoServiceImpl`）：
- 志愿者上线（`/getVolunteerChannelAndUid`）：生成 `channel = "CHANNEL_KEY"+uid`，入 Redis 队列 `VIDEO_QUEUE`，写 `video` 表（状态 0 等待），用户 `callStatus=等待`。
- 盲人发起呼叫（`/getBlindChannelAndUid`）：从 `VIDEO_QUEUE` `poll` 一个志愿者 channel，异步（线程池）更新双方状态为通话中、记录 `beginTime`。
- 任一方挂断（`/leaveVolunteerChannel`、`/leaveBlindChannel`）：区分“接听前离开（取消，回滚队列）”与“接听后离开（结束，写 endTime）”，并复位双方 `callStatus`。
- `uid` 生成：用户 id 左对齐补 0 至 8 位。

#### C. 紧急求助（`CallHelpController` → `/shiwujie/callHelp`）
盲人向**家属**发起的求助通话，与志愿者通道相互独立：
- `/getBlindChannelAndUid`、`/joinBlindChannelAndUid`、`/leaveCallHelpByBlind`、`/leaveBlindChannel`。
- 配合 Netty WebSocket 推送：盲人 App 发 `{title:"CALL_HELP", blindUid}`，服务端 `MyWebsocketHandler.handlerCALLHELP` 找到该家庭其他在线成员 channel，向其推送求助通知（见 5.5）。

#### D. 家庭（`FamilyController`，注意类上无 `@RequestMapping`，方法路径直接挂在根）
- 创建家庭（`/add`，家主即创建人）、按账号查询（`/get/account`）、按 id 查询（`/get/id`）。
- 改家庭名（仅家主，`/update/name`）、移除成员（家主，移除自己=解散，`/remove/user`）。
- 加入家庭（`/join`，凭 `familyAccount` 直接加入）、退出家庭（`/leave`）、删除家庭（`/delete`）。
- 成员关系以 JSON 数组字符串存在 `family.addId`（Gson 序列化）。

#### E. App 下载（`ApkDownloadController`）
- `GET /app/download`：从 classpath `apks/shiwujie.apk` 返回安装包；Web 首页“下载客户端”按钮即指向此。

### 5.5 实时通信（Netty WebSocket）

- `NettyServer`（`@PostConstruct` 独立线程）→ `MyWebsocketChannelHandler`（pipeline：HttpServerCodec → HttpObjectAggregator → WebSocketServerProtocolHandler(`/shiwujie/websocket`) → ChunkedWriteHandler → `MyWebsocketHandler`）。
- `MyWebsocketHandler` 维护内存 `channelPool: Map<userId, Channel>`，握手完成后从 URI 末段解析 `userId` 入池；断开/异常自动清理。
- 业务消息目前仅处理 `title == "CALL_HELP"`：校验用户已加入家庭 → 取家庭成员 → 向在线家属 `writeAndFlush` 求助 JSON。
- 前端 `App.vue.connectWebSocket(userId)` 建立 `ws://43.139.38.62:8082/shiwujie/websocket/{userId}` 长连，`onMessage` 收到 `CALL_HELP` 即弹出 `Ba-FloatWinWeb` 悬浮窗通知。

> 说明：一期用 Netty 自建 WS；二期已改为 Spring WebSocket（并引入 MQTT）。`generate-certificate.sh` 提示曾计划上 WSS，但当前常量仍为 `ws://`。

### 5.6 AI 能力（重要：外置，不在本后端）

- App 的“智能 AI”页（`pages/AI-assistant/AI-help.nvue`）调用 `utils/requestAI.js`，其 `BASE_URL = 'http://47.92.170.90:8080'`，提供三类接口：
  - `POST /api/chat`（默认模型 `qwen2.5-vl-72b-instruct`，视觉对话）
  - `POST /api/tts`（文本转语音）
  - `POST /api/speech-to-text`（音频上传转文字，multipart）
- **该 AI 服务器的源码不在 v1.0 仓库内**，是一个独立部署的实验性服务。
- 后端 `pom.xml` 虽声明了 `dashscope-sdk-java 2.15.1`，但全仓库**无任何 Java 代码 import/调用**它 —— 即后端本身不做 AI，AI 完全在 App 端直连外部服务器。

---

## 六、前端说明

### 6.1 uniapp 客户端

- **技术形态**：HBuilderX 工程，`vueVersion: "3"`，编译为 Android App（appid `__UNI__17405B4`，版本 `2025.0426.2126`）。页面混用 `.vue`（普通页面）与 `.nvue`（原生渲染，用于摄像头/AI/视频页）。
- **关键原生能力**：
  - `AR-RtcModule`（anyRTC）：视频通话加入/离开频道、切换摄像头。
  - `Ba-FloatWinWeb`：通话中的全局悬浮控制（翻转摄像头、挂断、回主页、唤起 AI、求助通知）。
  - 高德地图：`restapi.amap.com/v3/place/text` 文本搜索 + 调起系统地图导航。
  - `uniCloud-aliyun` 的 `getPhoneNumber` 云函数：手机号一键登录。
- **主要页面**（`pages.json`，自定义 tabBar）：
  | 路径 | 名称 | 角色 |
  | --- | --- | --- |
  | `pages/leader/leaderPage` | 启动引导页 | 全部 |
  | `pages/register-login/login` | 登录 | 全部 |
  | `pages/register-login/chooseRole` | 身份选择（盲人/志愿者） | 全部 |
  | `pages/register-login/loginAndRegisterQickliy` | 手机号一键登录 | 全部 |
  | `pages/blindPages/blindHome` | 盲人主页（连线志愿者 / 紧急求助） | 盲人 |
  | `pages/blindPages/navigation` | 搜索目的地导航 | 盲人 |
  | `pages/blindPages/community` / `benefit` | 社区 / 福利 | 盲人 |
  | `pages/AI-assistant/AI-help` | 智能 AI（视觉+语音） | 盲人 |
  | `pages/videoTest/videoTest` | 志愿者视频通话 | 志愿者 |
  | `pages/videoTest/BlindHelp` | 求助通话-求助人 | 盲人 |
  | `pages/videoTest/HelpUser` | 求助通话-协助家属 | 家属 |
  | `pages/volunteerPages/volunteerHome` / `community` | 志愿者主页/社区 | 志愿者 |
  | `pages/family/family` / `volunteerFamily` | 家庭管理 | 全部 |
  | `pages/userLayout/userLayout` / `infor` | 个人中心/修改信息 | 全部 |
- **请求封装**：`api/*.js`（loginAndRegister/video/family/callHelp/user）各自封装 `uni.request`，**baseUrl 硬编码 `http://43.139.38.62:8081`**，Token 取自 `utils/storage` 并以 `Authorization: Bearer` 头携带。AI 请求单独走 `utils/requestAI.js` 的另一域名。
- **无障碍**：盲人页面大量使用 `:accessible`、`aria-label`、`role` 等属性，配合系统读屏。

### 6.2 Web 前端（`shiwujie-web`）

- **角色**：对外门户站 + 志愿者控制中心。Vue3 + TS + Ant Design Vue 4 + Pinia（持久化）+ vue-router 4。
- **路由**（`src/router/index.ts`）：
  - `/home` → `HomePage.vue`：项目介绍页，含“下载客户端”（指向后端 `/app/download`）和“控制中心”入口。
  - `/login` → `RegisterAndLogin.vue`：登录/注册。
  - `/volunteer/{community,activity,activityContent}` → 志愿者后台（`BasicLayout` 嵌套，`requiresAuth` 守卫基于 `localStorage.isLogin`）。
- **构建**：`yarn install` → `yarn serve`（开发）/ `yarn build`（生产）。

---

## 七、如何本地运行

> 以下命令基于 `pom.xml`、`package.json` 与 uniapp 工程惯例推断。

### 7.1 准备基础设施

1. **MySQL 8**：执行 `shiwujie-backend/shiwujie-单一架构版本/sql/sql_table.sql` 建库建表；建议另建用户或直接使用现有账号。
2. **Redis 6+**：默认连 `43.139.38.62:6379`（db=1，密码 `123456`），本地运行请改为本机并在 `application-dev.yml` 调整 `host/password`。
3. 修改 `application-dev.yml`（或新增 `application-local.yml` 并 `--spring.profiles.active=local`）中的 `datasource` 与 `redis` 为本机地址。

### 7.2 启动后端

```bash
cd shiwujie-backend/shiwujie-单一架构版本
./mvnw spring-boot:run            # 或 mvn spring-boot:run
# 默认 HTTP:8081 ，Netty WS:8082
```

- 接口文档：启动后访问 `http://localhost:8081/doc.html`（Knife4j）。
- 下载 App：`http://localhost:8081/app/download`。

### 7.3 运行 Web 前端

```bash
cd shiwujie-frontend/shiwujie-web
yarn install        # 或 npm install
yarn serve          # 开发服务器（默认 http://localhost:8080）
yarn build          # 生产构建到 dist/
```

### 7.4 运行 uniapp 客户端

1. 用 **HBuilderX** 打开 `shiwujie-frontend/shiwujie-client-develop 2 (1)/shiwujie-client-develop 2/`。
2. 修改 `api/*.js` 与 `App.vue` 中硬编码的 `43.139.38.62:8081`（HTTP）和 `43.139.38.62:8082`（WS），以及 `utils/requestAI.js` 中的 AI 服务地址，指向你的环境。
3. 在 HBuilderX 中“运行到手机或模拟器 → 运行到 Android App 基座”（需已购置/绑定 `AR-RtcModule`、`Ba-FloatWinWeb` 云端插件，appid `__UNI__17405B4`）。
4. 或“发行 → 原生 App-云打包”生成 apk。

> ⚠️ anyRTC 插件与高德 SDK 需要有效的 appkey；当前 `manifest.json` 中高德 `appkey_ios/android` 为空，需自行填写。

---

## 八、已知局限 / 与二期相比缺失的能力

1. **无微服务、无服务治理**：单体部署，不能横向扩展；无 Nacos / Gateway / Dubbo。
2. **实时通信为 Netty 自建 WS**：`channelPool` 是单机内存 Map，**无法多实例**（多机会话不互通）；二期改为 Spring WebSocket + MQTT。
3. **AI 能力割裂**：v1.0 的 AI 走外置服务器（`47.92.170.90:8080`，Qwen2.5-VL），源码不在仓库；后端虽引入 DashScope SDK 但**未使用**。二期把 AI 收敛进 `shiwujie-ai` 模块（Spring AI）。
4. **无 MQTT**：二期已引入，v1.0 无。
5. **配置硬编码**：前端 `baseUrl` 散落各 `api/*.js`，未集中管理；后端依赖云上 `43.139.38.62`。
6. **安全隐患（仅限历史快照，勿直接上生产）**：
   - `application-dev.yml` 中明文数据库/Redis 口令。
   - `WebConfig` 跨域放开 `http://*:*`、`https://*:*`、`file://*` 并 `allowCredentials(true)`。
   - 一键登录接口无短信验证码，仅凭手机号即可建号/登录。
7. **视频匹配队列**：`VIDEO_QUEUE` 用 Redis 序列化整个 `LinkedList`，并发 poll/offer 非原子，高并发下可能丢匹配（二期已优化）。
8. **未完成项**：`VideoServiceImpl` 中 `//todo 视频耗时计算` 未实现；`callHelp` 多协助人字段为预留。
9. **AI SDK 冗余依赖**：`dashscope-sdk-java` 与重复声明的 `hutool-all`（5.2.5 与 5.8.25 并存）应清理。

---

## 九、版本与标签信息

- **Git 标签**：`v1.0`（annotated，指向提交 `06d1752`，提交说明「项目一期」）
- **打标签者**：迪dyal
- **打标签说明**：项目一期：早期快速开发版本（技术栈与二期不同）
- **根提交日期**：2025-06-30

本仓库另有标签 `二期开发后初步稳定版`，指向二期主干，与 v1.0 互为演进关系。
