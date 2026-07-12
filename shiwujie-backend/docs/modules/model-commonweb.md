# model 模块（契约层）

> v3.0.0 收敛为 **2 模块**：`shiwujie-model`（契约层，本文件）+ `shiwujie-bootstrap`（唯一 app：原 common-web 公共层 + user/call/community/ai 全部业务代码已于阶段2.8 并入，见 [bootstrap.md](bootstrap.md)）。本文为 development 细化；用户可见契约（FR-MODEL / AC-MODEL）见 [product/current.md](../../../docs/product/current.md)。

> ⚠️ **v3.0.0 单体化后**：SB 统一为 3.4.5/jakarta，「model/common-web 两层因 SB 割裂而被迫分立」的根因消失——ai 接入 common-web、两层重复类去重（阶段2.5）；阶段2.8 进一步把 common-web 并入 bootstrap，现仅 model 作独立库模块保留。当前态见 [tech-stack](../../../docs/architecture/tech-stack.md) as-built。

## 模块定位

| 模块 | 类型 | 依赖 | 被谁依赖 |
|---|---|---|---|
| `shiwujie-model` | jar（无 Spring） | 无 | **bootstrap**（阶段2.8 起 common-web/user/call/community/ai 均并入 bootstrap，仅 bootstrap 依赖 model） |
| ~~`shiwujie-common-web`~~ | **已并入 bootstrap**（阶段2.8 `a215d9e`） | — | 公共层代码现处 bootstrap 的 `com.swj.shiwujie.{common,config,constants,exception,interceptor,utils}` 子包，详见 [bootstrap.md](bootstrap.md) |

> 为什么曾有 model/common-web 两层？（v2.1.0 根因）详见 [`../../../docs/architecture/tech-stack.md`](../../../docs/architecture/tech-stack.md) 的「SB 2.7 与 SB 3.4.5 割裂」：Spring AI 强制 SB3+/Java21，而 common-web 是 SB2.7（`javax.*` 命名空间），ai 用不了。纯契约的 model 不绑 Spring，故所有模块都能依赖。**v3.0.0 SB 统一为 3.4.5/jakarta 后此根因消失**（阶段2.5 `35b81ed`），ai 接入 common-web、两层重复类去重；阶段2.8 进一步把 common-web 并入 bootstrap——契约/公共职责现以「model 契约 + bootstrap 公共子包」两模块形态承载。

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

## ~~shiwujie-common-web~~ → 已并入 bootstrap

common-web 的公共能力（`BaseResponse`/`ErrorCode`、`WebConfig`、`LoginCheckInterceptor`、`JwtUtils`/`RedisUtils`/`LoginUtils`、`GlobalExceptionHandler`/`ThrowUtils`、`RedisTemplateConfig`）已于阶段2.8（`a215d9e`）整体并入 `shiwujie-bootstrap` 的 `com.swj.shiwujie.{common,config,constants,exception,interceptor,utils}` 子包，对外路径与行为零变更。详见 [bootstrap.md](bootstrap.md)「公共层」节。

> v2.1.0 缺陷（common-web 与 model 重复代码、LoginCheckInterceptor 复制到 4 模块、ai 用不了 common-web）已随 v3.0.0 单体化消灭（阶段2.5 + 阶段2.8），✅ 标记见 [`../known-issues.md`](../known-issues.md)。
