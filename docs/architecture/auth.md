# 鉴权架构

> 跨切面概览：JWT + Redis 单点鉴权链路（架构级流程图）。用户可见契约（FR-AUTH / AC-AUTH）见 [product/current.md](../product/current.md)；风险点与 `file:line` 明细（续期 key 拼接 bug、JWT 过期校验关闭、弱密钥硬编码、MD5 无盐、ai 默认用户后门、WS 绕过鉴权、URL 放行过宽等）见 [shiwujie-backend/docs/known-issues.md](../../shiwujie-backend/docs/known-issues.md)。鉴权工具位于公共层（common-web），但拦截器在每个业务模块各复制一份。

## 总体链路

平台采用 **JWT + Redis 双重校验** 实现单点登录：

```mermaid
flowchart TD
    L["① 登录(user 模块)<br/>generateLoginToken"] --> JWT["签发 JWT<br/>Hutool HS256, 密钥 TOKEN_SECRETKEY<br/>payload: {id, isBlind, phone, role}<br/>有效期 90 天"]
    L --> R["写 Redis<br/>key = REDIS_SECRETKEY-{role}-{id}<br/>value = token<br/>TTL = 90 天"]

    REQ["② 请求(任意业务服务)<br/>Authorization: Bearer token"] --> INT["LoginCheckInterceptor.preHandle"]
    INT --> P1["放行 OPTIONS / 登录类 URL"]
    INT --> P2["JwtUtils.validateToken<br/>(ignoreExp=true 忽略 JWT 自身过期)"]
    INT --> P3["Redis 取 REDIS_SECRETKEY-{role}-{id}"]
    INT --> P4["比对 token 字符串<br/>(不等 → NOT_LOGIN)"]
    INT --> P5["续期(设计为滑动会话)"]
    INT --> P6["注入 loginBlindId/VolunteerId/phone/role 到 request"]
```

**单点原理**：同账号在他处再次登录会**覆盖** Redis 中的 token，旧 token 字符串不再匹配 Redis 值，立即失效。

## 登录与 Token 签发

实现位于 user 模块 `BlindServiceImpl.generateLoginToken` / `VolunteerServiceImpl.generateLoginToken`：

1. 校验手机号格式（`PhoneUtil.isPhone`）、密码格式（`PASSWORD_REGEX`：须含字母+数字、仅字母数字）。
2. 密码 **MD5 加密（未加盐）** 存库/比对。
3. 跨表查重：Blind/Volunteer 手机号互斥（同一号只能存在一处）。
4. 构造 payload `{blindId|volunteerId, isBlind, phone, role}` → `JwtUtils.generateToken(payload, TOKEN_SECRETKEY, 90天)`。
5. `redisUtils.setToRedis("REDIS_SECRETKEY-blind-"+id, token, 90L)`（单位=**天**）。

**Redis Token key 规则**：

| 身份 | key | TTL |
|---|---|---|
| 视障者 | `REDIS_SECRETKEY-blind-{blindId}` | 90 天 |
| 志愿者 | `REDIS_SECRETKEY-volunteer-{volunteerId}` | 90 天 |

**关键常量**（`shiwujie-model/.../constants/UserConstants.java`，全模块共享）：

| 常量 | 值 |
|---|---|
| `TOKEN_SECRETKEY` | 固定 HS256 签名密钥（**硬编码、弱**——值与风险见 [known-issues](../../shiwujie-backend/docs/known-issues.md) 风险 #3） |
| `REDIS_SECRETKEY` | 字符串 `"REDIS_SECRETKEY"`（Redis key 前缀） |
| `PASSWORD_REGEX` | `^(?=.*[A-Za-z])(?=.*\d)[A-Za-z0-9]+$` |

## 请求鉴权（LoginCheckInterceptor）

`preHandle` 流程：

1. 放行 OPTIONS（CORS 预检）。
2. URL 含 `loginAndRegister` / `Login` / `Register` 子串 → 放行。
3. 取 `Authorization: Bearer <token>`，缺失抛 `NOT_LOGIN`。
4. `JwtUtils.validateToken(token, TOKEN_SECRETKEY, ignoreExp=true)` —— **第三参恒为 true，即忽略 JWT 自身 exp**，仅校验签名与算法；过期完全交给 Redis。
5. 从 payload 解析 blindId / volunteerId / phone / role。
6. 查 Redis 对应 key，为 null 抛 `NOT_LOGIN`。
7. **比对 token 字符串**（请求 token 必须 == Redis 值）。
8. `renewKey(..., 1L)` 续期（单位=天，见下文风险）。
9. 注入 `loginBlindId` / `loginVolunteerId` / `phone` / `role` 到 `request.setAttribute`。

注销：`/login/logout` 直接删 Redis key。

## 功能需求 / 验收标准 / 已知风险

FR-AUTH / AC-AUTH（含「续期不生效」当前不满足项）见 [../product/v2.0.0/functional-requirements.md](../product/v2.0.0/functional-requirements.md) · [../product/v2.0.0/acceptance-criteria.md](../product/v2.0.0/acceptance-criteria.md)。

风险点 #1–#9（续期 key 拼接 bug、JWT 过期校验关闭、弱密钥硬编码、MD5 无盐、拦截器 4 处复制、ai 默认用户后门、URL 放行过宽、WS 绕过鉴权、社区/家庭审核权限校验不完整）含 `file:line` 明细，统一登记于 [../../shiwujie-backend/docs/known-issues.md](../../shiwujie-backend/docs/known-issues.md)（🔴 项同步进 [../ROADMAP.md](../ROADMAP.md) 安全加固）。
