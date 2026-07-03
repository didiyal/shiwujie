# 后端部署

> 启动命令、Dubbo 注册 IP 坑、端口/防火墙、Docker。development 层（允许启动命令与 yml 片段）。路由表与端口表见 [architecture/gateway-dubbo.md](../../docs/architecture/gateway-dubbo.md)。

## 端口与基础设施

| 模块 | HTTP | context-path | Dubbo 端口 | MySQL 库 | Redis db |
|---|---|---|---|---|---|
| gateway | 8100 | `/` | — | — | — |
| user | 8200 | `/api/user` | 21200 | shiwujieuser | 2 |
| call | 8300 | `/api` | 21300 | shiwujiecall | 2 |
| community | 8400 | `/api/community` | 21400 | shiwujiecommunity | 2 |
| ai | 8500 | （未设） | 21500 | shiwujieai | 2 |

> Dubbo provider 端口用 21200–21500，避让 Windows Hyper-V/WSL2 动态保留的 TCP 排除段（原 502xx 落入会 bind 抛 `Address already in use` 而 `netstat` 查无进程）。见 [CHANGELOG.md](../../docs/CHANGELOG.md) 阶段 9。

## dev / prod 拓扑

- **dev**：MySQL/Redis 连服务器（共享数据），但 **Nacos（Spring Cloud 发现 + Dubbo 注册中心）走本机**——整套服务在本机自洽，纯本机 dev 不存在跨网注册问题。
- **prod**：全部连服务器（`47.112.114.139` 单机承载基础设施与注册中心）。
- Nacos 地址由 `${nacos.address:47.112.114.139}` 占位符驱动：dev profile=`127.0.0.1`，prod profile=`47.112.114.139`；命令行 `-Dnacos.address=X` 优先级最高。
- 凭据已抽取为 `${ENV:default}` 占位符（`MYSQL_PASSWORD`/`REDIS_PASSWORD`/`NACOS_USERNAME`/`NACOS_PASSWORD`/`DASHSCOPE_API_KEY`/`SEARCH_API_KEY`），default 值仍硬编码在 yml（按约定保留在配置里）。

## 生产部署：Dubbo 注册 IP 不可达坑（两条独立注册链路）

> **适用范围**：prod / 多机部署。纯本机 dev（Nacos 走本机、服务全在本机、注册 `127.0.0.1`）**不触发**。坑只在「部署到服务器 / 多机 / 本机连服务器 Nacos」时发作（本机服务把私网 IP 注册进服务器 Nacos，服务器侧回连不到）。

**问题**：Dubbo 在 yml 未显式配置注册 IP 时自动枚举网卡挑一个，常选中不可跨机路由的地址——`127.0.0.1`（回环）、`172.17.0.1`（Docker `docker0` 网桥）、各类虚拟网卡（Hyper-V/WSL/vEthernet）。消费者日志 `failed to connect to server /<不可达IP>:<port> ... client-side timeout`。

**根因——两条独立 Nacos 注册链路，IP 由不同机制控制**：

| 注册链路 | 用途 | IP 控制方式 |
|---|---|---|
| Spring Cloud Nacos Discovery | 网关 `lb://` 路由发现服务实例 | `spring.cloud.nacos.discovery.ip`（dev/prod profile） |
| Dubbo Registry（`dubbo.registry.address: nacos://...`） | Dubbo RPC 消费者发现提供者 | **`-DDUBBO_IP_TO_REGISTRY`（启动命令 JVM 参数）**——yml 中无任何 Dubbo IP 配置 |

Dubbo 经 `dubbo.registry.address` **独立**注册到 Nacos，**不复用** Spring Cloud 的 `discovery.ip`。故 `spring.cloud.nacos.discovery.ip` 只修正网关 `lb://` 实例 IP，**对 Dubbo 注册地址无效**。实测部署仅配 `discovery.ip` 时 Dubbo 仍注册 `127.0.0.1`、消费者无法 RPC；**生产必须在启动命令加 `-DDUBBO_IP_TO_REGISTRY=<公网IP>`。**

**生产启动命令（实测，两个参数都要）**——user/call/community/ai 四服务同构，仅 jar 名不同：

```bash
java -Xms128m -Xmx256m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m \
  -DDUBBO_IP_TO_REGISTRY=47.112.114.139 \
  -jar /home/liu/shiwujie/shiwujieUser-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.cloud.nacos.discovery.ip=47.112.114.139
```

> `--spring.cloud.nacos.discovery.ip` 与 prod profile 内的值一致（命令行再传为双保险）。多环境重构（commit `2e5573a`）只调整了 `discovery.ip` 与凭据占位符化，**未改动 Dubbo 注册机制**，故 `-DDUBBO_IP_TO_REGISTRY` 的必要性是结构性的，并非重构引入的回归。

**端口可达性（同一超时症状的第二大病因）**：注册 IP 改对之后，消费者仍要直连 `dubbo://<IP>:<port>`。故须：① 云安全组/防火墙放行 Dubbo 端口（user 21200 / call 21300 / community 21400 / ai 21500）；② 若提供者跑在容器里，`docker run -p 21400:21400` 发布端口，否则注册了正确 IP 也连不进容器。改完 IP 仍超时，几乎都是这两条没做。

**更省事的写法（把 IP 收进配置，免启动命令传参）**：`-DDUBBO_IP_TO_REGISTRY` 是 JVM 系统属性，yml 改不了它。想省掉，可用 `dubbo.protocol.host`——普通 Dubbo 配置项，可写进 dev/prod profile，与 `discovery.ip` 完全对称：

```yaml
# application-dev.yml
dubbo:
  protocol:
    host: 127.0.0.1   # 本机联调；多机联调填本机 LAN IP
# application-prod.yml
dubbo:
  protocol:
    host: 47.112.114.139
```

注意：`dubbo.protocol.host` **同时控制绑定与注册**，要求该 IP 是本机真实网卡地址。dev 本机 IP 总能满足；**prod 若是云厂商 NAT 弹性 IP（公网 IP 不在网卡上，本机只有私网 172.x），`protocol.host=公网IP` 会让 Dubbo 尝试绑定该 IP 而失败**——这种情况 prod 仍须保留 `-DDUBBO_IP_TO_REGISTRY`（只改注册、绑定照旧本地，是 NAT 场景正解）。若提供者跑在 Docker 且宿主为 Linux，`docker run --network host`（或 compose `network_mode: host`）让容器共享宿主网卡，Dubbo 自动探测正确 IP，连 IP 配置和端口映射都省了。

**残留风险**：`${nacos.address:47.112.114.139}` 把生产 IP 硬编码进代码库默认值；新服务器部署需**同时**改 prod yml 的 `discovery.ip` 与启动命令的 `DUBBO_IP_TO_REGISTRY`。
