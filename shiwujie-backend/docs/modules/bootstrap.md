# bootstrap 模块（单体唯一应用模块）

> v3.0.0 收敛为 **2 模块**：`shiwujie-model`（契约层，见 [model-commonweb.md](model-commonweb.md)）+ `shiwujie-bootstrap`（本文件，唯一 `@SpringBootApplication` 入口）。原 common-web + user/call/community/ai 五模块已于**阶段2.8（`a215d9e`）**并入本模块——同包根 `com.swj.shiwujie.*` 搬迁，不改 import、不改对外契约。用户可见契约（HTTP 路径 / WS 信令 / 业务码 / 返回字段）见 [product/current.md](../../../docs/product/current.md)；架构现状见 [tech-stack.md](../../../docs/architecture/tech-stack.md)。

## 模块定位

| 模块 | 类型 | 职责 |
|---|---|---|
| `shiwujie-model` | jar（无 Spring） | 契约层：domain/enums/request/VO/常量 + `Inner*` 接口（见 [model-commonweb.md](model-commonweb.md)） |
| `shiwujie-bootstrap` | 可执行 fat jar（SB 3.4.5/Java21，唯一 repackage） | 唯一 app：聚合原 common-web 公共层 + user/call/community/ai 全部业务代码。`@SpringBootApplication(scanBasePackages="com.swj.shiwujie")` + `@MapperScan("com.swj.shiwujie.mapper")` + `@EnableAsync` |

## 启动与部署

- 端口 **8100**（复用原 gateway），context-path `/`，单 datasource 指向 `shiwujie` 库，Redis db=2。无 Nacos/Dubbo。
- 构建：`mvn -f shiwujie-backend/pom.xml install -DskipTests`（2 模块 reactor）产 `target/shiwujieBootstrap-0.0.1-SNAPSHOT.jar`，`java -jar` 即起（启动级回归约 8s）。详见 [deployment.md](../deployment.md)。

## 子包结构（`com.swj.shiwujie.*`）

bootstrap 吸收的五模块按原职责落在以下子包（**包名即原模块划分，搬迁未改包名/类路径**，故所有 import 零变更）：

### 公共层（← 原 common-web）`common/ config/ constants/ exception/ interceptor/ utils/`

- `common/{BaseResponse, ErrorCode}`：统一响应信封与业务码（`NOT_LOGIN` / `NO_AUTH` / `PARAMS_ERROR` 等）。
- `config/WebConfig`：注册 `LoginCheckInterceptor`（作用域 `/api/user/**` + `/api/call/**` + `/api/community/**`）+ 全局 CORS；`config/RedisTemplateConfig` Redis 序列化。
- `interceptor/LoginCheckInterceptor`：JWT 校验 + Redis token 比对 + 滑动续期（90 天）。
- `exception/{BusinessException, GlobalExceptionHandler, ThrowUtils}`：业务异常 → `BaseResponse` 统一兜底。
- `utils/{JwtUtils, RedisUtils, LoginUtils, ResultUtils, ConverterUtils}`：HS256 签发/校验、Redis TTL、登录上下文等。

### user（用户/家庭/志愿者）`controller/ service/ service/impl/inner/ mapper/`

- 盲人 / 志愿者 / 家庭 / 家庭加入审核的注册登录与 CRUD。
- 对外路径：`/api/user/blind/**`、`/api/user/volunteer/**`、`/api/user/family/**`、`/api/user/familyJoinReview/**`（原 context-path 前缀下沉到 controller 类级 `@RequestMapping`，阶段2.4）。
- `Inner{Blind,Volunteer,Family}Service`（`service/impl/inner/`）：跨模块本地调用契约（v2.1.0 为 Dubbo，v3.0.0 同进程注入）。
- mapper XML：`BlindMapper` / `FamilyMapper` / `FamilyJoinReviewMapper` / `VolunteerMapper`（4 个，`resources/mapper/`）。

### call（视频/紧急求助 + WebSocket）`controller/ service/ socket/ mapper/ constants/`

- 紧急求助 / 视频求助的发起、匹配、就绪、AI 联动通知；实时 WebSocket 协调。
- 对外路径：`/api/call/urgenthelp/**`、`/api/call/videohelp/**`；WebSocket **`/api/ws/call`**（`@ServerEndpoint`，jakarta；`socket/CoordinationSocketHandler` + `static sessionMap`，12 信令码 `-1/0/1/2/3/4/5001~5006`）。
- `socket/inner/InnerSocket`：AI → 前端推送的本地调用契约（6 类推送）。
- mapper XML：`UrgenthelpMapper` / `VideohelpMapper`（2 个）。

### community（社区/活动/求助帖/审核）`controller/ service/ mapper/`

- 社区 / 社区管理员 / 活动 / 活动报名 / 求助帖 / 社区加入审核的 CRUD + 级联（删志愿者 / 删社区单事务，阶段2.6）。
- 对外路径：`/api/community/{community,communitymanager,activity,helppost,communityjoinreview}/**` 等。
- `Inner{Community,Communityjoinreview,Communitymanager}Service`：跨模块本地调用契约（原 3 个无消费者冗余 Inner 已删，阶段2.2）。
- mapper XML：`ActivityMapper` / `ActivitysignMapper` / `CommunityMapper` / `CommunityjoinreviewMapper` / `CommunitymanagerMapper` / `HelppostMapper`（6 个）。

### ai（旧 Java AI 实现）`agent/ + 依赖` —— **已彻底删除（chunk-2b / 2b-5 / 2b-6b）**

> 旧 Java AI 实现**已大部删除**：`app/`、`controller/ChatController`、`service/ChatServiceImpl`、`tools/`（ToolChoiceCenter + `app/*` + `mytools/*`）、`chatmemory/*`、`advisor/MyRagAdvisor`、AI 专用 common DTO（`AiToolRequest`/`ToolCallRequest`）、`AiSmokeTest`、`resources/prompttemplate/*`，替换为独立 Python LangGraph 服务（polyglot 双进程）。设计全貌见 [architecture/ai-rewrite.md](../../../docs/architecture/ai-rewrite.md)，Python 服务实现见 [shiwujie-ai/docs/](../../../shiwujie-ai/docs/)。重写后 bootstrap 角色：**从「自带 AI 实现」变为「网关 + MCP server」** —— Java 保留 WS 终结点 / JWT 鉴权 / user/call/community 业务真相源，并**新增 MCP server**（`mcp/*`，streamable HTTP，业务 4 工具 `join_family`/`leave_family`/`family_info`/`update_profile` + 信令 4 工具 `launch_navigation`(5006)/`request_video_help`(5002)/`request_emergency_help_prepare`+`_confirm`(5003，turn-bound token 拆分，2b-4b)/`open_app`(5004)）+ **缝 A WS 中继**（`ai/relay/*`）供 Python 调用。
>
> **保留**（冻结 / 待删）：
> - ~~`agent/*`（MyManus 自研 ReAct 雏形）冻结保留~~ **已彻底删（chunk-2b-6b，2026-07-20，撤销原冻结保留决策）** —— 原作 Java-graph 备选 B-prime 回退起跑线冻结保留，撤销理由：零活路径引用、B-prime 用 alibaba-graph 非自建 ReAct、红队 Q2 揭「已弃用」真相；连带删依赖 `advisor/MyLoggerAdvisor`/`config/AiConfig`/`constants/AiConstants`/`utils/MessageSerializer`（kryo 孤儿）= 9 文件 + bootstrap pom 5 依赖（`spring-ai-alibaba-starter-dashscope`/`spring-ai-openai`/`jsoup`/`kryo`/`paho`）+ 父 pom 4 DM + `application.yml` 3 块。
> - `interceptor/AiLoginCheckInterceptor` + `config/AiWebConfig` **已删（2b-6a，2026-07-20）**——dev 后门死代码清理（无 Authorization 静默登录 blind id=1）；`/api/ai/**` 在 2b-5 删 `ChatController` 后已成空集，拦截器拦空集，删除零功能影响；`common/ErrorCode` **保留**（53 个其它引用者）。WS phone 冒充（known-issues #7）的 ticket 鉴权留 chunk-2e 与 Android WS 改造同批。
>
> 历史快照（已删类，作「被替换对象」参考）：DashScope qwen3 对话（SSE 流）/ 工作流式工具路由（`app/ToolChoiceApp`）/ 自研 ChatMemory（Redis 精简 + MySQL 全量 kryo）/ 网页搜索（searchapi + jsoup）；3 个对外 SSE 端点 `/api/ai/ai/{doChatByText,doChatByImage,NewApp}` 随 ChatController 删除，AI 通道迁 WS `/api/ws/call` AI-turn 消息。缺陷明细见 [known-issues.md](../known-issues.md) ai 节。

- mapper XML：`AiLogsMapper`（1 个，待 Phase 5 定去留）。资源：`logback-spring.xml`（`application.yml` 的 `logging.config` 指向）；`prompttemplate/*` 已删（2b-5）。
- spring-ai BOM / spring-milestones 仓库随 ai 并入迁**父 pom**集中管理（阶段2.8）；chunk-2b-6b（2026-07-20）后仅留 `spring-ai-bom` 1.1.0（托管 MCP server starter），`spring-ai-alibaba-bom`/`dashscope`/`openai`/`jsoup`/`kryo`/`paho` 随旧 AI 模块 + MyManus 死代码闭环全删。

## 跨模块本地调用（原 Dubbo 接缝点）

v3.0.0 阶段2.2（`199e6f3`）`@DubboService`→`@Service`、`@DubboReference`→`@Resource`，接口定义留 `shiwujie-model`。完整契约清单见 [gateway-dubbo.md](../../../docs/architecture/gateway-dubbo.md)「Dubbo 接口契约清单」。模块间双向耦合（社区↔志愿者↔社区管理员级联删除）以 `spring.main.allow-circular-references: true` 放开，登记 [known-issues.md](../known-issues.md)。

> 历史（v2.1.0）各模块独立端口 8200/8300/8400/8500、独立库、Dubbo RPC 的架构见 [gateway-dubbo.md](../../../docs/architecture/gateway-dubbo.md) 历史段。
