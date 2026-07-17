# MCP 翻车降级方案（缝 C 改 REST-wrap）

> 缝 C（Python→Java 能力）的首选是 MCP streamable HTTP（Java 作 MCP server，8 工具）。本文是 **MCP 依赖翻车时的降级方案**：缝 C 改 Python 8 个薄 REST wrapper 直调 Java REST 端点。development 层。**状态：设计敲定（Phase 1–4）· 实现待 Phase 5**——降级方案为已敲定的备选路径，非首选、非已实现。依赖风险见 [known-issues.md](known-issues.md) AI-1；缝 C 设计见 [design.md](design.md) ⑦。

---

## 触发条件

满足**任一**即触发降级（即 MCP 在本栈不可用）：

1. **`spring-ai-starter-mcp-server-webmvc` 与 spring-ai-bom 1.0.0 不兼容**（版本错位 / starter 不被 BOM 托管须手锁版本且手锁 destabilize 其它依赖）。
2. **MCP server starter 在 bootstrap 单体内起不来**（streamable HTTP 端点无法暴露 / 自动配置失败）。
3. **`spring-ai-starter-mcp-server-webmvc` 与 `spring-ai-alibaba-starter`（止血期已引入）不共存**（两个 alibaba / spring-ai 体系自动配置互斥 / Bean 冲突）。
4. Python 侧 `langchain-mcp-adapters` 的 open bug（见 [known-issues](known-issues.md) AI-2）在本项目用法下被咬中且无绕过。

> 上述均须 Phase 5 spike 验证（见 [known-issues](known-issues.md) AI-1）。spike 前 MCP 仍是首选假设。

## 降级方案

缝 C 改 **Python 8 个薄 REST wrapper**，直调 Java 暴露的对应 REST 端点：

| MCP 工具（首选） | 降级 = Python REST wrapper（直调 Java REST 端点） |
|---|---|
| `join_family` | Python wrapper → `POST /api/.../family/join`（Java REST） |
| `leave_family` | Python wrapper → `POST /api/.../family/leave` |
| `family_info` | Python wrapper → `GET /api/.../family/info` |
| `update_profile`（仅 nickname/phone/gender） | Python wrapper → `POST /api/.../profile/update`（窄 DTO，硬修正 2 不变） |
| `launch_navigation`（5006） | Python wrapper → Java 信令 REST（触发 5006） |
| `request_video_help`（5002） | Python wrapper → Java 信令 REST（触发 5002） |
| `request_emergency_help`（5003，prepare/confirm） | Python wrapper → Java 信令 REST（拆 prepare/confirm 两端点，硬修正 1 不变） |
| `open_app`（5004） | Python wrapper → Java 信令 REST（触发 5004） |

**对 LLM 无差异**：8 个 wrapper 用 `StructuredTool`（langchain）实现，与 native 工具同 schema（name / description / args_schema），并入 toolset 后 LLM **不感知**它是 REST 而非 MCP——agent loop、system prompt、Pi 金标准契约（design.md ⑫）全部不变。**工具语义层降级对 agent 透明**。

**不变项**（降级只换缝 C 传输，不动其它）：
- 硬修正 1（紧急门 prepare/confirm + parallel_tool_calls=False + turn-bound token + App 非-MCP 确认面）——**仍全部保留**，只是 prepare/confirm 从 MCP 工具变 REST 端点。
- 硬修正 2（update_profile inputSchema 硬卡 + 窄 DTO + 单测）——Java REST 端点仍绑窄 DTO，schema 校验仍在。
- 缝 A（对话流）——完全不受影响。
- 14 工具清单、navigation skill、BM25 KB、两层记忆——均不变。

## 代价

| 代价 | 说明 |
|---|---|
| **Python 要手写 8 个调用** | 每个工具一个 REST 调用（构造请求 / 鉴权 header / 解析响应 / 错误处理），MCP 则是 `get_tools()` 自动拿现成 schema。 |
| **维护两套协议** | MCP（首选）+ REST（降级）两套缝 C 实现，Phase 5 后若 MCP 修好要切回，REST wrapper 待删（或反之）。需在代码 / 文档明确「当前生效哪套」。 |
| **攻击面略增** | Java 须为 8 工具各暴露一个 REST 端点（即使仅内网），比单一 MCP 端点多 8 个入口；须每个端点验 `X-Internal-Secret` + `X-Blind-Id`（与 MCP 同鉴权，但端点多了）。 |
| **schema 双份** | MCP 的 JSON-schema（首选）与 REST 的 args_schema（降级）各一份，硬修正 2 的字段门须两处都守（单测断言 DTO 无敏感字段 setter 仍护 Java 侧，Python 侧 args_schema 也要断言无敏感字段）。 |

## 何时回 MCP

满足**全部**则切回 MCP（首选）：
1. 触发条件（上文）已消除——`spring-ai-starter-mcp-server-webmvc` 与 BOM / alibaba-starter 共存问题修复（上游新版本 / 本项目 pom 调通）。
2. Python 侧 `langchain-mcp-adapters` open bug 不再咬中本项目用法。
3. spike 验证 MCP server 起 streamable HTTP + client 拿到 8 工具 schema 全通。

切回时删 Python 8 个 REST wrapper，缝 C 复用 MCP（`MultiServerMCPClient.get_tools()`），toolset 对 LLM 仍无差异。

## MCP 仍为首选

**MCP 是缝 C 的首选**，理由：
- **Python→Java 唯一缝**：MCP 把 8 工具统一成一份 schema、一个端点，Python `get_tools()` 自动拿现成工具定义，**零胶水**。
- **语言无关**：MCP 工具设计语言无关（design.md ⑬ 硬修正 5），Decision A（Python LangGraph）与 Decision B-prime（Java-graph）两套主线下 schema 都存活——降级 REST 反而绑死 Python 调用形态。
- **窄缝**：单一 MCP 端点 + 统一鉴权，攻击面比 8 个 REST 端点窄。

降级是兜底，不是平替。Phase 5 先 spike MCP（[known-issues](known-issues.md) AI-1），过则用 MCP、不过才走本文 REST-wrap。
