# 数据模型

> **v3.0.0 单体化已落地（2026-07-11）**：4 分库已合并为单库 `shiwujie`（远程 16 表已验证），call 驼峰映射已统一，跨库写已升单事务——详见下方「[v3.0.0 单体化（已落地）](#v300-单体化已落地启动级验证通过-2026-07-11)」。下方「分库设计」「跨服务数据契约」为 **v2.1.0 现状**（历史保留）。

> （v2.1.0 历史叙述）本篇描述分库设计、表字典与跨服务数据契约。一期为单库 4 表，二期拆为 4 个独立库（每微服务一库），共享 Redis db=2。表实体与 VO 集中定义在 `shiwujie-model`。

## v3.0.0 单体化（已落地，启动级验证通过 2026-07-11）

> 下方「分库设计」为 v2.1.0 现状。v3.0.0 单体化已合并为单库（详见 [`task-breakdown`](../development/v3.0.0/task-breakdown.md) 阶段 2.6）：

- ✅ **4 库 → 单库 `shiwujie`**（阶段2.6 `61cb18a` + 合库导入）：`shiwujieuser` / `shiwujiecall` / `shiwujiecommunity` / `shiwujieai` 共 **16 表**（13 业务主表 + `CommunityLevel`/`CommunityRolePermission`/`CommunityType` 3 字典表；表名 PascalCase 无冲突）已 mysqldump 导入新库 `shiwujie`（`47.112.114.139:3306`，user=`shiwujie`），单体单一 datasource。表字典（字段/枚举）不变。导入步骤见 [`release-checklist`](../development/v3.0.0/release-checklist.md)「合库执行步骤」。
- ✅ **跨库写 → 单库单事务**（阶段2.6 `a157991`）：社区入驻 / 审核通过 / 删志愿者 / 删社区 4 场景，原先 Dubbo 跨库 + `synchronized` + 级联 updateById 无事务保证，合并后同库用 `@Transactional(rollbackFor=Exception.class)` 包裹（`synchronized` 保留作同库并发护栏），强一致、无脏数据残留。
- ✅ **驼峰映射统一**（阶段2.6 `61cb18a`）：call 模块 `map-underscore-to-camel-case: false`（实体 snake_case）原是唯一不一致点；已统一为全局 `true`，call 的 `Videohelp` / `Urgenthelp` 实体字段 snake→camel（`blind_id`→`blindId`、`help_status`→`helpStatus`、`is_delete`→`isDelete`），2 个 Mapper XML 的 resultMap property 同步改名；**DB 列名保持 snake_case** 由下划线映射自动匹配，无 DDL 改列。
- ✅ **跨服务数据契约退化为本地调用**（阶段2.2 `199e6f3`）：Dubbo `Inner*Service` RPC 改为同进程 Bean 注入（`@DubboService`→`@Service`、`@DubboReference`→`@Resource`），跨库数据访问仍不走 JOIN（保持现有解耦），契约清单见 [`gateway-dubbo.md`](gateway-dubbo.md)。

> Redis 共享 db=2 不变（与合库正交）。

## 分库设计

| 库 | 归属模块 | 说明 |
|---|---|---|
| `shiwujieuser` | user | 三类用户 + 家庭 + 家庭审核 |
| `shiwujiecall` | call | 视频求助 + 紧急求助记录 |
| `shiwujiecommunity` | community | 社区 + 管理者 + 审核 + 求助帖 + 活动 + 报名 |
| `shiwujieai` | ai | AI 对话日志（AiLogs） |
| （无） | gateway | 网关无库，仅路由 |

> 一期 `table_sql.sql`（全量 18 张表）保留为历史快照；二期实际生效为主库拆分后的 ~13 张主表（下表）。SQL 文件位于 `shiwujie-gateway/sql/`。

## 共享缓存

Redis 单实例 `47.112.114.139:6379`，所有模块共享 **db=2**：

| 用途 | key 模式 | TTL |
|---|---|---|
| 登录 token | `REDIS_SECRETKEY-{role}-{id}` | 90 天（续期 key 拼接 bug 见 [known-issues](../../shiwujie-backend/docs/known-issues.md)） |
| AI 对话记忆（精简） | `chat:memory:{blindId}` | 文本侧 10 分钟 / 图片侧 5 天 |
| 志愿者匹配队列 | `VOLUNTEER_QUEUE_REDIS_KEY` | 30 秒 |
| AI 重写后（设计敲定·待 Phase 5） | 短期记忆 `ai:ckpt:{blind_id}`（checkpoint）+ 长期偏好 hash `{blind_id}` | checkpoint 无固定 TTL（随会话） / 偏好长期 |

> **AI 重写后记忆（设计敲定·待 Phase 5）**：短期 = LangGraph checkpoint（Redis db=2，key `ai:ckpt:{blind_id}`，thread_id=blind_id，`ai:ckpt:` 前缀避撞 spring-session / JWT key）；长期 = 偏好（Redis hash `{blind_id}` + MySQL，后台异步抽取）。重写后 Python 用 `ai:ckpt:` 前缀与上表现有 `chat:memory:` / `REDIS_SECRETKEY` key 分离，不互撞。详见 [`ai-rewrite.md`](ai-rewrite.md)。

## 表字典（二期）

### shiwujieuser（user 模块，4 表）

| 表 | 实体 | 说明 | 关键字段 |
|---|---|---|---|
| blind | `Blind` | 视障者 | phone(md5 pwd) / familyId / communityId / 身份证md5 / 残疾证md5 |
| volunteer | `Volunteer` | 志愿者 | phone(md5 pwd) / familyId / communityId |
| family | `Family` | 家庭 | creatorVolunteerId |
| family_join_review | `FamilyJoinReview` | 家庭加入审核 | familyId / blindId / volunteerId / reviewStatus(0待审/1通过/2拒绝) / applyTime / reviewTime / reviewerId |

### shiwujiecall（call 模块，2 表）

| 表 | 实体 | 说明 | 关键字段 |
|---|---|---|---|
| videohelp | `Videohelp` | 视频帮扶记录 | blindId / volunteerId / helpStatus / responseTime / endTime / duration |
| urgenthelp | `Urgenthelp` | 紧急求助记录 | blindId / volunteerId / familyId / helpStatus / responseTime |

> （v2.1.0）call 模块实体字段曾用 **snake_case**（`blind_id`、`help_status`）配合 `map-underscore-to-camel-case: false`；v3.0.0 已统一为全局 `true`，实体字段改 camelCase，此行为已废弃。

### shiwujiecommunity（community 模块，9 表 = 6 业务主表 + 3 字典表）

| 表 | 实体 | 说明 | 关键字段 |
|---|---|---|---|
| community | `Community` | 社区（省/市/街道三级） | parentCommunityId / levelId / communityTypeId / status / registerVolunteerId / isDefault |
| communitymanager | `Communitymanager` | 社区管理者 | communityId / volunteerId / rolePermissionId(1注册人/2管理员/3员工) |
| communityjoinreview | `Communityjoinreview` | 社区加入审核 | communityId / blindId / volunteerId / reviewStatus / reviewerId |
| helppost | `Helppost` | 求助帖 | blindId / communityId / postStatus(0待响应/1处理中/2完成/3取消) / volunteerId |
| activity | `Activity` | 活动 | communityId / managerId / activityStatus(0未开始/1进行中/2结束/3取消) |
| activitysign | `Activitysign` | 活动报名 | activityId / blindId / volunteerId / signUpTime / checkInTime / checkOutTime |
| communitylevel | `CommunityLevel` | 字典·社区层级（省/市/街道） | levelId / levelName |
| communitytype | `CommunityType` | 字典·社区类型 | communityTypeId / communityTypeName |
| communityrolepermission | `CommunityRolePermission` | 字典·角色权限（1注册人/2管理员/3员工） | rolePermissionId / rolePermissionName |

> 3 张字典表在 v3.0.0 合库导出中为空表（CREATE only，无数据）——角色判定走 Java `CommunityRolePermissionEnum`，不依赖 `CommunityRolePermission` 表数据，空表不影响运行。

### shiwujieai（ai 模块，1 表）

| 表 | 实体 | 说明 | 关键字段 |
|---|---|---|---|
| ai_logs | `AiLogs` | AI 对话日志 + 图片对象存储（kryo 序列化整条消息） | operator(blindId) / time / content / 索引 `idx_ailogs_operator_time` |

> **现状（v2.1.0 / 单体化）**：AiLogs 表身兼两职——①对话审计日志；②图片瘦身的对象存储载体（多轮历史中的图片以占位符 `image{logId}` 替换，回放时按 logId 反查还原）。
>
> **重写后（设计敲定·待 Phase 5）**：AiLogs **降级为追加只写审计 / 可观测日志**（不再当记忆读——短期记忆迁 LangGraph checkpoint `ai:ckpt:{blind_id}`，长期偏好迁 Redis hash + MySQL）；图片 offload 去留待 Phase 5。kryo 序列化与 TTL 不一致（文本侧 10 分钟 / 图片侧 5 天）详见 [`../../shiwujie-backend/docs/known-issues.md`](../../shiwujie-backend/docs/known-issues.md)，重写后该机制随 AiLogs 记忆职责一并退场。现状实现见 [`../../shiwujie-backend/docs/modules/ai.md`](../../shiwujie-backend/docs/modules/ai.md)，重写记忆设计见 [`ai-rewrite.md`](ai-rewrite.md)。

## 全局约定

- **逻辑删除**：全局字段 `isDelete`（0 未删 / 1 已删），MyBatis-Plus 自动过滤。
- **驼峰映射**（v3.0.0 已统一）：全局 `map-underscore-to-camel-case: true`（SQL snake_case ↔ 实体 camelCase）。v2.1.0 时 call 单独 `false`（SQL 与实体都用 snake_case）的不一致已随阶段2.6 消除。
- **大数字 ID**：主键为雪花 ID（19 位），前端 JS 精度会丢失 → Web 后台做了多层字符串转换防护（详见 [`../../shiwujie-frontend/web/docs/vue-admin.md`](../../shiwujie-frontend/web/docs/vue-admin.md)）。

## 跨服务数据契约（v2.1.0 Dubbo → v3.0.0 本地调用）

> v2.1.0：各库**独立**，跨库数据访问**不走 JOIN、不走 DB**，而是通过 Dubbo `Inner*Service` RPC。v3.0.0：库已合并、Dubbo 已移除，`Inner*Service` 改同进程 Bean 注入（接口定义留 model 不动），访问仍不走 JOIN（保持解耦）。完整契约清单见 [`gateway-dubbo.md`](gateway-dubbo.md#dubbo-接口契约清单单一真相源)。

> 跨库一致性（v2.1.0 无 Seata、单机锁；v3.0.0 已升单事务）、QueryWrapper 跨模块传递反模式（v3.0.0 起同进程，序列化风险消失但强耦合仍在）等取舍与已知缺陷，登记于 [../../shiwujie-backend/docs/known-issues.md](../../shiwujie-backend/docs/known-issues.md)「跨切面技术债」。
