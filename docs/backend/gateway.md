# gateway 模块

> Spring Cloud Gateway 网关，端口 8100。**仅路由 + 负载均衡，不做鉴权**。鉴权下沉到各业务服务。

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

完整路由表、端口表、Dubbo 注册 IP 坑详见 [`../architecture/gateway-dubbo.md`](../architecture/gateway-dubbo.md)。

要点：
- `lb://` 轮询（Spring Cloud LoadBalancer）。
- WebSocket 走 `lb:ws://`（原生 + SockJS 双形态），承接 `/api/ws/call`。
- Knife4j 4.4.0 手动聚合 user/call/community（**未聚合 ai**，因 SB3/OpenAPI3 不兼容）。
- 注册 IP：dev=`127.0.0.1`，prod=`47.112.114.139`（解决 Dubbo 注册 127.0.0.1，见架构文档）。

## 关键数据流

请求 → Gateway 匹配 `Path` 谓词 → LoadBalancer 选实例 → 转发。鉴权由下游各服务的 `LoginCheckInterceptor` 完成（见 [`../architecture/auth.md`](../architecture/auth.md)）。

## 功能需求（FR-GATEWAY）

- **FR-GATEWAY-01**：基于 Nacos 服务发现对 user/call/community/ai 做路径前缀路由。
- **FR-GATEWAY-02**：支持 WebSocket 与 SockJS 双形态路由。
- **FR-GATEWAY-03**：聚合各服务 Swagger（当前未聚合 ai）。
- **FR-GATEWAY-04**：提供轮询负载均衡。
- **FR-GATEWAY-05**：多机部署时正确注册对外可达 IP（dev/prod profile）。

## 验收标准（AC-GATEWAY）

- **AC-GATEWAY-01**：`curl http://<gateway>:8100/api/user/...` 经路由可达 user:8200。
- **AC-GATEWAY-02**：WebSocket 到 `/api/ws/call` 握手成功并保持长连接。
- **AC-GATEWAY-03**：`/doc.html` 展示聚合的 user/call/community 文档。
- **AC-GATEWAY-04**：停掉某实例后 LoadBalancer 不再路由到它。
- **AC-GATEWAY-05**：`-Dspring.profiles.active=prod` 后 Nacos 注册 IP=47.112.114.139。

## 已知问题

1. **Knife4j 未聚合 ai 服务**——API 文档不完整（SB2/SB3 文档协议不兼容）。
2. **网关不做鉴权**——每个业务服务都要复制一份 `LoginCheckInterceptor`（4 处重复的根因）。
3. **生产 IP 硬编码默认值**——`${nacos.address:47.112.114.139}`，部署新服务器忘记改 prod yml 则注册旧 IP。
