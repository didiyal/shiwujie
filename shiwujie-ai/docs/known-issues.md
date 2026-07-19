# Python AI 服务缺陷与风险登记

> Python LangGraph 服务的缺陷 / 技术债 / 依赖风险登记。development 层（允许源码符号 / 依赖名 / open bug 编号）。**状态：设计敲定（Phase 1–4）· 实现待 Phase 5**——下列风险项是已识别的设计期风险，实现时须逐项验证 / 化解；非已运行系统的线上缺陷。用户契约在根 [product/current.md](../../docs/product/current.md)；Java 侧缺陷在 backend [known-issues.md](../../shiwujie-backend/docs/known-issues.md)；鉴权风险总览在根 [architecture/auth.md](../../docs/architecture/auth.md)。

> 本文件用自己的局部编号（AI-1, AI-2 …），与 backend `known-issues.md` 的安全漏洞编号（1–9）/ auth.md 的「风险 #N」**互相独立**，交叉引用即可——不发明跨文件统一编号。

---

## AI-1. 依赖风险：spring-ai MCP server BOM 托管与冲突

**背景**：缝 C 由 Java 侧作 MCP server（暴露 8 工具），Python 经 `langchain-mcp-adapters` 的 `MultiServerMCPClient` 消费。Java 侧需 `spring-ai-starter-mcp-server-webmvc`（streamable HTTP transport）。

**风险**：
- `spring-ai-starter-mcp-server-webmvc` 须由 **spring-ai-bom 1.0.0** 托管版本，且**不得**与 `spring-ai-alibaba-starter`（本项目止血期已引入，见 backend [known-issues](../../shiwujie-backend/docs/known-issues.md) ai #11）冲突——两个 alibaba / spring-ai 体系混用易触发版本错位 / 自动配置互斥。
- Phase 5 须 **spike 验证**：① MCP server starter 在 bootstrap 单体内能起 streamable HTTP 端点；② 与 alibaba-starter 共存（同 BOM 托管或显式锁版本）；③ Python `MultiServerMCPClient.get_tools()` 能拿到 8 工具的 schema。

**降级**：若不可共存 → 触发 [fallback.md](fallback.md)（缝 C 改 Python 8 个薄 REST wrapper）。MCP 仍为首选（Python→Java 唯一缝、零胶水）。

> **Spike-2 实测落锤（2026-07-18）**：✅ **全部化解，REST-wrap 未触发**——
> - spring-ai **1.0.0→1.1.0** + alibaba **1.0.0.2→1.1.0.0**（Boot 维持 3.4.5；1.0.0 的 MCP starter **无 streamable HTTP**，必升 1.1.0，见 [[memory] version-target]）；
> - `spring-ai-starter-mcp-server-webmvc` 由 1.1.0 BOM 托管；⚠️ **#4204 命中**：1.1.x BOM **不托管** `spring-ai-alibaba-starter-dashscope` → bootstrap pom 显式锁 `<version>1.1.0.0</version>`（其余走 BOM），已落地；
> - MCP server starter 与 dashscope/openai starter **共存 OK**（#3041 build/test 期不咬），app 启动正常、`McpServerAutoConfiguration` 加载（仅 `@McpTool` scanner 几条噪声告警，用 `ToolCallbackProvider` 不走它）；
> - Python `MultiServerMCPClient.get_tools()` **拿到 8 工具**、round-trip 通。
> **→ 缝 C MCP 确认为基线**（[fallback.md](fallback.md) 4 触发条件均未命中）。

## AI-2. 依赖风险：langchain-mcp-adapters open bug

**背景**：Python 侧 MCP 客户端用 `langchain-mcp-adapters`。

**已知 open bug**：
- **关停 RuntimeError #466**：客户端关停时抛 RuntimeError。
- **通知进度丢失 #244**：MCP server → client 的进度通知丢失。

**对本项目影响**：本项目缝 C 是**无状态逐 turn 用法**（Python 每个 turn 拉一次工具列表 / 调一次工具，不长连）——两个 bug 主要影响长连 / 持久会话场景，**对本项目影响小**。但 Phase 5 仍需验证：① client 正常析构不拖垮 agent loop；② 若依赖 MCP 进度通知（v1 不依赖，进度走 stream custom 见 design.md ⑥），需规避 #244。

> **Spike-2 实测落锤（2026-07-18）**：langchain-mcp-adapters **0.3.0** 直接式 `client.get_tools()`（**非** `async with`——0.3.0 禁了 `__aexit__`，抛 NotImplementedError）+ 单连接，**#466 teardown RuntimeError 未复现**（loop 关停噪声交 `__main__` 外层捕，不计失败）。生产多 turn 复用 / 并发连接时仍可能冒，按逐 turn 用法不受非阻塞 teardown 影响；若成 nuisance 再处理。#244（进度通知丢失）v1 不依赖（进度走 stream custom），不受影响。

## AI-3. 紧急求助确认门：qwen 单轮并行调用洞（🔴 高危，硬修正 1）

**问题**：qwen 单个 AIMessage **可发并行 `tool_calls`**。若紧急求助的 `prepare()` / `confirm()` 在同一轮被并行调用，模型可在一次返回里 `prepare + 伪造 confirm` 一气呵成，绕过用户确认——紧急求助直接发出，盲人未确认。

**根因**：LLM function-calling 的并行能力是默认开启的，对「敏感动作 + 确认」类工具组合是结构性漏洞。

**修复（设计敲定，四道闸）**：
1. `request_emergency_help` 拆 `prepare()` / `confirm()` 双工具。
2. qwen 请求对**可达紧急工具**强制 `parallel_tool_calls=False`（堵单轮并行）。
3. Redis token 绑三元组 **(blind_id, thread_id, issuing_turn)**；`confirm()` **拒绝同轮 token**（issuing_turn 比对，prepare 和 confirm 不可能同轮）。
4. v1 即做 **App 侧显式确认面**（按钮 / 长按）经**非-MCP HTTP 端点**消费 token——盲人单声道无视觉冗余，第三道门。

详见 design.md ⑬ 硬修正 1。

## AI-4. qwen function-calling 可靠性待 spike（硬修正 3）

**问题**：Decision A（原生 FC 杀旧 2-call 税）**依赖 qwen FC 稳**。qwen FC 历史上有 schema 违例 / 幻觉工具名 / 漏调工具等问题。公开基准：typia strict-schema 基准从 6.75% → 100%（强 schema 约束下），说明**工具定义的 schema 严格程度直接决定 FC 可靠性**。

**spike（前置，Phase 5 第一件事）**：用**本工具集（14 + read_skill）+ 本 prompt**测 FC 通过率，建议阈值 **≥90%**。测：① 工具选择正确率；② 入参 schema 遵守率；③ 幻觉工具名出现率。

**两护栏（无论 spike 结果都上）**：
- MCP 服务端 **strict JSON-schema 校验**（拒违例入参，typia 式强约束）。
- **tool-name 白名单**（拒未注册名，堵幻觉名冒充 confirm——如模型编出 `confirm_emergency` 伪名绕过 AI-3 的拆工具）。

spike 不达标 → 退回 2-call 路由（Decision A 备选），但护栏仍留。详见 design.md ⑬ 硬修正 3。

> **Spike-1 实测落锤（2026-07-18，720 调用）**：✅ **gate 通过**——qwen3.7-plus `parallel_tool_calls=True` 干净集 pass-rate **95.2%** ≥ 90% → **Decision A 成立**（溶进 loop）。写工具误调用率 **0%**、args-invalid / halluc / errored **全 0**、对抗集 **94.4%**（> 50% 降级线，不触发 B 兜底）。失败聚类：`multi_turn` 83.3%（漏调 `gaode_poi_search` / `get_weather`，recall 不足）、`multi_tool` 94.4%（偶漏 `web_search`）——均「漏调」型非「误调」型，靠 navigation skill + checkpoint 缓解。两护栏仍全上。
>
> ⚠️ **模型名核验缺口**：`qwen3.7-plus` 在 provisioned MaaS 端点下 720 调用零错、行为正常，但**是否为公开 `qwen-plus` 别名 / 私有路由未核**（provisioned 部署可能静默降级到更便宜模型）——不影响「溶进 loop 可行」结论（FC 行为是裁 Decision A 的真值），但**生产前建议 `/v1/models` 核确切 model id**。

## AI-5. AiLogs 图片 offload 去 Phase 5

**问题**：AiLogs 表降级为追加只写审计 / 可观测日志（design.md ⑩）。但旧用法把图片存进 AiLogs、用 logId 主键当 KV 查（backend [known-issues](../../shiwujie-backend/docs/known-issues.md) ai #8「图片占位符用 logId 查 AiLogs = 把日志表当 KV」）。重写后图片走 HTTP multipart（缝 A 文本 turn 走 WS、图片仍 HTTP），offload 去向（对象存储 / 本地 / 留 AiLogs）**待 Phase 5 决策**。

**当前态**：登记为待决策项，不阻塞设计落地；Phase 5 选型后补本条结论。

## AI-6. Java-graph B-prime 前置 spike（备选反转 gate）

**背景**：若缝 A 跨语言流式踩坑 / Python 进程负担过重 / 缝税超收益，回退单进程 spring-ai-alibaba-graph（Decision B-prime，见 design.md ⑪ / ⑬ 硬修正 5）。

**前置 spike（Phase 5 早期，与 AI-4 并行）**：先验 **alibaba-graph HITL-resume 在本 qwen3.x 栈是否被 open bug 咬中**：
- **#3297 / #3266**：alibaba-graph 的中断恢复（HITL-resume）open bug。本项目的紧急求助确认门恰需跨轮恢复（design.md ④）——若 bug 咬中，B-prime **暂不可行**（回退也没救），须等上游修。
- spike 结论决定 B-prime 是否保留为可行备选。

**MCP 工具设计语言无关**：无论 A 还是 B-prime，8 工具的 MCP schema 设计不变（缝 C 在 A 是 MCP、在 B-prime 退化为直接 Java 调用，工具定义存活）。

## AI-7. MCP vs REST-wrap fallback

缝 C 的 MCP 依赖（AI-1）若翻车，降级为 Python 8 个薄 REST wrapper。完整触发条件 / 降级方案 / 代价 / 回 MCP 时机见 [fallback.md](fallback.md)。

## AI-8. chunk-2a 实现状态与 stub 边界（2026-07-19 落地）

> Phase 5 第一块实现里程碑。下列「已完成」项 = chunk-2a 已落地 + FakeChatModel 全绿（122 测）；「Stub 边界」= 2a 用 mock 占位、后续 chunk 接真。

**已完成**：graph 装配（State / agent encode-不抛 / ToolNode 自定义 `_handle_tool_error`）/ 14 工具 schema + 三道安全护栏（emergency 拆 prepare/confirm、update_profile `extra="forbid"`、open_app Literal 白名单）/ safety turn-bound token + tool-name 白名单 / navigation `SKILL.md` + read_skill / BM25 KB + 种子文档 / 两层记忆（Redis checkpoint + compress/preference 框架）/ FastAPI `/ai/turn` ndjson 流式 + `/health` / budget 熔断纯逻辑（`graph/budget.py`，18 测）/ encode-不抛 + 工具 isError 自愈测试。

**Stub 边界（2a mock → 后续 chunk 接真）**：
- LLM = FakeChatModel（脚本）→ **chunk-2c** 真 qwen3.7-plus。
- 6 native 外部 API（searchapi / 高德 / weather）= mock 返回 → **chunk-2c** 接真（外部 API 免费）。
- recognize_photo VLM = fake 描述 → **chunk-2c** 真 qwen3-vl-flash。
- 8 Java-MCP 工具 = stub 回退（MCP 可达则载真，Spike-2 已证通）→ **chunk-2b** Java 真实现 + strict schema。
- 短期压缩 LLM 摘要 = fake → **chunk-2c**；长期偏好后台抽取 = 不触发（异步不阻塞）→ **chunk-2c**。

**留 chunk-2c wiring**：budget 熔断 `should_stop_after_turn` 已落地为纯逻辑模块 + 单测（2a），但 live-loop 接线（agent 每步后调 `derive_turn_stats` + `should_stop_after_turn`，触发即 `encode_stop_reply`）留 2c——FakeChatModel 脚本模型不会失控循环，2a 接上也咬不到牙；真 qwen 才可能烧 token / 失控，那时 wiring 才有意义。

**设计文档不准确已更正（本回卷）**：design ⑫ / nodes.py docstring 原暗示 LangGraph prebuilt ToolNode「自带」isError 自愈——实测 LangGraph 1.2.9 默认 `_default_handle_tool_errors` **只捕 `ToolInvocationError`、其余 re-raise**，真实工具网络/API 异常会崩 graph。已注入自定义 `_handle_tool_error` 兜所有异常（见 `graph/nodes.py`）；本回卷更正 design ⑫ 与 nodes.py docstring 表述。

## AI-9. chunk-2b Java 实现进度（2026-07-19，2b-1/2b-2 已落地）

> 缝 A Java 侧（网关 WS + Java→Python 内部 HTTP 流式中继）+ 缝 C Java MCP 真实现 + 删旧 Java AI 模块，按 6 片推进。
> 本块是「实现进度登记」非「线上缺陷」——与 [[AI-8]]（chunk-2a Python 侧）正交，跟踪 Java 侧切片落地与残留风险。

**2b-1（WS handler 改造，已落地）**：`sessionMap`/`sessionPhoneMap` 换 `ConcurrentHashMap`（线程安全）；
删 `onMessage` 对所有消息无条件回显「收到客户端的数据」噪声（ping/login/join 已有专门应答，加 default 记录未知类型）；
删 6 处 `notice*` 死代码（构造错误响应却丢弃未发）；修 `AiLoginCheckInterceptor` Redis 续期 key 漏 `-blind-` 段（token 永不续期）。

**2b-2（AI-turn 流式中继，缝 A 核心，已落地）**：
- `Position` DTO（lat/lng/address，model 契约层）+ `SocketData` 加 `text`/`position` 两可选字段（加不改名，仅 requestType=100 携带）。
- **WS requestType 契约**（`AiWsTypes`）：入站 `100`=AI-turn（text+position）；出站 `110`=delta / `111`=progress / `112`=turn_end / `113`=error。既有 5001-5006 信令码不变。
- `ApplicationContextHolder`（`ApplicationContextAware` 静态持有器）：`@ServerEndpoint` 实例由 WS 容器按 session new、不经 Spring DI，需取 bean 时（`AiWsRelayService`/`InnerBlindService`）用它兜底。
- `AiWsRelayService`：`HttpClient`（JDK 自带，免新依赖）POST Python `/ai/turn`，`BodyHandlers.ofLines()` 逐行解析 ndjson → `AiTurnEvent` → `toFrame` 映射 `SocketVO` → `session.getAsyncRemote()` 非阻塞推回 App；后台线程池（不阻塞 WS 容器线程）；**断连省 token**（每行前查 `session.isOpen()`，断开即抛内部信号中止拉流 + try-with-resources 关流取消上游订阅 → Python 停发，是 2c 真 qwen 的成本闸门）。
- `application.yml` 加 `shiwujie.ai.python.{base-url,internal-secret,turn-path,timeout-seconds}`（凭据 `${ENV:default}` 内联）。
- phone→blindId 临时经 `InnerBlindService.getByPhone`（WS 会话键是 phone，Python 要 blind_id）；**2b-6 ticket 鉴权后改由 ticket 解出**。
- 测试：`AiWsRelayServiceTest`（13 测，ndjson 解析 + 帧映射纯逻辑）；bootstrap 全测 **303 绿**（290 旧 + 13 新）。HTTP/WS 胶水端到端留 chunk-2c 真 qwen / 手工 curl（FakeChatModel 零 token）验。

**2b-3/4/5/6（待）**：MCP 8 工具真身（现仍 `SpikeMcpTools` stub）+ strict schema（2b-3 业务工具 / 2b-4 信令工具）；`SocketData` 加 `destination` 字段供 5006 载终点（2b-4）；删旧 Java AI 模块（`tools/app/*`、`tools/mytools/*`，MyManus 冻结保留，2b-5）；WS ticket 鉴权堵 phone 冒充 + 删 `AiLoginCheckInterceptor` dev 后门（2b-6）。

**残留风险（2b 未解，登记追踪）**：① WS 零鉴权（phone 可冒充）→ 2b-6 ticket 修（见根 [architecture/auth.md](../../docs/architecture/auth.md)）；② `AiLoginCheckInterceptor` dev 后门（无 token 静默登录 blind id=1）→ 2b-6 删；③ `getAsyncRemote()`（流式）与既有 `getBasicRemote()`（信令）同 session 混用——jakarta 规范行为未定义，单盲人低并发可接受，并发增高需统一 async 或加 per-session 发送锁。

---

## 与其它文件的交叉引用

| 本文件项 | 相关处 |
|---|---|
| AI-1（MCP server BOM） | design.md ⑦（MCP 工具来源）；fallback.md（降级） |
| AI-3（紧急门并行洞） | design.md ⑬ 硬修正 1；product（紧急求助契约）；app docs（App 确认面） |
| AI-4（qwen FC spike） | design.md ⑬ 硬修正 3（Decision A 命门） |
| AI-6（Java-graph B-prime） | design.md ⑪（反转 gate）/ ⑬ 硬修正 5；自研 ReAct 见 backend [known-issues](../../shiwujie-backend/docs/known-issues.md) ai #1 |
| AI-5（AiLogs 图片） | backend [known-issues](../../shiwujie-backend/docs/known-issues.md) ai #8 |
| AI-9（chunk-2b Java 进度） | design.md 3.7 / 4.2-4.3（缝 A 中继）；plan chunk-2b 6 片；WS 零鉴权见 [architecture/auth.md](../../docs/architecture/auth.md) |
