# v3.0.0 发布检查清单

> 本文件属 `development/v3.0.0/`（单体化改造·进行中）。打 **v3.0.0** tag 前逐项核对。版本指针 [../current.md](../current.md)。

## 文档体系

- [ ] `product/v3.0.0/` 4 文件定稿（单体化契约变更补齐 FR/AC）
- [ ] `development/v3.0.0/` 三件套定稿（task-breakdown / testing-strategy / 本文件）
- [ ] 内容边界自检零命中：`grep -rniE 'src/main|com/swj|\.java:|@DubboService|@DubboReference|java -jar|DUBBO_IP_TO_REGISTRY|mvn |\.yml' docs/product/`
- [ ] FR/AC 去重自检零命中：`grep -rniE 'FR-[A-Z]+-[0-9]+|AC-[A-Z]+-[0-9]+' docs/ | grep -v 'docs/product/'`
- [ ] 全仓相对链接可达
- [ ] ROADMAP v3.0.0 项与 task-breakdown 一致

## 版本号三处同步

- [ ] `product/current.md` 写 `v3.0.0`
- [ ] `development/current.md` 写 `v3.0.0`
- [ ] `docs/README.md` 当前工作版本段写 `v3.0.0`

## 单体化交付（单体重写两阶段，详见 [task-breakdown](task-breakdown.md)）

- [x] 阶段 1：业务模块统一 SB 3.4.5/Java21（jakarta 迁移 + MP 3.5.9 + knife4j openapi3 + nacos 2023.0.1.0），逐模块 `contextLoads` 全绿 ✅
- [x] 阶段 2：合并单体（bootstrap 唯一入口 + 删 gateway + Dubbo→本地 + 路径内化 + 4 拦截器收敛 + ai 副本/重复类清理）✅
- [x] 阶段 2：合库 `shiwujie`（mysqldump 旧 4 库导入 + 单 datasource + call snake→camel + 跨库写单事务）✅ 远程 `47.112.114.139` 的 `shiwujie` 库 16 表已导入验证
- [x] 阶段 2.8 模块合并（7→2）：`<modules>` = {shiwujie-model, shiwujie-bootstrap}；common-web + user/call/community/ai 并入 bootstrap；spring-ai BOM/版本/spring-milestones 仓库迁父 pom；`mvn install` 2 模块 reactor 全绿 + jar 启动契约回归（74 路由四模块前缀齐）✅
- [x] 契约回归零变更（**启动级 ✅ / 功能级部分过**）：HTTP 路径 + WS `/api/ws/call` 12 信令 + 业务码 + 返回字段启动级回归通过（前端 App/Web 不改可对接，详见 [testing-strategy](testing-strategy.md)）；功能级（2026-07-12 实测）WS 视频往返 ✅ / 事务级联 ✅（配方见 testing-strategy 事务回归段）/ AI SSE ⏭️ 跳过 / token 滑动 ⏳ 待补
- [ ] 🔴 安全加固全部清零（ai 后门 / 删改权限 / WS 鉴权 / 密码哈希 / 弱密钥 / 前端 TLS）——独立项
- [ ] App 高德 SDK 集成 ——独立项
- [ ] Docker 化 + 压测 + AiLogs 索引调优 ——独立项

## AI 重写交付项（设计敲定·待 Phase 5，全 `[ ]`）

> **状态：设计敲定（Phase 1–4）· 实现待 Phase 5。** 把现有 Java AI 模块整体替换为 Python LangGraph 智能体（Agent）驱动，Java 业务单体保留。下列全 `[ ]` 尚未落地；打 v3.0.0 tag 前 AI 重写未完成则视为本版本不收 AI 重写（状态诚实，绝不把设计写成「已落地」）。详见 [task-breakdown](task-breakdown.md)「AI 重写」3.1–3.10、[testing-strategy](testing-strategy.md)「AI 重写测试关系」、[architecture/tech-stack](../../architecture/tech-stack.md) AI 重写段、[product/v3.0.0/](../../product/v3.0.0/) 契约变更。

- [ ] **前置 spike**：qwen function-calling 可靠性（本工具集 + 本 prompt 通过率，建议 >=90%，决定 Decision A）/ `spring-ai-starter-mcp-server-webmvc` 与 `spring-ai-bom 1.0.0` + `alibaba-starter` 共存 / alibaba-graph HITL-resume 是否被 open bug（#3297 / #3266）咬中（B-prime 评估）
- [ ] **3.1 Python graph**：State + 节点 + 标准环 + Redis checkpointer（`ai:ckpt:` 前缀 / `thread_id=blind_id`）+ HITL 两处（导航 / 紧急）+ stream custom 反馈
- [ ] **3.2 工具 / 技能 / KB**：14 tool（6 native + 8 MCP）+ `read_skill` + `navigation` skill + BM25 功能 KB
- [ ] **3.3 两层记忆**：短期 checkpoint + 压缩（~8–16k，recent-tail~10 不压）/ 长期偏好后台异步抽取
- [ ] **3.4 Java WS 改造**：ticket 鉴权 / `getBasicRemote`→`getAsyncRemote` / `HashMap`→`ConcurrentHashMap` / 拦回显 / 删 AI 拦截器 dev 后门（[known-issues](../../../shiwujie-backend/docs/known-issues.md) #1 / [auth](../../architecture/auth.md) 风险#6）/ 修 AI 拦截器 Redis 续期错 key
- [ ] **3.5 Java MCP server 8 工具**：业务 4 + 信令 4；`update_profile` inputSchema 硬卡 `{nickname, phone, gender}` + 窄 DTO + 单测断言无敏感字段 setter
- [ ] **3.6 删 Java AI 模块**：`app/agent/tools/advisor/chatmemory` + `AiConfig`/`AiConstants`/`ChatServiceImpl`/`ChatController` + 依赖；`MyManus` 原计划冻结保留，2026-07-20（chunk-2b-6b）撤销删除（零活引用 + B-prime 用 alibaba-graph 非自建 ReAct）
- [ ] **3.7 两进程配置 + Docker**：`scripts/` + `docker-compose.yml` + 两 Dockerfile + `config/.env`；java 公网 `8100:8100` + python 内网（`http://python:8500`）+ `host-gateway`
- [ ] **3.8 APK 改**：SSE client→WS turn client + `SocketDataV0` 加 `destination` + `AiFragment` 5006 读 `destination` + 4-button 重写 + WS ticket
- [ ] **3.9 测试**：删 `AiSmokeTest` + Java WS 契约测试（mock Python）+ Python tool 单测 + graph 集成 + 安全门测试
- [ ] **3.10 灰度**：硬切换（后端镜像 + APK 同批发，SSE↔WS 不兼容须版本配对）
- [ ] **安全门**（红队硬修正，非协商）：紧急确认 token 绑 `(blind_id, thread_id, issuing_turn)` + 同轮拒绝 + App 非-MCP 确认面（第三道门）+ `parallel_tool_calls=False`；`update_profile` 窄 DTO 无敏感字段断言；tool-name 白名单拒幻觉名 + strict JSON-schema 校验（两条护栏无论 spike 结果都上）
- [ ] **qwen FC spike 结果记录**：通过率实测值 + Decision A go/no-go + Decision B-prime 评估结论归档

### AI 重写文档自检（随交付滚动核对）

- [ ] **product grep 零命中**：`grep -rniE 'src/main|com/swj|\.java:|@DubboService|@DubboReference|java -jar|DUBBO_IP_TO_REGISTRY|mvn |\.yml|LangGraph|Python|checkpoint|function-calling|Agent|SSE' docs/product/` 期望零命中（product 层只写用户面措辞：AI 助手 / 智能体驱动 / 对话通道 / 求助信令 / 记住偏好 / 导航分步确认）
- [ ] **FR/AC 去重**：FR/AC 只在 [product/v3.0.0/](../../product/v3.0.0/) 出现一次；architecture/development 引用写「见 product/...」不重复
- [ ] **三处版本号一致**：`product/current.md` + `development/current.md` + `docs/README.md` 均 `v3.0.0`
- [ ] **全仓相对链接可达**：AI 重写新增/引用的 architecture / development / product 交叉链接逐一可点
- [ ] **ROADMAP 不勾**：AI 重写相关 ROADMAP `[ ]` 项在实现未落地前绝不勾选（进行中版本的诚实要求）

## 合库执行步骤（旧 4 库导出 → 单库 `shiwujie`）✅ 已完成

> 远程 `47.112.114.139` 的 `shiwujie` 库已按本节导入并校验 16 表（2026-07-11）。下述命令留作回滚/复现参考。

> 旧 4 库 `shiwujieuser` / `shiwujiecall` / `shiwujiecommunity` / `shiwujieai` 共 **16 表**（13 业务主表 + 3 字典表；表名 PascalCase 无冲突）合并导入空库 `shiwujie`（`47.112.114.139:3306`，user=`shiwujie`，密码见 [`shiwujie-bootstrap/src/main/resources/application.yml`](../../../shiwujie-backend/shiwujie-bootstrap/src/main/resources/application.yml)）。由具备远程访问权限的连接执行（应用账号默认仅授权服务端本机/白名单 IP）。

**已知导出格式坑**（实测 `localhost_2026-07-11_04-56-49_mysql_data.zip`，Navicat 风格、源 MySQL 5.7）：每份 `*.sql` 顶部带 `CREATE DATABASE IF NOT EXISTS \`<旧库>\`` + `USE \`<旧库>\`;`——直接导入会把表建回旧库。导入前用 `sed` 把反引号包裹的旧库名整体换成 `shiwujie`（`CREATE DATABASE` 变空操作、`USE` 指向正确库；裸词旧库名只出现在注释里，不受影响）。导出已内置 `FOREIGN_KEY_CHECKS=0`，无需再包。`shiwujieai.sql` 约 110MB（`AiLogs.content` longtext 存 base64 图片，141 条多行 INSERT），属可弃历史——**建议 ai 仅导结构**跳过图片数据。

```bash
# 0) 凭据（PWD 取自 bootstrap application.yml 的 ${MYSQL_PASSWORD:默认}）
export MPWD='SGaxER3XisXFfRKw'
DBH=47.112.114.139   # 若导入本机 dev 库改 localhost
cd <含 4 份 *.sql 的目录>

# 1) user / call / community：全量（结构+数据，都很小）；sed 反引号旧库名→shiwujie
for db in shiwujieuser shiwujiecall shiwujiecommunity; do
  sed "s/\`${db}\`/\`shiwujie\`/g" "${db}.sql" \
    | mysql -h"$DBH" -P3306 -ushiwujie -p"$MPWD"
done

# 2) ai：仅结构（保留 AiLogs CREATE TABLE，跳过 110MB 图片历史 INSERT）
grep -v '^INSERT INTO' shiwujieai.sql | sed 's/`shiwujieai`/`shiwujie`/g' \
  | mysql -h"$DBH" -P3306 -ushiwujie -p"$MPWD"
# （若要保留 ai 历史，改用第 1 步同样写法导入全量 shiwujieai.sql）

# 3) 校验：目标库表数应 = 16（user4 + call2 + community9 + ai1）
mysql -h"$DBH" -P3306 -ushiwujie -p"$MPWD" -e \
  "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='shiwujie';"
```

> 表名 PascalCase 全程反引号包裹，大小写由服务端 `lower_case_table_names` 决定——旧库既已按现名工作，导入后一致。导入完成后保留 4 份 `.sql` 作为回滚源，直到 2.7 验证通过。

## 启动与冒烟验证（合库后起栈）

> 前置：JDK21（fat jar 为 SB3.4.5/Java21）；MySQL `shiwujie` 库已按上节合库导入；Redis `47.112.114.139:6379` db=2 可连。无 Nacos/Dubbo 依赖。

```bash
# 1) 启动单体（唯一进程；端口 8100 复用原 gateway）
java -jar shiwujie-backend/shiwujie-bootstrap/target/shiwujieBootstrap-0.0.1-SNAPSHOT.jar
# 期望日志：Started ShiwujieApplication ... on port(s): 8100

# 2) 冒烟：公开登录校验接口（login 白名单放行，无需 token，返回 BaseResponse JSON）
curl "http://localhost:8100/api/user/blind/login/check?phone=13800000000"

# 3) 契约回归要点（详见 testing-strategy.md「契约保护铁律」）：
#    - HTTP 全路径前缀：/api/user/** /api/call/** /api/community/** /api/ai/** 不变
#    - ai SSE：POST /api/ai/ai/doChatByText?text=...（produces text/event-stream，需 blind token）
#    - WS：ws://host:8100/api/ws/call，12 信令码 -1/0/1/2/3/4/5001~5006 往返
#    - 业务码 NOT_LOGIN/NO_AUTH/PARAMS_ERROR 不变
# 4) 事务回归：删志愿者→级联清家庭/社区；删社区→清成员——中途异常不残留脏数据
```

## 部署回归

- [x] 单体统一端口 / 单库 / 统一 Spring Boot 版本 `contextLoads` 全绿 ✅（8100 / `shiwujie` 库 / SB3.4.5）
- [x] 凭据 `${ENV:default}` 占位符齐全 + prod 公网 IP `47.112.114.139` 硬编码生效
- [ ] 手动联调：App + Web + 单体后端起栈，核心链路（登录 / 视频求助 / AI 对话 / 社区审核）跑通

## 打 tag 动作

- [ ] `git tag v3.0.0`
- [ ] CHANGELOG `## v3.0.0（进行中，未发布）` → `## v3.0.0 - <tag 日期>`
- [ ] 新建下一版目录，`current.md` ×2 + README 改指下一版
