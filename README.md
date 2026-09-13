# 视无界（shiwujie）

> 一款专为视障人士打造的 AI 助手应用——开口就能用：对话问答、拍照识物、打开应用、规划路线、一键求助。

## 视无界能做什么

视障朋友在使用手机时常常遇到障碍：屏幕上的字看不见、界面找不到、想做的事情不知道怎么操作。视无界把这一切变成**说话就能解决**：

- **和 AI 助手"小界"聊天**：想查什么问什么，小界联网搜索后语音告诉你；
- **拍照识别**：说"帮我识别前面"，小界自动拍照，告诉你眼前是什么，看不清的可以继续追问；
- **语音操控**：说"帮我打开微信"就能打开应用，说"我要去哪里"就能规划路线；
- **一键求助**：紧急情况说一声就联系家人视频；需要人帮您"看"时，连线志愿者；
- **AI 悬浮球**：退出软件后屏幕上留着一颗小界悬浮球，任何时候点一下就回来。

视无界同时提供家庭与社区功能：与家人绑定后可随时视频求助，也能在社区里参与活动、发布互助。

## 三类用户

| 身份 | 说明 |
|---|---|
| 视障人士 | 平台的核心用户，使用 AI 助手、发起求助、加入家庭与社区 |
| 志愿者 | 通过视频连线远程协助视障人士，可参与社区服务 |
| 家属 | 与视障家人绑定，第一时间接收紧急求助并视频接入 |

## 下载使用

- 官网下载页直接安装 Android 应用，应用内支持自动更新
- 软件内置无障碍支持（TalkBack 全流程可用），首次进入有语音友好的功能引导

## 仓库组成

```text
Phase2/
├── docs/                  ← 文档中心（规范 + 产品契约 + 架构 + 更新日志）
├── shiwujie-backend/      ← 后端服务（Spring Boot 单体）
├── shiwujie-frontend/
│   ├── app/shiwujie/      ← Android 应用（视障者 + 志愿者双端）
│   └── web/               ← 官网与管理后台
├── shiwujie-ai/           ← AI 智能体服务（LangGraph，持续演进中）
└── scripts/ docker/ config/  ← 部署脚本与配置
```

## 文档入口

| 想了解 | 进入 |
|---|---|
| 文档怎么写、分层规范 | [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) |
| 项目是什么、做什么 | [docs/product/current.md](docs/product/current.md) |
| 全部功能需求 | [docs/product/v3.0.0/functional-requirements.md](docs/product/v3.0.0/functional-requirements.md) |
| 架构概览 | [docs/architecture/overview.md](docs/architecture/overview.md) |
| 更新日志 | [docs/CHANGELOG.md](docs/CHANGELOG.md) |
| 各端技术细节 | [shiwujie-backend/docs/](shiwujie-backend/docs/) · [shiwujie-frontend/app/docs/](shiwujie-frontend/app/docs/) · [shiwujie-frontend/web/docs/](shiwujie-frontend/web/docs/) |

## 版本

- 当前版本 **v3.0.0**（2026-09-12 发布）：AI 优先的全新体验、句子级语音播报、强制更新机制
- 版本沿革：`v1.0`（项目起点）→ `v2.0.0` → `v2.1.0` → `v3.0.0`，详见 [docs/CHANGELOG.md](docs/CHANGELOG.md)
