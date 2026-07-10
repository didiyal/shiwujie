# 技术栈

> 项目存在**版本割裂**：业务模块与 AI 模块分别基于 Spring Boot 2.7 与 3.4.5，这一事实约束了整个工程结构（尤其 common / common-web 两层设计）。本篇详述各层技术选型与割裂根因。

## 后端技术栈总览

| 层 | 技术 | 版本 | 说明 |
|---|---|---|---|
| 基础框架（业务） | Spring Boot | **2.7.0** / Java **17** | gateway / user / call / community / common-web |
| 基础框架（AI） | Spring Boot | **3.4.5** / Java **21** | shiwujie-ai |
| 微服务 | Spring Cloud + Spring Cloud Alibaba | 2021.0.x | 服务发现、网关 |
| 注册/配置 | Nacos | 2.x | `47.112.114.139:8848`，账号 nacos/nacos |
| RPC | Apache Dubbo | **3.3.0** | dubbo-spring-boot-starter + dubbo-nacos |
| 负载均衡 | Spring Cloud LoadBalancer | — | 网关 `lb://` 轮询 |
| 持久层 | MyBatis-Plus | 3.5.x | 全局逻辑删除 `isDelete`(0/1) |
| 数据库 | MySQL | 8.x | `47.112.114.139:3306`，**4 个分库** |
| 缓存 | Redis | — | `47.112.114.139:6379`，**db=2 共享**，密码默认 123456 |
| 鉴权 | JWT (Hutool HS256) + Redis | — | 单点鉴权，详见 [`auth.md`](auth.md) |
| API 文档 | Knife4j | 4.4.0 | 网关聚合（SB2 部分），**未聚合 ai** |

### SB 2.7 与 SB 3.4.5 的割裂

根因：**Spring AI 1.0.0 + spring-ai-alibaba 1.0.0.2 强制要求 Spring Boot 3.x 与 Java 21**，而业务模块（gateway/user/call/community）仍稳定在 SB 2.7 + Java 17（一期延续，且 Lombok 1.18.24 与 Java 21 存在 `JCTree$JCImport.qualid` 不兼容）。

这一约束直接逼出了 **common / common-web 两层** 的工程结构：

| 模块 | 依赖关系 | 能被谁依赖 |
|---|---|---|
| `shiwujie-model` | 纯契约层（无 Spring）：domain 实体 / enums / request-VO / `Inner*Service` Dubbo 接口 / 常量 / PageRequest | **全部模块**（含 SB3 的 ai） |
| `shiwujie-common-web` | 依赖 model；提供 JwtUtils / RedisUtils / BaseResponse / GlobalExceptionHandler / ThrowUtils / RedisTemplateConfig | **仅 SB2 业务模块**（ai 用不了 `javax.*` 命名空间） |

> ai 模块需要 JWT/Redis 工具，但因 SB3 命名空间从 `javax.*` 变为 `jakarta.*`，无法复用 common-web，故在 ai 模块内**自带一份** `JwtUtils` / `RedisTemplateConfig` / `LoginCheckInterceptor`（jakarta 版）。这是版本割裂的必然代价，详见 [`../../shiwujie-backend/docs/modules/ai.md`](../../shiwujie-backend/docs/modules/ai.md) 与 [`../../shiwujie-backend/docs/modules/model-commonweb.md`](../../shiwujie-backend/docs/modules/model-commonweb.md)。

> 附带产物：common-web 与 model 之间存在**重复代码**——`PageRequest`、`CommonConstant`、`UserConstants` 在两层各有一份（model 版 `PageRequest` 默认 pageSize=20 且 Serializable；common-web 版默认 10 且非 Serializable），属历史演进残留。

## v3.0.0 单体化目标（进行中）

> 反思 v2.1.0 微服务对当前体量过度设计，v3.0.0 去微服务、合并 user/call/community/ai 为单体（保留模块化分包）。本篇下方描述的 **SB 双栈割裂与 model/common-web 两层结构为 v2.1.0 现状**；v3.0.0 将消解：

- **统一 SB 3.4.5 / Java 21**：业务模块（gateway/user/call/community/common-web）从 SB 2.7/Java17 升到与 ai 一致的 SB 3.4.5/Java21。Spring AI 1.0 强制 SB3，无回退。
- **两层结构根因消失**：SB 统一后 common-web（jakarta 命名空间）可被 ai 依赖，model/common-web 两层不再被版本割裂逼迫——ai 删自带 common-web 副本，两层重复类去重。
- **javax → jakarta**：业务模块 `javax.servlet` / `javax.annotation.Resource` / `javax.websocket` 全量迁 `jakarta.*`（无 persistence/validation/xml.bind，机械化迁移）。
- **坐标升级**：MyBatis-Plus 3.5.1→3.5.9（`mybatis-plus-spring-boot3-starter`）；knife4j openapi2→openapi3-jakarta（83 处注解重写）；mysql 驱动换 `mysql-connector-j`；nacos-discovery `2021.0.5.0`→`2023.0.1.0`（顺带修 JDK21+nacos-client 测试坑）。
- **去微服务**：删 gateway / Spring Cloud / Dubbo / Nacos（仅服务发现 + Dubbo 注册中心，非配置中心），`Inner*Service` 从 Dubbo RPC 退化为本地 Bean 注入（详见 [`gateway-dubbo.md`](gateway-dubbo.md)）。
- **合库**：4 分库 → 单库 `shiwujie`（详见 [`data-model.md`](data-model.md)）。

> 两阶段交付（先升版本后合并）见 [`development/v3.0.0/task-breakdown.md`](../development/v3.0.0/task-breakdown.md)。契约（HTTP 路径 / WS 信令 / 状态码）零变更。

## 前端技术栈

### Android 原生 App

| 项 | 选型 |
|---|---|
| 语言 / UI | Java + ViewBinding（**非** uniapp） |
| 构建 | compileSdk **35**，minSdk 30 |
| 网络 | Retrofit2 + OkHttp |
| WebSocket | OkHttp WebSocket（独立前台 Service） |
| 音视频 | anyRTC SDK |
| 语音 | 讯飞 TTS / ASR（IatResultParser） |
| 相机/避障 | Camera2（封装为 CameraPreviewManager；注：依赖声明提及 CameraX，实际用 Camera2） |
| 导航 | 高德地图（URI 调起，NavigationManager） |
| 架构 | 按角色分包（`blind/` / `volunteer/` / `common/` / `data/model/`） |

> 详见 [`../../shiwujie-frontend/app/docs/android.md`](../../shiwujie-frontend/app/docs/android.md)。

### Web 管理后台

| 项 | 选型 |
|---|---|
| 框架 | Vue **3.3** + Vue Router 4 + Pinia 2 |
| UI | Ant Design Vue **4** |
| 网络 | Axios 1.4（禁用自动 JSON.parse，手动处理大数字 ID） |
| 构建 | Vite **4**，dev 端口 9090，代理 `/api` → 网关 8100 |
| 图表 | **未引入**（统计页为占位） |

> 详见 [`../../shiwujie-frontend/web/docs/vue-admin.md`](../../shiwujie-frontend/web/docs/vue-admin.md)。

## AI 能力栈

| 能力 | 实现 |
|---|---|
| 文本模型 | DashScope **qwen3-max**（OpenAI 兼容端点 `compatible-mode/v1`） |
| 图像模型 | DashScope **qwen3-vl-flash** |
| 框架 | Spring AI 1.0.0 + spring-ai-alibaba 1.0.0.2 |
| 记忆 | **自研 ChatMemory**（Redis 精简 + MySQL 全量，kryo 序列化），非 Spring AI 默认 |
| 工具路由 | 工作流式 `ToolChoiceApp`（约定 JSON 解析，非原生 function-calling） |
| 检索增强 | RAG **已移除**（半残留 Bean，未启用） |
| 网页搜索 | searchapi.io（engine=baidu）+ jsoup 摘要 |
| 推送通道 | Dubbo → call 模块 `InnerSocket` → WebSocket 推前端 |

> 详见 [`../../shiwujie-backend/docs/modules/ai.md`](../../shiwujie-backend/docs/modules/ai.md)。

## 基础设施地址

> **dev / prod 拓扑不同**：dev 期 **MySQL/Redis 连服务器**（共享数据），但 **Nacos（Spring Cloud 发现 + Dubbo 注册中心）走本机**——整套服务在本机自洽，纯本机 dev 不存在跨网注册问题。prod 期全部连服务器。Nacos 地址由 `${nacos.address:47.112.114.139}` 占位符驱动，dev/prod profile 各自覆盖。

| 设施 | dev | prod |
|---|---|---|
| Nacos（Spring Cloud 发现 + Dubbo 注册中心） | 本机 `127.0.0.1:8848` | `47.112.114.139:8848` |
| MySQL | `47.112.114.139:3306` | `47.112.114.139:3306` |
| Redis | `47.112.114.139:6379`（db=2 共享） | `47.112.114.139:6379`（db=2 共享） |

- **Nacos 地址**：`nacos.address` 变量——dev profile=`127.0.0.1`，prod profile=`47.112.114.139`（占位符默认值亦为 `47.112.114.139`）。命令行 `-Dnacos.address=X` 优先级最高，可临时覆盖（如 dev 期复现多机部署、连服务器 Nacos）。
- **服务注册 IP**（服务注册到 Nacos 的自身 IP，与上面"基础设施地址"是两回事）：Spring Cloud 发现走 `spring.cloud.nacos.discovery.ip`（dev=`127.0.0.1` / prod=`47.112.114.139`）；**Dubbo 注册独立，须启动命令 `-DDUBBO_IP_TO_REGISTRY`**——详见 [`gateway-dubbo.md`](gateway-dubbo.md)。
- **token key 前缀** `REDIS_SECRETKEY-{role}-{id}`。

> 凭据已抽取为 `${ENV:default}` 占位符（`MYSQL_PASSWORD` / `REDIS_PASSWORD` / `NACOS_USERNAME` / `NACOS_PASSWORD` / `DASHSCOPE_API_KEY` / `SEARCH_API_KEY`），default 值仍硬编码在 yml（按用户要求保留在配置里）。
