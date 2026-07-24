# 后端已知问题与技术债

> 后端技术债 / 缺陷登记。development 层（允许源码引用 `file:line`）。🔴 = 安全漏洞或高危，已同步进 [ROADMAP.md](../../docs/ROADMAP.md) 待实现「安全加固」作必办；✅ = 已修复（保留作历史）。鉴权链路总览见 [architecture/auth.md](../../docs/architecture/auth.md)。

## 🔴 安全漏洞（必办）

1. **ai 默认用户兜底（生产后门）**：`LoginCheckInterceptor`（ai 模块，line 52-60）无 Authorization 时注入 blindId=1 / phone=19872250169。生产未关闭则任何人可白嫖 AI（消耗 DashScope token）。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #6。
2. ✅ **community 权限检查（部分修复 2026-07-12，`fix/v3.0.0-security-hardening`）**：求助帖删改、社区修改/删除、社区管理员增删改的权限检查已恢复（仅作者/注册人/管理员，员工无权，+末位注册人护栏；`CommunitymanagerController.deleteCommunityManager` 自删 bug 一并修正）。**仍待办（同类未网关）**：Activity add/update/delete 无作者/角色校验、Communityjoinreview 审核无角色校验、Activitysign add 无调用者校验。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #9。
3. ✅ **续期 key 拼接 bug（已修复 2026-07-10）**：`renewKey`/`expire` 曾拼的 key **少了 `-blind-`/`-volunteer-` 段**（`LoginCheckInterceptor` 续期、`deleteBlind`/`deleteVolunteer` 删 token 均漏前缀；登录存/注销删/拦截器读则正确）→ 滑动会话静默失效，活跃用户 90 天后被踢；删用户旧 token 残留。**修复**：user/call/community 三份拦截器提取共享 `redisKey`（读/续期共用）杜绝拼接分叉，续期对齐登录 90 天；`BlindController:143`/`VolunteerServiceImpl:410` 删 token 补回前缀。ai 模块同型 bug 暂未处理。原风险见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #1。
4. **JWT 过期校验被关闭**：`validateToken(..., true)` 第三参 `ignoreExp=true`，JWT 自身 exp 永不生效，过期完全依赖 Redis TTL。Redis 故障/误写则 JWT 形同永久有效。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #2。
5. **TOKEN_SECRETKEY 硬编码且弱**：密钥即字符串 `"TOKEN_SECRETKEY"`，明文在共享 model 模块，HS256 弱密钥有离线爆破/伪造 token 风险。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #3。
6. ✅ ~~MD5 存密码、无加盐~~（2026-07-12 修复）：改 BCrypt（Hutool `BCrypt` cost=10，盐内嵌），新增 `utils/PasswordUtils`；盲人/志愿者/社区登录注册与改密全改 BCrypt，存量无盐 MD5 行登录通过即懒升级。身份证/残疾证 PII 哈希仍 MD5（与口令无关）。原风险见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #4。
7. **/ws/call 未在鉴权白名单**：`WebConfig.excludePathPatterns` 不含 `/ws/call`，`@ServerEndpoint` 走独立容器 → **WS 实际绕过 JWT**。任何人构造 `{requestType:0, volunteerPhone:"任意号"}` 即可冒名 bind，接收他人求助通知。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #8。
8. **URL 放行规则过宽**：`url.contains("loginAndRegister")`/`contains("Login")` 按子串放行未限定 path，任何含该子串的路径都绕过鉴权。建议 AntPathMatcher。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #7。
9. ✅ **改密接口账户接管（2026-07-12 修复，`fix/v3.0.0-security-hardening`）**：`BlindController.updateBlindPassword`/`VolunteerController.updateVolunteerPassword` 原仅校验请求体非空，**不校验登录人 == body.blindId/volunteerId**；且 service 侧原密码校验整段包在 `if(StrUtil.isNotBlank(originPassword))` 内——**originPassword 留空即跳过原密码校验**。组合利用：任意登录用户传 `{blindId:<受害者>, newPassword:"abc123", originPassword:""}` 即改任意账号密码（完整账户接管原语）。**修复**：controller 两处加 ownership 校验（`LoginUtils.getLoginBlindId/getLoginVolunteerId` 与 body 比对，不等即 `NO_AUTH`）；service 侧对**已设密码**用户要求 originPassword 必填且 `PasswordUtils.matches` 通过，仅**当前确实无密码**（快注册首次设密）用户可免 origin（此时所有权已过，无原密码可比对）。新增 `VideohelpServiceImplTest.notInRedis_throws` 等回归。属 P0 审查新发现，非历史登记项。

## 鉴权链路（✅ v3.0.0 收敛为 common-web 单份）

> v2.1.0：`{user, call, community, ai}/.../interceptor/LoginCheckInterceptor.java` 各一份——user/call/community 三份几乎逐字相同（仅 URL 放行规则略异）；**ai 那份完全不同**（jakarta 命名空间、直调 RedisTemplate、`@DubboReference InnerBlindService` 拉 Blind 实体、无 token 时 fallback 测试用户）。应抽到 common-web 统一份，但被 SB2/SB3 割裂阻挡。
>
> **v3.0.0 已解**（阶段2.5 `35b81ed`）：SB 统一为 3.4.5/jakarta 后，4 份 `LoginCheckInterceptor` 收敛为 common-web 单份，ai 删自带副本改用之。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #5。

## 各模块缺陷

### user

1. **审核权限校验不完整**：`updateFamilyJoinReview` 仅比对 `reviewerId == loginVolunteerId`（reviewerId 来自前端入参），**未校验登录人是否为该家庭 creator** → 任一志愿者伪造 reviewerId 可审核他人家庭。
2. **`deleteBlind`/`deleteVolunteer` 权限空洞**：注释「管理员可用」，实际无角色/本人校验，任何登录用户传 id 即可逻辑删任意人。
3. **`getBlindVOById` 顺序错误**：先 getById→getBlindVO→之后才判 null，「用户不存在」校验形同虚设，返回空 VO。
4. **`createFamily` 用 GET 触发写**：`GET /family/add` 违反 REST 语义。
5. **`joinFamily` 未校验「已加入他家庭」**：申请人已有 familyId 仍可再申请并通过，原家庭关系静默丢失。

### call

1. ✅ ~~netty-all 冗余残留依赖~~：阶段1.5（`af162c9`）删除 `netty-all:4.1.50.Final`（call 源码零 Netty 引用，阶段 8 已迁 Spring WebSocket，SB3.4.5 BOM 自带所需）。
2. **🔴 sessionMap 并发隐患（高危）**：`sessionMap`/`sessionPhoneMap` 是普通 `HashMap`，javax.websocket 每连接独立线程触发 onOpen/onMessage/onClose/onError → 并发 put 可致链表环/数据丢失；复合操作 TOCTOU。应改 `ConcurrentHashMap`。
3. **5xxx 失败分支不抛错**：用户不在线时 noticeXxx 仅打 log，返回值被丢弃 → ai 侧仍收到「成功」，前端实际未收到。应抛 `BusinessException` 透传给 ai 重新生成回复。
4. ✅ ~~`removeVolunteerFromVideohelp` 潜在 NPE~~（2026-07-12 修复，`fix/v3.0.0-security-hardening`）：Redis 无队列 key 时 `queue` 为 null，下一行 `queue.contains` 直接 NPE（原登记为「潜在」，实为必现）。**修复**：`!hasKey` 分支直接抛 `PARAMS_ERROR("您不在匹配之中,无法取消匹配")`，不再碰 queue；新增 `VideohelpServiceImplTest.notInRedis_throws` 回归。
5. ✅ ~~Redis 队列 TTL 单位错误（30 天，非 30 秒）~~（2026-07-12 修复，`fix/v3.0.0-security-hardening`）：原登记「30s TTL 与匹配窗口错配」**单位订正**——`RedisUtils.setToRedis` 旧重载硬编码 `TimeUnit.DAYS`，`VideohelpServiceImpl` 传 `30L` 实为 **30 天**：志愿者入队后关 App，其 id 在队列头滞留一个月，每个盲人 `joinVideohelp` 都 `poll()` 到僵尸 id → 建 HELPING 行 + WS 推送离线用户 → 盲人侧失败。**修复**：`RedisUtils` 新增 `setToRedis(key,value,timeout,TimeUnit)` 重载（旧重载委托 DAYS 保兼容），`CallConstant.VOLUNTEER_QUEUE_TTL_SECONDS=30L`，三处队列写回改传 `TimeUnit.SECONDS`；token 等 String 值仍走 DAYS 不受影响。
6. ✅ ~~逻辑删除字段命名~~：阶段2.6（`61cb18a`）全局 `map-underscore-to-camel-case: true` + `logic-delete-field: isDelete`，call 实体改 camelCase（`isDelete`），与 user/community 一致；DB 列名 `is_delete` 由下划线映射匹配，逻辑删除生效不再有歧义。

### community

1. ✅ ~~Controller 层直接注入跨模块 Service~~（2026-07-12 核实/修复，`fix/v3.0.0-security-hardening`）：`CommunitymanagerController` 现仅注入 `CommunitymanagerService`，`deleteCommunityManager` 走 service 层（不再直注 `InnerVolunteerService`）。v2.1.0 为 `@DubboReference`，v3.0.0 阶段2.2 改 `@Resource`，本审查期确认 Controller 已无 Inner 直调。
2. **权限模型未落地**：3 角色 `CommunityRolePermissionEnum` 存在但无接口按 roleId 鉴权；`CommunityRolePermission` 表存在但无运行时判定；审核接口仅校验登录态。
3. **命名残留**：`ActivityStatusEnum` 内部字段叫 `postStatus`（从 PostStatusEnum 复制未改名），`END_HELP`/`FALL` 名字残留自 Post。
4. **社区创建无人工审核**：`communityRegister` 直接 `setCommunityStatus(1)`，SQL 注释的「0-未审核」状态未走审核流。
5. **Activitysign 缺签到/签退接口**：表有 check_in/out 字段但无对应更新方法；未校验 max_participants / activity_status。

### ai（试错-移除残留 + 其它发现）

> v3.0.0 AI 重写（Phase 5 进行中）：旧 Java AI 模块**已大部删除（chunk-2b / 2b-5，2026-07-19）** —— 删 `app/`（TextApp/ImageApp/ToolChoiceApp + `app/model/*`）、`controller/ChatController` + `service/{ChatService,impl/ChatServiceImpl}`、`tools/`（ToolChoiceCenter + `tools/app/{FrontendTools,UserTools}` + `tools/mytools/{WebSearchTool,AiModelTools,TerminateTool}`）、`chatmemory/*`（3 个 ChatMemoryRes）、`advisor/MyRagAdvisor`、`common/{AiToolRequest,ToolCallRequest}`、`AiSmokeTest`、`resources/prompttemplate/*`，替换为 Python LangGraph 服务（见 [architecture/ai-rewrite.md](../../docs/architecture/ai-rewrite.md) 与 [shiwujie-ai/docs/](../../shiwujie-ai/docs/)）。下列 ai 缺陷随模块删除消灭（保留作历史真相）。
>
> **保留集**（冻结 / 待删 / 新增）：
> - ① ~~`agent/*`（MyManus 自研 ReAct 雏形）冻结保留~~ **已彻底删（chunk-2b-6b，2026-07-20，撤销原冻结保留决策）** —— 原作 Java-graph 备选 B-prime 回退起跑线冻结保留，撤销理由：零活路径引用、B-prime 用 alibaba-graph 非自建 ReAct、红队 Q2 揭「已弃用」真相；连带删依赖 `advisor/MyLoggerAdvisor`/`config/AiConfig`（`qwenText`/`qwenImage` bean）/`constants/AiConstants`/`utils/MessageSerializer`（kryo 孤儿）= 9 文件 + bootstrap pom 5 依赖（`spring-ai-alibaba-starter-dashscope`/`spring-ai-openai`/`jsoup`/`kryo`/`paho`）+ 父 pom 4 DM + `application.yml` 3 块（`spring.ai.dashscope`/`spring.ai.chat.client`/`search-api`）。⚠️ 历史侦查记录：MyManus 的 `ToolCallback[]` 是构造器参数外部传入、`@Component` 注释掉无实例化点，**不**注入 FrontendTools/UserTools 等——此即 tools/* 早在 2b-5 可全删的依据。
> - ② `interceptor/AiLoginCheckInterceptor` + `config/AiWebConfig` **已删（2b-6a，2026-07-20）**——dev 后门死代码清理（无 Authorization 静默登录 blind id=1 / phone 19872250169）；`/api/ai/**` 在 2b-5 删 `ChatController` 后已成空集，拦截器拦空集，删除零功能影响；`common/ErrorCode` **保留**（53 个其它引用者，非 AI 专用）。WS phone 冒充（下条 #7）的 ticket 鉴权留 chunk-2e 与 Android WS 改造同批。
> - ③ **新增** `mcp/*`（BusinessMcpTools/SignalMcpTools/SpikeMcpConfig/McpTransportConfig/BlindMcpContext，8 工具 MCP server）+ `ai/relay/*`（AiWsRelayService 等，缝 A WS 中继）。AiLogs 图片 offload 去留待 Phase 5。
>
> 本节是 AI 实现细节的唯一真值（架构层只指向此处）：AI 拦截器 dev 后门 = 下条 #1（已登记，重写必修删项）；MyManus `@Component` 注释（自研 ReAct 雏形已弃用，现冻结保留）；TextApp Redis TTL 10 分钟（注释谎称 5 天）/ ImageApp 5 天 / kryo 序列化；`ToolChoiceAppChatMemoryRes.saveAll` 空实现——这些细节留在这里，[architecture/auth.md](../../docs/architecture/auth.md) 只指向。
>
> 移除历史见 [CHANGELOG.md](../../docs/CHANGELOG.md) 阶段 7；契约状态见 [product/v2.1.0/functional-requirements.md](../../docs/product/v2.1.0/functional-requirements.md) FR-AI-13~16。

1. **自研 ReAct Agent 未启用（✅ 坐实）**：`MyManus.java:9` `//@Component` 被注释 → 整条继承链（`BaseAgent`/`ReActAgent`/`ToolCallAgent`/`MyManus`）无 Bean 入容器；`ToolCallAgent` 自维护上下文（`withInternalToolExecutionEnabled(false)` + 自管 messageList）正是「重复调用工具」之源。实际生效路径是注入 `ToolChoiceApp`。弃用原因：自写 ReAct 工具重复调用/调用失败，周期紧放弃，改工作流式路由。
2. **community 求助帖工具未注入（✅ 坐实）**：community Inner 服务完全未被消费；提示词 `toolChoice-template.txt` 中「处理用户、家庭、社区、活动、求助帖相关操作」是历史残留，ToolChoiceCenter switch 无 community 分支。
3. ✅ ~~**mqtt：已取消但 pom 残留**~~：MQTT 代码早已全删，仅 `pom.xml` 残留 paho 依赖（grep `mqtt|MqttClient|paho` 命中 0 个 .java 文件）；**paho 依赖已删（chunk-2b-6b，2026-07-20）**，随 MyManus 死代码闭环一并清。
4. **RAG 半残留（✅ 坐实）**：`MyRagAdvisor`（`@Configuration` + Bean，知识库名 `视无界`）启动时仍初始化，但 `TextApp.java:53` 的 `// myRagAdvisor` 被注释未加入 defaultAdvisors → 运行时不参与对话。属「删了一半」。
5. **ToolChoiceAppChatMemoryRes.saveAll 空实现**：路由阶段不写 Memory（省 token），靠共享 key 读文本侧历史——非显然设计。
6. **TextApp Redis TTL 注释不一致**：TextApp TTL=10 分钟（注释写「5 天」），ImageApp=5 天。文本侧 10 分钟即过期，可能有意也可能是 bug。
7. **CORS 全开**：`http://*:*` + allowCredentials=true。
8. **诚实缺口**：无压测 / 无 Docker / 无索引调优；AiLogs 仅 `idx_ailogs_operator_time` 单索引；图片占位符用 logId 主键查 AiLogs = 把日志表当 KV 存储。
9. ✅ ~~`@EnableScheduling` 残留~~：原 ai Application 声明但无 `@Scheduled` 方法；v3.0.0 删 ai Application 后，新入口 `ShiwujieBootstrapApplication` 仅声明 `@EnableAsync`，残留消失。
10. **ToolIndex 枚举与 ToolChoiceCenter switch 不完全一致**：枚举是给 ReAct 用的旧映射，随框架弃用失同步；以 switch case 1-9 为准。
11. ✅ ~~文本模型 spring-ai-alibaba 客户端 + qwen3.x 不兼容（url error）~~（2026-07-12 修复）：`AiConfig.qwenText` 原用 `DashScopeChatModel`，qwen3-max 过期后换 qwen3.6-flash，调文本模型报 `400 InvalidParameter: url error`。裸 HTTP 直调 compatible-mode 拿 200，证明 key/model/服务均正常，问题在 spring-ai-alibaba 1.0.0.2 客户端构造的请求体不被 qwen3.x 接受。**止血**：文本 bean 改官方 `OpenAiChatModel`（新增 `spring-ai-openai` 依赖，版本由父 pom spring-ai-bom 管）+ DashScope compatible-mode base-url（`application.yml` 的 `spring.ai.dashscope.base-url` 改为**不带 `/v1`**——OpenAiApi 默认拼 `/v1/chat/completions`，带则重复成 404）；图像仍用 `DashScopeChatModel`（多模态走原生端点正常）。消费方（TextApp/ToolChoiceApp/ImageApp/MyManus）构造器参数 `DashScopeChatModel`→`ChatModel` 接口（两 bean 均实现之）。新增 `AiSmokeTest`（3 用例：文本 OpenAI 兼容 / 裸 HTTP 对照 / 图像 DashScope，`@EnabledIfSystemProperty(ai.smoke)` 守卫默认不进 `mvn test`，`-Dai.smoke=true` 单独跑）。AI 后续将用别的技术重构（见 ROADMAP），此为止血非治本。

### gateway（✅ v3.0.0 模块整体删除）

> gateway 模块已于 v3.0.0 单体化阶段2.1（`4f10d11`）整体删除，下列 v2.1.0 缺陷随之消灭：

1. ✅ ~~Knife4j 未聚合 ai 服务~~：单体统一 SB3.4.5 / openapi3，SB2/SB3 文档协议割裂消失（knife4j openapi3-jakarta 阶段1.4 `4db2c53`）。
2. ✅ ~~网关不做鉴权 / 4 处拦截器重复~~：4 份 `LoginCheckInterceptor` 收敛为 common-web 单份（阶段2.5 `35b81ed`）。
3. ✅ ~~生产 IP 硬编码默认值~~：Nacos 整体移除（阶段2.3 `106902b`），`${nacos.address:...}` 不复存在。

### model / common-web（✅ v3.0.0 去重 + ai 接入 common-web）

1. ✅ ~~common-web 与 model 重复代码~~：阶段2.5（`35b81ed`）去重，保留 common-web 版 `PageRequest`/`CommonConstant`/`UserConstants`，删 model 版（原 model 版 `PageRequest` 默认 pageSize=20 且 Serializable；common-web 版默认 10 非 Serializable）。
2. ✅ ~~ai 用不了 common-web~~：SB 统一为 3.4.5/jakarta 后，ai 删自带 jakarta 版 `JwtUtils`/`RedisTemplateConfig`/`LoginCheckInterceptor`，改用 common-web（阶段2.5 `35b81ed`）。

## 跨切面技术债

- ✅ ~~无分布式事务（Seata）~~：v3.0.0 单体化阶段2.6（`a157991`）合并为单库 `shiwujie`，原跨库写场景（社区入驻/审核通过/删志愿者/删社区）升 `@Transactional(rollbackFor=Exception.class)` 单事务，中途异常不残留脏数据；`synchronized(phone.intern())` 表面保留作同库并发护栏，**实有 synchronized/@Transactional 边界错位缺陷（见下条「假原子」）**。user 模块 seata 依赖已随去微服务清理。
- **反模式：QueryWrapper 跨模块传递**（v3.0.0 起同进程）：`InnerCommunityjoinreviewService.getOne(QueryWrapper)` 原跨 Dubbo 传（强耦合 + 序列化风险）；Dubbo 移除后改同进程注入，**序列化风险消失**，但 QueryWrapper 作跨模块参数的强耦合仍在（建议改传 DTO/条件参数）。见 [architecture/data-model.md](../../docs/architecture/data-model.md)。
- ✅ ~~冗余 Inner 契约~~：v3.0.0 阶段2.2（`199e6f3`）删除 community 的 `InnerActivityService`/`InnerActivitysignService`/`InnerHelppostService`（全局无消费方的冗余契约）。
- **单机 JVM 锁多实例失效**：`synchronized(loginUserPhone.intern())` 在多实例部署下无法防并发写。v3.0.0 单体默认单实例部署，当前有效；若未来横向扩展多实例仍需换分布式锁。
- **synchronized/@Transactional 边界错位（同库并发「假原子」，2026-07-12 登记）**：`VolunteerServiceImpl.deleteVolunteer:372` 的 `synchronized(loginUserPhone.intern()){...}` 包在 `@Transactional(rollbackFor=Exception.class)` 方法体内——锁在方法体末尾（`:413`）释放，而事务提交发生在代理层、方法 return 之后，**锁先于事务提交释放**。故同一登录手机号的并发删除请求，后者拿到锁时读到的是前者提交前的快照（MySQL RR 隔离），`synchronized` 给的是「假原子」。来源：跨库时代的 JVM 级护栏，合库 + `@Transactional` 后既冗余又错位。影响窄（仅同手机号并发删除有竞态，单线程/正常操作不受影响），不阻塞单体化封版。正解（均属行为变更，待独立决策）：① 删 `synchronized`，让 DB 事务 + 行锁兜底原子性；② 或把 `synchronized` 上提到 `@Transactional` 之外（controller / wrapper 方法），使 锁获取→开事务→提交→释放锁 顺序正确。回归配方见 [testing-strategy](../../docs/development/v3.0.0/testing-strategy.md) 事务回归段。
- **单体 bean 循环依赖（已用配置还原 阶段2.7 `5f21cf4`）**：`@DubboReference→@Resource`（阶段2.2）后，社区↔志愿者↔社区管理员级联删除的双向耦合显化为 Spring bean 环（`communityServiceImpl↔volunteerServiceImpl↔communitymanagerServiceImpl`，启动报 `BeanCurrentlyInCreationException`）。Dubbo 时代远程代理天然解耦构造期，单体以 `spring.main.allow-circular-references: true`（早期引用）等价还原，行为不变；后续可改 `@Lazy` 精细化解环、消除全局开关。
- ✅ ~~RedisUtils 双注入 → 视频求助匹配队列序列化断裂~~（2026-07-12 修复，`fix/v3.0.0-security-hardening`，P0 审查新发现）：`RedisUtils` 同时声明 `@Resource RedisTemplate` 字段与 `RedisUtils(StringRedisTemplate)` 构造器——Spring 按唯一构造器注入自动配置的 `StringRedisTemplate`，`@Resource` 字段成死代码，`RedisTemplateConfig` 那个 `RedisTemplate<String,Object>` bean 从未被消费。`StringRedisTemplate` 的 `StringRedisSerializer` 把非 String 强转 String：String 值（登录 token）读写正常（解释了 WS 信令往返测过），但 `VideohelpServiceImpl` 存的 `LinkedList<Long>` 队列首写即 `ClassCastException`。**修复**：删构造器，字段改 `@Resource RedisTemplate<String,Object>`，由 `RedisTemplateConfig` 配置的 bean（JDK 序列化）兜底——String 与 LinkedList 均可正确往返；未改 RedisTemplateConfig 以免影响已工作的 token 路径。
- ✅ ~~取实体后未判空即解引用 NPE 簇~~（2026-07-12 修复，`fix/v3.0.0-security-hardening`，P0 审查新发现）：多处按 id/phone 取实体后直接 `.getXxx()` 解引用，实体不存在即 NPE。涉及：`BlindServiceImpl.removeFromCommunity`（communitymanager null）、`VolunteerServiceImpl.deleteVolunteer`（community null，并 `Objects.equals` 防御）、`FamilyServiceImpl.deleteFamily`（creatorVolunteer/familyId null）、`CommunityServiceImpl.checkLogin`（volunteer null）、`CommunityjoinreviewServiceImpl.getCommunityJoinReviewVOList`（volunteer null）、`UrgenthelpServiceImpl.joinUrgenthelp`（blind null）、`VideohelpController`/`UrgenthelpController.blindUpdateVideoPath`（实体 null）。**修复**：统一取实体后 `ObjUtil.isNull` 判空并抛 `PARAMS_ERROR`/`NO_AUTH`/`NOT_LOGIN`。
