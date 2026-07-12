# 后端模块合并设计:model + bootstrap 两模块

> 设计文档(brainstorming 产物)。分支 `refactor/backend-collapse-to-bootstrap`,基于 master `88a1179`。
> 版本归属:**并入 v3.0.0**(改 v3.0.0 既定"保留模块化分包"→"合并为 model+bootstrap 两模块")。
> 起草日期 2026-07-12。实施以本文 + 后续 writing-plans 产出的实现计划为准。

## 1. 背景与动机

v3.0.0 单体化把 user/call/community/ai + model/common-web + bootstrap 保留为 7 个 Maven 模块(原计划"保留模块化分包")。单体化后业务模块不再独立部署、无外部消费者,模块边界退化为"纯打包分区"。对新人接手而言,7 模块 + 互依关系认知负担偏高:**"一个单体为什么要切 7 个模块?功能 X 在哪个模块?"**

本设计把 common-web + user/call/community/ai 全部并入 bootstrap,**只保留 model(契约层)作独立模块**。结果:7 → 2 模块。新人看 model 懂领域词典、看 bootstrap 懂全部实现。

## 2. 目标 / 非目标

**目标**
- bootstrap 吸收 common-web + 4 业务模块的 src 与资源;model 保留为唯一库模块。
- 父 pom `<modules>` = {model, bootstrap};删除 common-web/user/call/community/ai 五个模块。
- 单 reactor `mvn install` 全绿;启动 jar 行为与合并前一致。
- 对外契约(HTTP 路径 / WS / 业务码 / 返回字段)零变更。
- v3.0.0 相关 as-built 文档同步改写为两模块态。

**非目标(出范围)**
- 🔴 安全加固(含 known-issues #7 `/ws/call` 绕过 JWT)——独立项,本次不动 WS 鉴权与权限逻辑。
- model 内部清理、`getOne(QueryWrapper)` 反模式重构——登记不本次做。
- 功能级联调(WS 12 信令往返 / ai SSE / 事务 / token)——仍待 App/Web 起栈,与本次结构重构正交。
- 不改任何 controller `@RequestMapping`、`CoordinationSocketHandler` 逻辑、application.yml 业务配置。

## 3. 契约保护铁律(全程不得违反)

只搬文件、改 pom、迁资源、回卷文档。**不动**:controller 类级/方法级 `@RequestMapping`;`@ServerEndpoint("/api/ws/call")` 与 12 信令码;业务码枚举;返回字段名。验收:前端 App/Web 零改动即可对接。

## 4. 现状(合并可行性基础)

### 4.1 内部依赖图
```
model  ←  common-web  ←  { user, call, community, ai }
                              (ai 还直接 ← model)
bootstrap  ←  { user, call, community, ai }
```
**关键性质**:4 业务模块**互相之间无 Maven 依赖**——跨模块调用走 `shiwujie-model` 里的 `Inner*` 接口 + Spring 运行时注入(v3.0.0 阶段 2.2 Dubbo→本地后定型)。故 4 业务模块可按任意顺序并入。**common-web 是它们的共同编译依赖,必须最后并**(它消失前业务模块仍需靠它编译)。

### 4.2 包结构
所有代码已在同一包根 `com.swj.shiwujie.*`(bootstrap `scanBasePackages="com.swj.shiwujie"` 早已覆盖)。**文件在模块间搬迁不改任何 `import`**——这是合并低风险的核心。

### 4.3 资源清单(需迁入 bootstrap `src/main/resources/`)
- mapper XML ×14:user(5)+ community(6)+ call(2)+ ai(1,`AiLogsMapper.xml`)
- ai:`logback-spring.xml`、`prompttemplate/{image,text,toolChoice}-template.txt`
- bootstrap 现有:`application.yml`(不动)

## 5. 目标结构
```
shiwujie-backend/
  pom.xml                 父 pom:<modules> = { shiwujie-model, shiwujie-bootstrap }
  shiwujie-model/         契约层(pom 不动)
  shiwujie-bootstrap/     ★ app + common-web + user/call/community/ai 全在这
    pom.xml               唯一 repackage;依赖 = 五模块外部依赖并集 + shiwujieModel
    src/main/java/com/swj/shiwujie/
      common/ config/ interceptor/ exception/ ...   ← 原 common-web
      (user/call/community/ai 各自的 controller/service/mapper/...)
      ShiwujieBootstrapApplication
    src/main/resources/
      application.yml
      mapper/*.xml          ← 14 个
      logback-spring.xml    ← 原 ai
      prompttemplate/*.txt  ← 原 ai
```

## 6. 合并方案:逐模块增量(已定)

顺序 **user → call → community → ai → common-web**,每步 = 搬 src/资源 → 并外部依赖 → 删模块 → 改父 pom → `mvn compile` 验证 → 提交。ai 放倒数第二(隔离 BOM 迁移风险),common-web 压轴(其消费者此时已全部在 bootstrap 内)。

替代方案"一次性全量"已否决:ai 的 BOM/仓库迁移若炸,单大 commit 难定位,且违反"每完成一步就提交"。

## 7. 步骤详解

> 通则:每步用 `git mv` 搬迁(保历史);搬迁后该模块的 `src/main/{java,resources}` 空目录随模块删除一并清除。

### 步骤 0 — 合并前撞名核查(一次性,开工先做)
grep 核查多模块共有包(`com.swj.shiwujie.common`/`config`/`utils`/`constants`)下**无同名类**。v3.0.0 阶段 2.5 已做 model/common-web/ai 去重(`PageRequest`/`CommonConstant`/`UserConstants` 等),预期无冲突;若发现重名,就地重命名后再继续。

### 步骤 1 — user 并入
- `git mv shiwujie-user/src/main/java/com/swj/shiwujie/* shiwujie-bootstrap/src/main/java/com/swj/shiwujie/`
- `git mv shiwujie-user/src/main/resources/mapper/*.xml shiwujie-bootstrap/src/main/resources/mapper/`
- bootstrap pom:加 `shiwujieCommonWeb` + `shiwujieModel` 直接依赖(此时起 bootstrap 直接含用它们的代码);并 user 外部依赖(`mysql-connector-j runtime`);移除 `shiwujieUser` 依赖
- 父 pom:`<modules>` 删 `shiwujie-user`;DM 删 `shiwujieUser`
- 删 `shiwujie-user/` 整模块(含 pom)
- `mvn -pl shiwujie-bootstrap -am -DskipTests compile` → 绿 → 提交

### 步骤 2 — call 并入
- 搬 src + `mapper/{Urgenthelp,Videohelp}Mapper.xml`
- bootstrap pom:并 `spring-boot-starter-websocket`;移除 `shiwujieCall`
- 父 pom:删 `shiwujie-call`/`shiwujieCall`
- 删 `shiwujie-call/`
- compile → 提交

### 步骤 3 — community 并入
- 搬 src + 6 个 mapper XML
- bootstrap pom:移除 `shiwujieCommunity`(外部依赖 web/redis/session/mysql 已在)
- 父 pom:删 `shiwujie-community`/`shiwujieCommunity`
- 删 `shiwujie-community/`
- compile → 提交

### 步骤 4 — ai 并入(难点,见 §9 专项)
- 搬 src + `mapper/AiLogsMapper.xml` + `logback-spring.xml` + `prompttemplate/`
- 父 pom:加 spring-ai 版本属性 + 两 BOM 的 DM + spring-milestones repo(见 §9)
- bootstrap pom:并 ai 特殊依赖(mqtt paho / dashscope / kryo / jsoup / actuator / jackson-databind);移除 `shiwujieAi`
- 删 `shiwujie-ai/`(含其独立 parent/DM/repositories/compiler 配置——全部由父+bootstrap pom 承接)
- compile → 提交

### 步骤 5 — common-web 并入(压轴)
- 搬 src(common-web 无 resources)
- bootstrap pom:并 common-web 外部依赖(knife4j-openapi3-jakarta / redis / spring-session-data-redis / commons-lang3 / spring-boot-configuration-processor / hutool-all / mybatis-plus 三件套);**移除 `shiwujieCommonWeb` 直接依赖**(代码已在同模块);`shiwujieModel` 直接依赖保留
- 父 pom:`<modules>` 删 `shiwujie-common-web`;DM 删 `shiwujieCommonWeb`
- 删 `shiwujie-common-web/`
- compile → 提交

### 步骤 6 — 收尾
- 父 pom `<modules>` 应只剩 `shiwujie-model` + `shiwujie-bootstrap`;DM 仅留 `shiwujieModel`(内部)+ 外部库版本
- `mvn -f shiwujie-backend/pom.xml install -DskipTests`(2 模块 reactor 全绿)
- 启动 jar 验证(见 §13)
- 文档回卷(见 §12)随各步/收尾提交

## 8. pom 整合

### 8.1 bootstrap pom 最终依赖(五模块外部依赖并集,去重)
| 依赖 | 来源 | 备注 |
|---|---|---|
| `shiwujieModel`(内部) | model | 唯一内部依赖,直接声明 |
| `spring-boot-starter-web` | common-web/user/... | |
| `spring-boot-starter-data-redis` | common-web | |
| `spring-session-data-redis` | common-web | |
| `spring-boot-starter-websocket` | call | |
| `spring-boot-starter-actuator` | ai | |
| `spring-boot-starter-test`(test) | user/ai | |
| `spring-boot-configuration-processor`(optional) | common-web | |
| `spring-boot-devtools`(runtime,optional) | common-web/model | model 自己那份留 model pom |
| `knife4j-openapi3-jakarta-spring-boot-starter` | common-web | |
| `mybatis-plus-spring-boot3-starter` + `-core` + `-extension` | common-web | |
| `mysql-connector-j`(runtime) | user/call/... | |
| `commons-lang3` | common-web | |
| `hutool-all` | common-web | |
| `lombok`(optional) | 各模块 | |
| `jackson-databind` | ai | |
| `org.eclipse.paho:org.eclipse.paho.client.mqttv3` | ai | 1.2.5 |
| `spring-ai-alibaba-starter-dashscope` | ai | 版本走 spring-ai-alibaba-bom |
| `kryo` | ai | 5.6.2 |
| `jsoup` | ai | 1.19.1 |

`spring-boot-maven-plugin` repackage 配置(排除 lombok)保留不变。

### 8.2 父 pom 增改
- `<modules>` 最终 = `shiwujie-model` + `shiwujie-bootstrap`
- `<properties>` 加:`spring-ai.version=1.0.0`、`spring-ai-alibaba.version=1.0.0.2`
- `<dependencyManagement>` 加:
  - `spring-ai-bom`(`${spring-ai.version}`,pom import)
  - `spring-ai-alibaba-bom`(`${spring-ai-alibaba.version}`,pom import)
  - `org.eclipse.paho:org.eclipse.paho.client.mqttv3`(1.2.5,硬编——单消费者,不值当抽属性)
  - `kryo`(5.6.2)、`jsoup`(1.19.1)——集中管版本,bootstrap 无版本引用
  - 删 `shiwujieCommonWeb`/`shiwujieUser`/`shiwujieCall`/`shiwujieCommunity`/`shiwujieAi` 五条内部 DM(留 `shiwujieModel`)
- 加 `<repositories>`:spring-milestones(`https://repo.spring.io/milestone`,snapshots=false)
- 注:**不**重复 import `spring-boot-dependencies`——父 pom 已继承 `spring-boot-starter-parent:3.4.5`,其 DM 已含。

### 8.3 model pom —— 不动

## 9. ai 迁移专项

ai 现自带 `spring-boot-starter-parent:3.4.5` 作 parent(历史遗留,非 shiwujieBackend),独立 `<dependencyManagement>`(spring-ai-bom / spring-ai-alibaba-bom / spring-boot-dependencies)、独立 `<repositories>`(spring-milestones)、`maven-compiler-plugin` 显式 lombok annotationProcessorPaths。并入 bootstrap 后:
- parent:由 bootstrap 继承 shiwujieBackend→SB parent 承接,**删 ai 独立 parent/DM**。
- BOM + 版本属性 + spring-milestones repo:迁**父 pom**(集中管版本,见 §8.2)。
- ai 特殊依赖:进 bootstrap pom(见 §8.1)。
- compiler annotationProcessorPaths(lombok):**删**——SB starter-parent 已配置 lombok 注解处理;实施时验证 ai 的 lombok 生成代码(如 `@Data`/`@Slf4j`)编译通过。
- ai 对 `shiwujieModel` 的 `<exclusions>`(排除旧 mybatis-plus 三件套)随模块删除自然消失——model 现已是 SB3 坐标,无需排除。

## 10. 资源迁移核对点(实施时逐项确认)

- **mapper-locations**:bootstrap `application.yml` 的 mybatis-plus `mapper-locations` 若为 `classpath*:mapper/*.xml`,14 个 XML 并入 `resources/mapper/` 后无需改(单体 jar 内 `BOOT-INF/classes/mapper/` 仍命中)。实施时核对实际配置串。
- **logback 冲突**:ai 的 `logback-spring.xml` 并入 bootstrap 后,核对 `application.yml` 的 `logging.*` 段无冲突;若 yml 已有 logging 配置,以 logback-spring.xml 为准或合并。
- **prompttemplate 路径**:核对 ai 代码读取 `prompttemplate/*.txt` 的 classpath 路径,并入后仍命中。

## 11. 文档回卷(并入 v3.0.0,改"保留模块化分包"→"两模块")

- `docs/development/v3.0.0/task-breakdown.md`:阶段 2 加 **2.8 模块合并(model+bootstrap 两模块)** 子项,标本次完成;阶段 2.1 描述里"7 模块"措辞同步改。
- `docs/architecture/tech-stack.md` + `gateway-dubbo.md`:删/改"7 模块 / 保留模块化分包"叙述为"2 模块(model 契约 + bootstrap app)"。
- `docs/CHANGELOG.md` v3.0.0 段:补行为变更明细——"模块合并:common-web + user/call/community/ai 并入 bootstrap,model 保留;对外契约零变更"。
- `shiwujie-backend/docs/modules/*.md`:common-web/user/call/community/ai 五份模块文档改写为 bootstrap 子包说明(或并移到 bootstrap 模块文档下)。
- `docs/development/v3.0.0/release-checklist.md`:加"两模块结构 `<modules>`={model,bootstrap}"核对项。
- `docs/product/v3.0.0/`:契约面零变更,overview 补一句架构演进(单体→两模块),不涉 FR/AC/端点。
- `shiwujie-backend/docs/known-issues.md`:无新增(纯结构重构,不改行为);#7 等 🔴 项原样保留。

## 12. 验证

1. `mvn -f shiwujie-backend/pom.xml install -DskipTests`——2 模块 reactor 全绿,bootstrap 产出可执行 jar。
2. `java -jar shiwujie-backend/shiwujie-bootstrap/target/shiwujieBootstrap-0.0.1-SNAPSHOT.jar`——`Started ShiwujieApplication ... port(s): 8100`,远程 `shiwujie` 库 16 表可连。
3. 契约启动级回归(对照 v2.1.0 契约):
   - HTTP 全路径前缀 `/api/{user,call,community,ai}/**`(OpenAPI 文档实测路由注册)
   - WS `/api/ws/call` 握手 101
   - 业务码 `40010 NOT_LOGIN` 等信封不变
4. 功能级(WS 往返 / ai SSE / 事务 / token)仍待 App/Web 起栈——本次不验,登记延续。

## 13. 分支与提交策略

- 分支 `refactor/backend-collapse-to-bootstrap`(已建,基于 master `88a1179`)。
- 每步(1–6)独立提交,Conventional Commits、不带署名。提交粒度 = 一个模块一次(收尾 + 文档回卷可再分)。
- 推 `origin`(Gitee);合并回 master 走 Gitee PR(禁本地 merge)。
- 每步 `mvn compile` 必绿才提交;任一步红了当场修,不累积。

## 14. 风险与回滚

| 风险 | 缓解 |
|---|---|
| ai BOM/仓库迁移致依赖解析失败 | ai 单独一步(步骤 4),隔离定位;父 pom DM 集中管版本 |
| 跨模块同包同名类撞文件 | 步骤 0 grep 核查先行 |
| mapper-locations / logback 路径不命中 | §10 核对点逐项确认 |
| lombok 注解处理在删 ai compiler 配置后失效 | 步骤 4 验证 ai 的 `@Data`/`@Slf4j` 类编译通过 |
| 资源漏搬(14 个 mapper + logback + prompttemplate) | §4.3 清单 + 每步 git mv 时对照 |

**回滚**:纯结构重构、每步独立提交——任一步出问题 `git revert` 该提交即可;最差 `git checkout master` 弃整分支(代码无业务变更,不污染契约面)。
