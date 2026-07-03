# community 模块

> 社区组织（省/市/街道三级）+ 成员审核 + 求助帖 + 活动与报名签到。Dubbo 提供 6 个 Inner 服务。本文为 development 细化；用户可见契约（FR-COMMUNITY / AC-COMMUNITY）见 [product/current.md](../../../docs/product/current.md)。

## 模块定位

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-community/` |
| 端口 | 8400，context-path `/api/community` |
| Dubbo 端口 | 21400 |
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

- 端口 8400 / context-path `/api/community`；Dubbo 21400；MySQL `shiwujiecommunity`；Redis db=2。
- MyBatis-Plus 驼峰映射开启，逻辑删除 `isDelete`。
- 拦截器放行含 `Login`/`Register`/`loginAndRegister` 的 URL（见 [`../../../docs/architecture/auth.md`](../../../docs/architecture/auth.md)）。
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

> community 模块缺陷（Controller 层直调 Dubbo、🔴 权限检查被注释、权限模型未落地、命名残留、社区创建无人工审核、Activitysign 缺签到签退、跨库写无事务）见 [`../known-issues.md`](../known-issues.md)。
