# model 与 common-web 模块

> 共享层：`shiwujie-model` 是纯契约（所有模块可依赖），`shiwujie-common-web` 是 SB2.7 公共能力（仅 SB2 业务模块依赖）。二者构成跨服务「单一真相源」。

## 模块定位

| 模块 | 类型 | 依赖 | 被谁依赖 |
|---|---|---|---|
| `shiwujie-model` | jar（无 Spring） | 无 | **全部模块**（含 SB3 的 ai） |
| `shiwujie-common-web` | jar（SB2.7） | model | gateway/user/call/community（**不含 ai**） |

> 为什么两层？详见 [`../architecture/tech-stack.md`](../architecture/tech-stack.md) 的「SB 2.7 与 SB 3.4.5 割裂」：Spring AI 强制 SB3+/Java21，而 common-web 是 SB2.7（`javax.*` 命名空间），ai 用不了。纯契约的 model 不绑 Spring，故所有模块都能依赖。

## shiwujie-model（契约层）

```
src/main/java/com/swj/shiwujie/
├── common/PageRequest.java          # 分页基类（pageSize=20, Serializable）
├── constants/{CommonConstant, UserConstants}.java
├── model/
│   ├── domain/{ai,call,community,user}/*.java   # 13 个实体
│   ├── enums/{call,community,user}/*.java       # 11 个枚举
│   ├── request/{call,community,user}/**/*.java  # ~40 个请求对象
│   └── VO/{call,community,user}/**/*.java       # 13 个 VO
└── service/
    ├── user/Inner{Blind,Volunteer,Family}Service.java
    ├── call/InnerSocket.java
    └── community/Inner{Community,Communityjoinreview,Communitymanager,Activity,Activitysign,Helppost}Service.java
```

**核心交付物**：

1. **Dubbo 接口契约**：10 个 `Inner*Service` 接口，提供者 `@DubboService` 实现、消费者 `@DubboReference` 引用。完整清单见 [`../architecture/gateway-dubbo.md`](../architecture/gateway-dubbo.md)。
2. **domain / enums / request / VO**：前后端与跨服务的数据契约。
3. **共享常量**：`TOKEN_SECRETKEY` / `REDIS_SECRETKEY` / `PASSWORD_REGEX` / `BLIND_REGEX`（见 [`../architecture/auth.md`](../architecture/auth.md)）。

## shiwujie-common-web（SB2 公共层）

```
src/main/java/com/swj/shiwujie/
├── common/{BaseResponse, ErrorCode, PageRequest}.java   # ⚠ PageRequest 重复
├── constants/{CommonConstant, UserConstants}.java        # ⚠ 常量重复
├── config/RedisTemplateConfig.java
├── exception/{BusinessException, GlobalExceptionHandler, ThrowUtils}.java
└── utils/
    ├── JwtUtils.java           # HS256 签发/校验（Hutool）
    ├── RedisUtils.java         # setToRedis/renewKey(TTL=DAYS)/getFromRedis
    ├── LoginUtils.java         # 从 request 取登录用户
    ├── ResultUtils.java
    └── ConverterUtils.java
```

**核心能力**：

- **JwtUtils**：`generateToken(payload, secret, duration)` / `validateToken(token, secret, ignoreExp)`。业务模块调用时恒传 `ignoreExp=true`（忽略 JWT 自身过期，见 [`../architecture/auth.md`](../architecture/auth.md) 风险 #2）。
- **RedisUtils**：`setToRedis(key, val, days)` / `renewKey(key, days)` → `redisTemplate.expire(key, days, TimeUnit.DAYS)` / `getFromRedis` / `removeToRedis`。
- **GlobalExceptionHandler**：`BusinessException` / `RuntimeException` 统一转 `BaseResponse`。
- **ThrowUtils**：`throwIf(cond, code, msg)` 断言式抛业务异常。

## 功能需求（FR-MODEL）

- **FR-MODEL-01**：所有跨服务 RPC 接口集中在 shiwujie-model 定义。
- **FR-MODEL-02**：domain / enums / request / VO 统一维护作为数据契约。
- **FR-MODEL-03**：分页请求提供统一 PageRequest 基类。
- **FR-MODEL-04**：统一返回为 BaseResponse；统一异常经 GlobalExceptionHandler 转换。

## 验收标准（AC-MODEL）

- **AC-MODEL-01**：shiwujie-model 可独立 `mvn install`，被其余模块依赖。
- **AC-MODEL-02**：Inner*Service 签名变更后编译期所有提供者/消费者同步报错。
- **AC-MODEL-03**：BaseResponse 经 GlobalExceptionHandler 对各类异常产出统一结构。

## 已知问题

1. **common-web 与 model 重复代码**：
   - `PageRequest` 两份（model 默认 pageSize=20 且 Serializable；common-web 默认 10 非 Serializable）。
   - `CommonConstant` / `UserConstants` 两层完全重复（common-web 依赖 model 却复制了常量）。
   - 属历史演进残留，纯 POJO 本应只放 model。
2. **LoginCheckInterceptor 复制到 4 个业务模块**：本应在 common-web 提供一份，但因 SB2/SB3 割裂 + 各模块放行规则微差，实际各自复制（见 [`../architecture/auth.md`](../architecture/auth.md) 风险 #5）。
3. **ai 用不了 common-web**：ai 自带一份 jakarta 版的 `JwtUtils` / `RedisTemplateConfig` / `LoginCheckInterceptor`，与 common-web 逻辑近似但命名空间不同（详见 [`ai.md`](ai.md)）。
