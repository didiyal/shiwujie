# 后端已知问题与技术债

> 后端技术债 / 缺陷登记。development 层（允许源码引用 `file:line`）。🔴 = 安全漏洞或高危，已同步进 [ROADMAP.md](../../docs/ROADMAP.md) 待实现「安全加固」作必办。鉴权链路总览见 [architecture/auth.md](../../docs/architecture/auth.md)。

## 🔴 安全漏洞（必办）

1. **ai 默认用户兜底（生产后门）**：`LoginCheckInterceptor`（ai 模块，line 52-60）无 Authorization 时注入 blindId=1 / phone=19872250169。生产未关闭则任何人可白嫖 AI（消耗 DashScope token）。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #6。
2. **community 权限检查被注释**：`HelppostServiceImpl.deleteHelppost`/`updateHelppost` 的「创建者或管理员」检查**整段被注释** → 任何登录视障者可删/改任意帖；`CommunityController.deleteCommunity`/`updateCommunity` 注释「仅注册人可改」但**实现未校验** → 任意志愿者可改/删任意社区；Activity delete/update、Activitysign add 均无身份与角色校验。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #9。
3. **续期 key 拼接 bug**：`renewKey`/`expire` 拼的 key **少了 `-blind-`/`-volunteer-` 段**（`LoginCheckInterceptor` 续期、`deleteBlind` 删 token 均漏前缀；登录存/注销删/拦截器读则正确）→ 滑动会话静默失效，活跃用户 90 天后被踢；删用户删 token 同样漏前缀。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #1。
4. **JWT 过期校验被关闭**：`validateToken(..., true)` 第三参 `ignoreExp=true`，JWT 自身 exp 永不生效，过期完全依赖 Redis TTL。Redis 故障/误写则 JWT 形同永久有效。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #2。
5. **TOKEN_SECRETKEY 硬编码且弱**：密钥即字符串 `"TOKEN_SECRETKEY"`，明文在共享 model 模块，HS256 弱密钥有离线爆破/伪造 token 风险。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #3。
6. **MD5 存密码、无加盐**：`SecureUtil.md5(password)`，不适用口令存储且无 salt → 彩虹表风险。建议 BCrypt/Argon2。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #4。
7. **/ws/call 未在鉴权白名单**：`WebConfig.excludePathPatterns` 不含 `/ws/call`，`@ServerEndpoint` 走独立容器 → **WS 实际绕过 JWT**。任何人构造 `{requestType:0, volunteerPhone:"任意号"}` 即可冒名 bind，接收他人求助通知。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #8。
8. **URL 放行规则过宽**：`url.contains("loginAndRegister")`/`contains("Login")` 按子串放行未限定 path，任何含该子串的路径都绕过鉴权。建议 AntPathMatcher。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #7。

## 鉴权链路（LoginCheckInterceptor 4 处复制 + 行为分叉）

`{user, call, community, ai}/.../interceptor/LoginCheckInterceptor.java` 各一份：user/call/community 三份几乎逐字相同（仅 URL 放行规则略异）；**ai 那份完全不同**（jakarta 命名空间、直调 RedisTemplate、`@DubboReference InnerBlindService` 拉 Blind 实体、无 token 时 fallback 测试用户）。应抽到 common-web 统一份，但被 SB2/SB3 割裂阻挡。见 [architecture/auth.md](../../docs/architecture/auth.md) 风险 #5。

## 各模块缺陷

### user

1. **审核权限校验不完整**：`updateFamilyJoinReview` 仅比对 `reviewerId == loginVolunteerId`（reviewerId 来自前端入参），**未校验登录人是否为该家庭 creator** → 任一志愿者伪造 reviewerId 可审核他人家庭。
2. **`deleteBlind`/`deleteVolunteer` 权限空洞**：注释「管理员可用」，实际无角色/本人校验，任何登录用户传 id 即可逻辑删任意人。
3. **`getBlindVOById` 顺序错误**：先 getById→getBlindVO→之后才判 null，「用户不存在」校验形同虚设，返回空 VO。
4. **`createFamily` 用 GET 触发写**：`GET /family/add` 违反 REST 语义。
5. **`joinFamily` 未校验「已加入他家庭」**：申请人已有 familyId 仍可再申请并通过，原家庭关系静默丢失。

### call

1. **netty-all 冗余残留依赖**：`pom.xml:84-89` 仍声明 `netty-all:4.1.50.Final`，但 call 源码**零 Netty 引用**（阶段 8 已迁 Spring WebSocket）。可安全删除，当前仅膨胀 jar（~4MB）。
2. **🔴 sessionMap 并发隐患（高危）**：`sessionMap`/`sessionPhoneMap` 是普通 `HashMap`，javax.websocket 每连接独立线程触发 onOpen/onMessage/onClose/onError → 并发 put 可致链表环/数据丢失；复合操作 TOCTOU。应改 `ConcurrentHashMap`。
3. **5xxx 失败分支不抛错**：用户不在线时 noticeXxx 仅打 log，返回值被丢弃 → ai 侧仍收到「成功」，前端实际未收到。应抛 `BusinessException` 透传给 ai 重新生成回复。
4. **`removeVolunteerFromVideohelp` 潜在 NPE**：Redis 无队列 key 时 `queue` 为 null，下一行 `queue.contains` 直接 NPE。
5. **Redis 队列 30s TTL 与匹配窗口错配**：志愿者挂机 30s 后队列过期，需确认产品是否接受。
6. **逻辑删除字段命名**：实体属性是否为 `isDelete` 需与 model 实体核对（call 关闭驼峰映射，可能影响逻辑删除生效）。

### community

1. **Controller 层直接 `@DubboReference`（架构瑕疵）**：`CommunitymanagerController.java:38-39` 在 Controller 内注入 `InnerVolunteerService` 并直调（`deleteCommunityManager:86`），跨模块 Inner 调用应封装在 Service 层。
2. **权限模型未落地**：3 角色 `CommunityRolePermissionEnum` 存在但无接口按 roleId 鉴权；`CommunityRolePermission` 表存在但无运行时判定；审核接口仅校验登录态。
3. **命名残留**：`ActivityStatusEnum` 内部字段叫 `postStatus`（从 PostStatusEnum 复制未改名），`END_HELP`/`FALL` 名字残留自 Post。
4. **社区创建无人工审核**：`communityRegister` 直接 `setCommunityStatus(1)`，SQL 注释的「0-未审核」状态未走审核流。
5. **Activitysign 缺签到/签退接口**：表有 check_in/out 字段但无对应更新方法；未校验 max_participants / activity_status。

### ai（试错-移除残留 + 其它发现）

> 移除历史见 [CHANGELOG.md](../../docs/CHANGELOG.md) 阶段 7；契约状态见 [product/v2.0.0/functional-requirements.md](../../docs/product/v2.0.0/functional-requirements.md) FR-AI-13~16。

1. **自研 ReAct Agent 未启用（✅ 坐实）**：`MyManus.java:9` `//@Component` 被注释 → 整条继承链（`BaseAgent`/`ReActAgent`/`ToolCallAgent`/`MyManus`）无 Bean 入容器；`ToolCallAgent` 自维护上下文（`withInternalToolExecutionEnabled(false)` + 自管 messageList）正是「重复调用工具」之源。实际生效路径是注入 `ToolChoiceApp`。弃用原因：自写 ReAct 工具重复调用/调用失败，周期紧放弃，改工作流式路由。
2. **community 求助帖工具未注入（✅ 坐实）**：community Inner 服务完全未被消费；提示词 `toolChoice-template.txt` 中「处理用户、家庭、社区、活动、求助帖相关操作」是历史残留，ToolChoiceCenter switch 无 community 分支。
3. **mqtt：已取消但 pom 残留（✅ 坐实）**：MQTT 代码已全删，仅 `pom.xml:30-48` 残留 paho 依赖。grep `mqtt|MqttClient|paho` 命中 0 个 .java 文件。
4. **RAG 半残留（✅ 坐实）**：`MyRagAdvisor`（`@Configuration` + Bean，知识库名 `视无界`）启动时仍初始化，但 `TextApp.java:53` 的 `// myRagAdvisor` 被注释未加入 defaultAdvisors → 运行时不参与对话。属「删了一半」。
5. **ToolChoiceAppChatMemoryRes.saveAll 空实现**：路由阶段不写 Memory（省 token），靠共享 key 读文本侧历史——非显然设计。
6. **TextApp Redis TTL 注释不一致**：TextApp TTL=10 分钟（注释写「5 天」），ImageApp=5 天。文本侧 10 分钟即过期，可能有意也可能是 bug。
7. **CORS 全开**：`http://*:*` + allowCredentials=true。
8. **诚实缺口**：无压测 / 无 Docker / 无索引调优；AiLogs 仅 `idx_ailogs_operator_time` 单索引；图片占位符用 logId 主键查 AiLogs = 把日志表当 KV 存储。
9. **`@EnableScheduling` 残留**：启动类声明但无 `@Scheduled` 方法。
10. **ToolIndex 枚举与 ToolChoiceCenter switch 不完全一致**：枚举是给 ReAct 用的旧映射，随框架弃用失同步；以 switch case 1-9 为准。

### gateway

1. **Knife4j 未聚合 ai 服务**：API 文档不完整（SB2/SB3 文档协议不兼容）。
2. **网关不做鉴权**：每个业务服务复制一份 `LoginCheckInterceptor`（4 处重复根因）。
3. **生产 IP 硬编码默认值**：`${nacos.address:47.112.114.139}`，部署新服务器忘记改 prod yml 则注册旧 IP。

### model / common-web

1. **common-web 与 model 重复代码**：`PageRequest` 两份（model 默认 pageSize=20 且 Serializable；common-web 默认 10 非 Serializable）；`CommonConstant`/`UserConstants` 两层完全重复。历史演进残留，纯 POJO 本应只放 model。
2. **ai 用不了 common-web**：ai 自带一份 jakarta 版 `JwtUtils`/`RedisTemplateConfig`/`LoginCheckInterceptor`，与 common-web 逻辑近似但命名空间不同。

## 跨切面技术债

- **无分布式事务（Seata）**：user 模块 pom 中 seata 依赖被注释。跨库写靠 `synchronized(phone.intern())` 单机锁 + 业务级联 updateById，中途异常留脏数据。已知跨库写场景：社区入驻（community 写社区 + user 写 volunteer.communityId）、加入审核通过（community 改 review_status + user 写 communityId）、删志愿者（级联删家庭 + 清社区）、删社区（清成员 communityId + 删 communitymanager）。
- **反模式：QueryWrapper 跨 Dubbo 传递**：`InnerCommunityjoinreviewService.getOne(QueryWrapper)` 把 MyBatis-Plus QueryWrapper 跨 Dubbo 传（强耦合 + 序列化风险）。见 [architecture/data-model.md](../../docs/architecture/data-model.md)。
- **冗余 Inner 契约**：community 的 `InnerActivityService`/`InnerActivitysignService`/`InnerHelppostService` 已 `@DubboService` 暴露但全局无 `@DubboReference` 消费方——预留或清理遗漏。见 [architecture/gateway-dubbo.md](../../docs/architecture/gateway-dubbo.md)。
- **单机 JVM 锁多实例失效**：`synchronized(loginUserPhone.intern())` 在多实例部署下无法防并发写。
