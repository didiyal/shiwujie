# user 模块

> 三类用户（Blind/Volunteer/Family）的身份、资料、家庭关系与家庭审核；**JWT 单点鉴权签发方**；Dubbo 提供者。

## 模块定位

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-user/` |
| 端口 | 8200，context-path `/api/user` |
| Dubbo 端口 | 50200 |
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
| `InnerFamilyService` | getFamilyVOById / joinFamily / userLeaveFromFamily（注释"AI 调用"） |

**消费者**（`@DubboReference` → community）：`InnerCommunityService` / `InnerCommunityjoinreviewService` / `InnerCommunitymanagerService`（用于加入/退出社区、创建审核记录、按社区查角色）。

## 配置要点

- 端口 8200 / context-path `/api/user`；Dubbo 50200；MySQL `shiwujieuser`；Redis db=2。
- MyBatis-Plus：驼峰映射开启，逻辑删除 `isDelete`。
- dev/prod 仅差 `spring.cloud.nacos.discovery.ip`。
- 鉴权 key 规则：`REDIS_SECRETKEY-blind-{id}` / `-volunteer-{id}`，TTL 90 天（见 [`../architecture/auth.md`](../architecture/auth.md)）。

## 关键数据流

### 注册/登录（Blind 为例，Volunteer 同构）

`BlindController.loginAndRegister` → `BlindServiceImpl.loginAndRegister`：
1. 校验手机号 + 密码格式，MD5 加密（未加盐）。
2. `getByPhone` 查 Blind 表：命中→比对 MD5；未命中→跨表查 Volunteer，已注册志愿者则拒绝，否则自动注册。
3. `generateLoginToken`：构造 payload → `JwtUtils.generateToken(..., 90天)` → `redisUtils.setToRedis("REDIS_SECRETKEY-blind-"+id, token, 90天)`。
4. 返回 `BlindLoginSuccessVO`（脱敏 + token）。

一键登录 `loginAndRegisterQuickly(phone)`：无密码分支。

### 鉴权

见 [`../architecture/auth.md`](../architecture/auth.md)——JWT(ignoreExp=true) + Redis token 比对 + 续期（注：续期有 key 拼接 bug）。

### 家庭关系与审核

- **创建家庭**（`/family/add`，仅志愿者，`synchronized(phone.intern())`）：查重 creator → 新建 Family → 回填家主 volunteer.familyId。
- **申请加入**（`/family/join`）：按家主手机号查 familyId → 建 `FamilyJoinReview`（reviewStatus=0 待审）。
- **家主审核**（`/familyJoinReview/update`）：通过→PASSED + 更新申请人 familyId；拒绝→REJECTED。
- 状态机 `FamilyReviewStatusEnum`：0 待审 / 1 通过 / 2 拒绝。

## 功能需求（FR-USER）

| ID | 需求 |
|---|---|
| FR-USER-01~04 | 视障者/志愿者 一键登录 与 密码登录（自动注册） |
| FR-USER-05 | 跨表手机号唯一（盲人/志愿者互斥） |
| FR-USER-06 | 登录签发 JWT + Redis 单点 |
| FR-USER-07 | 注销清 Redis |
| FR-USER-08~11 | 改密 / 改手机号 / 资料更新（身份证/残疾证 MD5）/ 资料脱敏 VO |
| FR-USER-12~17 | 创建家庭 / 申请加入 / 审核 / 查列表 / 踢人退出 / 删家庭（级联） |
| FR-USER-18 | 删志愿者（级联删家庭、清社区） |
| FR-USER-19~21 | 加入社区 / 踢出社区（需管理员权限）/ 分页查社区成员 |
| FR-USER-22 | Inner 服务供 ai 调用 |

## 验收标准（AC-USER）

| ID | 验收点 |
|---|---|
| AC-USER-01 | 合法手机号：未注册→自动建号返回 token；已注册→直接返回 token |
| AC-USER-02 | 密码不符合 `字母+数字` → 报错 |
| AC-USER-03 | 同一手机号 Blind/Volunteer 只能一处 → 第二处报"已被注册" |
| AC-USER-04 | 再次登录覆盖旧 token，旧 token 立即失效（单点） |
| AC-USER-05 | 错误/无 Authorization → `NOT_LOGIN` |
| AC-USER-06 | 非本人改他人资料 → "操作用户错误" |
| AC-USER-07 | 身份证/残疾证存库为 MD5；VO 仅回 `isIdCard/isDisabilityCard` 布尔 |
| AC-USER-08~13 | 仅志愿者可建家庭；家主不能加自家；审核通过更新 familyId；非家主审核报错；删家庭级联清成员；社区管理员(非 EMPLOYEE)可踢人 |
| AC-USER-14 | Inner*Service 经 Nacos 注册，ai `@DubboReference` 可调用 |

## 已知问题

> 鉴权相关风险（JWT ignoreExp、续期 key bug、MD5 无 salt、密钥硬编码、URL contains 放行过宽）见 [`../architecture/auth.md`](../architecture/auth.md)，此处仅列 user 模块特有项。

1. **审核权限校验不完整**：`updateFamilyJoinReview` 仅比对 `reviewerId == loginVolunteerId`（reviewerId 来自前端入参），**未校验登录人是否为该家庭 creator** → 任一志愿者伪造 reviewerId 可审核他人家庭。
2. **`deleteBlind`/`deleteVolunteer` 权限空洞**：注释"管理员可用"，实际无角色/本人校验，任何登录用户传 id 即可逻辑删任意人。
3. **`getBlindVOById` 顺序错误**：先 getById→getBlindVO→之后才判 null，"用户不存在"校验形同虚设，返回空 VO。
4. **未引入 Seata**：删志愿者级联删家庭/社区跨库多写，仅 `synchronized` + 顺序 updateById，无分布式事务。
5. **`createFamily` 用 GET 触发写**：`GET /family/add` 违反 REST 语义。
6. **`joinFamily` 未校验"已加入他家庭"**：申请人已有 familyId 仍可再申请并通过，原家庭关系静默丢失。
7. **单机 JVM 锁多实例失效**：`synchronized(loginUserPhone.intern())` 在多实例部署下无法防并发写。
