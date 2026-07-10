# v3.0.0 任务拆解

> 本文件属 `development/v3.0.0/`（单体化改造·进行中）。**交付范围**（单体化目标）+ **任务**（随单体化落地）。版本指针 [../current.md](../current.md)。大方向与废弃项见 [../../ROADMAP.md](../../ROADMAP.md) v3.0.0 段。

## v3.0.0 交付范围（承接 ROADMAP 待实现）

### 单体重写

- [ ] 去 Spring Cloud / Dubbo，合并 user / call / community / ai 为单体应用（保留模块化分包，统一 Spring Boot 版本）

### 🔴 安全加固（v2.0.0 收尾遗留项平移，详见 [known-issues.md](../../../shiwujie-backend/docs/known-issues.md) + [auth.md](../../architecture/auth.md)）

- [ ] 关闭 ai 默认用户兜底
- [ ] 恢复 Helppost / Community 删除/更新权限检查
- [ ] /ws/call 与社区/家庭审核补鉴权
- [ ] 密码 MD5 → BCrypt/Argon2；`TOKEN_SECRETKEY` 走环境变量
- [ ] 前端 TLS + 移除硬编码 SDK Key

### 能力补全

- [ ] App 集成高德 SDK（替代 v2.0.0 的 URI 调起）

### 工程化

- [ ] Docker 化部署
- [ ] 压力测试 + 性能基线 + AiLogs 索引调优

### 因单体化废弃（自 v2.0.0 收尾移除，见 ROADMAP 删除线）

- ~~分布式事务（Seata）~~：单体化后单库，无需跨库事务
- ~~网关统一鉴权~~：单体化后单拦截器，4 处 `LoginCheckInterceptor` 重复自然消除
- ~~Knife4j 聚合 ai~~：单体化后统一 Spring Boot 版本，无 SB2/SB3 文档协议割裂

> 🔴 安全加固项的代码定位与机制见 [known-issues.md](../../../shiwujie-backend/docs/known-issues.md) 与 [architecture/auth.md](../../architecture/auth.md)。
