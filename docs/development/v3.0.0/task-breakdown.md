# v3.0.0 任务拆解

> 本文件属 `development/v3.0.0/`（单体化改造·进行中）。**交付范围**（单体化目标）+ **任务**（随单体化落地）。版本指针 [../current.md](../current.md)。大方向与废弃项见 [../../ROADMAP.md](../../ROADMAP.md) v3.0.0 段。

## v3.0.0 交付范围（承接 ROADMAP 待实现）

### 单体重写

> 文档先写：v3.0.0 架构目标态已写入 [tech-stack](../../architecture/tech-stack.md) / [data-model](../../architecture/data-model.md) / [gateway-dubbo](../../architecture/gateway-dubbo.md) 的「v3.0.0 目标」段；本节为实现 spec。两阶段推进，验证点见 [testing-strategy](testing-strategy.md)。

**契约保护铁律**（全程不得违反）：对外 HTTP 路径 `/api/{user,call,community,ai}/**`、WebSocket `/api/ws/call` + 12 信令码（`-1/0/1/2/3/4/5001~5006`）、HTTP/业务状态码、返回字段名——**零变更**，前端 App/Web 不改即可对接。

#### 阶段 1 · 业务模块升 SB 3.4.5 / Java 21（仍微服务，逐模块验证可启动）

> 消除 SB 双栈根因（Spring AI 1.0 强制 SB3/Java21）。ai 模块为迁移样板。

- [x] 1.1 父 pom：parent→SB 3.4.5、`java.version` 17→21、`mybatis-plus` 3.5.1→3.5.9、spring-cloud 2021.0.5→**2024.0.0**、spring-cloud-alibaba 2021.0.5.0→2023.0.1.0（lombok 1.18.36/dubbo 3.3.0 保留）`9997b89`
- [x] 1.2 jakarta 迁移：`javax.servlet.*`/`javax.annotation.Resource`/call `javax.websocket`→jakarta（42 文件；确认无 persistence/validation/xml.bind）`c698b66`
- [x] 1.3 MyBatis-Plus 换坐标 `mybatis-plus-boot-starter`→`mybatis-plus-spring-boot3-starter`（model + common-web）；ai 对 model 的 exclusion 三件套**推迟到阶段 2**——ai 不继承父 pom，现仍需排除 model 无版本 core/extension `34b0f6b`
- [x] 1.4 knife4j openapi2→openapi3：换 `knife4j-openapi3-jakarta-spring-boot-starter`（**收敛到 common-web 单处声明**，业务模块传递依赖）；删 3 份 SwaggerConfig 改 common-web 单份 springdoc `OpenApiConfig`；83 注解 `@Api`→`@Tag`、`@ApiOperation`→`@Operation`；删 pagehelper `4db2c53`
- [x] 1.5 驱动/注册中心坐标：mysql→`com.mysql:mysql-connector-j`（解除 reactor version 阻塞）；nacos 随 1.1 升 2023.0.1.0（修 JDK21+nacos-client 坑）；删 call pom netty-all 死依赖（grep 确认 0 处 io.netty 代码引用）`af162c9`
- [x] 1.6 配置：3 业务模块 yml `spring.redis`→`spring.data.redis`；call WebSocket 的 jakarta import 已在 1.2 完成，`@ServerEndpoint("/ws/call")` + `ServerEndpointExporter` + static sessionMap 复核保留 `6da5253`
- [x] 1.7 验证：全 7 模块 reactor `mvn install -DskipTests` 全绿（SB 双栈根因消除）；`contextLoads` + 手动联调（登录→受保护接口→WS 信令→ai SSE）需 Nacos/MySQL/Redis 基础设施，为运行时验证（待起栈）

#### 阶段 2 · 合并单体 + 去微服务 + 合库

- [x] 2.1 新建 `shiwujie-bootstrap` 模块（唯一 `@SpringBootApplication(scanBasePackages=...)` + `@MapperScan` + `@EnableAsync` + repackage）；删 user/call/community/ai 四个 Application 类；**删 gateway 模块** `4f10d11`
- [x] 2.2 Dubbo→本地：10 处 `@DubboService`→`@Service`、15+ 处 `@DubboReference`→`@Resource`（接口定义留 model 不动）；删 3 个无消费者冗余 Inner（`InnerActivityService`/`InnerActivitysignService`/`InnerHelppostService`） `199e6f3`
- [x] 2.3 去微服务基础设施：删 Dubbo/Nacos 依赖+配置+`@EnableDubbo`/`@EnableDiscoveryClient`；父 pom 删 spring-cloud/nacos/dubbo 的 dependencyManagement；dev/prod profile 的 nacos IP 覆盖随移除 `106902b`
- [x] 2.4 路径内化（保契约）：单体 context-path 置空，原 context-path 前缀下沉到 controller 类级 `@RequestMapping`（user `/api/user/...`、community `/api/community/...`、call `/api/call/...`；ai `/api/ai/ai` 不动）；WS `@ServerEndpoint("/ws/call")`→`("/api/ws/call")`；收敛后 WebConfig 的 excludePathPatterns 同步用新全路径 `03c0d96`
- [x] 2.5 收敛重复：4 份 `LoginCheckInterceptor` + 4 份 `WebConfig` → common-web 各 1 份；ai 删自带 common-web 副本（JwtUtils/RedisTemplateConfig/拦截器/异常/BaseResponse 等）改用 common-web；model vs common-web 去重（PageRequest/CommonConstant/UserConstants） `35b81ed`
- [ ] 2.6 合库（⏳ 代码部分完成，mysqldump 导入待远程执行）：bootstrap 单一 datasource（yml 指向 `shiwujie`）✅；call 实体字段 snake→camel + 全局 `map-underscore-to-camel-case: true`（DB 列名不动，call 2 个 Mapper XML 的 resultMap property 同步改名）`61cb18a`；跨库写场景改单 `@Transactional`（删志愿者/删社区/社区审核/家庭审核；`synchronized` 保留作同库并发护栏）`a157991`；⏳ mysqldump 旧 4 库导入空 `shiwujie` 待远程执行（步骤见 [release-checklist](release-checklist.md)「合库执行步骤」）
- [ ] 2.7 验证（⏳ 待合库后远程起栈）：`mvn install` 全 reactor（JDK21，bootstrap 产出可执行 jar）✅ 已绿 + 启动单 jar + 契约回归（HTTP 全路径 + WS 12 信令链 + ai SSE）+ 事务回归（删用户级联）+ token 滑动会话回归

### 🔴 安全加固（v2.1.0 收尾遗留项平移，详见 [known-issues.md](../../../shiwujie-backend/docs/known-issues.md) + [auth.md](../../architecture/auth.md)）

- [ ] 关闭 ai 默认用户兜底
- [ ] 恢复 Helppost / Community 删除/更新权限检查
- [ ] /ws/call 与社区/家庭审核补鉴权
- [ ] 密码 MD5 → BCrypt/Argon2；`TOKEN_SECRETKEY` 走环境变量
- [ ] 前端 TLS + 移除硬编码 SDK Key

### 能力补全

- [ ] App 集成高德 SDK（替代 v2.1.0 的 URI 调起）

### 工程化

- [ ] Docker 化部署
- [ ] 压力测试 + 性能基线 + AiLogs 索引调优

### 因单体化废弃（自 v2.1.0 收尾移除，见 ROADMAP 删除线）

- ~~分布式事务（Seata）~~：单体化后单库，无需跨库事务
- ~~网关统一鉴权~~：单体化后单拦截器，4 处 `LoginCheckInterceptor` 重复自然消除
- ~~Knife4j 聚合 ai~~：单体化后统一 Spring Boot 版本，无 SB2/SB3 文档协议割裂

> 🔴 安全加固项的代码定位与机制见 [known-issues.md](../../../shiwujie-backend/docs/known-issues.md) 与 [architecture/auth.md](../../architecture/auth.md)。
