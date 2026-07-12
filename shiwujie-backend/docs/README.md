# 后端文档（development 细化）

> 后端单体（v3.0.0）技术实现文档。**不含用户契约**——FR/AC/HTTP 路径/WebSocket 码/接口清单在根 [product/current.md](../../docs/product/current.md)；跨切面（路由/调用图/鉴权/分库/选型）在根 [architecture/](../../docs/architecture/)。规范见 [docs/CONTRIBUTING.md](../../docs/CONTRIBUTING.md)。

## 目录

| 文档 | 内容 |
|---|---|
| [modules/bootstrap.md](modules/bootstrap.md) | ★ 单体唯一 app：聚合原 user/call/community/ai/common-web，子包结构 + 跨模块本地调用 |
| [modules/model-commonweb.md](modules/model-commonweb.md) | 契约层（model）；common-web 已并入 bootstrap |
| [modules/gateway.md](modules/gateway.md) | （已删历史标记）原网关路由表 |
| [known-issues.md](known-issues.md) | 缺陷/技术债登记（🔴 安全洞同步 ROADMAP） |
| [deployment.md](deployment.md) | 单 jar 启动、MySQL/Redis；（历史段）Dubbo 注册 IP 坑、端口/防火墙 |

## 单体拓扑（v3.0.0）

```
单体 bootstrap :8100（context-path /，原 gateway/user/call/community/ai/common-web 五模块代码同进程）
  ├─ HTTP 路由内化：/api/user/**  /api/call/**  /api/community/**  /api/ai/**
  ├─ WebSocket：/api/ws/call（@ServerEndpoint，12 信令）
  └─ 跨模块调用 = 同进程 Bean 注入（原 Dubbo Inner* 接口退化为本地的 @Service/@Resource）
外部依赖：MySQL 库 shiwujie + Redis db=2（无 Nacos / Dubbo / gateway）
```

> 完整路由表 + 原 Dubbo 契约清单（v3.0.0 退化为本地调用）+ 调用图见 [../../docs/architecture/gateway-dubbo.md](../../docs/architecture/gateway-dubbo.md)。
