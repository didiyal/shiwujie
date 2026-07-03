# user 模块

> 三类用户（Blind/Volunteer/Family）的身份、资料、家庭关系与家庭审核；**JWT 单点鉴权签发方**；Dubbo 提供者。本文为 development 细化；用户可见契约（FR-USER / AC-USER）见 [product/current/](../../../docs/product/current/)。

## 模块定位

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-user/` |
| 端口 | 8200，context-path `/api/user` |
| Dubbo 端口 | 21200 |
| 框架 | SB 2.7.0 + Java 17 |
| MySQL | `shiwujieuser`（Blind/Volunteer/Family/FamilyJoinReview） |
| 启动类 | `ShiwujieUserApplication`（`@EnableDubbo`） |
| 角色 | Dubbo **提供者**（InnerBlind/Volunteer/FamilyService）+ **消费者**（InnerCommunity*） |

## 目录与核心类

```
src/main/java/com/swj/shiwujie/
├── ShiwujieUserApplication.java
├── config/{SwaggerConfig, WebConfig}.java       # WebConfig 注册 LoginCheckInterceptor + CORS
├── interceptor/LoginCheckInterceptor.java
├── controller/{Blind, Volunteer, Family, FamilyJoinReview}Controller.java
├── service/{Blind, Volunteer, Family, FamilyJoinReview}Service(+impl).java
├── service/impl/inner/                          # @DubboService
│   ├── InnerBlindServiceImpl.java
│   ├── InnerVolunteerServiceImpl.java
│   └── InnerFamilyServiceImpl.java
└── mapper/{Blind, Volunteer, Family, FamilyJoinReview}Mapper.java
```

## 接口与 Dubbo 契约

**提供者**（`@DubboService`，被 ai/call/community 消费）：

| 接口 | 方法 |
|---|---|
| `InnerBlindService` | getById / getByPhone / updateById / removeCommunityId |
| `InnerVolunteerService` | getById / save / updateById / getByPhone / getListByFamilyId / generateLoginToken / getVolunteerVO / removeCommunityId |
| `InnerFamilyService` | getFamilyVOById / joinFamily / userLeaveFromFamily（注释「AI 调用」） |

**消费者**（`@DubboReference` → community）：`InnerCommunityService` / `InnerCommunityjoinreviewService` / `InnerCommunitymanagerService`（用于加入/退出社区、创建审核记录、按社区查角色）。

## 配置要点

- 端口 8200 / context-path `/api/user`；Dubbo 21200；MySQL `shiwujieuser`；Redis db=2。
- MyBatis-Plus：驼峰映射开启，逻辑删除 `isDelete`。
- dev/prod 仅差 `spring.cloud.nacos.discovery.ip`。
- 鉴权 key 规则：`REDIS_SECRETKEY-blind-{id}` / `-volunteer-{id}`，TTL 90 天（见 [`../../../docs/architecture/auth.md`](../../../docs/architecture/auth.md)）。

## 关键数据流

### 注册/登录（Blind 为例，Volunteer 同构）

`BlindController.loginAndRegister` → `BlindServiceImpl.loginAndRegister`：
1. 校验手机号 + 密码格式，MD5 加密（未加盐）。
2. `getByPhone` 查 Blind 表：命中→比对 MD5；未命中→跨表查 Volunteer，已注册志愿者则拒绝，否则自动注册。
3. `generateLoginToken`：构造 payload → `JwtUtils.generateToken(..., 90天)` → `redisUtils.setToRedis("REDIS_SECRETKEY-blind-"+id, token, 90天)`。
4. 返回 `BlindLoginSuccessVO`（脱敏 + token）。

一键登录 `loginAndRegisterQuickly(phone)`：无密码分支。

### 鉴权

见 [`../../../docs/architecture/auth.md`](../../../docs/architecture/auth.md)——JWT(ignoreExp=true) + Redis token 比对 + 续期（注：续期有 key 拼接 bug）。

### 家庭关系与审核

- **创建家庭**（`/family/add`，仅志愿者，`synchronized(phone.intern())`）：查重 creator → 新建 Family → 回填家主 volunteer.familyId。
- **申请加入**（`/family/join`）：按家主手机号查 familyId → 建 `FamilyJoinReview`（reviewStatus=0 待审）。
- **家主审核**（`/familyJoinReview/update`）：通过→PASSED + 更新申请人 familyId；拒绝→REJECTED。
- 状态机 `FamilyReviewStatusEnum`：0 待审 / 1 通过 / 2 拒绝。

> user 模块特有缺陷（审核权限校验不完整、deleteBlind/deleteVolunteer 权限空洞、getBlindVOById 顺序错误、无 Seata、createFamily 用 GET、joinFamily 未校验已加入他家庭、单机锁多实例失效）见 [`../known-issues.md`](../known-issues.md)。
