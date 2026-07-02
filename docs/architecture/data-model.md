# 数据模型

> 本篇描述分库设计、表字典与跨服务数据契约。一期为单库 4 表，二期拆为 4 个独立库（每微服务一库），共享 Redis db=2。表实体与 VO 集中定义在 `shiwujie-model`。

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
| 登录 token | `REDIS_SECRETKEY-{role}-{id}` | 90 天（见 [`auth.md`](auth.md) 续期 bug） |
| AI 对话记忆（精简） | `chat:memory:{blindId}` | 文本侧 10 分钟 / 图片侧 5 天 |
| 志愿者匹配队列 | `VOLUNTEER_QUEUE_REDIS_KEY` | 30 秒 |

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

> call 模块实体字段用 **snake_case**（`blind_id`、`help_status`），配合 `map-underscore-to-camel-case: false`。

### shiwujiecommunity（community 模块，6 表）

| 表 | 实体 | 说明 | 关键字段 |
|---|---|---|---|
| community | `Community` | 社区（省/市/街道三级） | parentCommunityId / levelId / communityTypeId / status / registerVolunteerId / isDefault |
| communitymanager | `Communitymanager` | 社区管理者 | communityId / volunteerId / rolePermissionId(1注册人/2管理员/3员工) |
| communityjoinreview | `Communityjoinreview` | 社区加入审核 | communityId / blindId / volunteerId / reviewStatus / reviewerId |
| helppost | `Helppost` | 求助帖 | blindId / communityId / postStatus(0待响应/1处理中/2完成/3取消) / volunteerId |
| activity | `Activity` | 活动 | communityId / managerId / activityStatus(0未开始/1进行中/2结束/3取消) |
| activitysign | `Activitysign` | 活动报名 | activityId / blindId / volunteerId / signUpTime / checkInTime / checkOutTime |

### shiwujieai（ai 模块，1 表）

| 表 | 实体 | 说明 | 关键字段 |
|---|---|---|---|
| ai_logs | `AiLogs` | AI 对话日志 + 图片对象存储（kryo 序列化整条消息） | operator(blindId) / time / content / 索引 `idx_ailogs_operator_time` |

> ai 模块的 AiLogs 表身兼两职：①对话审计日志；②图片瘦身的对象存储载体（多轮历史中的图片以占位符 `image{logId}` 替换，回放时按 logId 反查还原）。详见 [`../backend/ai.md`](../backend/ai.md)。

## 全局约定

- **逻辑删除**：全局字段 `isDelete`（0 未删 / 1 已删），MyBatis-Plus 自动过滤。
- **驼峰映射不一致**：
  - user / community：`map-underscore-to-camel-case: true`（SQL snake_case ↔ 实体 camelCase）
  - call：`map-underscore-to-camel-case: false`（SQL 与实体**都用 snake_case**）
  - 跨模块字段命名风格不统一，潜在映射坑（已在 [`tech-stack.md`](tech-stack.md) 标注）。
- **大数字 ID**：主键为雪花 ID（19 位），前端 JS 精度会丢失 → Web 后台做了多层字符串转换防护（详见 [`../frontend/web-vue.md`](../frontend/web-vue.md)）。

## 跨服务数据契约（Dubbo）

各库**独立**，跨库数据访问**不走 JOIN、不走 DB**，而是通过 Dubbo `Inner*Service` RPC。完整契约清单见 [`gateway-dubbo.md`](gateway-dubbo.md#dubbo-接口契约清单单一真相源)。

> 典型反模式（已记录）：`InnerCommunityjoinreviewService.getOne(QueryWrapper)` 把 MyBatis-Plus 的 QueryWrapper 跨 Dubbo 传递（强耦合 + 序列化风险），见 [`../backend/community.md`](../backend/community.md)。

## 跨库一致性

项目**未引入分布式事务（Seata）**（user 模块 pom 中 seata 依赖被注释）。跨库写靠 `synchronized(phone.intern())` 单机锁 + 业务级联 updateById 保证，中途异常会留脏数据。已知跨库写场景：

- 社区入驻（community 写社区 + user 写 volunteer.communityId）
- 加入审核通过（community 改 review_status + user 写 communityId）
- 删志愿者（级联删家庭 + 清社区）
- 删社区（清成员 communityId + 删 communitymanager）

详见各模块文档「已知问题」。
