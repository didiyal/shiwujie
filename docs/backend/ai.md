# ai 模块

> 视障辅助 App 的 AI 大脑。Spring AI Alibaba 多模型对话 + 自研记忆 + 工作流式工具路由 + 图片瘦身上下文工程。Dubbo **纯消费方**（无对外 Inner 服务）。

> 这是全套文档**最重要的一篇**——AI Agent 方向的核心工程价值在此。本文同时**如实标注**已弃用/未启用/未完成的能力。

## 模块定位

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-ai/` |
| 端口 | 8500（无 context-path） |
| Dubbo 端口 | 21500 |
| 框架 | **SB 3.4.5 + Java 21** + Spring AI 1.0.0 + spring-ai-alibaba 1.0.0.2 + Dubbo 3.3.0 |
| MySQL | `shiwujieai`（AiLogs） |
| Redis | 用 `spring.data.redis`（SB3 规范，非 `spring.redis`），db=2 |
| 角色 | Dubbo **纯消费者**（无 `@DubboService`），仅 HTTP/SSE 对外 |

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

## Dubbo 消费契约（仅消费，不提供）

全模块 `@DubboReference` 共 **3 处**：

| 消费类 | Inner 服务 | 提供方 | 用途 |
|---|---|---|---|
| `UserTools.java:30` | `InnerFamilyService` | user | joinFamily / userLeaveFromFamily / getFamilyVOById |
| `FrontendTools.java:22` | `InnerSocket` | call | noticeTakePhoto/VideoHelp/UrgentHelp/JumpSoftware/Navigation（5001/5002/5003/5004/5006） |
| `LoginCheckInterceptor.java:36` | `InnerBlindService` | user | getById 查盲人（鉴权） |

> **未注入任何 community Inner 服务**（见疑点 #2）。

## 工具索引（ToolChoiceCenter，case 1–9 生效）

| case | 工具 | 实现路径 | 触发推送 |
|---|---|---|---|
| 1 | 拍照识别 | `FrontendTools.noticeTakePhoto` | Dubbo→call→WS 5001 |
| 2 | 图片追问 | `AiModelTools.TakePhoto`（调 ImageApp） | — |
| 3 | 跳转应用 | `FrontendTools.noticeJumpSoftware` | WS 5004 |
| 4 | 高德导航 | `FrontendTools.noticeNavigation` | WS 5006 |
| 5 | 视频求助 | `FrontendTools.noticeVideoHelp` | WS 5002 |
| 6 | 紧急求助 | `FrontendTools.noticeUrgentHelp` | WS 5003 |
| 7 | 加入家庭 | `UserTools`→InnerFamily.joinFamily | — |
| 8 | 家庭信息 | `UserTools`→InnerFamily.getFamilyVOById | — |
| 9 | 退出家庭 | `UserTools`→InnerFamily.userLeaveFromFamily | — |

> **生效工具仅 1–9**。default 分支返回"请输入1-18"是历史残留注释。

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
- Dubbo 21500；`compatibility-verifier.enabled: false`（SB3.4.5 + SCA 共存需要）。
- Spring Cloud 注册 IP 由 dev/prod profile 覆盖；**Dubbo 注册 IP 需启动命令 `-DDUBBO_IP_TO_REGISTRY`**（discovery.ip 对 Dubbo 无效，见 [`../architecture/gateway-dubbo.md`](../architecture/gateway-dubbo.md)）。
- 凭据：`${DASHSCOPE_API_KEY:...}` / `${SEARCH_API_KEY:...}`。

## 功能需求（FR-AI）

| ID | 需求 | 状态 |
|---|---|---|
| FR-AI-01 | 文本多轮对话（流式 SSE） | ✅ |
| FR-AI-02 | 图片识别（多模态流式） | ✅ |
| FR-AI-03 | 自定义 ChatMemory（Redis+MySQL 双写 + kryo） | ✅ |
| FR-AI-04 | 工具路由（9 类业务工具，约定 JSON） | ✅ |
| FR-AI-05 | 拍照识别 / 图片追问 | ✅ |
| FR-AI-06 | 视频求助 / 紧急求助（前端跳转） | ✅ |
| FR-AI-07 | 家庭加入/退出/查询 | ✅ |
| FR-AI-08 | 高德导航 / 跳转他应用 | ✅ |
| FR-AI-09 | 图片瘦身上下文工程（占位符 + MySQL 回放） | ✅ |
| FR-AI-10 | Web 搜索（searchapi.io / baidu + jsoup 摘要） | ✅ |
| FR-AI-11 | 登录鉴权（JWT+Redis+Dubbo 查 Blind） | ✅（带默认用户兜底） |
| FR-AI-12 | AI 操作日志（MySQL AiLogs） | ✅ |
| FR-AI-13 | RAG 知识库增强 | ❌ **已弃用**（半残留 Bean，未注入） |
| FR-AI-14 | 自研 ReAct Agent | ❌ **未启用** |
| FR-AI-15 | MQTT/IoT 硬件接入 | ❌ **已取消**（pom 残留） |
| FR-AI-16 | community 求助帖工具 | ❌ **未实现**（仅提示词残留） |

## 验收标准（AC-AI）

| ID | 标准 |
|---|---|
| AC-AI-01 | 文本对话返回 SSE 流 |
| AC-AI-02 | 图片对话返回 SSE 流且 100 字内 |
| AC-AI-03 | 上下文按 blindId 隔离（CONVERSATION_ID） |
| AC-AI-04 | 文本上下文窗口=10 轮 |
| AC-AI-05 | 图片上下文窗口=6 轮 |
| AC-AI-06 | Redis key 前缀统一 `chat:memory:` |
| AC-AI-07 | 工具调用走约定 JSON（非原生 function-calling） |
| AC-AI-08 | 工具索引覆盖 1-9 且无 community |
| AC-AI-09 | 非末条历史图片被脱敏（移除 Media） |
| AC-AI-10 | 末条图片存 MySQL 换占位符 `image{logId}` |
| AC-AI-11 | Dubbo 消费仅 family/socket/blind（3 处） |
| AC-AI-12 | 模块无 RPC 暴露 |
| AC-AI-13 | MyLoggerAdvisor 记录 token 大小 |
| AC-AI-14 | 默认用户兜底（调试便利，生产风险） |
| AC-AI-15 | 诚实缺口：无压测 / 无索引优化 / 无 Docker |

## 已知问题与「试错-移除」能力（诚实记录）

### 疑点 #1：自研 ReAct Agent 未启用 —— ✅ 已坐实

自研 ReAct 框架（`BaseAgent`/`ReActAgent`/`ToolCallAgent`/`MyManus`）**完整存在但未启用**，线上仅 `ToolChoiceApp` 工作流生效。

- `MyManus.java:9`：`//@Component` **被注释**（唯一具体子类未注册为 Bean → 整条继承链无 Bean 入容器）。
- `BaseAgent`/`ReActAgent`/`ToolCallAgent` 均无 `@Component`；`TerminateTool` 也无。
- `ToolCallAgent` 自维护上下文（`withInternalToolExecutionEnabled(false)` + 自管 messageList）——正是「重复调用工具」问题之源。
- 全模块无任何注入 MyManus 的代码；实际生效路径是注入 `ToolChoiceApp`。

> 弃用原因：自写 ReAct 出现「工具重复调用 / 调用失败」问题，周期紧放弃，改为工作流式路由（模型输出约定 JSON、后端解析分发）。**这是真实工程权衡**。

### 疑点 #2：community 求助帖工具未注入 —— ✅ 已坐实

community 的 Inner 服务**完全未被消费**。提示词 `toolChoice-template.txt` 中"处理用户、家庭、社区、活动、求助帖相关操作"是**历史残留**，无对应工具实现；ToolChoiceCenter switch 无 community 分支。

### mqtt：已取消但 pom 残留 —— ✅ 已坐实

MQTT 代码**已全部删除**，仅 `pom.xml:30-48` 残留 paho 依赖（且重复 2 次，连 jackson-databind 也重复）。grep `mqtt|MqttClient|paho` 命中 **0 个 .java 文件**。本是为硬件 IoT 设计，后因硬件成本取消。

> 该能力在 [`../CHANGELOG.md`](../CHANGELOG.md) 阶段 7 以「移除/变更」形式记录。

### RAG：半残留 —— ✅ 已坐实

`MyRagAdvisor`（`@Configuration` + Bean，知识库名 `视无界`）启动时仍会初始化，但 `TextApp.java:53` 的 `// myRagAdvisor` **被注释**未加入 defaultAdvisors → 运行时不参与任何对话。属"删了一半"。pom 无独立向量库依赖（用 DashScope 托管 RAG）。

### 与 CLAUDE.md 性能叙事的对照

| 论断 | 代码佐证 | 一致性 |
|---|---|---|
| 自研 Memory：Redis 精简 + MySQL 全量 | `*ChatMemoryRes implements ChatMemoryRepository` | ✅ |
| kryo 序列化 | `MessageSerializer` 用 kryo + ThreadLocal + Base64 | ✅ |
| 双模型上下文共享 | 共享 KEY_PREFIX + blindId 作 conversationId | ✅（共享读） |
| 图片瘦身：占位符替换 | `image{logId}` 占位 + MySQL 回放 | ✅ |
| 首字 20s+→1-3s、整轮~7s | 瘦身降低文本模型上下文 token | ✅ 机制成立（**无压测日志佐证具体数字**，属开发者实测口述） |
| 自写 ReAct 工具重复调用坑 | ToolCallAgent 自管 messageList + ToolCallingManager | ✅ 机制可信 |
| spring-ai-alibaba 1.0.0.1 | pom 实际 1.0.0.2 | ⚠ GA 后小版本迭代 |
| 图像模型 | 代码为 qwen3-vl-flash（CLAUDE.md 未具名） | ✅ |
| Bocha 搜索 | 实际 searchapi.io 聚合的 baidu 引擎 | ⚠ 措辞对齐 |

### 其它发现

1. **ToolChoiceAppChatMemoryRes.saveAll 空实现**：路由阶段不写 Memory（省 token），靠共享 key 读文本侧历史——非显然设计，需文档说明。
2. **TextApp Redis TTL=10 分钟**（注释写"5 天"），ImageApp=5 天。**注释与代码不一致**；文本侧 10 分钟即过期，可能有意（文本对话即时性强）也可能是 bug。
3. **LoginCheckInterceptor 默认用户兜底**（line 52-60）：无 Authorization 时注入 blindId=1 / phone=19872250169。**生产后门**——任何人可白嫖 AI（消耗 DashScope token），见 [`../architecture/auth.md`](../architecture/auth.md) 风险 #6。
4. **CORS 全开**（`http://*:*` + allowCredentials=true）。
5. **无压测 / 无索引优化 / 无 Docker**（诚实缺口）：AiLogs 仅 `idx_ailogs_operator_time` 单索引；图片占位符用 logId 主键查 AiLogs = 把日志表当 KV 存储。
6. **`@EnableScheduling`** 在启动类但无 `@Scheduled` 方法（残留）。
7. **ToolIndex 枚举与 ToolChoiceCenter switch 不完全一致**：枚举是给 ReAct 用的旧映射，随框架弃用失同步；文档以 **switch case 1-9 为准**。
