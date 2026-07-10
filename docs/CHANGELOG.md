<!-- markdownlint-disable MD024 -->

# 变更日志（CHANGELOG）

> 迭代历程明细。每条变更四分类：新增 / 变更 / 修复 / 移除。提交 hash 可用 `git show <hash>` 溯源。
>
> git tag 现有 `v1.0`（一期·uniapp+单体，独立根提交）与 `二期开发后初步稳定版`（二期里程碑）；二期**尚无 semver tag**。版本线：`v1.0`（一期）→ `v2.0.0`（二期·微服务）。当前工作版本 `v2.0.0` = **二期首个 semver 版本**（进行中；起点 = 阶段 0–9 累积现状，明细见下方「历史时期」）。阶段 0–9 是按能力域对提交的聚类，时间区间为代表性提交范围（部分阶段能力并行推进，区间有重叠），日期**不确定**（标「约」）。打 tag 时把下方 `## v2.0.0（进行中，未发布）` 改为 `## v2.0.0 - YYYY-MM-DD`（精确日期）。

---

## v2.0.0（进行中，未发布）

> 二期首个 semver 版本（一期 `v1.0` tag 在前）。起点 = 阶段 0–9 的累积现状（明细见下方「历史时期」），随收尾工作滚动；打 tag 时补精确日期。

**文档体系**
- 版本分级落地：`product/` 与 `development/` 改为 `current.md` 指针 + `vX.Y.Z/` 目录模型（工作直接写在进行中版本目录，发布即冻结、新建下一版；历史版本目录保留不删）。详见 [CONTRIBUTING.md](CONTRIBUTING.md) 第五节。

**修复**
- **token 续期 key 漏身份前缀**：`LoginCheckInterceptor` 续期（`renewKey`）与删用户删 token（`deleteBlind`/`deleteVolunteer`）拼的 Redis key 漏了 `-blind-`/`-volunteer-` 段，与登录存/拦截器读的 key 不匹配 → 续期静默失效（活跃用户 90 天后被踢）、删用户旧 token 残留。改为提取共享 `redisKey`（读/续期共用）杜绝拼接分叉，续期对齐登录 90 天（真滑动会话）；删 token 补回前缀。涉及 user/call/community 三份拦截器 + user 模块两处删除（ai 模块同型 bug 暂不动）。明细见 [known-issues](../shiwujie-backend/docs/known-issues.md) #3。

**待办（打 tag 前须完成）**
- 🔴 安全加固 / 能力补全 / 工程化：见 [ROADMAP.md](ROADMAP.md) 待实现段 + [development/v2.0.0/task-breakdown.md](development/v2.0.0/task-breakdown.md)。

---

## 历史时期（阶段 0–9，约 2025-06 ~ 2026-07，日期不确定）

> 以下各阶段为过去开发的明细，大方向 rolled-up 见 [ROADMAP.md](ROADMAP.md#历史时期阶段-09约-2025-06--2026-07日期不确定)。

### 阶段 9 · 工程化收尾（约 2026-07）

> call 路由对齐、dev/prod 多环境、凭据占位符化、后端模块扁平化与仓库卫生。

**新增**
- dev/prod 多环境 profile 拆分（仅覆盖 `spring.cloud.nacos.discovery.ip`：dev=127.0.0.1，prod=47.112.114.139）。（`2e5573a`）
- 凭据占位符化：`MYSQL_PASSWORD` / `REDIS_PASSWORD` / `NACOS_USERNAME` / `NACOS_PASSWORD` / `DASHSCOPE_API_KEY` / `SEARCH_API_KEY` 走 `${ENV:default}`。（`2e5573a`）
- **引入 `shiwujie-backend` 父 pom**（`<packaging>pom</packaging>`）：7 模块聚合 + 版本统一（统一 `0.0.1-SNAPSHOT`、dependencyManagement 集中版本）；ai 因 SB 3.4.5 / Java 21 保留直继承 `spring-boot-starter-parent`、仅纳入聚合；root properties 锁 `lombok 1.18.36` 以兼容 JDK 21 reactor 构建。（`91e0998`）

**变更**
- 调整 call 模块路由与 web 代理目标对齐。（`6da3060`）
- **后端模块扁平化**：model / common-web / user / call / community / ai 六模块从 `shiwujie-gateway/` 子目录移至 `shiwujie-backend/` 同级。gateway 原非 aggregator（pom 无 `<modules>`）、各模块 `<parent>` 均指向外部 starter-parent，故纯 `git mv`、**零 pom 改动**，Maven 关系与 Java 包名均不变。（`640c171`）
- 仓库卫生：`.idea/`、`*.iml`、`logs/*.log` 移出 git 跟踪（`--cached` 仅退索引、保留磁盘）；根 `.gitignore` 补全通用 IDE/OS 规则，backend `.gitignore` 上移覆盖全部同级模块并补 `logs/`、`*.log`。（`640c171`）
- 清理 ai pom 重复依赖声明（`jackson-databind` / `org.eclipse.paho.client.mqttv3` 各去一份），消除 Maven `dependencies must be unique` 警告。（`e4b097b`）

**修复**
- **Dubbo provider 端口迁出 Hyper-V/WSL 保留段**：原 50200 / 50300 / 50400 / 50500 全部落入 Windows Hyper-V/WSL2 动态登记的 TCP 排除段，导致 `contextLoads` bind 抛 `Address already in use` 而 `netstat` 查无进程——隐蔽环境坑，重启后段还会变。改用 21200 / 21300 / 21400 / 21500（远离保留段与临时端口区 49152+）。（`0bdbbc5`）

**移除**
- 清理无用文件 `how 3dcf577` 与 `testgit.txt`。（`03e60ea`）

---

### 阶段 8 · 分布式与生产化（约 2026-01）

> Netty→Spring WebSocket、gateway 负载均衡、多机 nacos+dubbo 通信。

**新增**
- gateway 基于 Nacos 服务发现 + Spring Cloud LoadBalancer 轮询负载均衡。（`87c651a`）
- 多服务器间 nacos 与 dubbo 通信配置。（`8a16377`）

**变更**
- **call 模块 WebSocket 从 Netty 实现改为 Spring WebSocket**（`@ServerEndpoint` + javax.websocket）。（`3f88ea7`）
- GateWay 改名、pom 简化。（`da7a6ab`）
- 数据库/redis/nacos 配置统一；ai 提示词修复、拦截器 redis 生效问题修复。（`088b14f`）
- 删除 AI 语气切换功能。（`088b14f`）

**修复**
- ai 模块拦截器 redis 不生效问题。（`088b14f`）
- 修改部署 IP 配置。（`7312d4f`）

---

### 阶段 7 · 引擎升级与稳定性（约 2025-10 ~ 11）

> deepseek、Spring AI Alibaba M6→1.0、工作流定型；**mqtt、RAG、自研 ReAct 经试错后移除/弃用**。

**新增**
- 添加 mqtt 服务（为硬件 IoT 设计）。（`ae5465c` 2025-10-24）
- 使用 deepseek 实现并简化代码（新 App 部分测试）。（`3093cc8` 2025-10-30）
- ai 模块拦截器添加默认用户信息（调试用，**生产后门风险**）；call 工具调用调试信息。（`7737559` 2025-11-09）
- 开启 dubbo 空保护（`empty-protection: true`）。（`d326353` 2025-11-12）

**变更**
- **Spring AI Alibaba 由 M6 升级为 1.0**（两次提交重构）：文字模型与图像模型同时回答；动态配置两模型持久化策略（Redis 低消耗）；提示词改用文档引入；图片追问功能（不占 Redis 空间）；性能调优。（`75e5ace` / `07459f6` 2025-11-18）
- 设置 `spring-data-redis` 配置解决 AI 模块连接问题；取消 dubbo qos。（`7737559`）

**修复**
- AI 模块拦截器 redis 连接问题。（`7737559`）

**移除**
- **mqtt 硬件 IoT 通道**：因硬件成本问题取消，代码删除（**pom 残留 paho 依赖**，见 [shiwujie-backend/docs/modules/ai.md](../shiwujie-backend/docs/modules/ai.md) 已知问题）。（git 历史可溯，代码已无 MqttClient 引用）
- **自研 ReAct Agent**：经大量测试（多种提示词、RAG 注入、Agent 调用），确定工具调用最佳方案为**代码实现工作流**，自研 ReAct 框架弃用、`@Component` 注释（保留代码未启用）。（`07459f6`，见 [shiwujie-backend/docs/modules/ai.md](../shiwujie-backend/docs/modules/ai.md)）
- **RAG 知识库增强**：测试后效果不及工作流，移除接入（`MyRagAdvisor` Bean 半残留，未注入）。（`07459f6`）
- **语气切换功能**。（后续 `088b14f` 彻底删除）

---

### 阶段 6 · AI 能力跃升（约 2025-08-14 ~ 08-20）

> 从「能对话」到「能执行」：避障、高德导航、跳转应用、性能 +50%、图片处理独立 App。

**新增**
- AI 页面集成避障功能。（`220f185` / `9de40fe` 2025-08-14）
- **AI 对话帮助用户执行操作**（工具执行真实设备动作）。（`831b7e0` / `03c5333` 2025-08-16）
- AI 跳转到外部应用。（`33dab95` 2025-08-18）
- 高德导航功能，自动开启步行导航。（`b1e3bef` / `13d311c` / `69ff858` 2025-08-18~20）
- 单独创建图片处理 App，图片追问不占 Redis。（`1a9d4c7` 2025-08-20）

**变更**
- 修改调用架构，Redis 存储分化加速，暂取消图片上下文存储（后由瘦身工程替代）。（`9ea825f` 2025-08-20）
- 图片识别输出结果优化。（`b1e3bef`）

**修复**
- AI 悬浮窗在志愿者端误出现。（`06f623d` 2025-08-18）
- APP 崩溃与前台服务错误造成的崩溃，AI 悬浮球自由跳转。（`065ae26`）
- 紧急求助重复点击 bug、用户端弹窗问题。（`56f87f8`）
- AI 避障功能 bug。（`aaaab82` 2025-08-15）

---

### 阶段 5 · AI 模块从零到能用（约 2025-08-04 ~ 08-13）

> model 子模块独立、Spring AI、向量库、dubbo+工具、流式/TTS/图片识别。

**新增**
- **分开子模块 model，ai 模块初始化**。（`d9a6005` 2025-08-04）
- 修改 SpringBoot AI 版本，基于 redis 存储的 advisor，多模态测试。（`2c80ff9` 2025-08-05）
- 基于内存的向量存储 → 阿里云向量数据库。（`282a52e` / `d5819d0` 2025-08-06）
- deepseek 与千问多模型使用。（`d5819d0`）
- **社区修改内部服务，添加 AI 工具相关服务**（Dubbo Inner 服务接入 AI）。（`797f3a2` 2025-08-08）
- 工具调用、提示词设计。（`b65c7f8`）
- ai 模块两接口，上传图片识别，流式返回。（`a13a3dd` 2025-08-11）
- 基本工具非流式调用。（`e104ed4`）
- 工具调用流式输出（拆解优化）；AI 对话流式输出展示。（`dc4200b` / `3ff5e56` 2025-08-12）
- 集成讯飞 TTS 自动播报。（`5f26639` / `e282427`）
- 拍照识别功能。（`9a8b292` 2025-08-13）
- 基本实现前端联动，加入 socket。（`98a070a`）
- Redis 持久化 + MySQL 异步存储（自研 ChatMemory 双写）。（`1c822ef`）

**变更**
- 调用返回速度提高约 50%（基于 redis 持久化、提示词优化、RAG 搜索）；model 补充序列化。（`4df17f8` 2025-08-12）

**修复**
- ai 接口重复调用 bug；上传文件大小限制 10Mb；Linux 路径不适配；调用 404；图片过大。（`f9398e0` / `e31e3d4` / `eb2fc6b` 2025-08-11）
- 数据库建表索引优化；redis 自动删除多余信息；删除定时任务。（`1a6688e` 2025-08-13）

---

### 阶段 4 · 社区治理（约 2025-07-26 ~ 08-05）

> 社区/审核/管理者/求助帖/活动/报名六大主体。

**新增**
- 社区加入审核、管理员设置、社区增删改查。（`d2d149a` 2025-07-26）
- 社区求助帖、活动功能。（`b5a072e` 2025-07-27）
- web 端社区管理者功能、加入与审核。（`223f66e` / `7a6b398` 2025-08-02~03）
- web 端活动功能、社区用户管理。（`9327fca` / `4afbf0c` 2025-08-04）
- web 端社区完成；APP 活动功能。（`51e35a6` / `ce0a729` 2025-08-05）

**修复**
- 社区注册 bug；web 端社区用户管理 bug。（`049572d` / `4afbf0c`）

---

### 阶段 3 · 视频通话与紧急求助（约 2025-07-21 ~ 08-13）

> call 模块诞生（早期 Netty socket）。

**新增**
- 前端视频实现紧急求助。（`4bae798` 2025-07-21）
- 视频模块不刷新页面使用。（`8f47b46` 2025-08-08）
- 加入心跳包与 APP 自启动。（`078f60d` / `d96a5aa` 2025-08-08）
- 长连接和心跳包完成。（`d96a5aa`）

**修复**
- 视频模块异常记录与 bug。（`8f47b46`）

---

### 阶段 2 · 用户与家庭模块（约 2025-07-24 ~ 08-07）

> 三类用户账号、家庭关系、common 工具类。

**新增**
- 社区注册登录（未处理分布式事务）。（`a81f13a` 2025-07-24）
- 改为公共拦截器代码（common-web 抽取）。（`61f5102` 2025-07-24）
- gateway 配置。（`a650f92` 2025-07-25）
- 社区分页条件查询志愿者/视障人士；加入社区。（`a69c609` 2025-07-26）
- 加入家庭修改。（`8a3d6e9` 2025-08-07）
- 盲人端移动端适配优化。（`68d4c50` 2025-08-07）

**变更**
- 修改社区注册登录返回类、用户模块登录返回类。（`3ee8034` 2025-07-25）

---

### 阶段 1 · 二期微服务脚手架（约 2025-07-21 ~ 08-01）

> 多模块切分、nacos 引入、JWT/Redis 工具骨架、前端视频求助起步。

**新增**
- 前端视频实现紧急求助（起步）。（`4bae798` 2025-07-21）
- 删除多余 sql 文件（一期遗留清理）。（`7d72662` 2025-07-21）
- 志愿者社区雏形。（`c26d203` 2025-08-01）

---

### 阶段 0 · 一期单体封版（约 2025-06-30）

> 演进起点。

**新增**
- 项目一期：uniapp + Spring 单体 + 单库 4 表。（`423c0fa` 2025-06-30）

> 一期代码作为二期微服务演进的对照基线保留在 git 历史，不再迭代。
