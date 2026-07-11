# model 与 common-web 模块

> 共享层：`shiwujie-model` 是纯契约（所有模块可依赖），`shiwujie-common-web` 是公共能力（v3.0.0 全模块含 ai 可依赖；v2.1.0 因 SB 割裂仅 SB2 业务模块可用）。二者构成「单一真相源」。本文为 development 细化；用户可见契约（FR-MODEL / AC-MODEL）见 [product/current.md](../../../docs/product/current.md)。

> ⚠️ **v3.0.0 单体化后**：SB 统一为 3.4.5/jakarta，「model/common-web 两层因 SB 割裂而被迫分立」的根因消失——ai 已接入 common-web，两层重复类已去重。当前态见 [tech-stack](../../../docs/architecture/tech-stack.md) as-built。

## 模块定位

| 模块 | 类型 | 依赖 | 被谁依赖 |
|---|---|---|---|
| `shiwujie-model` | jar（无 Spring） | 无 | **全部模块**（含 ai） |
| `shiwujie-common-web` | jar（SB 3.4.5，v3.0.0 统一） | model | user/call/community/ai（v3.0.0 起 ai 也依赖；v2.1.0 因 SB 割裂不含 ai） |

> 为什么两层？（v2.1.0 根因）详见 [`../../../docs/architecture/tech-stack.md`](../../../docs/architecture/tech-stack.md) 的「SB 2.7 与 SB 3.4.5 割裂」：Spring AI 强制 SB3+/Java21，而 common-web 是 SB2.7（`javax.*` 命名空间），ai 用不了。纯契约的 model 不绑 Spring，故所有模块都能依赖。**v3.0.0 SB 统一为 3.4.5/jakarta 后此根因消失**（阶段2.5 `35b81ed`），ai 接入 common-web、两层重复类去重——但分层结构本身保留（契约/公共职责分离仍有价值）。

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

1. **Inner 接口契约**：7 个 `Inner*Service` 接口（v2.1.0 为 10 个 Dubbo 契约，v3.0.0 阶段2.2 `199e6f3` 删 3 个无消费者冗余），提供者 `@Service` 实现、消费者 `@Resource` 引用（v2.1.0 为 `@DubboService` / `@DubboReference`）。完整清单见 [`../../../docs/architecture/gateway-dubbo.md`](../../../docs/architecture/gateway-dubbo.md)。
2. **domain / enums / request / VO**：前后端与跨服务的数据契约。
3. **共享常量**：`TOKEN_SECRETKEY` / `REDIS_SECRETKEY` / `PASSWORD_REGEX` / `BLIND_REGEX`（见 [`../../../docs/architecture/auth.md`](../../../docs/architecture/auth.md)）。

## shiwujie-common-web（SB2 公共层）

```
src/main/java/com/swj/shiwujie/
├── common/{BaseResponse, ErrorCode, PageRequest}.java   # （v3.0.0 去重：PageRequest 仅留 common-web 版）
├── constants/{CommonConstant, UserConstants}.java        # （v3.0.0 去重：仅留 common-web 版）
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

- **JwtUtils**：`generateToken(payload, secret, duration)` / `validateToken(token, secret, ignoreExp)`。业务模块调用时恒传 `ignoreExp=true`（忽略 JWT 自身过期，见 [`../../../docs/architecture/auth.md`](../../../docs/architecture/auth.md)）。
- **RedisUtils**：`setToRedis(key, val, days)` / `renewKey(key, days)` → `redisTemplate.expire(key, days, TimeUnit.DAYS)` / `getFromRedis` / `removeToRedis`。
- **GlobalExceptionHandler**：`BusinessException` / `RuntimeException` 统一转 `BaseResponse`。
- **ThrowUtils**：`throwIf(cond, code, msg)` 断言式抛业务异常。

> 上述 v2.1.0 缺陷（common-web 与 model 重复代码、LoginCheckInterceptor 复制到 4 模块、ai 用不了 common-web）**已随 v3.0.0 单体化消灭**（阶段2.5 `35b81ed`），✅ 标记见 [`../known-issues.md`](../known-issues.md)。
