<!-- markdownlint-disable MD024 -->

# 变更日志（CHANGELOG）

> 迭代历程明细。每条变更四分类：新增 / 变更 / 修复 / 移除。提交 hash 可用 `git show <hash>` 溯源。
>
> git tag 现有 `v1.0`（一期·uniapp+单体，独立根提交）、`v2.0.0`（二期开发后初步稳定版，2025-11-12）、`v2.1.0`（二期微服务封版，2026-07-11）。版本线：`v1.0`（一期）→ `v2.0.0`（二期初步稳定）→ `v2.1.0`（二期微服务封版）→ `v3.0.0`（单体化改造，进行中）。当前工作版本 `v3.0.0`。阶段 0–9 是按能力域对提交的聚类，时间区间为代表性提交范围（部分阶段能力并行推进，区间有重叠），日期**不确定**（标「约」）；其累积现状作为 v2.1.0 的起点内容。打 tag 时把进行中版本的 `## vX.Y.Z（进行中，未发布）` 改为 `## vX.Y.Z - YYYY-MM-DD`（精确日期）。

---

## v3.0.0（进行中，未发布）

> 单体化改造版本。反思 v2.1.0 微服务对当前体量过度设计，去微服务、合并 user/call/community/ai 为单体（收敛为 model 契约层 + bootstrap 唯一 app 两模块、统一 Spring Boot 版本）。用户可见契约原则上不变（继承 v2.1.0），变更以工程架构为主。大方向见 [ROADMAP.md](ROADMAP.md) 待实现段；交付任务见 [development/v3.0.0/task-breakdown.md](development/v3.0.0/task-breakdown.md)。打 tag 时补精确日期。

**架构决策（2026-07-11）**

- **两阶段推进**：阶段 1 业务模块原地升 SB 3.4.5/Java21（仍微服务、逐模块验证）；阶段 2 合并单体 + 去微服务 + 合库。版本迁移与模块合并解耦，bug 易定位。
- **合并为单库 `shiwujie`**：取代旧 4 库（shiwujieuser / shiwujiecall / shiwujiecommunity / shiwujieai），`47.112.114.139:3306`，user=`shiwujie`；结构 + 数据从旧 4 库 mysqldump 导入。
- **契约保护（硬约束）**：对外 HTTP 路径 `/api/{user,call,community,ai}/**` + WebSocket `/api/ws/call`（12 信令码）+ 状态码 / 返回字段名零变更，前端 App/Web 不改可对接。
- **call WebSocket 保留 `@ServerEndpoint`**：仅 `javax.websocket`→`jakarta.websocket`（嵌入式 Tomcat10 原生支持），不重写为 Spring 原生 handler。

**行为变更明细（阶段 1 + 阶段 2，2026-07-11，启动级验证通过）**

> 用户可见契约（HTTP 路径 / WS 12 信令 / 状态码 / 返回字段）启动级回归零变更；功能级（WS 往返 / ai SSE / 事务级联 / token 滑动）待 App/Web 联调。

**变更**
- **统一 Spring Boot 3.4.5 / Java 21**（阶段1.1 `9997b89`）：业务模块从 SB 2.7/Java17 升级，消除 SB2/SB3 双栈根因（Spring AI 1.0 强制 SB3）。MyBatis-Plus 3.5.1→3.5.9 换 `mybatis-plus-spring-boot3-starter`（阶段1.3 `34b0f6b`）；knife4j openapi2→openapi3-jakarta，83 处 `@Api`/`@ApiOperation`→`@Tag`/`@Operation` 注解重写（阶段1.4 `4db2c53`）；MySQL 驱动 `mysql-connector-java`→`mysql-connector-j`（阶段1.5 `af162c9`）。
- **javax → jakarta**（阶段1.2 `c698b66`）：业务模块 `javax.servlet` / `javax.annotation.Resource` / `javax.websocket` 全量迁 `jakarta.*`（无 persistence/validation/xml.bind，机械化迁移）。
- **Dubbo → 本地调用**（阶段2.2 `199e6f3`）：10 处 `@DubboService`→`@Service`、15+ 处 `@DubboReference`→`@Resource`（接口定义留 model 不动）；删 3 个无消费者冗余 Inner（Activity/Activitysign/Helppost）。
- **路径内化（保对外契约）**（阶段2.4 `03c0d96`）：单体 context-path 置空，原各服务 context-path 前缀下沉到 controller 类级 `@RequestMapping`——`/api/{user,call,community,ai}/**` 与 WS `/api/ws/call` 对外不变。
- **收敛重复**（阶段2.5 `35b81ed`）：4 份 `LoginCheckInterceptor` + 4 份 `WebConfig` → common-web 各 1 份；ai 删自带 common-web 副本（JwtUtils/拦截器/异常/BaseResponse 等）改用 common-web；model vs common-web 重复类（PageRequest/CommonConstant/UserConstants）去重。
- **合库 + 跨库写升单事务**（阶段2.6 `61cb18a` / `a157991` / `9afd831`）：4 分库 → 单库 `shiwujie`（mysqldump 导入，远程 16 表已验证）；call 实体 snake_case→camelCase + 全局 `map-underscore-to-camel-case: true`（DB 列名不动，2 个 Mapper XML resultMap property 同步改名）；社区入驻/审核通过/删志愿者/删社区 4 场景升 `@Transactional` 单事务（`synchronized` 保留作同库并发护栏）。
- **模块合并（7→2）**（阶段2.8 `a215d9e`）：common-web + user/call/community/ai 五模块 src 与资源（mapper XML ×13 / `logback-spring.xml` / prompttemplate ×3）并入 `shiwujie-bootstrap`，model 保留为唯一库模块；父 pom `<modules>` 收敛为 {model, bootstrap}，spring-ai BOM/版本/spring-milestones 仓库随 ai 并入迁父 pom 集中管理。同包根 `com.swj.shiwujie.*` 搬迁，不改 import、不改契约。

**新增**
- `shiwujie-bootstrap` 模块：单体唯一启动入口（`@SpringBootApplication(scanBasePackages=...)` + `@MapperScan` + `@EnableAsync`）+ 合并后 `application.yml`（单 datasource 指向 `shiwujie`、redis db=2、统一 SB3 配置 key），`spring-boot-maven-plugin` repackage 产出自包含 fat jar（约 64M，单 jar 部署）。（阶段2.1 `4f10d11`）

**修复**
- **Dubbo→本地 bean 循环依赖**（阶段2.7 `5f21cf4`）：`@DubboReference→@Resource` 后，社区↔志愿者↔社区管理员级联删除的双向耦合显化为 Spring bean 环（启动报 `BeanCurrentlyInCreationException`）。Dubbo 远程代理时代天然解耦构造期，单体以 `spring.main.allow-circular-references: true`（早期引用）等价还原，行为不变；登记 [known-issues](../shiwujie-backend/docs/known-issues.md)，后续可 `@Lazy` 精细化解环。

**移除**
- **gateway 模块**（阶段2.1 `4f10d11`）：Spring Cloud Gateway 整体删除（纯路由 + LB、无 Java 逻辑）。
- **Spring Cloud / Nacos / Dubbo**（阶段2.3 `106902b`）：删 Dubbo/Nacos 依赖 + 配置 + `@EnableDubbo`/`@EnableDiscoveryClient` 注解；父 pom 删 spring-cloud/nacos/dubbo 的 dependencyManagement；Dubbo provider 端口 21200–21500 全消失；dev/prod profile 的 nacos IP 覆盖随移除。
- **call `netty-all` 死依赖**（阶段1.5 `af162c9`）：源码零 Netty 引用，删冗余 `netty-all:4.1.50.Final`。
- **pagehelper 死依赖**（阶段1.4 `4db2c53`）：全代码 0 处 `PageHelper.startPage`。

**安全加固（2026-07-12，`fix/v3.0.0-security-hardening`）**

> 落地 ROADMAP「安全加固」项。对外 HTTP 路径 / WS 信令 / 业务码（`NO_AUTH` 40030）不变；权限边界**收紧**与口令算法升级为行为变更。明细见 [known-issues](../shiwujie-backend/docs/known-issues.md) #2/#6、[architecture/auth.md](architecture/auth.md) 风险 #4/#9。

**变更**
- **恢复社区域删改权限检查**：求助帖删改——仅作者（盲人本人）或本社区注册人/管理员可操作，员工（EMPLOYEE）无权，违者返 `NO_AUTH`；社区修改/删除——仅注册人；社区管理员增删改——仅注册人/管理员，并加「末位注册人护栏」（禁降级/删除唯一注册人、禁新增第二个注册人）。此前这些检查被注释或缺失，任意登录用户可删/改任意求助帖与社区、随意增删管理员。
- **修正 `deleteCommunityManager` 自删 bug**：原实现忽略请求体、恒删调用者自己的管理记录；改为按请求体目标删除并鉴权。
- **口令存储 MD5 → BCrypt**：新增 `PasswordUtils`（Hutool `BCrypt`，cost=10，盐内嵌），盲人/志愿者/社区登录注册与改密全改 BCrypt；存量无盐 MD5 行于首次以原明文口令登录通过时**懒升级**为 BCrypt（对用户透明，无离线迁移脚本）。身份证/残疾证等 PII 单向哈希仍用 MD5（与口令无关）。`password varchar(100)` 足装 60 字符 BCrypt 串，不加 salt 列、不改 DDL。

**AI 文本路径止血（2026-07-12）**

> `qwen3-max` 文本模型 key 过期；且 spring-ai-alibaba 1.0.0.2 的 `DashScopeChatModel` 调 qwen3.x 文本模型返 `400 InvalidParameter: url error`（裸 HTTP 同 key 同模型 200，定位为客户端与 qwen3.x 文本模型不兼容）。最小止血——对外契约零变更；AI 模块后续会用别的技术重构（见 [ROADMAP](ROADMAP.md)），本轮为止血非治本。明细见 [known-issues](../shiwujie-backend/docs/known-issues.md) #11。

**变更**
- **文本模型 `qwen3-max` → `qwen3.6-flash`** + **文本路径改 OpenAI 兼容直连**：`AiConfig.qwenText` 由 `DashScopeChatModel` 改为官方 `OpenAiChatModel`（新增 `spring-ai-openai` 依赖，版本随 spring-ai-bom 1.0.0 管理），经 DashScope OpenAI 兼容端点 `compatible-mode` 直连。消费方（`TextApp`/`ToolChoiceApp`/`ImageApp`/`MyManus`）注入类型 `DashScopeChatModel` → `ChatModel` 接口（两实现均实现该接口，类型无关注入）。**图像路径不变**（仍 `DashScopeChatModel` + `qwen3-vl-flash` + `withMultiModel`）。
- **`base-url` 去 `/v1`**：`OpenAiApi` 默认拼 `/v1/chat/completions`（与 `DashScopeApi` 语义不同，后者不拼），`application.yml` 的 `spring.ai.dashscope.base-url` 由 `.../compatible-mode/v1` 改为 `.../compatible-mode`，否则路径双拼 404。注：`spring.ai.dashscope.chat.options.model` 为**死配置**（`AiConfig` 用 `AiConstants` 常量装配，不读 yml），仅 `api-key` / `base-url` 经 `@Value` 生效。

**新增**
- `AiSmokeTest`：AI 集成冒烟测试（文本模型返回非空 / 裸 HTTP 对照隔离 / 图像模型识别三用例），`@EnabledIfSystemProperty(ai.smoke=true)` 守卫，默认跳过、不污染常规 `mvn test`（与现有 286 例纯 Mockito 单测性质不同）。临时验证用，AI 重构后丢弃。

**AI 模块重写（设计敲定·实现待 Phase 5）**

> 本节是**设计阶段记录，非已落地变更**。与上文「单体化（已落地）/ 安全加固（已落地）/ 单测层（已落地）」明确区分：以下全部设计决策与行为变更预告均为**尚未实现**，落地方在 Phase 5，落地后才会回卷进各对应层级。设计敲定 = Phase 1-4 梳理（功能分析 / 技术分析 / 技术方案 / 系统整合）完成；总图见 [architecture/ai-rewrite.md](architecture/ai-rewrite.md)，大方向见 [ROADMAP.md](ROADMAP.md) 待实现段「AI 重写-*」7 条（全 `[ ]` 未勾）。

**设计决策（polyglot 双进程）**
- **Java 业务单体 = 网关 / 业务真相源 / MCP server**：保留 WS 终结点 + JWT/Redis 鉴权 + 全部业务真相，新增对外 MCP server（streamable HTTP，暴露 8 工具）。**Python LangGraph = 计算大脑**（agent loop + 工具 + 记忆 + KB）。Python 不持用户 JWT，Java 鉴权后内部传 `blind_id`。
- **选 Python 的诚实三条理由**（**非**「Java 做不到」——红队已证伪：spring-ai-alibaba-graph 1.0 GA 在本项目已用的 alibaba-bom 1.0.0.2 内确有 graph/checkpoint/interrupt 原语）：
  1. **成熟参考实现**——LangGraph 是原版、生态成熟；spring-ai-alibaba-graph 是其 1.0 移植，HITL-resume（跨轮中断恢复）路径有 open bug，而紧急求助确认门恰需跨轮恢复。
  2. **解耦已反复踩坑的 Alibaba 模型绑定**——项目已踩坑（见上文「AI 文本路径止血」：spring-ai-alibaba 1.0.0.2 的 `DashScopeChatModel` 调 qwen3.x 文本模型报 url error，止血改 `OpenAiChatModel`）；LangGraph 经 OpenAI 兼容端点解耦模型绑定，降低再被坑风险。
  3. **学可迁移的设计层**——AI 时代语言渐非约束，真正可迁移的是容错/并发/架构设计（语言无关）；成熟 Python 实现是更好的老师；且 alibaba-graph 本是 LangGraph 移植、设计相通，Java 知识不丢。
- **反转 gate（备选 Decision B-prime）**：Java AI 框架（spring-ai-alibaba-graph）生产稳定约满 1 年后，重新考虑回 Java-graph；前置 spike 先验其 HITL-resume 在本 qwen3.x 栈是否被 open bug（#3297/#3266）咬中。
- **两条缝取代旧 SSE + 信令中继**：缝 A（对话流）= App↔Java WS 全合一（单双向通道，承载文本/语音/位置/图片/流式回/5001-6 信令/未来主动推送；Java↔Python 逐 turn 内部 HTTP 流式回）；缝 C（Java 能力）= Python↔Java MCP streamable HTTP（Python 零业务/信令代码）。原「缝 B 信令中继」已并入缝 C。
- **意图识别溶进 agent loop（Decision A）**：主 LLM 原生 function-calling 即「意图识别+工具选择」一步，杀旧 2-call 税；依赖 qwen FC 稳（spike 前置，建议 ≥90% 通过率）。
- **两层记忆**：短期 = LangGraph checkpoint（Redis db=2，`thread_id=blind_id`，Python 用 `ai:ckpt:` 前缀避撞 spring-session/JWT key；超 token 阈值滚动压缩、recent-tail 永不压缩护导航状态、崩溃可恢复）；长期 = 偏好（跨会话、后台异步抽取、用户不可见、merge-with-latest 入 Redis hash + MySQL、turn 起注 system prompt 短段、绝不阻塞）。AiLogs 旧表降级为追加只写审计/可观测日志（不再当记忆读）。
- **BM25 功能 KB（非向量 RAG）**：~10-40 篇 markdown（frontmatter title/aliases/tags/summary），启动载入内存，`search_kb` 返回整篇给主 LLM；>100 篇或非结构化语料才升向量 RAG。
- **Pi 式 read-on-demand**：`read_skill(name)` 元工具按需加载技能文档，作自建 loop 的「金标准契约」参考。

**行为变更预告（全部「设计敲定·未落地」，落地后回卷进 product 契约）**
- **AI 通道 SSE → WS**：`/api/ai/ai/{doChatByText,doChatByImage,NewApp}` SSE 废弃，迁 `/api/ws/call` 新 AI-turn 消息类型（入站新 requestType + 出站流式 token 帧）；属本次重写范围，App 同步改，**非违约**（AI 通道本就重写）；`/NewApp` 重复端点随 SSE 清。业务契约 user/call/community **零变更**。
- **SocketData / SocketVO（Java model）+ SocketDataV0（Android）加可选 `destination{name,lat,lng,address}`**：加字段不改名，5006 首次具备终点载荷能力；6 信令码 5001-5006 码值不变（5006 仅多可选 destination 载荷）；WS 12 信令码框架不变。
- **App 4-button 重写**：AI 页交互重写以适配新对话通道 + 显式确认面（紧急求助第三道门）。
- **导航多步人工确认（HITL）**：navigation skill 6 步流程（poi→报选项→问交通方式[HITL]→route→朗读摘要→launch），问交通方式处插人工确认。
- **偏好记忆**：跨会话记住说话方式/常用 APP/导航习惯等软事实（用户不可见、不阻塞）。
- **删 Java AI 模块**：工作流式 prompt-as-router + 自研 ChatMemory + 半残留 RAG + qwen 止血整体替换；**MyManus 自研 ReAct 骨架冻结保留非删**（342 行作 Java-graph B-prime 回退起跑线）。✅ chunk-2b / 2b-5（2026-07-19）已删活路径代码（`app/` + `controller/ChatController` + `service/ChatServiceImpl` + `tools/` + `chatmemory/` + `advisor/MyRagAdvisor` + AI 专用 common DTO + `AiSmokeTest` + `prompttemplate/*`，21 .java + 3 模板 + 1 测试，bootstrap 321 单测绿）；2b-5 侦查证 MyManus 仅依赖 `MyLoggerAdvisor`/`AiConfig`，不注入 tools/* 故可全删；`AiLoginCheckInterceptor`/`AiWebConfig` 已于 2b-6a（2026-07-20）删（dev 后门：无 Authorization 静默登录 blind id=1 / phone 19872250169；`/api/ai/**` 在 2b-5 删 `ChatController` 后已空集，拦截器拦空集，删除零功能影响；`ErrorCode` **保留**，53 个其它引用者）。WS phone 冒充（known-issues #7）的 ticket 鉴权留 chunk-2e 与 Android WS 改造同批。✅ 2b-4b（2026-07-20）紧急求助拆 `request_emergency_help_prepare`/`_confirm` 双工具落地（Java 侧闸 ②，红队 Q18）：新增 `EmergencyTokenStore`（Redis `ai:emerg:{token}` TTL 5min，绑 `(blind_id, issuing_turn)`，一次性消费）+ `BlindMcpContext.issuingTurn`/`McpTransportConfig` 加 `X-Issuing-Turn` header 提取（turn 概念来自 Python graph，经 MCP header 跨进程传，chunk-2c 真 MCP client 接）；`confirm` 拒同轮 token（结构闸 ②），跨轮通过才推 WS 5003；fail-closed（turn 未传→0→同轮永拒）；bootstrap 337 单测绿。gate ①（Python `parallel_tool_calls=False`）+ gate ③（App 显式确认面）留 Python/App 侧。
- **两进程 Docker 部署**：Java 发布 8100:8100（公网），Python 不发布端口（仅内网，Java 经服务名调）；新增根级 `scripts/`（start/stop/logs/export/import/clear.sh）+ `docker/docker-compose.yml` + `config/.env`；非 docker 本地模式仍可。灰度 = 硬切换（后端镜像 + APK 同批发，SSE↔WS 不兼容须版本配对）。✅ chunk-2d（2026-07-20）Docker 物料落地：根级 `scripts/{start,stop,logs,export,import,clear}.sh`（bash，`-p shiwujie -f docker/docker-compose.yml`，`--build` 强制重建，`start.sh` 首跑自动 touch 空 `config/.env`）+ `docker/docker-compose.yml`（project `shiwujie`，两 service；`java` 发布 8100、`python` 仅 `expose 8500`；`extra_hosts host.docker.internal:host-gateway` + `environment` 覆盖 `MYSQL_HOST`/`REDIS_HOST=host.docker.internal` 连宿主 MySQL/Redis——yml 默认仍硬编码公网 IP 不变；`python` healthcheck `/health`，`java` 不 hard depends_on）+ 两多阶段 `Dockerfile`（backend：`maven:3.9-eclipse-temurin-21` install → `eclipse-temurin:21-jre`；ai：`python:3.12-slim` + `uv sync --frozen --no-dev` → 拷 `.venv`+`src`，`PYTHONPATH=/app/src`）+ `config/.env.example`（凭据全注释，走 committed 默认）+ 子项目 `scripts/start.sh`（本地非 docker 模式）+ `application.yml` `MYSQL_HOST`/`REDIS_HOST` 占位符化（默认硬编码公网 IP，Docker 覆盖 host.docker.internal）+ `shiwujie-ai/__main__.py` `SHIWUJIE_AI_HOST` env（默认 127.0.0.1，Docker 覆盖 0.0.0.0）。**端到端生产部署尚待**：chunk-2c 真 qwen（当前 FakeChatModel）+ chunk-2e APK（App WS AI-turn 消费 + destination 字段 + 4-button）。

**WS 必修改造（缝 A，设计敲定·未落地）**
- ticket 鉴权（堵 phone 冒充）：盲人经已鉴权 HTTP 换短时 WS ticket，type=0 登录带 ticket 非自报 phone；流式中继 `getBasicRemote()`→`getAsyncRemote()`（非阻塞推 token，保留 `@ServerEndpoint`）；两 `static HashMap`→`ConcurrentHashMap`；拦「收到客户端的数据」回显（AI 类消息不发）；删 AI 拦截器 dev 后门（无 token 静默登录 blind id=1，见 [known-issues](../shiwujie-backend/docs/known-issues.md) #1 / [architecture/auth.md](architecture/auth.md) 风险 #6）；顺手修 AI 拦截器 Redis 续期错 key bug（漏 `-blind-` 前缀，见 [known-issues](../shiwujie-backend/docs/known-issues.md) #3）；图片仍走 HTTP multipart（文本 turn 走 WS）。

**硬修正（红队推动·非协商）**
- 紧急求助确认门做对：`request_emergency_help` 拆 `prepare()`/`confirm()` 双工具；qwen 请求对可达紧急工具强制 `parallel_tool_calls=False`（堵单轮并行 prepare + 伪造 confirm）；Redis token 绑 `(blind_id, thread_id, issuing_turn)`，`confirm()` 拒绝同轮 token；v1 即做 App 侧显式确认面经非-MCP HTTP 端点消费 token（盲人单声道无视觉冗余，第三道门）。
- update_profile 字段门：MCP 工具 inputSchema 只暴露 nickname/phone/gender（password/idCard/disabilityCard 结构上不在 schema）；Java 绑窄 DTO（非泛 Blind 实体）+ 单测断言 DTO 无敏感字段 setter（防约定腐烂）——明示是 schema 校验硬卡，非提示词约束。
- qwen FC spike 前置：无论 spike 结果都上两护栏（MCP 服务端 strict JSON-schema 校验 + tool-name 白名单拒未注册名，堵幻觉名冒充 confirm）。

**测试（设计敲定·未落地）**
- 现有 20 单测类 / 286 例（纯 Mockito）零 AI 引用 → **不动保绿**；`AiSmokeTest` → 删（throwaway），「打 DashScope」价值迁 Python pytest（mock + 真 qwen FC spike）；新增 Java WS 契约测试（AI-turn roundtrip，mock Python）+ Python tool 单测 + graph 集成测试。

**App 前端 P0 快速止血（2026-07-12）**

> 落地 [`问题.md`](../问题.md) 视频通话/紧急求助前后端问题分析 + 本轮 App 代码审查中核实仍存在的前端阻断/高危项。最小修复，不触碰结构（blind/volunteer 合并、AiFragment 拆分、ViewModel 引入等留待结构重构批）。对外 HTTP/WS 契约零变更。明细见 [App known-issues](../shiwujie-frontend/app/docs/known-issues.md)「已修复（P0 快速止血）」。`assembleDebug` + `assembleRelease`（R8 混淆+资源压缩+lintVital）均已通过。

**修复**
- **WS 心跳间隔 2 小时 → 30 秒**：`WebSocketService` 心跳实际间隔为 `HOURS`、注释与日志却写 30s，长连接被 NAT 静默掐断，紧急求助/视频信令不可靠送达。对齐为 30s。
- **视频通话监听器泄漏**：blind/volunteer 两份 `VideoCallActivity.onDestroy` 误调 `removeXxxListener(null)`（`List.remove(null)` 为 no-op），匿名监听器永不移除，Activity 销毁后仍收 WS 回调致 NPE/泄漏。改为存字段、按真实引用移除。
- **紧急求助"无法再次求助"死锁**：`EmergencyHelpManager` 求助成功后 `isInEmergencyHelp` 仅在失败/取消/挂断时复位，家属无响应时永真，用户无法再次求助。新增 60s "家属无响应"超时兜底（复用主线程 Handler，取消/挂断/响应/通话建立时取消），超时自动复位；回调接口加 `default onHelpTimeout()`（不破坏既有实现）。
- **AI 页 WebSocket 断线不重连**：`AiFragment.attemptWebSocketReconnect` 仅打日志从不调 `connect`、却谎报"重连完成"，AI 推送（5001~5006）断线后永不恢复。改为真正调 `WebSocketManager.connectWebSocket`。

**变更**
- **统一 token 注入拦截器**：`RetrofitClient` 新增 OkHttp 拦截器，仅当请求未自带 `Authorization` 头时从本地注入 `Bearer <token>`，兜住历史漏带；既有手拼 `"Bearer "+token` 调用不受影响（已有头则跳过），调用点清理留待结构批。HTTP BODY 日志改为仅 DEBUG 包打印（release 关闭）。
- **release 构建加固**：开启 R8 混淆 + 资源压缩，补 ProGuard keep（Gson 泛型/`@SerializedName`、`TypeToken`、`AiFragment` 内被 Gson 序列化的内部数据类 `Message`/`Conversation`、anyRTC/讯飞/Retrofit、`org.slf4j` 缺失类抑制——java-websocket 传递依赖拉进 slf4j-api 但无 binding，运行期退化 NOP 不影响功能）；`AndroidManifest` `allowBackup=false`；讯飞 SDK `setShowLog` 按 `BuildConfig.DEBUG` 守卫；`buildFeatures.buildConfig=true` 以生成 `BuildConfig`。`assembleDebug` + `assembleRelease`（R8 混淆+资源压缩+lintVital）均已通过。

**移除**
- `network_security_config` 中把 CIDR（`192.168.0.0/16` 等）当作 `<domain>` 的无效条目（语法不支持、本就不生效）。生产 IP 明文 HTTP 放行暂保留（待后端 TLS）。
- `gradle-wrapper.properties` 失效的 macOS 本地分发路径 `file:/Users/luna/...`（仅该开发机可用），切回腾讯镜像远程 URL（已本地缓存）。

**App 前端 批次A：协议对齐 + 超时竞态止血（2026-07-12）**

> 接 P0 快速止血后的下一批。修 P0 引入的高危竞态 + WS 重连失活 + 线程安全，并把散落的 requestType 魔数常量化、固化信令真值表。不碰结构（blind/volunteer 合并、AiFragment 拆分等留批次 D）。对外 HTTP/WS 契约零变更（requestType 码值不变，仅前端常量替换）。明细见 [App known-issues](../shiwujie-frontend/app/docs/known-issues.md)。

**修复**
- **紧急求助超时在通话中误触发（P0 副作用，高危）**：P0 给紧急求助加了 60s "家属无响应"超时兜底，但取消该超时的 `EmergencyHelpManager.handleSocketMessage(type=2)` 是死代码——`WebSocketManager.handleMessage` 从不调它，导致家属正常接听、通话进行中时 60s 后超时仍 fire、误复位 `isInEmergencyHelp`。`WebSocketManager` 收到消息后现补发 `EmergencyHelpManager.handleSocketMessage`（type=2 取消超时+更新状态，其他 type 仅打日志），超时不再在通话中误触发。
- **WS 重连用尽后静默永久失活**：`MAX_RECONNECT_ATTEMPTS=5` 用尽后 `onClose`/`checkConnectionStatus` 的重连门槛永假，弱网下断连 >5 次即永久断开、无 UI 提示。改为：快速重连窗口（<5 次）内 3s 重试，用尽后转 60s 慢速持续重试（不再放弃），并在进入慢速首次通知 `onReconnectNeeded`；通话/匹配中仍跳过重连（`canReconnect` 不变）。
- **`VideoCallManager` 监听器回调跑在子线程**：`notifyStatusChanged`/`notifyMessageReceived` 在 java-websocket 读线程直接 for-loop 调监听器，Activity 回调改 UI 可能触发 "Only the original thread..." 崩溃。两处统一切到主线程 `Handler.post`（与 `WebSocketManager` 自身一致）。

**变更**
- **前台通知按角色跳首页**：`WebSocketService` 前台通知点击 Intent 原硬编码 `VolunteerHomeActivity`，盲人账号点通知进错志愿者首页；改为按 `SharedPrefsUtil.isBlind()` 选 `BlindHomeActivity`/`VolunteerHomeActivity`。
- **避障客户端 HTTP BODY 日志守卫 DEBUG**：`ObstacleDetectionRetrofitClient` 日志拦截器原无条件 `Level.BODY`，release 也会打印请求体；改为仅 DEBUG 打印（与 P0 对 `RetrofitClient` 的改法一致，补漏）。
- **信令码常量化 + 固化真值表（零行为变更）**：`SocketDataV0` 补 requestType -1~5 命名常量（按实际语义）+ 类头真值表注释；`VideoCallManager` switch 与 blind/volunteer `HomeFragment`、`EmergencyHelpManager` 的魔数比较改用常量。审查修正：原报告称「`VideoCallManager` 应补 type 3/4/5 处理」系误判——3/4/5 是紧急求助通知（接收型），已在 volunteer/blind `HomeFragment` 正确处理，落 `VideoCallManager` default 合理。`SocketDataV0` 5 个零调用点工厂方法标 `@Deprecated`（命名误导，如 `createVolunteerAccept=4` 实为"取消"），不删（删属批次 D）。

**App 前端 批次B：死代码 / 死资源 / 死依赖清理（2026-07-12）**

> 接批次A后的「简化」批。三个 Explore agent 扫描 + 人工逐项 grep 复核，把全 App 核实**零引用**的死代码、死资源、死依赖、一个无效权限一次性清除。对外 HTTP/WS 契约、信令码值零变更（纯删除死代码——R8 release 本就裁掉其中大部分，源码层面仍在拖慢 debug 构建、污染自动补全、制造假信号）。无 `getIdentifier` 动态查资源，删除均由 build 兜底验证。明细见 [App known-issues](../shiwujie-frontend/app/docs/known-issues.md) #8/#10/#12。`assembleDebug` + `assembleRelease`（R8 混淆+资源压缩+lintVital）+ 全量 `lint` 均已通过（全量 lint 16 项既有错误零新增）。

**移除**
- **未用类（15）**：Compose 模板残留（`MainActivity.kt` + `ui/theme/{Color,Theme,Type}.kt`——launcher 实为 `ChooseIdentityActivity`、App 全 View 体系，`MainActivity.kt` 不在 manifest）；空占位/未用 POJO（`SegmentedTTSManager` 0 字节空文件、`User`/`UserInfo`/`BlindLoginSuccessVO`/`ActivityQueryRequest`/`AiChatRequest`/`AiChatResponse`）；避障 mock 脚手架（`ObstacleDetectionManager` 纯模拟数据零调用方 + 3 个 `ObstacleDetection{Health,Result,Session}Response`——真实避障路径走 `ObstacleDetectionRetrofitClient` + `ApiService.startObstacleDetectionSession/processFrame`，均 `Call<String>` 不碰这些模型）。
- **类内死方法 / 字段 / 接口**：`SocketDataV0` 删批次A标的 5 个 `@Deprecated` 工厂 + `setContent`/`checkConnectionStatus`（保留 `getContent`——`AiFragment` 在用）；`ApiService` 删 2 个避障健康检查方法 + 相关 import；`WebSocketManager` 删 `getSocketData`/`setConnectionStatusListener` + 字段（实际走 `globalConnectionStatusListeners` 列表）；`VideoCallManager` 删 `addMessageListener`/`removeMessageListener` + 接口 `VideoCallMessageListener`、7 个未用查询方法（`getCurrentCallStatus/Id`/`getMatchedBlindPhone`/`getMatchedVolunteerPhone`/`getCallDuration`/`isInCall`/`isWaitingForMatch`）、`updateCallStatus` 收为 `private`（保留 `getCallStartTime` + 状态监听器——VideoCallActivity 在用）；`EmergencyHelpManager` 删 `getCurrentHelpData`/`respondToEmergencyHelp(String)`（volunteer HomeFragment 自己直调 API）/`destroy`；`EmergencyHelpData` 删 3 个未用工厂 + 9 个未用 getter + 5 个状态判定方法 + `getStatusDescription`（保留对应 setter——manager 内部在用）；`ApiCallback.getContext`；`AiChatManager` 删 `startStreamingOutput`/`isStreaming`/`getTypingSpeed`、`ImageRecognitionManager` 删 `isStreaming`/`getTypingSpeed`（保留 `setTypingSpeed`）。
- **死资源（57 文件 + strings 12 项 / 2 数组）**：布局 9（`activity_main`/`fragment_community`/`fragment_dashboard`/`fragment_notifications`/`card_join_request_status`/`ifly_layout_mnotice_image`/`fragment_blind_community_joined`/`fragment_blind_community_no_join`/`fragment_volunteer_community`）；菜单 2（`menu_bottom_nav`/`menu_top_app_bar`）；`xml/tts_setting.xml`（无 PreferenceFragment 加载）→ 级联删 string-array `stream_entries`/`stream_values`；drawable 顶层 20 项 + 整目录 `drawable/icon/`（25 项）；strings 12 项（`title_home/dashboard/notifications`/`blind_count`/`volunteer_count`/`blind_label`/`volunteer_label`/`register_date`/`chinese`/`english`/`learn_call`/`help_notification`）。注：`drawable/icon/` 子目录因与顶层资源同名冲突、本就不是合法 Android 资源目录（其文件从未打进 APK），删它是纯源码树清理。
- **死依赖**：`app/build.gradle.kts` 删 lifecycle livedata/viewmodel ktx + 整组 CameraX（5 个 `implementation`——对应已知问题 #12「依赖声明 CameraX、实用 Camera2」）；`gradle/libs.versions.toml` 同步删版本与 library 声明。
- **无效权限**：`AndroidManifest.xml` 删 `READ_PRIVILEGED_PHONE_STATE`（signature/system 级，第三方 app 拿不到、声明无效；代码只用 `READ_PHONE_STATE`/`READ_PHONE_NUMBERS`）。

**后端单元测试层（2026-07-12，ai 外）**

> 给 ai 模块外的后端补纯单元测试（用户决策：纯 Mockito、不起 Spring/DB/Redis、零新依赖；覆盖优先级「先深耕安全热路径再铺广」）。`spring-boot-starter-test`（test scope）已在 bootstrap pom，提供 JUnit5 + Mockito5（inline mock maker）+ AssertJ。20 个测试类 / 286 例，`mvn test` 全绿，覆盖 utils/common/exception（`PasswordUtils` BCrypt、`JwtUtils`、`LoginUtils`、`ResultUtils`、`ConverterUtils`、`ThrowUtils`、`ErrorCode`）、拦截器 `LoginCheckInterceptor` 全分支、user 域（Blind/Volunteer/Family/FamilyJoinReview）、community 域（Community/Communitymanager/Helppost/Activity/Activitysign/Communityjoinreview）、call 域（Urgenthelp/Videohelp）。MyBatis-Plus 3.5.9 单测三坑（`ServiceImpl.baseMapper` 需 `ReflectionTestUtils` 注入、`getOne(QueryWrapper)` 走两参 `selectOne`、`insert`/`updateById`/`deleteById` 多态重载须 typed matcher）已踩平固化进模板，明细见 [testing-strategy](development/v3.0.0/testing-strategy.md)「自动化单元测试」段。ai 模块、`@SpringBootTest` 集成测试（需测试用 DB/Redis profile 或 H2/Testcontainers）本轮暂不覆盖。

**新增**
- `shiwujie-bootstrap/src/test/`：20 个纯单元测试类，286 例。

**后端代码审查 P0 止血（2026-07-12，`fix/v3.0.0-security-hardening`）**

> 安全加固 + 单测落地后的一次独立全量审查（ai 外）。验证刚落地的加固（BCrypt + 权限恢复）正确；并修掉一批**新发现**的 P0 高危（账户接管、Redis 队列序列化断裂、TTL 单位错、NPE 簇）。对外 HTTP 路径 / WS 信令 / 业务码（`NO_AUTH` 40030、`PARAMS_ERROR` 40000）零变更，前端零改。明细见 [known-issues](../shiwujie-backend/docs/known-issues.md) #4/#5/#9 + 跨切面 ✅ 段、[architecture/auth.md](architecture/auth.md) 权限矩阵。`mvn install -DskipTests` + `mvn test`（287 例，+1 回归）全绿。

**修复**
- **改密账户接管**：`BlindController`/`VolunteerController` 改密接口原不校验登录人 == body 目标 id，且 service 侧原密码校验整段在 `if(isNotBlank(originPassword))` 内——originPassword 留空即跳过原密码校验。组合 `{blindId:<受害者>, newPassword, originPassword:""}` 可改任意账号密码。加 ownership 校验（不等即 `NO_AUTH`）+ 已设密码用户 originPassword 必填且须 `PasswordUtils.matches` 通过；首次设密（当前确实无密码）免 origin，此时所有权已过。
- **视频求助匹配队列序列化断裂 + TTL 单位错**：`RedisUtils` 双注入（`@Resource` 字段 + `StringRedisTemplate` 构造器）致实际用 `StringRedisTemplate`，存 `LinkedList<Long>` 队列首写即 `ClassCastException`（token 等 String 值正常，故 WS 信令测过未暴露）；且队列写回 `setToRedis(key,queue,30L)` 走硬编码 `TimeUnit.DAYS` = **30 天**（非登记的 30s），僵尸志愿者 id 滞留队列头致盲人匹配失败。删构造器、字段改 `@Resource RedisTemplate<String,Object>` 走配置 bean（JDK 序列化兜底两类值）；新增 `setToRedis(key,value,timeout,TimeUnit)` 重载，`CallConstant.VOLUNTEER_QUEUE_TTL_SECONDS=30L`，三处队列写回改 `TimeUnit.SECONDS`。
- **NPE 簇加固**：`removeVolunteerFromVideohelp` Redis 无队列 key 时原对 null queue 调 `contains` 必现 NPE，改直接抛 `PARAMS_ERROR("您不在匹配之中")`；并补多处取实体后未判空即解引用（Blind/Volunteer/Family 删除路径、Community `checkLogin`、Communityjoinreview 列表、Urgenthelp `joinUrgenthelp`、Videohelp/Urgenthelp 上传视频路径）统一 `ObjUtil.isNull` 判空。

> 架构现状（已落地）见 [architecture/tech-stack.md](architecture/tech-stack.md) / [architecture/data-model.md](architecture/data-model.md) / [architecture/gateway-dubbo.md](architecture/gateway-dubbo.md) 的「v3.0.0 单体化（已落地）」段；交付 spec 见 [development/v3.0.0/task-breakdown.md](development/v3.0.0/task-breakdown.md)。

---

## v2.1.0 - 2026-07-11

> 二期微服务封版（tag `v2.1.0`，2026-07-11）。一期 `v1.0`、二期初步稳定 `v2.0.0`（2025-11-12）在先。起点 = 阶段 0–9 的累积现状（明细见下方「历史时期」）。未完成的收尾项（🔴 安全加固 / 能力补全 / 工程化）整体平移至 v3.0.0（单体化改造）。

**文档体系**
- 版本分级落地：`product/` 与 `development/` 改为 `current.md` 指针 + `vX.Y.Z/` 目录模型（工作直接写在进行中版本目录，发布即冻结、新建下一版；历史版本目录保留不删）。详见 [CONTRIBUTING.md](CONTRIBUTING.md) 第五节。

**修复**
- **token 续期 key 漏身份前缀**：`LoginCheckInterceptor` 续期（`renewKey`）与删用户删 token（`deleteBlind`/`deleteVolunteer`）拼的 Redis key 漏了 `-blind-`/`-volunteer-` 段，与登录存/拦截器读的 key 不匹配 → 续期静默失效（活跃用户 90 天后被踢）、删用户旧 token 残留。改为提取共享 `redisKey`（读/续期共用）杜绝拼接分叉，续期对齐登录 90 天（真滑动会话）；删 token 补回前缀。涉及 user/call/community 三份拦截器 + user 模块两处删除（ai 模块同型 bug 暂不动）。明细见 [known-issues](../shiwujie-backend/docs/known-issues.md) #3。

---

## v2.0.0 - 2025-11-12（二期开发后初步稳定版）

> 二期首个 semver 版本（tag `v2.0.0`，原中文名「二期开发后初步稳定版」，2026-07 更名为 `v2.0.0`）。二期开发至阶段 7 前段、Spring AI Alibaba 仍处 **M6.1**（M6 线）时的一个稳定里程碑——提交为开启 dubbo empty-protection（`d326353`，见阶段 7 明细），尚未进行紧随其后的 M6→1.0 引擎重构与 mqtt/RAG/ReAct 移除（那些在其后，归入 v2.1.0 起点）。无独立 `product/v2.0.0/` 目录——版本分级模型 2026-07 才落地，此 tag 为历史快照（同 `v1.0` 性质）；其后的工程化收尾（阶段 8–9）+ 文档体系 + token 续期修复于 v2.1.0 封版。

---

## 历史时期（阶段 0–9，约 2025-06 ~ 2026-07，日期不确定）

> 以下各阶段为过去开发的明细，大方向 rolled-up 见 [ROADMAP.md](ROADMAP.md#历史时期阶段-09约-2025-06--2026-07日期不确定)。

### 阶段 9 · 工程化收尾（约 2026-07）

> call 路由对齐、dev/prod 多环境、凭据占位符化、后端模块扁平化与仓库卫生。

**新增**
- dev/prod 多环境 profile 拆分（仅覆盖 `spring.cloud.nacos.discovery.ip`：dev=127.0.0.1，prod=47.112.114.139）。（`2e5573a`）
- 凭据占位符化：`MYSQL_PASSWORD` / `REDIS_PASSWORD` / `NACOS_USERNAME` / `NACOS_PASSWORD` / `DASHSCOPE_API_KEY` / `SEARCH_API_KEY` 走 `${ENV:default}`。（`2e5573a`）
- **引入 `shiwujie-backend` 父 pom**（`<packaging>pom</packaging>`）：7 模块聚合 + 版本统一（统一 `0.0.1-SNAPSHOT`、dependencyManagement 集中版本）；ai 因 SB 3.4.5 / Java 21 保留直继承 `spring-boot-starter-parent`、仅纳入聚合；root properties 锁 `lombok 1.18.36` 以兼容 JDK 21 reactor 构建。（`91e0998`）

**变更**
- 调整 call 模块路由与 web 代理目标对齐。（`6da3060`）
- **后端模块扁平化**：model / common-web / user / call / community / ai 六模块从 `shiwujie-gateway/` 子目录移至 `shiwujie-backend/` 同级。gateway 原非 aggregator（pom 无 `<modules>`）、各模块 `<parent>` 均指向外部 starter-parent，故纯 `git mv`、**零 pom 改动**，Maven 关系与 Java 包名均不变。（`640c171`）
- 仓库卫生：`.idea/`、`*.iml`、`logs/*.log` 移出 git 跟踪（`--cached` 仅退索引、保留磁盘）；根 `.gitignore` 补全通用 IDE/OS 规则，backend `.gitignore` 上移覆盖全部同级模块并补 `logs/`、`*.log`。（`640c171`）
- 清理 ai pom 重复依赖声明（`jackson-databind` / `org.eclipse.paho.client.mqttv3` 各去一份），消除 Maven `dependencies must be unique` 警告。（`e4b097b`）

**修复**
- **Dubbo provider 端口迁出 Hyper-V/WSL 保留段**：原 50200 / 50300 / 50400 / 50500 全部落入 Windows Hyper-V/WSL2 动态登记的 TCP 排除段，导致 `contextLoads` bind 抛 `Address already in use` 而 `netstat` 查无进程——隐蔽环境坑，重启后段还会变。改用 21200 / 21300 / 21400 / 21500（远离保留段与临时端口区 49152+）。（`0bdbbc5`）

**移除**
- 清理无用文件 `how 3dcf577` 与 `testgit.txt`。（`03e60ea`）

---

### 阶段 8 · 分布式与生产化（约 2026-01）

> Netty→Spring WebSocket、gateway 负载均衡、多机 nacos+dubbo 通信。

**新增**
- gateway 基于 Nacos 服务发现 + Spring Cloud LoadBalancer 轮询负载均衡。（`87c651a`）
- 多服务器间 nacos 与 dubbo 通信配置。（`8a16377`）

**变更**
- **call 模块 WebSocket 从 Netty 实现改为 Spring WebSocket**（`@ServerEndpoint` + javax.websocket）。（`3f88ea7`）
- GateWay 改名、pom 简化。（`da7a6ab`）
- 数据库/redis/nacos 配置统一；ai 提示词修复、拦截器 redis 生效问题修复。（`088b14f`）
- 删除 AI 语气切换功能。（`088b14f`）

**修复**
- ai 模块拦截器 redis 不生效问题。（`088b14f`）
- 修改部署 IP 配置。（`7312d4f`）

---

### 阶段 7 · 引擎升级与稳定性（约 2025-10 ~ 11）

> deepseek、Spring AI Alibaba M6→1.0、工作流定型；**mqtt、RAG、自研 ReAct 经试错后移除/弃用**。

**新增**
- 添加 mqtt 服务（为硬件 IoT 设计）。（`ae5465c` 2025-10-24）
- 使用 deepseek 实现并简化代码（新 App 部分测试）。（`3093cc8` 2025-10-30）
- ai 模块拦截器添加默认用户信息（调试用，**生产后门风险**）；call 工具调用调试信息。（`7737559` 2025-11-09）
- 开启 dubbo 空保护（`empty-protection: true`）。（`d326353` 2025-11-12）

**变更**
- **Spring AI Alibaba 由 M6 升级为 1.0**（两次提交重构）：文字模型与图像模型同时回答；动态配置两模型持久化策略（Redis 低消耗）；提示词改用文档引入；图片追问功能（不占 Redis 空间）；性能调优。（`75e5ace` / `07459f6` 2025-11-18）
- 设置 `spring-data-redis` 配置解决 AI 模块连接问题；取消 dubbo qos。（`7737559`）

**修复**
- AI 模块拦截器 redis 连接问题。（`7737559`）

**移除**
- **mqtt 硬件 IoT 通道**：因硬件成本问题取消，代码删除（**pom 残留 paho 依赖**，见 [shiwujie-backend/docs/modules/ai.md](../shiwujie-backend/docs/modules/ai.md) 已知问题）。（git 历史可溯，代码已无 MqttClient 引用）
- **自研 ReAct Agent**：经大量测试（多种提示词、RAG 注入、Agent 调用），确定工具调用最佳方案为**代码实现工作流**，自研 ReAct 框架弃用、`@Component` 注释（保留代码未启用）。（`07459f6`，见 [shiwujie-backend/docs/modules/ai.md](../shiwujie-backend/docs/modules/ai.md)）
- **RAG 知识库增强**：测试后效果不及工作流，移除接入（`MyRagAdvisor` Bean 半残留，未注入）。（`07459f6`）
- **语气切换功能**。（后续 `088b14f` 彻底删除）

---

### 阶段 6 · AI 能力跃升（约 2025-08-14 ~ 08-20）

> 从「能对话」到「能执行」：避障、高德导航、跳转应用、性能 +50%、图片处理独立 App。

**新增**
- AI 页面集成避障功能。（`220f185` / `9de40fe` 2025-08-14）
- **AI 对话帮助用户执行操作**（工具执行真实设备动作）。（`831b7e0` / `03c5333` 2025-08-16）
- AI 跳转到外部应用。（`33dab95` 2025-08-18）
- 高德导航功能，自动开启步行导航。（`b1e3bef` / `13d311c` / `69ff858` 2025-08-18~20）
- 单独创建图片处理 App，图片追问不占 Redis。（`1a9d4c7` 2025-08-20）

**变更**
- 修改调用架构，Redis 存储分化加速，暂取消图片上下文存储（后由瘦身工程替代）。（`9ea825f` 2025-08-20）
- 图片识别输出结果优化。（`b1e3bef`）

**修复**
- AI 悬浮窗在志愿者端误出现。（`06f623d` 2025-08-18）
- APP 崩溃与前台服务错误造成的崩溃，AI 悬浮球自由跳转。（`065ae26`）
- 紧急求助重复点击 bug、用户端弹窗问题。（`56f87f8`）
- AI 避障功能 bug。（`aaaab82` 2025-08-15）

---

### 阶段 5 · AI 模块从零到能用（约 2025-08-04 ~ 08-13）

> model 子模块独立、Spring AI、向量库、dubbo+工具、流式/TTS/图片识别。

**新增**
- **分开子模块 model，ai 模块初始化**。（`d9a6005` 2025-08-04）
- 修改 SpringBoot AI 版本，基于 redis 存储的 advisor，多模态测试。（`2c80ff9` 2025-08-05）
- 基于内存的向量存储 → 阿里云向量数据库。（`282a52e` / `d5819d0` 2025-08-06）
- deepseek 与千问多模型使用。（`d5819d0`）
- **社区修改内部服务，添加 AI 工具相关服务**（Dubbo Inner 服务接入 AI）。（`797f3a2` 2025-08-08）
- 工具调用、提示词设计。（`b65c7f8`）
- ai 模块两接口，上传图片识别，流式返回。（`a13a3dd` 2025-08-11）
- 基本工具非流式调用。（`e104ed4`）
- 工具调用流式输出（拆解优化）；AI 对话流式输出展示。（`dc4200b` / `3ff5e56` 2025-08-12）
- 集成讯飞 TTS 自动播报。（`5f26639` / `e282427`）
- 拍照识别功能。（`9a8b292` 2025-08-13）
- 基本实现前端联动，加入 socket。（`98a070a`）
- Redis 持久化 + MySQL 异步存储（自研 ChatMemory 双写）。（`1c822ef`）

**变更**
- 调用返回速度提高约 50%（基于 redis 持久化、提示词优化、RAG 搜索）；model 补充序列化。（`4df17f8` 2025-08-12）

**修复**
- ai 接口重复调用 bug；上传文件大小限制 10Mb；Linux 路径不适配；调用 404；图片过大。（`f9398e0` / `e31e3d4` / `eb2fc6b` 2025-08-11）
- 数据库建表索引优化；redis 自动删除多余信息；删除定时任务。（`1a6688e` 2025-08-13）

---

### 阶段 4 · 社区治理（约 2025-07-26 ~ 08-05）

> 社区/审核/管理者/求助帖/活动/报名六大主体。

**新增**
- 社区加入审核、管理员设置、社区增删改查。（`d2d149a` 2025-07-26）
- 社区求助帖、活动功能。（`b5a072e` 2025-07-27）
- web 端社区管理者功能、加入与审核。（`223f66e` / `7a6b398` 2025-08-02~03）
- web 端活动功能、社区用户管理。（`9327fca` / `4afbf0c` 2025-08-04）
- web 端社区完成；APP 活动功能。（`51e35a6` / `ce0a729` 2025-08-05）

**修复**
- 社区注册 bug；web 端社区用户管理 bug。（`049572d` / `4afbf0c`）

---

### 阶段 3 · 视频通话与紧急求助（约 2025-07-21 ~ 08-13）

> call 模块诞生（早期 Netty socket）。

**新增**
- 前端视频实现紧急求助。（`4bae798` 2025-07-21）
- 视频模块不刷新页面使用。（`8f47b46` 2025-08-08）
- 加入心跳包与 APP 自启动。（`078f60d` / `d96a5aa` 2025-08-08）
- 长连接和心跳包完成。（`d96a5aa`）

**修复**
- 视频模块异常记录与 bug。（`8f47b46`）

---

### 阶段 2 · 用户与家庭模块（约 2025-07-24 ~ 08-07）

> 三类用户账号、家庭关系、common 工具类。

**新增**
- 社区注册登录（未处理分布式事务）。（`a81f13a` 2025-07-24）
- 改为公共拦截器代码（common-web 抽取）。（`61f5102` 2025-07-24）
- gateway 配置。（`a650f92` 2025-07-25）
- 社区分页条件查询志愿者/视障人士；加入社区。（`a69c609` 2025-07-26）
- 加入家庭修改。（`8a3d6e9` 2025-08-07）
- 盲人端移动端适配优化。（`68d4c50` 2025-08-07）

**变更**
- 修改社区注册登录返回类、用户模块登录返回类。（`3ee8034` 2025-07-25）

---

### 阶段 1 · 二期微服务脚手架（约 2025-07-21 ~ 08-01）

> 多模块切分、nacos 引入、JWT/Redis 工具骨架、前端视频求助起步。

**新增**
- 前端视频实现紧急求助（起步）。（`4bae798` 2025-07-21）
- 删除多余 sql 文件（一期遗留清理）。（`7d72662` 2025-07-21）
- 志愿者社区雏形。（`c26d203` 2025-08-01）

---

### 阶段 0 · 一期单体封版（约 2025-06-30）

> 演进起点。

**新增**
- 项目一期：uniapp + Spring 单体 + 单库 4 表。（`423c0fa` 2025-06-30）

> 一期代码作为二期微服务演进的对照基线保留在 git 历史，不再迭代。
