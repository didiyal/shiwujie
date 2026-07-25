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

### ai（AI 对话 / SSE / 工具路由 / 记忆）`advisor/ agent/ app/ chatmemory/ controller/ service/ mapper/ tools/ utils/ constants/ config/`

- DashScope qwen3-max / qwen3-vl 对话（SSE 流）、工作流式工具路由（`app/ToolChoiceApp`）、自研 ChatMemory（Redis 精简 + MySQL 全量，kryo 序列化）、网页搜索（searchapi + jsoup）。
- 对外路径：`/api/ai/ai/doChatByText`（SSE）、`/api/ai/ai/doChatByImage`（SSE）、`/api/ai/ai/NewApp`（SSE）。
- `config/AiWebConfig`：注册 `AiLoginCheckInterceptor`（仅 `/api/ai/**`，ai 鉴权行为独立——dev 默认用户兜底属 🔴 安全加固待修项，见 [known-issues.md](../known-issues.md)）。
- mapper XML：`AiLogsMapper`（1 个）。资源：`logback-spring.xml`（`application.yml` 的 `logging.config` 指向）、`prompttemplate/{image,text,toolChoice}-template.txt`。
- spring-ai BOM / 版本属性 / spring-milestones 仓库随 ai 并入迁**父 pom**集中管理（阶段2.8）。

## 跨模块本地调用（原 Dubbo 接缝点）

v3.0.0 阶段2.2（`199e6f3`）`@DubboService`→`@Service`、`@DubboReference`→`@Resource`，接口定义留 `shiwujie-model`。完整契约清单见 [gateway-dubbo.md](../../../docs/architecture/gateway-dubbo.md)「Dubbo 接口契约清单」。模块间双向耦合（社区↔志愿者↔社区管理员级联删除）以 `spring.main.allow-circular-references: true` 放开，登记 [known-issues.md](../known-issues.md)。

> 历史（v2.1.0）各模块独立端口 8200/8300/8400/8500、独立库、Dubbo RPC 的架构见 [gateway-dubbo.md](../../../docs/architecture/gateway-dubbo.md) 历史段。
