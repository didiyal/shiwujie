# v3.0.0 任务拆解

> 本文件属 `development/v3.0.0/`（单体化改造·进行中）。**交付范围**（单体化目标）+ **任务**（随单体化落地）。版本指针 [../current.md](../current.md)。大方向与废弃项见 [../../ROADMAP.md](../../ROADMAP.md) v3.0.0 段。

## v3.0.0 交付范围（承接 ROADMAP 待实现）

### 单体重写

> 文档先写：v3.0.0 架构目标态已写入 [tech-stack](../../architecture/tech-stack.md) / [data-model](../../architecture/data-model.md) / [gateway-dubbo](../../architecture/gateway-dubbo.md) 的「v3.0.0 单体化（已落地）」段；本节为实现 spec。两阶段推进，验证点见 [testing-strategy](testing-strategy.md)。

> ✅ **工程已落地（2026-07-11，启动级验证通过）**：阶段 1（1.1–1.7）+ 阶段 2（2.1–2.6 + 2.8 模块合并）全部完成；2.7 启动级验证通过，功能级联调待 App/Web。回卷文档已转 as-built（CHANGELOG / architecture / 模块文档 / known-issues）。

**契约保护铁律**（全程不得违反）：对外 HTTP 路径 `/api/{user,call,community,ai}/**`、WebSocket `/api/ws/call` + 12 信令码（`-1/0/1/2/3/4/5001~5006`）、HTTP/业务状态码、返回字段名——**零变更**，前端 App/Web 不改即可对接。

#### 阶段 1 · 业务模块升 SB 3.4.5 / Java 21（仍微服务，逐模块验证可启动）

> 消除 SB 双栈根因（Spring AI 1.0 强制 SB3/Java21）。ai 模块为迁移样板。

- [x] 1.1 父 pom：parent→SB 3.4.5、`java.version` 17→21、`mybatis-plus` 3.5.1→3.5.9、spring-cloud 2021.0.5→**2024.0.0**、spring-cloud-alibaba 2021.0.5.0→2023.0.1.0（lombok 1.18.36/dubbo 3.3.0 保留）`9997b89`
- [x] 1.2 jakarta 迁移：`javax.servlet.*`/`javax.annotation.Resource`/call `javax.websocket`→jakarta（42 文件；确认无 persistence/validation/xml.bind）`c698b66`
- [x] 1.3 MyBatis-Plus 换坐标 `mybatis-plus-boot-starter`→`mybatis-plus-spring-boot3-starter`（model + common-web）；ai 对 model 的 exclusion 三件套**推迟到阶段 2**——ai 不继承父 pom，现仍需排除 model 无版本 core/extension `34b0f6b`
- [x] 1.4 knife4j openapi2→openapi3：换 `knife4j-openapi3-jakarta-spring-boot-starter`（**收敛到 common-web 单处声明**，业务模块传递依赖）；删 3 份 SwaggerConfig 改 common-web 单份 springdoc `OpenApiConfig`；83 注解 `@Api`→`@Tag`、`@ApiOperation`→`@Operation`；删 pagehelper `4db2c53`
- [x] 1.5 驱动/注册中心坐标：mysql→`com.mysql:mysql-connector-j`（解除 reactor version 阻塞）；nacos 随 1.1 升 2023.0.1.0（修 JDK21+nacos-client 坑）；删 call pom netty-all 死依赖（grep 确认 0 处 io.netty 代码引用）`af162c9`
- [x] 1.6 配置：3 业务模块 yml `spring.redis`→`spring.data.redis`；call WebSocket 的 jakarta import 已在 1.2 完成，`@ServerEndpoint("/ws/call")` + `ServerEndpointExporter` + static sessionMap 复核保留 `6da5253`
- [x] 1.7 验证：全 7 模块 reactor `mvn install -DskipTests` 全绿（SB 双栈根因消除）；`contextLoads` + 手动联调（登录→受保护接口→WS 信令→ai SSE）需 Nacos/MySQL/Redis 基础设施，为运行时验证（待起栈）

#### 阶段 2 · 合并单体 + 去微服务 + 合库

- [x] 2.1 新建 `shiwujie-bootstrap` 模块（唯一 `@SpringBootApplication(scanBasePackages=...)` + `@MapperScan` + `@EnableAsync` + repackage）；删 user/call/community/ai 四个 Application 类；**删 gateway 模块** `4f10d11`
- [x] 2.2 Dubbo→本地：10 处 `@DubboService`→`@Service`、15+ 处 `@DubboReference`→`@Resource`（接口定义留 model 不动）；删 3 个无消费者冗余 Inner（`InnerActivityService`/`InnerActivitysignService`/`InnerHelppostService`） `199e6f3`
- [x] 2.3 去微服务基础设施：删 Dubbo/Nacos 依赖+配置+`@EnableDubbo`/`@EnableDiscoveryClient`；父 pom 删 spring-cloud/nacos/dubbo 的 dependencyManagement；dev/prod profile 的 nacos IP 覆盖随移除 `106902b`
- [x] 2.4 路径内化（保契约）：单体 context-path 置空，原 context-path 前缀下沉到 controller 类级 `@RequestMapping`（user `/api/user/...`、community `/api/community/...`、call `/api/call/...`；ai `/api/ai/ai` 不动）；WS `@ServerEndpoint("/ws/call")`→`("/api/ws/call")`；收敛后 WebConfig 的 excludePathPatterns 同步用新全路径 `03c0d96`
- [x] 2.5 收敛重复：4 份 `LoginCheckInterceptor` + 4 份 `WebConfig` → common-web 各 1 份；ai 删自带 common-web 副本（JwtUtils/RedisTemplateConfig/拦截器/异常/BaseResponse 等）改用 common-web；model vs common-web 去重（PageRequest/CommonConstant/UserConstants） `35b81ed`
- [x] 2.6 合库：bootstrap 单一 datasource（yml 指向 `shiwujie`）✅；call 实体字段 snake→camel + 全局 `map-underscore-to-camel-case: true`（DB 列名不动，call 2 个 Mapper XML 的 resultMap property 同步改名）`61cb18a`；跨库写场景改单 `@Transactional`（删志愿者/删社区/社区审核/家庭审核；`synchronized` 保留作同库并发护栏）`a157991`；mysqldump 旧 4 库导入 `shiwujie`（`47.112.114.139`，sed 反引号库名改名 + ai 仅结构）✅ 远程 `shiwujie` 库 16 表已验证（user4+call2+community9+ai1；步骤见 [release-checklist](release-checklist.md)「合库执行步骤」）
- [ ] 2.7 验证（**启动级 ✅ / 功能级部分过**）：`mvn install` 全 reactor ✅；启动单 jar ✅（8.8s 无错，远程 `shiwujie` 库 16 表可连）；修 Dubbo→本地 bean 环——`spring.main.allow-circular-references: true`（社区↔志愿者↔社区管理员级联双向耦合，Dubbo 远程代理时代天然解耦）`5f21cf4`；契约回归启动级 ✅——4 模块 HTTP 路由全注册且 `/api/{user,call,community,ai}/**` 前缀对（OpenAPI 文档实测）+ BaseResponse 信封/业务码 `40010 NOT_LOGIN` 不变 + WS `/api/ws/call` 握手 101；功能级（2026-07-12 用户本地实测）：✅ 本地启动 + WS 视频求助信令链往返 + 事务级联回归（删志愿者/删社区单事务一致性，配方见 [testing-strategy](testing-strategy.md) 事务回归段；synchronized 边界错位见 [known-issues](../../../shiwujie-backend/docs/known-issues.md)）；⏭️ AI SSE 跳过（即将新开发）；⏳ token 滑动会话 + 合库字段映射待补

- [x] 2.8 模块合并（model + bootstrap 两模块）：common-web + user/call/community/ai 五模块 src 与资源（mapper XML ×13 / `logback-spring.xml` / prompttemplate ×3）并入 `shiwujie-bootstrap`，model 保留；父 pom `<modules>`={model, bootstrap}、spring-ai BOM/版本/spring-milestones 仓库迁父 pom；7→2 模块。对外契约零变更（同包根 `com.swj.shiwujie.*` 搬迁，不改 import）。commit 序列：`f8d3ef3`(WebConfig 消解) → `572ab1f`(user) → `ab097e7`(call) → `2d43a2a`(community) → `8babfe6`(ai) → `a215d9e`(common-web) → `2ddd673`(收尾验证)

### 🔴 安全加固（v2.1.0 收尾遗留项平移，详见 [known-issues.md](../../../shiwujie-backend/docs/known-issues.md) + [auth.md](../../architecture/auth.md)）

- [ ] 关闭 ai 默认用户兜底
- [x] 恢复 Helppost / Community 删除/更新权限检查（2026-07-12；求助帖=作者或注册人/管理员、社区=注册人、社区管理员=注册人/管理员+末位注册人护栏；`deleteCommunityManager` 自删 bug 一并修正。**仍待办**：Activity/Communityjoinreview/Activitysign 同类未网关）
- [ ] /ws/call 与社区/家庭审核补鉴权
- [x] 密码 MD5 → BCrypt（2026-07-12，Hutool `BCrypt` cost=10 + 存量懒升级；分支 `fix/v3.0.0-security-hardening`）
- [ ] `TOKEN_SECRETKEY` 走环境变量（硬编码弱密钥，待办）
- [ ] 前端 TLS + 移除硬编码 SDK Key

### AI 重写（Agent 驱动）（设计敲定·实现待 Phase 5）

> **状态：设计敲定（Phase 1–4）· 实现待 Phase 5。** 以下全 `[ ]` 子任务尚未落地。把现有 Java AI 模块（工作流式 prompt-as-router + 自研 ChatMemory + 弃用的自研 ReAct 雏形 + 半残留向量检索 + qwen 止血）整体替换为 Python LangGraph 智能体（Agent）驱动，Java 业务单体保留。跨切面架构（polyglot 双进程 + 缝 A 对话流 / 缝 C Java 能力 + 两层记忆 + 工具体系）见 [architecture/tech-stack](../../architecture/tech-stack.md) AI 重写段；用户可见契约变更见 [product/v3.0.0/](../../product/v3.0.0/)。诚实 Python 理由（**非「Java 做不到」**）：① LangGraph 是原版、生态成熟，spring-ai-alibaba-graph 1.0 移植的 HITL-resume（中断恢复）路径有 open bug，而紧急求助确认门恰需跨轮恢复；② 项目已踩坑（spring-ai-alibaba 1.0.0.2 的 `DashScopeChatModel` 调 qwen3.x 报 url error，止血改 `OpenAiChatModel`），LangGraph 经 OpenAI 兼容端点解耦模型绑定，降低再被坑风险；③ AI 时代语言渐非约束，真正可迁移的是容错/并发/架构设计（语言无关），且 alibaba-graph 本是 LangGraph 移植、设计相通，Java 知识不丢。**反转 gate**：Java AI 框架（spring-ai-alibaba-graph）生产稳定约满 1 年后，重新考虑回 Java-graph（= Decision B，即备选 B-prime，见 3.6 MyManus 冻结保留）。

- [ ] **前置 spike**（三条，决定 Decision A 主路径与 Decision B-prime 评估）：
  - qwen function-calling 可靠性：本工具集 + 本 prompt 通过率（建议 >=90%）；Decision A（意图识别溶进 agent loop、主 LLM 原生 FC 即「意图识别 + 工具选择」一步、杀旧 2-call 税）依赖此结果。
  - `spring-ai-starter-mcp-server-webmvc` 与 `spring-ai-bom 1.0.0` + `alibaba-starter` 共存可用性（Java 作 MCP server 暴露 8 工具的前置）。
  - alibaba-graph HITL-resume 是否被 open bug（#3297 / #3266）咬中（B-prime 回退评估）。
- [ ] **3.1 Python graph**：State（`messages` / `blind_id` / `position`）/ 节点（entry fork / agent 原生 FC / tools `ToolNode`）/ 标准环 / checkpointer（Redis `db=2`，`ai:ckpt:` 前缀避撞 spring-session/JWT key，`thread_id=blind_id`）/ HITL 两处自然 turn + checkpoint（导航交通方式选择 / 紧急求助确认门）/ 动态反馈（`stream custom` 进度 + 工具副作用信令 5001–6）。
- [ ] **3.2 工具 / 技能 / KB**：14 tool（6 Python-native `recognize_photo`[VLM] / `web_search` / `get_weather`[用每轮 position] / `gaode_poi_search` / `gaode_route` / `search_kb`[BM25 功能 KB] + 8 Java-MCP 见 3.5）+ 元工具 `read_skill(name)`（Pi 式 read-on-demand 技能文档加载器）+ 1 技能 `navigation`（read-on-demand SKILL.md，6 步 poi→报选项→问交通方式[HITL]→route→朗读摘要→launch）；高德决策类（poi/route/weather）= 后端 Python 自建 3 个 REST wrapper tool（直调高德 web API、出参剪裁、盲人朗读友好），执行类（起导航 UI）= App 高德 SDK（`launch_navigation`→5006），不接高德官方 MCP；KB = BM25 文本库（非向量 RAG，~10–40 篇 markdown + frontmatter `title/aliases/tags/summary`，启动载入内存，`search_kb` 返回整篇给主 LLM，>100 篇或非结构化语料才升向量 RAG）。
- [ ] **3.3 两层记忆**：短期 = LangGraph checkpoint 存全量 messages + 压缩（超 token 阈值 ~8–16k，Phase 5 按 qwen 实测定，把最早一批压成滚动 summary，recent-tail 约最近 10 条永不压缩护导航中状态；崩溃/中途截止可恢复）；长期 = 偏好（跨会话、后台异步抽取、用户不可见：说话方式/常用 APP/导航习惯等软事实，merge-with-latest 入 Redis hash + MySQL，turn 起注 system prompt 短段，绝不阻塞或强制）；`AiLogs` 旧表降级为追加只写审计/可观测日志（不再当记忆读），图片 offload 去留待 Phase 5。
- [ ] **3.4 Java WS 改造（缝 A）**：ticket 鉴权（堵 phone 冒充：盲人经已鉴权 HTTP 换短时 WS ticket，Redis `db=2` 绑 `blindId->phone`，`type=0` 登录带 ticket 非自报 phone）；流式中继 `getBasicRemote()`（阻塞）→`getAsyncRemote()`（非阻塞推 token），保留 jakarta `@ServerEndpoint`；两 `static HashMap`→`ConcurrentHashMap`；拦「收到客户端的数据」回显（AI 类消息不发）；删 AI 拦截器 dev 后门（无 token 静默登录盲人 id=1，对应 [known-issues](../../../shiwujie-backend/docs/known-issues.md) #1 / [auth](../../architecture/auth.md) 风险#6）；顺手修 AI 拦截器 Redis 续期错 key bug（漏 `-blind-` 前缀）。
- [ ] **3.5 Java MCP server 8 工具**：业务 4（`join_family` / `leave_family` / `family_info` / `update_profile`[仅基本字段]）+ 信令 4（`launch_navigation`[5006] / `request_video_help`[5002] / `request_emergency_help`[5003] / `open_app`[5004 白名单]）；`update_profile` 字段门 = MCP 工具 inputSchema 硬卡 `{nickname, phone, gender}`（`password`/`idCard`/`disabilityCard` 结构上不在 schema），Java 绑窄 DTO（非泛 `Blind` 实体）+ 单测断言 DTO 无敏感字段 setter（防约定腐烂，明示是 schema 校验硬卡非提示词约束）。
- [ ] **3.6 删 Java AI 模块**：删 `app/agent/tools/advisor/chatmemory` + `AiConfig`/`AiConstants`/`ChatServiceImpl`/`ChatController` + 依赖（`spring-ai-alibaba-starter-dashscope` / `spring-ai-openai` / `kryo` / `jsoup` 若仅 AI 用）；`MyManus`（342 行自研 ReAct 雏形）**冻结保留非删**——作 Java-graph B-prime 回退起跑线，删了浪费沉没成本。
- [ ] **3.7 两进程配置 + Docker**：新增根级 `scripts/`（`start`/`stop`/`logs`/`export`/`import`/`clear.sh`，bash，对齐参考仓 ctgu-agents）+ `docker/docker-compose.yml`（两 service）+ `config/.env` + `.env.example`；`shiwujie-backend/Dockerfile`（多阶段 `mvn install`→`eclipse-temurin:21-jre`）+ `shiwujie-ai/Dockerfile`（多阶段 `pip`/`uv`→`python:3.12-slim`），各 `scripts/start.sh`（本地 + Docker CMD 同款）；compose：java 发布 `8100:8100`（公网）、python 不发布端口（仅内网，java 经服务名 `http://python:8500` 调）、两 service `extra_hosts: host-gateway` 连宿主 MySQL/Redis（`47.112.114.139`）、`restart:always` + `init:true`、python 加 `/health`；非 docker 本地模式仍可（`mvn install` + `java -jar` / `uvicorn` 直跑）。
- [ ] **3.8 APK 改**：SSE client→WS turn client；`SocketDataV0`（Android）+ `SocketData`/`SocketVO`（Java model）加可选 `destination{name,lat,lng,address}`（加字段不改名，5006 首次具备终点载荷能力）；`AiFragment` 5006 读 `destination`；4-button 重写；WS ticket（经已鉴权 HTTP 换 ticket 再连 WS）；图片仍走 HTTP multipart（文本 turn 走 WS）。
- [ ] **3.9 测试**：删 `AiSmokeTest`（throwaway）+ Java WS 契约测试（AI-turn roundtrip，mock Python）+ Python tool 单测 + graph 集成测试 + 安全门测试（紧急确认 token 绑 `(blind_id, thread_id, issuing_turn)` 跨轮/同轮拒、App 非-MCP 确认面消费 token；`update_profile` 窄 DTO 无敏感字段断言；tool-name 白名单拒幻觉名冒充 `confirm`）。20 单测类 / 286 例（纯 Mockito）零 AI 引用不动保绿。
- [ ] **3.10 灰度**：硬切换（后端镜像 + APK 同批发，SSE↔WS 不兼容须版本配对）。

> 紧急求助确认门做对（红队推动，非协商）：`request_emergency_help` 拆 `prepare()` / `confirm()` 双工具；qwen 请求对可达紧急工具强制 `parallel_tool_calls=False`（堵单轮并行 prepare + 伪造 confirm）；Redis token 绑 `(blind_id, thread_id, issuing_turn)`，`confirm()` 拒绝同轮 token；v1 即做 App 侧显式确认面（按钮/长按）经非-MCP HTTP 端点消费 token（盲人单声道无视觉冗余，第三道门）。两条护栏无论 qwen FC spike 结果都上：MCP 服务端 strict JSON-schema 校验 + tool-name 白名单（拒未注册名，堵幻觉名冒充 confirm）。

> Decision B-prime（备选）：若缝 A 跨语言流式踩坑 / Python 进程负担过重 / 缝税超收益，回退单进程 spring-ai-alibaba-graph（缝 A 变方法调用，缝 C 变直接 Java 调用，MCP 工具设计语言无关故存活）；前置 spike 先验 alibaba-graph HITL-resume 在本 qwen3.x 栈是否被 open bug 咬中。`MyManus` 冻结保留（3.6）即为此回退起跑线。

### 能力补全

- [ ] App 集成高德 SDK（替代 v2.1.0 的 URI 调起）

### 工程化

- [ ] Docker 化部署
- [ ] 压力测试 + 性能基线 + AiLogs 索引调优

### 因单体化废弃（自 v2.1.0 收尾移除，见 ROADMAP 删除线）

- ~~分布式事务（Seata）~~：单体化后单库，无需跨库事务
- ~~网关统一鉴权~~：单体化后单拦截器，4 处 `LoginCheckInterceptor` 重复自然消除
- ~~Knife4j 聚合 ai~~：单体化后统一 Spring Boot 版本，无 SB2/SB3 文档协议割裂

> 🔴 安全加固项的代码定位与机制见 [known-issues.md](../../../shiwujie-backend/docs/known-issues.md) 与 [architecture/auth.md](../../architecture/auth.md)。
