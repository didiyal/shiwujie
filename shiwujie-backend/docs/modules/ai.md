# ai 模块

> 视障辅助 App 的 AI 大脑。Spring AI Alibaba 多模型对话 + 自研记忆 + 工作流式工具路由 + 图片瘦身上下文工程。Inner 纯消费方（v2.1.0 经 Dubbo / v3.0.0 同进程，无对外 Inner 服务）。本文为 development 细化（全套文档最重要的一篇——AI Agent 方向的核心工程价值在此）；用户可见契约（FR-AI / AC-AI）见 [product/current.md](../../../docs/product/current.md)。

> ⚠️ **v3.0.0 单体化后**：ai 不再是独立服务（无 8500 端口 / 无 `AiApplication` / 无 Dubbo），并入 `shiwujie-bootstrap` 单进程，库 `shiwujie`，`@DubboReference`→`@Resource`。AI 业务逻辑（三层架构 / ChatMemory / 图片瘦身）不变，变化的仅基础设施框架。「模块定位」表已更新。当前态见 [tech-stack](../../../docs/architecture/tech-stack.md) / [deployment](../deployment.md) as-built。

## 模块定位

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-ai/` |
| 端口 | （v3.0.0 单体，无独立端口；对外经 bootstrap:8100，前缀 `/api/ai/ai`） |
| 框架 | **SB 3.4.5 + Java 21** + Spring AI 1.0.0 + spring-ai-alibaba 1.0.0.2（v3.0.0 去 Dubbo 3.3.0） |
| MySQL | `shiwujie`（AiLogs，v3.0.0 合库） |
| Redis | `spring.data.redis`（全栈统一 SB3 后与业务模块同规范），db=2 |
| 角色 | 本地 Bean **纯消费者**（v2.1.0 为 Dubbo `@DubboReference`，v3.0.0 改 `@Resource`，无对外 Inner），仅 HTTP/SSE 对外 |

## 三层架构（App / Agent / Tool）

```
controller/ChatController.java        唯一 HTTP 入口（3 个 SSE 端点）
service/ChatService(Impl)             编排：工具路由 → 文本/图像流式
app/                                  【路由层】
├── ToolChoiceApp.java                ★工具路由（非流式，产出 toolType JSON）
├── TextApp.java                      ★文本流式
├── ImageApp.java                     ★图像流式（多模态）
agent/                                【自研 ReAct 框架 —— 已弃用，未启用】
├── BaseAgent / ReActAgent / ToolCallAgent / MyManus
tools/
├── ToolChoiceCenter.java             ★工具分发（switch case 1–9）
├── app/{FrontendTools, UserTools}    Dubbo→前端推送 / 家庭操作
└── mytools/{AiModelTools, WebSearchTool, TerminateTool}
chatmemory/                           【自研 ChatMemory，三套实现】
advisor/{MyLoggerAdvisor, MyRagAdvisor(RAG 半残留)}
```

> 三层职责：**App 层** = 路由（ToolChoiceApp）/ 文本流（TextApp）/ 图像流（ImageApp）；**Agent 层** = 自研 ReAct（未启用）；**Tool 层** = 工具分发中心 + 具体 `@Tool`。

## 本地消费契约（v2.1.0 Dubbo `@DubboReference` → v3.0.0 同进程 `@Resource`，仅消费不提供）

全模块消费 `Inner*Service` 共 **3 处**（v2.1.0 `@DubboReference`，v3.0.0 `@Resource` 同进程注入）：

| 消费类 | Inner 服务 | 提供方 | 用途 |
|---|---|---|---|
| `UserTools.java:30` | `InnerFamilyService` | user | joinFamily / userLeaveFromFamily / getFamilyVOById |
| `FrontendTools.java:22` | `InnerSocket` | call | noticeTakePhoto/VideoHelp/UrgentHelp/JumpSoftware/Navigation（5001/5002/5003/5004/5006） |
| `LoginCheckInterceptor.java:36` | `InnerBlindService` | user | getById 查盲人（鉴权） |

> **未注入任何 community Inner 服务**（见 [`../known-issues.md`](../known-issues.md) ai 试错-移除）。

## 工具索引（ToolChoiceCenter，case 1–9 生效）

| case | 工具 | 实现路径 | 触发推送 |
|---|---|---|---|
| 1 | 拍照识别 | `FrontendTools.noticeTakePhoto` | 本地→call→WS 5001 |
| 2 | 图片追问 | `AiModelTools.TakePhoto`（调 ImageApp） | — |
| 3 | 跳转应用 | `FrontendTools.noticeJumpSoftware` | WS 5004 |
| 4 | 高德导航 | `FrontendTools.noticeNavigation` | WS 5006 |
| 5 | 视频求助 | `FrontendTools.noticeVideoHelp` | WS 5002 |
| 6 | 紧急求助 | `FrontendTools.noticeUrgentHelp` | WS 5003 |
| 7 | 加入家庭 | `UserTools`→InnerFamily.joinFamily | — |
| 8 | 家庭信息 | `UserTools`→InnerFamily.getFamilyVOById | — |
| 9 | 退出家庭 | `UserTools`→InnerFamily.userLeaveFromFamily | — |

> **生效工具仅 1–9**。default 分支返回「请输入1-18」是历史残留注释。

## 自研 ChatMemory（Redis + MySQL 双写）

三套实现，均 `implements ChatMemoryRepository`，用 kryo（`MessageSerializer`）序列化：

| 实现 | Redis | MySQL | 特点 |
|---|---|---|---|
| `ToolChoiceAppChatMemoryRes` | 只读 | 不写 | **saveAll 空实现**：路由阶段不持久化，靠共享 KEY_PREFIX 读文本侧历史，省 token |
| `TextAppChatMemoryRes` | TTL **10 分钟** | `@Async` 全量写 AiLogs | 文本侧，CONVERSATION_ROUND=10 |
| `ImageAppChatMemoryRes` | TTL **5 天** | 全量 + **图片瘦身** | 图片侧，IMAGE_CONVERSATION_ROUND=6 |

**多模型上下文共享**：三个 App 各自 Memory，但**共享 KEY_PREFIX=`chat:memory:` + blindId 作 conversationId** → ToolChoiceApp 能读到 TextApp 写的历史（共享读、不共享写）。

## 图片瘦身上下文工程（核心优化）

`ImageAppChatMemoryRes.saveAll`（line 147-187）——解决「整图上传→文本模型上下文混入乱码→首字 20s+」痛点：

| 消息位置 | 处理 |
|---|---|
| **非末条**含图（imageByte.length > 1000） | 脱敏：替换为纯文本 UserMessage，移除 Media → 文本侧不再混入图片字节 |
| **末条**含图（最新一轮） | 图片 kryo 序列化整条存 MySQL AiLogs 拿 logId；上下文仅保留占位符 `"帮我识别这张图片:image{logId}"` |
| 回放历史（findByConversationId） | 遇 `image{logId}` 占位 → 从 MySQL 反查反序列化还原 Media → 仅当前需要时才载入字节 |

效果：单图 4-5MB token 量在多轮上下文中压成几十字节占位符 → 首字延迟显著下降。

> 注：占位符方案把 AiLogs 日志表当对象存储用（因域名过期/OSS 弃用，走自服务器）——务实设计，数据量增长后是隐患。

## 关键数据流

### 带工具调用的多轮对话（文字）

```
前端 ASR 文本 → POST /api/ai/ai/doChatByText (SSE)
  → ChatServiceImpl.doChatWithTextSSE
    【阶段一：工具路由，非流式】ToolChoiceApp.call()
        · qwenText + MyLoggerAdvisor + MessageChatMemoryAdvisor(toolChoiceAppChatMemory)
        · 输出 JSON {"toolType":-1~-4,"data":{...}}
        · switch(toolType): -1 业务工具(case1-9) / -2 web搜索 / -3 不调工具 / -4 补充信息
    【阶段二：文本流式】TextApp.stream()
        · qwenText + Memory(textAppChatMemory) → Flux<String>
        · 文本侧 Memory 写 Redis(10min) + 异步写 MySQL
  → SSE → 前端 TTS
```

要点：**两次大模型调用**（路由非流式 + 生成流式）；**工具幻觉防护**——不走 LLM 原生 function-calling，而是让模型输出约定 JSON、后端解析 switch 分发（工作流校验思路）。

### 图片识别 + 瘦身（多模态）

```
前端拍照 MultipartFile → POST /api/ai/ai/doChatByImage
  → ChatServiceImpl.imageHandle → 存本地 ${user.home}/shiwujie/images/{blindId}/{ts}.png
  → ImageApp.doImage(filePath, blindId)
      · qwenImage + Memory(imageAppChatMemory，含图片瘦身)
      · .user(text + media(IMAGE_PNG)) → .stream()
```

## 配置要点

- `dashscope.chat.options.model: qwen3-max`；图像模型 `qwen3-vl-flash`（`withMultiModel(true)`）。
- `base-url: compatible-mode/v1`（OpenAI 兼容端点）。
- Redis 用 `spring.data.redis`（与 SB2 模块的 `spring.redis` 不同）。
- v3.0.0 单体经 bootstrap:8100 对外，前缀 `/api/ai/ai`；无 Dubbo / Nacos（注册 IP 相关配置全随去微服务移除）。
- `compatibility-verifier.enabled: false`（SB3.4.5 + SCA 共存需要）。
- 凭据：`${DASHSCOPE_API_KEY:...}` / `${SEARCH_API_KEY:...}`。

> ai 模块的「试错-移除」残留（自研 ReAct 未启用、community 工具未注入、mqtt pom 残留、RAG 半残留）与其它发现（ToolChoiceAppChatMemoryRes 空实现、TextApp TTL 注释不一致、🔴 默认用户后门、CORS 全开、无压测/Docker/索引调优、@EnableScheduling 残留、ToolIndex 枚举失同步）见 [`../known-issues.md`](../known-issues.md)。移除历史见 [`../../../docs/CHANGELOG.md`](../../../docs/CHANGELOG.md) 阶段 7，已弃用能力的契约状态见 [product/v2.1.0/functional-requirements.md](../../../docs/product/v2.1.0/functional-requirements.md) FR-AI-13~16。
