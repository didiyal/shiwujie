# 后端文档（development 细化）

> 后端微服务技术实现文档。**不含用户契约**——FR/AC/HTTP 路径/WebSocket 码/Dubbo 接口清单在根 [product/current.md](../../docs/product/current.md)；跨切面（路由/调用图/鉴权/分库/选型）在根 [architecture/](../../docs/architecture/)。规范见 [docs/CONTRIBUTING.md](../../docs/CONTRIBUTING.md)。

## 目录

| 文档 | 内容 |
|---|---|
| [modules/gateway.md](modules/gateway.md) | 网关：路由表配置、核心类、数据流 |
| [modules/model-commonweb.md](modules/model-commonweb.md) | 契约层（model）+ SB2 公共层（common-web） |
| [modules/user.md](modules/user.md) | 三类用户、家庭、JWT 签发 |
| [modules/call.md](modules/call.md) | WebSocket 信令中枢、视频/紧急求助、InnerSocket |
| [modules/community.md](modules/community.md) | 三级社区、审核、求助帖、活动报名 |
| [modules/ai.md](modules/ai.md) | ★ AI 大脑：多模型、自研记忆、工具路由、图片瘦身 |
| [known-issues.md](known-issues.md) | 缺陷/技术债登记（🔴 安全洞同步 ROADMAP） |
| [deployment.md](deployment.md) | 启动命令、Dubbo 注册 IP 坑、端口/防火墙、Docker |

## 微服务拓扑

```
gateway :8100 ──路由+LB──> user :8200 / call :8300 / community :8400 / ai :8500
                           Dubbo 端口 21200 / 21300 / 21400 / 21500
ai  ─Dubbo─> user(Blind/Family) 、 call(InnerSocket)
call/community ─Dubbo─> user ;  user ─Dubbo─> community
```

> 完整路由表 + Dubbo 契约清单 + 调用图见 [../../docs/architecture/gateway-dubbo.md](../../docs/architecture/gateway-dubbo.md)。
