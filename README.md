# 视无界（shiwujie）

> 面向**视障人士**的无障碍服务平台——「AI 助手 + 远程人工协助 + 社区互助」三条路径，弥补纯技术手段在真实出行、求助场景中的能力边界。

## 仓库布局

```text
Phase2/
├── docs/                  ← 文档中心（规则 + 产品契约 + 跨切面概览 + 方向/明细）
├── shiwujie-backend/      ← Spring Cloud 微服务（gateway/user/call/community/ai + model/common-web）
└── shiwujie-frontend/
    ├── app/shiwujie/      ← 原生 Android 客户端（视障者 + 志愿者双端）
    └── web/               ← Vue3 社区管理后台
```

## 文档从哪开始

| 想了解 | 进入 |
|---|---|
| 文档怎么写、内容边界在哪 | [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) |
| 项目是什么、做什么、不做什么 | [docs/product/current/product-overview.md](docs/product/current/product-overview.md) |
| 全部功能需求 / 验收标准（契约） | [docs/product/current/functional-requirements.md](docs/product/current/functional-requirements.md) |
| 跨切面架构（路由/调用图/鉴权/分库/选型） | [docs/architecture/](docs/architecture/) |
| 迭代历程（新增/变更/修复/移除） | [docs/CHANGELOG.md](docs/CHANGELOG.md) |
| 已完成 / 待实现 | [docs/ROADMAP.md](docs/ROADMAP.md) |
| 单服务技术实现（核心类/数据流/部署坑） | [shiwujie-backend/docs/](shiwujie-backend/docs/) · [shiwujie-frontend/app/docs/](shiwujie-frontend/app/docs/) · [shiwujie-frontend/web/docs/](shiwujie-frontend/web/docs/) |

> 文档分层：`docs/`（平台层·外部可见：概览 + 用户契约 + 规范 + 方向/明细）；各子项目 `docs/`（development 细化·内部详细）。详见 [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)。

## 技术栈速览

- **后端**：Spring Cloud + Nacos + Dubbo 3.3.0；业务模块 Spring Boot 2.7 / Java 17，AI 模块 Spring Boot 3.4.5 / Java 21（Spring AI Alibaba 强制）；MyBatis-Plus + MySQL 分库 + Redis（db=2 共享）。
- **前端 App**：原生 Android（Java + ViewBinding，compileSdk 35），anyRTC / 讯飞 TTS+ASR / Camera2 / 高德。
- **前端 Web**：Vue 3.3 + Ant Design Vue 4 + Pinia + Vite 4。

> 详见 [docs/architecture/tech-stack.md](docs/architecture/tech-stack.md)。
