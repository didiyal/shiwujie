# 视无界（shiwujie）

> 面向**视障人士**的无障碍服务平台——以「AI 助手 + 远程人工协助 + 社区互助」三条路径，弥补纯技术手段在真实出行、求助场景中的能力边界。

## 这是什么

**视无界** 是一个为视障人士（盲人及低视力者）打造的无障碍服务平台。视障者在出行、识别、求助等日常场景中，常常撞上纯软件 / 硬件能力的天花板——红绿灯看不到、路牌读不出、突发状况无人帮。本平台不奢望靠单一技术解决全部问题，而是把 **AI、远程真人、社区邻里** 三种能力编排成一张兜底网：

- **AI 兜底日常**：拍照识物、文字朗读、避障引导、工具调用——能机器解决的交给机器。
- **人工补位边界**：AI 搞不定的复杂场景，一键接通志愿者视频帮扶，或呼叫家属紧急救援。
- **社区织成网络**：把分散的视障者按省 / 市 / 街道组织进社区，形成可持续的互助与求助关系。

平台围绕三类身份组织业务：

| 身份 | 代号 | 角色 |
|---|---|---|
| 视障人士 | `Blind` | 平台核心用户，消费 AI 能力、发起求助、隶属社区 |
| 志愿者 | `Volunteer` | 协助者，可被匹配进行视频帮扶，可创建 / 管理家庭与社区 |
| 家属 | `Family` | 家庭关系中的协助者，接收视障者的紧急求助通知 |

## 核心功能

> 用户可见的完整功能需求与验收标准（契约）见 [docs/product/current.md](docs/product/current.md) → `v3.0.0/`。下列为能力域概览。

### 🤖 AI 助手

- **多模型多轮对话**：文本 + 图像双通道，支持流式 TTS 语音播报。
- **工作流式工具路由**：AI 不止「能说」，更要「能做」——识别意图后触发拍照、导航、跳转应用、视频 / 紧急求助、家庭 / 社区操作等真实设备动作。
- **拍照识别与避障**：图片瘦身上下文工程，单次交互控制 token 成本。
- *演进方向*（ROADMAP）：采用 LangChain / LangGraph 重写为 Agent 驱动。

### 📹 远程视频帮扶

- 视障者一键发起求助，系统从志愿者队列 **FIFO 匹配**最近可用志愿者。
- 双向音视频通话，志愿者远程「成为视障者的眼睛」。
- 全程 WebSocket 信令驱动（`/api/ws/call`，12 信令码覆盖匹配 / 就绪 / 紧急通知 / AI 联动）。

### 🚨 紧急求助

- 家庭域内一键紧急呼叫，**群发通知所有家属**。
- 与 AI 联动：AI 识别到危险场景可主动触发紧急求助（信令 `5003`）。

### 🏘️ 社区治理

- **省 / 市 / 街道三级**社区组织，成员加入审核。
- **求助帖**：社区内发布求助、互助响应。
- **活动与报名签到**：社区活动发布、报名、签到签退。

### 👥 用户与家庭

- 三类用户账号（视障者 / 志愿者 / 家属），家庭创建 / 加入 / 审核。
- 家庭关系绑定紧急求助通知网络。

### 📱 客户端

- **Android 原生 App**（视障者 + 志愿者双端）：集成音视频 / 语音 / 相机（Camera2）/ 导航（高德），悬浮窗 + 无障碍语音服务。
- **Vue3 管理后台**（Web）：社区管理员使用的 Ant Design Vue 后台。

## 仓库布局

```text
Phase2/
├── docs/                  ← 文档中心（规则 + 产品契约 + 跨切面概览 + 方向/明细）
├── shiwujie-backend/      ← 单体后端（v3.0.0：model 契约层 + bootstrap 唯一 app，含原 user/call/community/ai/common-web）
└── shiwujie-frontend/
    ├── app/shiwujie/      ← 原生 Android 客户端（视障者 + 志愿者双端）
    └── web/               ← Vue3 社区管理后台
```

## 文档从哪开始

| 想了解 | 进入 |
|---|---|
| 文档怎么写、内容边界在哪 | [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) |
| 项目是什么、做什么、不做什么 | [docs/product/current.md](docs/product/current.md) → product-overview |
| 全部功能需求 / 验收标准（契约） | [docs/product/v3.0.0/functional-requirements.md](docs/product/v3.0.0/functional-requirements.md) |
| 跨切面架构（路由/调用图/鉴权/分库/选型） | [docs/architecture/](docs/architecture/) |
| 迭代历程（新增/变更/修复/移除） | [docs/CHANGELOG.md](docs/CHANGELOG.md) |
| 已完成 / 待实现 | [docs/ROADMAP.md](docs/ROADMAP.md) |
| 单服务技术实现（核心类/数据流/部署坑） | [shiwujie-backend/docs/](shiwujie-backend/docs/) · [shiwujie-frontend/app/docs/](shiwujie-frontend/app/docs/) · [shiwujie-frontend/web/docs/](shiwujie-frontend/web/docs/) |

> 文档分层：`docs/`（平台层·外部可见：概览 + 用户契约 + 规范 + 方向/明细）；各子项目 `docs/`（development 细化·内部详细）。详见 [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)。

## 技术栈速览

- **后端**：v3.0.0 单体（model 契约层 + bootstrap 唯一 app）；统一 Spring Boot 3.4.5 / Java 21（Spring AI Alibaba 强制）；MyBatis-Plus + MySQL 单库（shiwujie）+ Redis（db=2）；无 gateway/Nacos/Dubbo。
- **前端 App**：原生 Android（Java + ViewBinding，compileSdk 35），anyRTC / 讯飞 TTS+ASR / Camera2 / 高德。
- **前端 Web**：Vue 3.3 + Ant Design Vue 4 + Pinia + Vite 4。

> 详见 [docs/architecture/tech-stack.md](docs/architecture/tech-stack.md)。
