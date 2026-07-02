# community 模块

> 社区组织（省/市/街道三级）+ 成员审核 + 求助帖 + 活动与报名签到。Dubbo 提供 6 个 Inner 服务。

## 模块定位

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-community/` |
| 端口 | 8400，context-path `/api/community` |
| Dubbo 端口 | 50400 |
| 框架 | SB 2.7.0 + Java 17 |
| MySQL | `shiwujiecommunity`（Community/Communitymanager/Communityjoinreview/Helppost/Activity/Activitysign） |
| 启动类 | `CommunityApplication`（`@EnableDubbo`） |
| 角色 | Dubbo **提供者**（6 个 Inner）+ **消费者**（InnerVolunteer/InnerBlindService） |

## 目录与核心类

```
src/main/java/com/swj/shiwujie/
├── CommunityApplication.java
├── config/{WebConfig, SwaggerConfig}.java
├── interceptor/LoginCheckInterceptor.java
├── controller/{Community, Communitymanager, Communityjoinreview, Helppost, Activity, Activitysign}Controller.java
├── service/...(+impl)/  + service/impl/inner/   # 6 个 @DubboService
└── mapper/...(6 个)
```

## 六大主体与状态机

| 主体 | 表 | 说明 | 状态机 |
|---|---|---|---|
| Community | community | 省/市/街道三级社区 | status: 0未审核/1已审核/2停用（创建时直接置 1） |
| Communitymanager | communitymanager | 社区管理者 | rolePermissionId: 1注册人/2管理员/3员工 |
| Communityjoinreview | communityjoinreview | 加入审核 | review_status: 0待审/1通过/2拒绝 |
| Helppost | helppost | 求助帖 | post_status: 0待响应/1处理中/2完成/3取消 |
| Activity | activity | 活动 | activity_status: 0未开始/1进行中/2结束/3取消 |
| Activitysign | activitysign | 活动报名 | signUpTime + checkInTime/checkOutTime（签到/签退） |

## 权限模型

`CommunityRolePermissionEnum` 定义 3 角色：

| roleId | 名称 | 描述 | 入口 |
|---|---|---|---|
| 1 | 注册人 | 社区注册人，可分配管理员 | 仅 `communityRegister` 时写入 |
| 2 | 管理员 | 可审核加入、管理活动 | addCommunityManager / updateCommunityManager |
| 3 | 员工 | 可发起活动签到 | 同上 |

> **模型缺陷**：枚举描述了能力边界，但代码中**几乎没有接口按 roleId 差异化鉴权**。`CommunityRolePermission` 表存在但无对应运行时判定；审核接口仅校验登录态，未校验是否为该社区 role=1/2。

## 接口与 Dubbo 契约

**提供者**（`@DubboService`）：

| 接口 | 方法 | 消费者 |
|---|---|---|
| `InnerCommunityService` | getById / deleteCommunity | user |
| `InnerCommunitymanagerService` | getCountByVolunteerIdAndCommunityId / getByVolunteerIdAndCommunityId / removeByVolunteerIdAndCommunityId | user |
| `InnerCommunityjoinreviewService` | save / getById / getOne(QueryWrapper) | user |
| `InnerActivityService` | getActivityVOById / listActivitiesByCommunity / listActivities | **无消费者** |
| `InnerActivitysignService` | addActivitySign / listActivitySignByActivity | **无消费者** |
| `InnerHelppostService` | addHelppost / listQueryHelpposts / deleteHelppost / updateHelppost | **无消费者** |

> Activity/Activitysign/Helppost 三个 Inner 已暴露但全局无消费方（冗余/预留）。`InnerCommunityjoinreviewService.getOne(QueryWrapper)` 跨 Dubbo 传 QueryWrapper 是反模式。

**消费者**（→ user）：`InnerVolunteerService` / `InnerBlindService`。

## 配置要点

- 端口 8400 / context-path `/api/community`；Dubbo 50400；MySQL `shiwujiecommunity`；Redis db=2。
- MyBatis-Plus 驼峰映射开启，逻辑删除 `isDelete`。
- 拦截器放行含 `Login`/`Register`/`loginAndRegister` 的 URL（见 [`../architecture/auth.md`](../architecture/auth.md) 风险 #7）。
- CORS 全开。

## 关键数据流

### 创建社区（入驻）

`POST /community/Register` → `CommunityServiceImpl.communityRegister`：
1. 手机号查志愿者，不存在则自动建并实名（身份证 MD5）。
2. **三级层级自动补齐**：缺省级则建默认省级社区(levelId=1)，同理市级(levelId=2, parent=省级)。
3. 建当前社区(levelId=3, parent=市级, **status=1 直接通过无人工审核**)。
4. 回写 volunteer.communityId + 绑定 Communitymanager(rolePermissionId=1 注册人)。
5. 返回 `CommunityLoginSuccessVO`。
> 注释 `//todo 处理分布式事务`——跨 user/community 库多写无事务保证。

### 成员加入审核

- **发起**：user 模块（BlindServiceImpl/VolunteerServiceImpl.joinCommunity）通过 `InnerCommunityjoinreviewService.save` 写审核记录(review_status=0)。
- **审核**：`PUT /communityjoinreview/update` → `synchronized(loginUserPhone.intern())` → 通过则置 1 + 回写申请人 communityId（双库写无事务）；拒绝则置 2。

### 发求助帖 / 活动报名

- `POST /helppost/add`：校验内容 → `innerBlindService.getById` 取 communityId，无社区 `NO_AUTH` → 建 Helppost(post_status=0)。
- `POST /activitysign/add`：校验活动有效 → 查重（同活动同人 count==1 报"已报名"）→ 建 Activitysign。**未校验人数上限 / 活动状态 / 无签到签退接口**。

## 功能需求（FR-COMMUNITY）

| ID | 需求 | 状态 |
|---|---|---|
| FR-COMMUNITY-01 | 注册人入驻社区，自动建省/市级默认社区 | ✅（无分布式事务） |
| FR-COMMUNITY-02 | 社区管理员登录 | ✅ |
| FR-COMMUNITY-03 | 修改/删除社区（联动清成员 + 删管理记录） | ✅ |
| FR-COMMUNITY-04 | 分页查子社区 | ✅ |
| FR-COMMUNITY-05 | 管理员增改删 + 员工分页 | ✅ |
| FR-COMMUNITY-06 | 成员加入审核（通过回写 communityId） | ✅（无事务） |
| FR-COMMUNITY-07 | 视障发求助帖（须隶属社区） | ✅ |
| FR-COMMUNITY-08 | 求助帖分页/删除/更新 | ⚠（权限检查被注释） |
| FR-COMMUNITY-09 | 活动 CRUD | ✅ |
| FR-COMMUNITY-10 | 活动报名（防重复） | ⚠（未校验人数/状态） |

## 验收标准（AC-COMMUNITY）

| ID | 验收点 |
|---|---|
| AC-COMMUNITY-01 | Register 全填 → 返回 VO + token；DB 出现三级社区，is_default=0/0/1，status=1 |
| AC-COMMUNITY-02 | Login 密码错误 → PARAMS_ERROR；非管理员 → NO_AUTH |
| AC-COMMUNITY-03 | 删社区后成员 communityId 清空、Communitymanager 记录删除 |
| AC-COMMUNITY-04 | 审核通过后申请人 communityId 回写；并发同 phone 被 synchronized 串行 |
| AC-COMMUNITY-05 | 未加入社区的视障发帖 → NO_AUTH |
| AC-COMMUNITY-06 | 同活动同人重复报名 → "活动已报名" |
| AC-COMMUNITY-07 | 求助帖状态仅接受四个中文名，非法 → PARAMS_ERROR |
| AC-COMMUNITY-08 | 无/错 token → NOT_LOGIN |
| AC-COMMUNITY-09 | URL 含 Login/Register 放行 |
| AC-COMMUNITY-10 | prod 注册 IP=服务器，dev=本机 |

## 已知问题

1. **🟠 Controller 层直接 `@DubboReference`（架构瑕疵）**：`CommunitymanagerController.java:38-39` 在 Controller 内注入 `InnerVolunteerService` 并直调（`deleteCommunityManager:86`），跨模块 Inner 调用应封装在 Service 层。
2. **🔴 权限检查被注释（安全漏洞）**：
   - `HelppostServiceImpl.deleteHelppost`/`updateHelppost` 的"创建者或管理员"权限检查**整段被注释** → 任何登录视障者可删/改任意帖。
   - `CommunityController.deleteCommunity`/`updateCommunity` 注释"仅注册人可改"但**实现未校验** → 任意志愿者可改/删任意社区。
   - Activity delete/update、Activitysign add 均无登录身份与角色校验。
3. **权限模型未落地**：3 角色枚举存在但无接口按 roleId 鉴权（见上）。
4. **命名残留**：`ActivityStatusEnum` 内部字段叫 `postStatus`（从 PostStatusEnum 复制未改名），`END_HELP`/`FALL` 名字残留自 Post。
5. **社区创建无人工审核**：`communityRegister` 直接 `setCommunityStatus(1)`，SQL 注释的"0-未审核"状态未走审核流。
6. **Activitysign 缺签到/签退接口**：表有 check_in/out 字段但无对应更新方法；未校验 max_participants / activity_status。
7. **跨库写无事务**：`communityRegister`、审核通过改 user 库 communityId 均无 Seata。
