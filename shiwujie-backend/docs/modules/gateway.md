# gateway 模块（✅ v3.0.0 已整体删除）

> ⚠️ **本模块已于 v3.0.0 单体化阶段2.1（`4f10d11`）整体删除**——Spring Cloud Gateway 是纯路由 + 负载均衡（无 Java 业务逻辑、不做鉴权），单体化后对外路径由 `shiwujie-bootstrap` 的 controller 直接承接（路径内化见阶段2.4）。本文件保留作 v2.1.0 历史参考。

## v2.1.0 历史

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-gateway/`（已删） |
| 启动类 | `GatewayApplication`（`@EnableDiscoveryClient`） |
| 端口 | 8100，context-path `/` |
| 框架 | SB 2.7.0 + Java 17 + Spring Cloud Gateway |
| 职责 | 路径前缀路由、WebSocket 双形态路由、Knife4j 聚合、轮询 LB |

- 完整路由表 / 端口表见 [architecture/gateway-dubbo.md](../../../docs/architecture/gateway-dubbo.md)（v2.1.0 历史段）。
- 鉴权下沉各业务服务的 `LoginCheckInterceptor`（v3.0.0 已收敛为 common-web 单份，阶段2.5）。
- 已知缺陷（Knife4j 未聚合 ai、4 处拦截器重复、生产 IP 硬编码默认值）均随模块删除消灭，✅ 标记见 [known-issues.md](../known-issues.md) gateway 段。

> 当前对外入口 = `shiwujie-bootstrap` 单进程（端口 8100 复用原 gateway），详见 [deployment.md](../deployment.md)「v3.0.0 单体部署」。
