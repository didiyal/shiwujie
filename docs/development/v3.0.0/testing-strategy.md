# v3.0.0 测试策略

> 本文件属 `development/v3.0.0/`（单体化改造·进行中）。测试基线**继承 v2.1.0**（见 [../v2.1.0/testing-strategy.md](../v2.1.0/testing-strategy.md)，已封版 2026-07-11），本版本补充单体化特有的验证点。版本指针 [../current.md](../current.md)。

## 继承基线

v2.1.0 的测试现状 / 环境表 / 验证点 / 缺口继续适用，见 [../v2.1.0/testing-strategy.md](../v2.1.0/testing-strategy.md)。

## 自动化单元测试（2026-07-12 新增，ai 外）

> v2.1.0 基线「后端无自动化测试」自本版本起**部分打破**：给 ai 模块外的后端补了一层纯单元测试。决策（用户）：**纯 Mockito**——不起 Spring 上下文、不连 DB/Redis、**零新依赖**（`spring-boot-starter-test` test scope 已在 bootstrap pom，提供 JUnit5 + Mockito5 inline mock maker + AssertJ）；覆盖优先级「**先深耕安全热路径再铺广**」——优先给本轮安全加固动过的代码（口令 BCrypt、JWT、`LoginCheckInterceptor`、community/helppost 权限、登录注册）补回归网，再铺其余 Service。

**现状**：20 个测试类 / 286 例，`mvn -f shiwujie-backend/pom.xml test` 全绿（0 failures / 0 errors）。

| 域 | 测试类 | 例数 | 覆盖点 |
|---|---|---|---|
| utils/common/exception | `PasswordUtilsTest` / `JwtUtilsTest` / `LoginUtilsTest` / `ResultUtilsTest` / `ConverterUtilsTest` / `ThrowUtilsTest` / `ErrorCodeTest` | 64 | BCrypt `hash`/`matches`/`isLegacyMd5`、HS256 签发校验、登录上下文（MockedStatic `RequestContextHolder`）、`ThrowUtils` 分支、`ErrorCode` |
| interceptor | `LoginCheckInterceptorTest` | 14 | `preHandle` 全分支：OPTIONS/登录类 URL 放行、无头/坏 token/Redis 空/不匹配 → `NOT_LOGIN`、成功注入 blind/volunteer |
| user | `BlindServiceImplTest` / `VolunteerServiceImplTest` / `FamilyServiceImplTest` / `FamilyJoinReviewServiceImplTest` | 65 | 登录注册（BCrypt + 存量 MD5 懒升级）、改密、token 签发、家庭 CRUD/加入审核 |
| community | `CommunityServiceImplTest` / `CommunitymanagerServiceImplTest` / `HelppostServiceImplTest` / `ActivityServiceImplTest` / `ActivitysignServiceImplTest` / `CommunityjoinreviewServiceImplTest` | 110 | 社区登录、删改权限（注册人相等）、社区管理员 CRUD（末位注册人护栏）、求助帖删改（作者/管理员）、活动/签到/审核 CRUD |
| call | `UrgenthelpServiceImplTest` / `VideohelpServiceImplTest` | 33 | 紧急/视频求助创建/加入/退出、Redis 队列匹配 |

**MyBatis-Plus 3.5.9 单测三坑（已固化进模板，新增 Service 测试照抄）**：

1. `ServiceImpl<M,T>` 继承的 `baseMapper` 字段 `@InjectMocks` 不会注入 → `@BeforeEach` 用 `ReflectionTestUtils.setField(service, "baseMapper", mapperMock)` 手填，否则 `MybatisPlusException: baseMapper can not be null`。
2. `this.getOne(QueryWrapper)` 经 `AbstractServiceImpl` 调的是**两参** `baseMapper.selectOne(qw, true)` → stub 须 `when(mapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(...)`（裸 `selectOne(any())` 漏第二参 → `PotentialStubbingProblem`）。
3. `insert(T)`/`insert(Collection<T>)`、`updateById(T)`/`updateById(Collection<T>)`、`deleteById(Serializable)`/`deleteById(Object,boolean)`/`deleteById(T)` 多态重载 → 裸 `any()` 编译歧义或运行期命中错重载，须 typed matcher `any(Entity.class)` / `anyLong()`。

**暂未覆盖（后续）**：ai 模块（用户明确排除，待 Agent 重写后另立）；`@SpringBootTest` 集成测试需测试专用 DB/Redis profile 或 H2 / embedded-redis / Testcontainers，本轮不引入。

## 契约保护铁律（v3.0.0 单体化硬约束）

单体化是工程架构内部重构，**对外行为零变更**。下列契约在阶段 1/2 完成后必须逐一回归通过，前端 App/Web 不改即可对接：

- **HTTP 路径不变**：`/api/user/**`（blind / volunteer / family / familyJoinReview）、`/api/call/**`（videohelp / urgenthelp）、`/api/community/**`（community / communitymanager / communityjoinreview / activity / activitysign / helppost）、`/api/ai/**`（doChatByText / doChatByImage / NewApp，SSE）。
- **WebSocket `/api/ws/call` 不变**：12 信令码 `-1`(心跳) / `0`(bind) / `1`(匹配成功) / `2`(就绪) / `3`(紧急通知) / `4`(取消) / `5001~5006`(AI 联动：拍照 / 视频求助 / 紧急求助 / 跳应用 / 跳编辑 / 导航) + `SocketData`(requestType / blindPhone / volunteerPhone / channelId) / `SocketVO`(code / message / socketData) 消息格式原样。
- **状态码 / 字段名不变**：HTTP 状态码、业务码（`NOT_LOGIN` / `NO_AUTH` / `PARAMS_ERROR`）、`BaseResponse` 字段名。

## v3.0.0 验证点（随两阶段落地滚动补充）

> **当前状态（2026-07-11）**：阶段 1 全 reactor `mvn install` 全绿、各模块 `contextLoads` 全绿（新 nacos-client 2023.0.1.0 修好 JDK21 坑）；阶段 2 单 jar **启动级验证通过**——8.8s 干净启动、4 模块 HTTP 路由全注册且 `/api/{user,call,community,ai}/**` 前缀对（OpenAPI 文档实测）、`BaseResponse` 信封 + 业务码 `40010 NOT_LOGIN` 不变、WS `/api/ws/call` 握手 101。**功能级回归待 App/Web 手动联调**：下方「阶段 2」清单中的 WS 12 信令往返 / 跨模块本地调用 / 事务级联 / 鉴权滑动 / 合库字段映射 / AI SSE 仍需带 token 客户端起栈验证。
>
> **增量（2026-07-12，用户本地实测）**：本地单 jar 启动 ✅、WS 视频求助信令往返 ✅、事务级联回归 ✅（删志愿者/删社区单 `@Transactional` 一致性确认，配方见下方「事务回归配方」；`deleteVolunteer` 的 synchronized/@Transactional 边界错位属已登记技术债，见 [known-issues](../../../shiwujie-backend/docs/known-issues.md)）；AI SSE ⏭️ 跳过（即将新开发）；token 滑动会话续期 + 合库字段映射 ⏳ 待补。

**阶段 1（升 SB 3.4.5/Java21，仍微服务）**：

- 每模块 `mvn compile`（JDK21）reactor 全绿；`contextLoads` 全绿（新 nacos-client 2023.0.1.0 修好 JDK21 坑，不再切 JDK17）。
- jakarta 迁移回归：登录 → 受保护接口（拦截器 `jakarta.servlet` + Redis token 比对 + 滑动续期）跑通。
- call WebSocket：`jakarta.websocket` import 后 `@ServerEndpoint("/ws/call")` 连接 + 信令往返正常（生产 Tomcat10 容器；MOCK 环境 `ServerEndpointExporter` 行为单独确认）。
- knife4j openapi3：文档页可访问，83 处注解映射正确。

**阶段 2（合并单体 + 合库）**：

- 单端口入口回归：原经 gateway 分发到 4 服务的链路，合并为单体统一入口后功能不变（按上述 HTTP 路径契约逐条回归）。
- 跨模块调用改本地调用回归：原 Dubbo Inner 调用改为同进程方法调用，幂等性与异常透传一致。
- 事务回归：删志愿者级联清家庭/社区、删社区清成员——单 `@Transactional` 一致性（中途异常无脏数据残留，原先跨库 + synchronized 无保证）。
- 鉴权回归：4 处 `LoginCheckInterceptor` 收敛为 common-web 1 处后，鉴权行为（含 v2.1.0 修复的 token 滑动会话续期 + 删用户清 token）不回归。
- 合库回归：13 表数据完整导入 `shiwujie`；call 实体 snake→camel 后查询/写入字段映射正确（DB 列名未变）。
- AI 回归：多模型对话 + 工具路由 + 流式 SSE + ChatMemory（Redis + MySQL 双写）正常。

## 事务回归配方（删志愿者 / 删社区，2026-07-12 补）

> 单体化阶段 2.6 把原跨库写场景（社区入驻/审核/删志愿者/删社区）升为单 `@Transactional(rollbackFor=Exception.class)`。下表为收尾 2.7 的事务级联回归配方，本地起栈逐项过。

| 场景 | 步骤 | 期望 |
|---|---|---|
| 删志愿者·happy | 删一个**有家庭 + 是社区注册人**的志愿者 | 家庭成员 `familyId` 清空、级联删社区、token 清掉、无残留 |
| 删志愿者·回滚 | 在 `VolunteerServiceImpl.deleteVolunteer` 的 `removeById` 前临时抛异常 | 之前清的 `familyId` / 删的社区**全部回滚不残留** |
| 删社区·happy | 删一个有成员的社区 | 盲人/志愿者 `communityId` 清空、管理记录删、社区删 |
| 删社区·回滚 | 在 `CommunityServiceImpl.deleteCommunity` 的 `removeById(communityId)` 前临时抛异常 | 前三步级联（清 communityId ×2 + 删管理记录）全部回滚 |

> 已确认（静态审计 2026-07-12）：两条级联方法均 `@Transactional` + public + 经代理调用 → 事务激活；`rollbackFor=Exception.class` 覆盖 `BusinessException`；内部 `removeCommunityId` / `removeByCommunityId` 不吞异常 → 单线程级联回滚正确。唯一技术债：`deleteVolunteer` 的 `synchronized` 边界错位（见 [known-issues](../../../shiwujie-backend/docs/known-issues.md)），不阻塞封版。

## AI 重写测试关系（设计敲定·待 Phase 5）

> **状态：设计敲定（Phase 1–4）· 实现待 Phase 5。** AI 重写（现有 Java AI 模块 → Python 自建 ReAct loop 智能体）的测试范围与现有单体化测试基线的关系，详见 [task-breakdown](task-breakdown.md)「AI 重写」3.9 子任务与 [architecture/tech-stack](../../architecture/tech-stack.md) AI 重写段。本节实现尚未落地，下列 `[ ]` 全部待办。

**对现有基线的影响（零回归保证）**：

- **20 单测类 / 286 例（纯 Mockito，零 AI 引用）不动保绿**——AI 模块本就被用户明确排除在单测覆盖外（见上方「自动化单元测试」「暂未覆盖」），删 Java AI 模块（`app/agent/tools/advisor/chatmemory` + `AiConfig`/`AiConstants`/`ChatServiceImpl`/`ChatController`）不触这 286 例任何一例；`mvn -f shiwujie-backend/pom.xml test` 期望依旧 0 failures / 0 errors。
- **`AiSmokeTest` 删除**（throwaway 冒烟，非回归资产）。
- **「打 DashScope」价值迁移**：原 Java 侧打真实 DashScope 的连通验证，价值迁到 **Python pytest**——既做 mock 单测（工具/节点），又复用为真实 **qwen function-calling spike**（前置 spike：本工具集 + 本 prompt 通过率，建议 >=90%，见 [task-breakdown](task-breakdown.md) AI 重写前置 spike）。

**新增测试（全 `[ ]`，待 Phase 5 落地）**：

| 域 | 测试 | 覆盖点 |
|---|---|---|
| Java WS 契约 | AI-turn roundtrip（mock Python） | 缝 A：App<->Java WS 新 AI-turn 入站消息类型 + 流式 token 帧出站往返；ticket 鉴权（堵 phone 冒充）；`getAsyncRemote()` 非阻塞推送；拦「收到客户端的数据」回显（AI 类消息不发） |
| Python tool | 工具单测（mock 外部 API） | 6 native（`recognize_photo`/`web_search`/`get_weather`/`gaode_poi_search`/`gaode_route`/`search_kb` BM25）+ `read_skill` + 高德 3 wrapper 出参剪裁（盲人朗读友好） |
| Python loop | 集成测试（mock LLM） | 标准环（入口分流→调模型 FC→执行 tool_calls→回环）；checkpoint 恢复（key=`blind_id`，崩溃/中途截止续跑）；HITL 两处（导航交通方式 / 紧急确认门）自然 turn + checkpoint |
| 安全门 | 紧急确认 token | `request_emergency_help` 拆 `prepare()`/`confirm()`；token 绑 `(blind_id, thread_id, issuing_turn)`，`confirm()` 拒同轮 token；`parallel_tool_calls=False` 堵单轮并行 prepare + 伪造 confirm；App 非-MCP HTTP 端点消费 token（第三道门） |
| 安全门 | `update_profile` 字段门 | MCP inputSchema 硬卡 `{nickname, phone, gender}`；Java 窄 DTO（非泛 `Blind`）+ 单测断言 DTO 无 `password`/`idCard`/`disabilityCard` setter（防约定腐烂） |
| 安全门 | tool-name 白名单 | MCP 服务端拒未注册工具名（堵 LLM 幻觉名冒充 `confirm`）；strict JSON-schema 校验 |

**两条护栏（无论 qwen FC spike 结果都上）**：MCP 服务端 strict JSON-schema 校验 + tool-name 白名单——即Decision A 依赖 qwen FC 稳，但即使 spike 通过率达标，这两条护栏仍强制上，作 FC 不稳时的兜底。
