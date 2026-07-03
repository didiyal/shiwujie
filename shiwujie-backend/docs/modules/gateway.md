# gateway 模块

> Spring Cloud Gateway 网关，端口 8100。**仅路由 + 负载均衡，不做鉴权**（鉴权下沉各业务服务）。本文为 development 细化；用户可见契约（FR-GATEWAY / AC-GATEWAY）见 [product/current/](../../../docs/product/current/)。

## 模块定位

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-gateway/src/main/java/com/swj/shiwujie/` |
| 启动类 | `GatewayApplication`（`@EnableDiscoveryClient`） |
| 端口 | 8100，context-path `/` |
| 框架 | Spring Boot 2.7.0 + Java 17 + Spring Cloud Gateway |
| 职责 | 路径前缀路由到各微服务、WebSocket 双形态路由、Knife4j 文档聚合、轮询负载均衡 |

## 核心类

| 类 | 职责 |
|---|---|
| `GatewayApplication` | 网关入口，开启 Nacos 服务发现 |
| `application.yml` | 路由表（route_user/call/community/ai + 两条 ws 路由） |
| `application-dev.yml` / `application-prod.yml` | 仅覆盖 `spring.cloud.nacos.discovery.ip` |

## 路由与配置

完整路由表、端口表详见 [`../../../docs/architecture/gateway-dubbo.md`](../../../docs/architecture/gateway-dubbo.md)；Dubbo 注册 IP 坑、启动命令、端口/防火墙见 [`../deployment.md`](../deployment.md)。

要点：
- `lb://` 轮询（Spring Cloud LoadBalancer）。
- WebSocket 走 `lb:ws://`（原生 + SockJS 双形态），承接 `/api/ws/call`。
- Knife4j 4.4.0 手动聚合 user/call/community（**未聚合 ai**，因 SB3/OpenAPI3 不兼容）。
- Spring Cloud 注册 IP：dev=`127.0.0.1`，prod=`47.112.114.139`（profile 覆盖 `discovery.ip`）；**Dubbo 注册 IP 另需启动命令 `-DDUBBO_IP_TO_REGISTRY`**（见 [`../deployment.md`](../deployment.md)）。

## 关键数据流

请求 → Gateway 匹配 `Path` 谓词 → LoadBalancer 选实例 → 转发。鉴权由下游各服务的 `LoginCheckInterceptor` 完成（见 [`../../../docs/architecture/auth.md`](../../../docs/architecture/auth.md)）。

> 已知缺陷（Knife4j 未聚合 ai、网关不做鉴权致 4 处拦截器重复、生产 IP 硬编码默认值）见 [`../known-issues.md`](../known-issues.md)。
