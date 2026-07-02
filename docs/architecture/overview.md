# 项目总览

> 跨前后端的共享架构文档。本篇定义项目定位、覆盖范围与边界；具体模块细节见 [`../backend/`](../backend/) 与 [`../frontend/`](../frontend/)。

## 定位

**视无界（shiwujie）** 是面向**视障人士**的无障碍服务平台，通过「AI 助手 + 远程人工协助 + 社区互助」三条路径，弥补纯技术手段在真实出行、求助场景中的能力边界。

平台围绕三类身份组织业务：

| 身份 | 代号 | 角色 |
|---|---|---|
| 视障人士 | `Blind` | 平台核心用户，消费 AI 能力、发起求助、隶属社区 |
| 志愿者 | `Volunteer` | 协助者，可被匹配进行视频帮扶、可创建/管理家庭与社区 |
| 家属 | `Family` | 家庭关系中的协助者，接收视障者的紧急求助通知 |

## 覆盖范围

二期平台覆盖以下能力域：

- **AI 助手**：多模型多轮对话（文本 + 图像）、工具路由、流式 TTS、图片识别与避障。详见 [`../backend/ai.md`](../backend/ai.md)。
- **远程视频帮扶**：视障者一键发起，系统从志愿者队列 FIFO 匹配，anyRTC 双向音视频。详见 [`../backend/call.md`](../backend/call.md)。
- **紧急求助**：家庭域内紧急呼叫，WebSocket 群发通知所有家属。
- **社区治理**：省/市/街道三级社区组织，成员加入审核、求助帖、活动与报名签到。详见 [`../backend/community.md`](../backend/community.md)。
- **用户与家庭**：三类用户账号、家庭创建/加入/审核。详见 [`../backend/user.md`](../backend/user.md)。
- **Android 客户端**：原生应用，集成 anyRTC/讯飞/CameraX/高德，悬浮窗与无障碍 TTS。详见 [`../frontend/app-android.md`](../frontend/app-android.md)。
- **Web 管理后台**：社区管理员使用的 Vue3 后台。详见 [`../frontend/web-vue.md`](../frontend/web-vue.md)。

## 核心目标

1. **AI 可执行**：不止「能对话」，更要「能执行」——通过工具路由让 AI 触发拍照、导航、跳转应用、视频/紧急求助、家庭操作等真实设备动作。
2. **人工兜底**：AI 解决不了的，一键接入志愿者视频帮扶或家属紧急求助。
3. **社区连接**：把分散的视障者组织进社区，形成可持续的互助与求助网络。
4. **微服务化与可演进**：Spring Cloud + Nacos + Dubbo 解耦各业务域，AI 栈独立演进（Spring AI Alibaba）而不拖累业务模块。

## 非目标

为防范围蔓延，以下明确**不在**当前平台目标内：

- **硬件 IoT 接入**：曾设计 MQTT 通道对接硬件（避障/传感器），因硬件成本问题**已取消**（git 历史可溯，见 [`../CHANGELOG.md`](../CHANGELOG.md) 阶段 7）。当前避障为纯软件方案。
- **多服务器/多区域部署**：当前为**单服务器**（`47.112.114.139` 同时承载基础设施与注册中心）。多机部署的 nacos/dubbo 通信已验证，但生产规模仍为单机。
- **RAG 知识库**：曾尝试接入 DashScope 托管 RAG，效果不及预期**已移除**（代码半残留，未启用，见 [`../backend/ai.md`](../backend/ai.md)）。
- **自研 ReAct Agent**：自研 Agent 框架（`MyManus`）因「重复调用工具」问题**已弃用**，线上走工作流式工具路由（`ToolChoiceApp`）。代码保留但未启用。
- **高并发/压测**：未做压力测试、无 Docker 化、无数据库索引调优（诚实缺口）。
- **网关统一鉴权**：网关仅路由，鉴权下沉到各业务服务（曾评估在网关统一鉴权，因重构量过大放弃）。

## 两期演进

| 维度 | 一期（2025-06 封版） | 二期（2025-07 至今） |
|---|---|---|
| 客户端 | uniapp | **原生 Android（Java + ViewBinding）** |
| 后端 | Spring 单体 | **Spring Cloud 微服务**（gateway + user + call + community + ai） |
| 数据库 | 单库 4 表 | **分库**：user / call / community / ai 各自独立库，~13 张主表 |
| 通信 | HTTP | HTTP + **Dubbo RPC** + **WebSocket** |
| AI | 无 | Spring AI Alibaba（qwen3-max + qwen3-vl-flash）+ 工具路由 |
| 注册发现 | 无 | **Nacos** |

> 一期代码已封版于 commit `423c0fa`，作为二期演进的起点保留在 git 历史。

## 能力-技术栈-时间 对照表

| 时间 | 新增能力 | 关键技术栈 | 阶段 |
|---|---|---|---|
| 2025-06 | 单体平台封版 | uniapp + Spring 单体 | 阶段 0 |
| 2025-07 初 | 微服务脚手架 | Spring Cloud + Nacos + JWT/Redis | 阶段 1 |
| 2025-07 | 用户与家庭 | 三类账号、家庭审核 | 阶段 2 |
| 2025-07 中末 | 视频通话 + 紧急求助 | call 模块、Netty socket（早期） | 阶段 3 |
| 2025-08 初 | 社区治理 | community 六大主体 | 阶段 4 |
| 2025-08 中 | AI 从零到能用 | Spring AI、向量库、dubbo+MCP、工具调用 | 阶段 5 |
| 2025-08 末 | AI 能执行 | 避障、高德导航、跳转应用、图片瘦身、分库 | 阶段 6 |
| 2025-10~11 | 引擎升级 | Spring AI Alibaba M6→1.0、工作流（mqtt/RAG/ReAct 试错后移除） | 阶段 7 |
| 2026-01 | 分布式与生产化 | Netty→Spring WebSocket、gateway 负载均衡、多机 nacos+dubbo | 阶段 8 |
| 2026-07 | 工程化收尾 | call 路由对齐、dev/prod 多环境、凭据占位符化 | 阶段 9 |

完整的迭代历程（新增/变更/修复/移除四分类）见 [`../CHANGELOG.md`](../CHANGELOG.md)。
