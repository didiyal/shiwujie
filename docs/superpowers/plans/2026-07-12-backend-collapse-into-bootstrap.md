# 后端模块合并:model + bootstrap 两模块 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 common-web + user/call/community/ai 五模块的 src 与资源并入 `shiwujie-bootstrap`,model 保留为唯一库模块,后端从 7 模块收敛为 2 模块,对外契约零变更。

**Architecture:** 纯结构重构——所有代码已在同一包根 `com.swj.shiwujie.*`,文件搬迁不改任何 `import`;每模块增量并入,`mvn compile` 验证后提交;ai 的 spring-ai BOM/版本/仓库迁父 pom;common-web 压轴(其消费者此时已全部在 bootstrap 内)。

**Tech Stack:** Spring Boot 3.4.5 / Java 21 / Maven 多模块(收尾 2 模块)/ MyBatis-Plus 3.5.9 / Spring AI Alibaba 1.0.0.2。

**Spec:** [`docs/superpowers/specs/2026-07-12-backend-collapse-into-bootstrap-design.md`](../specs/2026-07-12-backend-collapse-into-bootstrap-design.md)

## Global Constraints

- **契约零变更**:HTTP 路径 `/api/{user,call,community,ai}/**` + WS `/api/ws/call`(12 信令码 `-1/0/1/2/3/4/5001~5006`)+ 业务码(`NOT_LOGIN`/`NO_AUTH`/`PARAMS_ERROR`)+ 返回字段名全程不动。不得改任何 controller 的 `@RequestMapping`、`CoordinationSocketHandler` 逻辑、`application.yml`。
- **不动 `application.yml`**:已核对 mapper XML 合并后仍命中 `classpath:mapper/`,`logging.config: classpath:logback-spring.xml` 由 ai 随迁的 logback 承接——yml 零改。
- **凭据留 yml** `${ENV:default}`、**公网 IP `47.112.114.139` 硬编码**(本次都不碰)。
- **提交**:Conventional Commits、不带署名;每步 `mvn compile` 绿才提交;禁本地 merge(合并回 master 走 Gitee PR)。
- **分支**:`refactor/backend-collapse-to-bootstrap`(已建,基于 master `88a1179`)。所有工作在此分支。
- **构建验证口径**:`mvn -pl shiwujie-bootstrap -am -DskipTests compile`(每步)/ `mvn -f shiwujie-backend/pom.xml install -DskipTests`(收尾)。
- **搬迁手法**:`cp -r <module>/src/main/{java,resources}/. <bootstrap>/对应目录/` 合并目录树 → `git rm -r <module>` 删旧模块 → `git add -A` 暂存新位(git 按内容相似度自动推断 rename,历史保留)。

---

## File Structure

**修改**
- `shiwujie-backend/pom.xml`(父):`<modules>`、`<properties>`、`<dependencyManagement>`、新增 `<repositories>`
- `shiwujie-backend/shiwujie-bootstrap/pom.xml`:依赖并集 + repackage(保留)
- `shiwujie-backend/shiwujie-bootstrap/src/main/java/com/swj/shiwujie/`:吸收五模块代码
- `shiwujie-backend/shiwujie-bootstrap/src/main/resources/`:吸收 14 mapper XML + `logback-spring.xml` + `prompttemplate/*.txt`
- 文档(回卷 task 7-8):`docs/architecture/tech-stack.md`、`docs/architecture/gateway-dubbo.md`、`docs/CHANGELOG.md`、`docs/product/v3.0.0/overview.md`、`docs/development/v3.0.0/task-breakdown.md`、`docs/development/v3.0.0/release-checklist.md`、`shiwujie-backend/docs/modules/*.md`

**删除(整模块)**
- `shiwujie-backend/shiwujie-common-web/`、`shiwujie-user/`、`shiwujie-call/`、`shiwujie-community/`、`shiwujie-ai/`

**不动**
- `shiwujie-backend/shiwujie-model/`(契约层,pom 与 src 全保留)
- `shiwujie-backend/shiwujie-bootstrap/src/main/java/com/swj/shiwujie/ShiwujieBootstrapApplication.java`
- `shiwujie-backend/shiwujie-bootstrap/src/main/resources/application.yml`

---

## Task 0: 合并前撞名核查

**Files:** 无(只读核查)

**Why:** 五模块共享 `com.swj.shiwujie.{common,config,utils,constants}` 等包;并入单一 src 树前必须确认**同包下无同名类**(否则文件相撞)。v3.0.0 阶段 2.5 已做 model/common-web/ai 去重,预期零命中。

- [ ] **Step 1: 跨模块同包同名类扫描**

Run(仓库根 `D:/Projects/shiwujie/shiwujie/Phase2`):
```bash
find shiwujie-backend/shiwujie-common-web shiwujie-backend/shiwujie-user shiwujie-backend/shiwujie-call shiwujie-backend/shiwujie-community shiwujie-backend/shiwujie-ai \
  -path "*/src/main/java/com/swj/shiwujie/*" -name "*.java" \
  | sed 's#.*/com/swj/shiwujie/##' | sort | uniq -d
```
Expected: **空输出**(无同包同名类)。输出每行格式 `包路径/类名.java`,如 `common/Foo.java`。

- [ ] **Step 2: 若有命中,就地重命名后再继续**

如出现非空输出,逐个打开确认是否真同包(可能不同子包同名,合法)。真同包同名者:按所属域重命名(如两处 `common/Constants.java` → `common/AiConstants.java` vs `common/WebConstants.java`),单独提交 `refactor(backend): 消解同包同名类 X/Y` 后再回到 Task 1。预期无需此步。

- [ ] **Step 3: 不提交(Task 0 是只读核查,无文件变更)**

---

## Task 1: 并入 user 模块

**Files:**
- Move: `shiwujie-user/src/main/java/com/swj/shiwujie/*` → `shiwujie-bootstrap/src/main/java/com/swj/shiwujie/`
- Move: `shiwujie-user/src/main/resources/mapper/*.xml` → `shiwujie-bootstrap/src/main/resources/mapper/`(Blind/Family/FamilyJoinReview/Volunteer 等 5 个)
- Delete: `shiwujie-backend/shiwujie-user/`(整模块)
- Modify: `shiwujie-backend/shiwujie-bootstrap/pom.xml`、`shiwujie-backend/pom.xml`

**Interfaces:**
- Consumes: model、common-web(此时仍为独立模块,user 代码引用其类经 `shiwujieCommonWeb` 依赖可见)
- Produces: bootstrap 直接依赖 `shiwujieCommonWeb` + `shiwujieModel`(为后续步骤铺路)

- [ ] **Step 1: 搬迁 user 源码与资源(合并到 bootstrap)**

Run(仓库根):
```bash
cp -r shiwujie-backend/shiwujie-user/src/main/java/com/swj/shiwujie/. shiwujie-backend/shiwujie-bootstrap/src/main/java/com/swj/shiwujie/
cp -r shiwujie-backend/shiwujie-user/src/main/resources/. shiwujie-backend/shiwujie-bootstrap/src/main/resources/
git rm -r shiwujie-backend/shiwujie-user
git add -A shiwujie-backend/shiwujie-bootstrap
```
Expected: `git rm` 列出 user 模块所有文件已删;`git add` 暂存 bootstrap 新文件。

- [ ] **Step 2: bootstrap pom 加直接依赖 + 移除 shiwujieUser**

Modify `shiwujie-backend/shiwujie-bootstrap/pom.xml`:把 `<dependencies>` 开头的四业务模块依赖块中,**删掉** `shiwujieUser` 块:
```xml
<!-- 删除 -->
<dependency>
    <groupId>com.swj</groupId>
    <artifactId>shiwujieUser</artifactId>
</dependency>
```
在 `<dependencies>` 顶部(四业务模块依赖块之前)**新增** common-web + model 直接依赖 + user 的外部依赖:
```xml
<!-- 契约层 + 公共层(此时起 bootstrap 直接持有用其类的代码) -->
<dependency>
    <groupId>com.swj</groupId>
    <artifactId>shiwujieModel</artifactId>
</dependency>
<dependency>
    <groupId>com.swj</groupId>
    <artifactId>shiwujieCommonWeb</artifactId>
</dependency>
<!-- user 的外部依赖(web/redis/session 经 common-web 传递;mysql 需直接) -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```
保留其余 `shiwujieCall`/`shiwujieCommunity`/`shiwujieAi`/test 依赖块不动。

- [ ] **Step 3: 父 pom 删 user 模块登记**

Modify `shiwujie-backend/pom.xml`:
- `<modules>`:删 `<module>shiwujie-user</module>` 行
- `<dependencyManagement>/<dependencies>`:删 `shiwujieUser` 的 `<dependency>` 块

- [ ] **Step 4: 编译验证**

Run:
```bash
mvn -pl shiwujie-bootstrap -am -DskipTests compile
```
Expected: **BUILD SUCCESS**;user 的 controller/service/mapper 类全部在 bootstrap 编译通过(无 import 错误——包根未变)。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m 'refactor(backend): 并入 user 模块 → bootstrap（v3.0.0 两模块 1/5）'
```

---

## Task 2: 并入 call 模块

**Files:**
- Move: `shiwujie-call/src/main/java/com/swj/shiwujie/*` → bootstrap(java)
- Move: `shiwujie-call/src/main/resources/mapper/{Urgenthelp,Videohelp}Mapper.xml` → bootstrap resources/mapper/
- Delete: `shiwujie-backend/shiwujie-call/`
- Modify: bootstrap pom、父 pom

**Interfaces:**
- Produces: bootstrap 直接依赖 `spring-boot-starter-websocket`(call 的 `CoordinationSocketHandler` 用);WS 端点 `/api/ws/call` 路径与逻辑零变更。

- [ ] **Step 1: 搬迁 call 源码与资源**

```bash
cp -r shiwujie-backend/shiwujie-call/src/main/java/com/swj/shiwujie/. shiwujie-backend/shiwujie-bootstrap/src/main/java/com/swj/shiwujie/
cp -r shiwujie-backend/shiwujie-call/src/main/resources/. shiwujie-backend/shiwujie-bootstrap/src/main/resources/
git rm -r shiwujie-backend/shiwujie-call
git add -A shiwujie-backend/shiwujie-bootstrap
```

- [ ] **Step 2: bootstrap pom 加 websocket、删 shiwujieCall**

Modify `shiwujie-backend/shiwujie-bootstrap/pom.xml`:删 `shiwujieCall` 依赖块;新增:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

- [ ] **Step 3: 父 pom 删 call 登记**

`shiwujie-backend/pom.xml`:`<modules>` 删 `<module>shiwujie-call</module>`;DM 删 `shiwujieCall` 块。

- [ ] **Step 4: 编译验证**

Run: `mvn -pl shiwujie-bootstrap -am -DskipTests compile`
Expected: **BUILD SUCCESS**(含 `CoordinationSocketHandler` 等 jakarta websocket 类)。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m 'refactor(backend): 并入 call 模块 → bootstrap（v3.0.0 两模块 2/5）'
```

---

## Task 3: 并入 community 模块

**Files:**
- Move: `shiwujie-community/src/main/java/com/swj/shiwujie/*` → bootstrap
- Move: `shiwujie-community/src/main/resources/mapper/*.xml` → bootstrap(6 个:Activity/Activitysign/Community/Communityjoinreview/Communitymanager/Helppost)
- Delete: `shiwujie-backend/shiwujie-community/`
- Modify: bootstrap pom(仅删内部依赖,无新外部依赖)、父 pom

**Interfaces:** community 外部依赖(web/redis/session/mysql)已在 Task 1-2 进 bootstrap;无新增。

- [ ] **Step 1: 搬迁 community 源码与资源**

```bash
cp -r shiwujie-backend/shiwujie-community/src/main/java/com/swj/shiwujie/. shiwujie-backend/shiwujie-bootstrap/src/main/java/com/swj/shiwujie/
cp -r shiwujie-backend/shiwujie-community/src/main/resources/. shiwujie-backend/shiwujie-bootstrap/src/main/resources/
git rm -r shiwujie-backend/shiwujie-community
git add -A shiwujie-backend/shiwujie-bootstrap
```

- [ ] **Step 2: bootstrap pom 删 shiwujieCommunity**(无新增)

删 `shiwujieCommunity` 依赖块。

- [ ] **Step 3: 父 pom 删 community 登记**

`<modules>` 删 `<module>shiwujie-community</module>`;DM 删 `shiwujieCommunity` 块。

- [ ] **Step 4: 编译验证**

Run: `mvn -pl shiwujie-bootstrap -am -DskipTests compile`
Expected: **BUILD SUCCESS**。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m 'refactor(backend): 并入 community 模块 → bootstrap（v3.0.0 两模块 3/5）'
```

---

## Task 4: 并入 ai 模块(难点——spring-ai BOM/仓库迁父 pom)

**Files:**
- Move: `shiwujie-ai/src/main/java/com/swj/shiwujie/*` → bootstrap(advisor/agent/app/chatmemory/common/config/constants/controller/mapper/service/tools/utils)
- Move: `shiwujie-ai/src/main/resources/{mapper/AiLogsMapper.xml,logback-spring.xml,prompttemplate/*.txt}` → bootstrap resources 对应位置
- Delete: `shiwujie-backend/shiwujie-ai/`(含其独立 parent/DM/repositories/compiler 配置)
- Modify: **父 pom**(加 spring-ai 版本属性 + 两 BOM DM + paho/kryo/jsoup DM + spring-milestones repo)、bootstrap pom(加 ai 外部依赖、删 shiwujieAi)

**Interfaces:**
- Produces: 父 pom 集中管 spring-ai 版本;bootstrap 持有 ai 全部代码与特殊依赖;ai 的 lombok 注解处理改由 SB starter-parent 承接(删 ai 独立 compiler 配置)。

- [ ] **Step 1: 搬迁 ai 源码与资源**

```bash
cp -r shiwujie-backend/shiwujie-ai/src/main/java/com/swj/shiwujie/. shiwujie-backend/shiwujie-bootstrap/src/main/java/com/swj/shiwujie/
cp -r shiwujie-backend/shiwujie-ai/src/main/resources/. shiwujie-backend/shiwujie-bootstrap/src/main/resources/
git rm -r shiwujie-backend/shiwujie-ai
git add -A shiwujie-backend/shiwujie-bootstrap
```
注意:`logback-spring.xml` 落到 `bootstrap/src/main/resources/logback-spring.xml`,与 `application.yml` 的 `logging.config: classpath:logback-spring.xml` 对接——无冲突。

- [ ] **Step 2: 父 pom 加 spring-ai 版本属性 + BOM/依赖 DM + 仓库**

Modify `shiwujie-backend/pom.xml`:

(a) `<properties>` 块内追加(spring-ai 相关版本):
```xml
<spring-ai.version>1.0.0</spring-ai.version>
<spring-ai-alibaba.version>1.0.0.2</spring-ai-alibaba.version>
```

(b) `<dependencyManagement>/<dependencies>` 块内追加:
```xml
<!-- Spring AI（原 ai 模块独立 DM,合并后集中到父 pom） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>${spring-ai.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-bom</artifactId>
    <version>${spring-ai-alibaba.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
<!-- ai 特殊依赖版本集中 -->
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
    <version>1.2.5</version>
</dependency>
<dependency>
    <groupId>com.esotericsoftware</groupId>
    <artifactId>kryo</artifactId>
    <version>5.6.2</version>
</dependency>
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.19.1</version>
</dependency>
```
(不重复 import `spring-boot-dependencies`——父 pom 继承 SB starter-parent 已含。)

(c) `</project>` 前、`</dependencyManagement>` 之后追加 `<repositories>`(若父 pom 已有 `<repositories>` 则合并进去):
```xml
<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots><enabled>false</enabled></snapshots>
    </repository>
</repositories>
```

(d) `<modules>` 删 `<module>shiwujie-ai</module>`;DM 删 `shiwujieAi` 块。

- [ ] **Step 3: bootstrap pom 加 ai 外部依赖、删 shiwujieAi**

Modify `shiwujie-backend/shiwujie-bootstrap/pom.xml`:删 `shiwujieAi` 依赖块;新增:
```xml
<!-- ai 相关 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
</dependency>
<dependency>
    <groupId>com.esotericsoftware</groupId>
    <artifactId>kryo</artifactId>
</dependency>
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```
(版本均由父 pom DM 管,此处不写 `<version>`。)

- [ ] **Step 4: 编译验证**

Run: `mvn -pl shiwujie-bootstrap -am -DskipTests compile`
Expected: **BUILD SUCCESS**。重点确认:
- spring-ai 依赖经父 pom BOM 解析成功(下载 milestone 产物);
- ai 的 lombok 生成类(`@Data`/`@Slf4j` 等,如 `AiLogs`、`ChatService`)编译通过——证明删 ai 独立 compiler 配置后 SB parent 承接注解处理成功。
- 若 lombok 处理失败:在 bootstrap pom `<build>/<plugins>` 加 maven-compiler-plugin 的 annotationProcessorPaths lombok(同 ai 原 配置),再编译。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m 'refactor(backend): 并入 ai 模块 → bootstrap + spring-ai BOM/仓库迁父 pom（v3.0.0 两模块 4/5）'
```

---

## Task 5: 并入 common-web 模块(压轴)

**Files:**
- Move: `shiwujie-common-web/src/main/java/com/swj/shiwujie/*` → bootstrap(common/config/interceptor/exception/utils 等)
- 无 resources(common-web 无资源)
- Delete: `shiwujie-backend/shiwujie-common-web/`
- Modify: bootstrap pom(加 common-web 外部依赖、**删 shiwujieCommonWeb 直接依赖**)、父 pom

**Interfaces:** 至此 bootstrap 直接持有全部业务 + 公共代码;model 仍为唯一外部内部依赖。common-web 的消费者(user/call/community/ai 代码)已在 bootstrap 内,移除 common-web 模块后引用在同模块 src 内解析。

- [ ] **Step 1: 搬迁 common-web 源码**

```bash
cp -r shiwujie-backend/shiwujie-common-web/src/main/java/com/swj/shiwujie/. shiwujie-backend/shiwujie-bootstrap/src/main/java/com/swj/shiwujie/
git rm -r shiwujie-backend/shiwujie-common-web
git add -A shiwujie-backend/shiwujie-bootstrap
```

- [ ] **Step 2: bootstrap pom 加 common-web 外部依赖、删 shiwujieCommonWeb**

Modify `shiwujie-backend/shiwujie-bootstrap/pom.xml`:删 `shiwujieCommonWeb` 直接依赖块(Task 1 加的)。新增 common-web 的外部依赖(去重——已在的跳过):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
</dependency>
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
</dependency>
```
(hutool/mybatis-plus 亦由 model 传递,显式声明保险;web/redis 在 Task 1 后由 common-web 传递,此刻 common-web 消失故转直接。)

- [ ] **Step 3: 父 pom 删 common-web 登记**

`<modules>` 删 `<module>shiwujie-common-web</module>`;DM 删 `shiwujieCommonWeb` 块。

- [ ] **Step 4: 编译验证**

Run: `mvn -pl shiwujie-bootstrap -am -DskipTests compile`
Expected: **BUILD SUCCESS**。此时 `<modules>` 应只剩 model + bootstrap。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m 'refactor(backend): 并入 common-web → bootstrap，7→2 模块定型（v3.0.0 两模块 5/5）'
```

---

## Task 6: 父 pom 收尾 + 全量 install + jar 启动验证 + 契约启动级回归

**Files:** `shiwujie-backend/pom.xml`、`shiwujie-backend/shiwujie-bootstrap/pom.xml`(最终核对)

- [ ] **Step 1: 核对父 pom 最终态**

读 `shiwujie-backend/pom.xml`,确认:
- `<modules>` = `shiwujie-model` + `shiwujie-bootstrap`(两条)
- `<dependencyManagement>` 内部模块仅留 `shiwujieModel`;外部 + spring-ai BOM + paho/kryo/jsoup 齐
- `<properties>` 含 spring-ai 两版本
- `<repositories>` 含 spring-milestones
不符则就地理顺。

- [ ] **Step 2: 核对 bootstrap pom 最终态(参考真值)**

`shiwujie-bootstrap/pom.xml` 的 `<dependencies>` 最终应为(顺序可异):
```xml
<dependencies>
    <dependency><groupId>com.swj</groupId><artifactId>shiwujieModel</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
    <dependency><groupId>org.springframework.session</groupId><artifactId>spring-session-data-redis</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-websocket</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-configuration-processor</artifactId><optional>true</optional></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-devtools</artifactId><scope>runtime</scope><optional>true</optional></dependency>
    <dependency><groupId>com.github.xiaoymin</groupId><artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>com.baomidou</groupId><artifactId>mybatis-plus-spring-boot3-starter</artifactId></dependency>
    <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.apache.commons</groupId><artifactId>commons-lang3</artifactId></dependency>
    <dependency><groupId>cn.hutool</groupId><artifactId>hutool-all</artifactId></dependency>
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    <dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId></dependency>
    <dependency><groupId>org.eclipse.paho</groupId><artifactId>org.eclipse.paho.client.mqttv3</artifactId></dependency>
    <dependency><groupId>com.alibaba.cloud.ai</groupId><artifactId>spring-ai-alibaba-starter-dashscope</artifactId></dependency>
    <dependency><groupId>com.esotericsoftware</groupId><artifactId>kryo</artifactId></dependency>
    <dependency><groupId>org.jsoup</groupId><artifactId>jsoup</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
</dependencies>
```
`<build>/<plugins>` 的 spring-boot-maven-plugin repackage(排除 lombok)保留不变。若多/少依赖就地理顺。

- [ ] **Step 3: 全量 install**

Run:
```bash
mvn -f shiwujie-backend/pom.xml install -DskipTests
```
Expected: **BUILD SUCCESS**;reactor 仅 model + bootstrap 两模块;bootstrap 产出可执行 fat jar。

- [ ] **Step 4: 启动 jar 验证**

Run(后台或新终端;需连远程 MySQL `shiwujie` 库 + Redis):
```bash
java -jar shiwujie-backend/shiwujie-bootstrap/target/shiwujieBootstrap-0.0.1-SNAPSHOT.jar
```
Expected: 日志 `Started ShiwujieBootstrapApplication ... on port(s): 8100`,无启动异常,DB 16 表连上。Ctrl-C 停。

- [ ] **Step 5: 契约启动级回归(jar 运行中,另开终端)**

```bash
# HTTP 路由:登录校验白名单接口(返回 BaseResponse JSON,业务码可非 0 但信封对)
curl -s "http://localhost:8100/api/user/blind/login/check?phone=13800000000"
# WS 握手 101(用 curl 升级,期望 101 Switching Protocols 或服务端 WS 接受)
curl -s -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" -H "Sec-WebSocket-Version: 13" "http://localhost:8100/api/ws/call" | head -1
# OpenAPI 文档实测路由全注册
curl -s "http://localhost:8100/v3/api-docs" | grep -oE '"/api/(user|call|community|ai)/[^"]*"' | sort -u | head
```
Expected:HTTP 返回 BaseResponse 信封;WS 首行 `HTTP/1.1 101`;api-docs 列出四模块 `/api/**` 路由。路径前缀与合并前一致 = 契约未破。

- [ ] **Step 6: 提交收尾(若有 pom 微调;否则跳过)**

若 Step 1-2 有理顺改动:
```bash
git add -A
git commit -m 'build(backend): 父/bootstrap pom 收尾 + mvn install 全绿 + jar 启动契约回归通过（v3.0.0 两模块）'
```
无改动则跳过本步。

---

## Task 7: 回卷 A — architecture + CHANGELOG + product(叙事层文档)

**Files:** `docs/architecture/tech-stack.md`、`docs/architecture/gateway-dubbo.md`、`docs/CHANGELOG.md`、`docs/product/v3.0.0/overview.md`

**Why:** v3.0.0 同卷 as-built 文档现描述"7 模块/保留模块化分包",需改写为"2 模块(model 契约 + bootstrap app)"。product 层只补高层架构演进一句,**不涉** FR/AC/端点/路径(守 product 内容边界)。

- [ ] **Step 1: tech-stack.md 改两模块态**

打开 `docs/architecture/tech-stack.md`,定位"v3.0.0 单体化(已落地)"段及任何"7 模块/模块化分包/model + common-web + 四业务 + bootstrap"措辞,改为:

> v3.0.0 定型为 **2 模块**:`shiwujie-model`(契约层:实体/枚举/VO/DTO/`Inner*` 接口)+ `shiwujie-bootstrap`(唯一 app 模块:聚合原 common-web 公共层 + user/call/community/ai 全部业务代码,唯一 `@SpringBootApplication` + repackage)。原 7 模块结构(含独立 common-web/user/call/community/ai)已并入 bootstrap——同包根 `com.swj.shiwujie.*` 下文件搬迁,不改 import、不改契约。SB3.4.5/Java21 全栈统一不变。

- [ ] **Step 2: gateway-dubbo.md 改两模块态**

打开 `docs/architecture/gateway-dubbo.md`,把"单体化目标/落地"段的模块清单从"model/common-web/user/call/community/ai/bootstrap 7 模块"改为"model + bootstrap 2 模块(common-web + 四业务并入 bootstrap)"。补一句:spring-ai BOM/版本/spring-milestones 仓库随 ai 并入迁至**父 pom** 集中管理。

- [ ] **Step 3: CHANGELOG.md 补模块合并明细**

打开 `docs/CHANGELOG.md`,在 `## v3.0.0` 段的"行为变更明细(架构)"小节追加一条:

> - **模块合并(7→2)**:common-web + user/call/community/ai 五模块的 src 与资源(mapper XML/logback/prompttemplate)并入 `shiwujie-bootstrap`,model 保留为唯一库模块;父 pom `<modules>` 收敛为 {model, bootstrap},spring-ai BOM/版本/仓库迁父 pom 集中管理。**对外契约零变更**(HTTP 路径 / WS `/api/ws/call` / 业务码 / 返回字段全不变)。

- [ ] **Step 4: product/v3.0.0/overview.md 补一句架构演进(守边界)**

打开 `docs/product/v3.0.0/overview.md`(或对应 product overview 文件),在架构概述段补一句高层描述:**不写** 源码路径/符号/file:line/启动命令(守 product 内容边界):

> v3.0.0 后端为单体两模块结构(契约层 + 唯一应用模块),对外行为相对 v2.1.0 零变更(契约继承,详见各 FR/AC)。

- [ ] **Step 5: 内容边界自检**

Run(期望零命中 product):
```bash
grep -rniE 'src/main|com/swj|\.java:|@DubboService|java -jar|mvn |\.yml' docs/product/
```
Expected: 空输出。

- [ ] **Step 6: 提交**

```bash
git add -A docs/architecture docs/CHANGELOG.md docs/product
git commit -m 'docs(v3.0.0): 回卷A—architecture/CHANGELOG/product 改两模块态'
```

---

## Task 8: 回卷 B — development 三件套 + 后端模块文档(实现层文档)

**Files:** `docs/development/v3.0.0/task-breakdown.md`、`docs/development/v3.0.0/release-checklist.md`、`shiwujie-backend/docs/modules/*.md`

- [ ] **Step 1: task-breakdown.md 加阶段 2.8 子项**

打开 `docs/development/v3.0.0/task-breakdown.md`,在阶段 2 段(2.7 之后)新增:

```
- [x] 2.8 模块合并(model+bootstrap 两模块):common-web + user/call/community/ai 五模块 src 与资源(mapper XML ×14 / logback-spring.xml / prompttemplate ×3)并入 `shiwujie-bootstrap`,model 保留;父 pom `<modules>`={model, bootstrap}、spring-ai BOM/版本/spring-milestones 仓库迁父 pom;7→2 模块。对外契约零变更(同包根 `com.swj.shiwujie.*` 搬迁,不改 import)。`<本次合并 commit 序列>`
```
同步把 2.1 描述里"7 模块"措辞改为"两模块(model + bootstrap),原 common-web/四业务已并入 bootstrap"。

- [ ] **Step 2: release-checklist.md 加两模块核对项**

打开 `docs/development/v3.0.0/release-checklist.md`,在"单体化交付"节加:

```
- [x] 两模块结构:<modules> = {shiwujie-model, shiwujie-bootstrap}；spring-ai BOM/仓库在父 pom；mvn install 2 模块 reactor 全绿 ✅
```

- [ ] **Step 3: 后端模块文档改写**

先 `ls shiwujie-backend/docs/modules/` 列出现有模块文档。把 common-web/user/call/community/ai 五份的内容**合并**为单份 `shiwujie-backend/docs/modules/bootstrap.md`,结构按子包分节(公共层 ← 原 common-web / user / call / community / ai 各一节),说明其代码现处 `shiwujie-bootstrap` 的 `com.swj.shiwujie.*` 对应包下、对外路径不变;**删除**原五份独立模块文档;`model.md`(或 model 对应文档)不动。

- [ ] **Step 4: 提交**

```bash
git add -A docs/development shiwujie-backend/docs
git commit -m 'docs(v3.0.0): 回卷B—task-breakdown 2.8 + release-checklist + 模块文档改两模块'
```

---

## 收尾(计划全部任务完成后)

- [ ] 推 Gitee:`git push -u origin refactor/backend-collapse-to-bootstrap`
- [ ] 在 Gitee 发起 PR → master(禁本地 merge)
- [ ] (PR 合并后)三处版本号自检仍为 v3.0.0;functional 联调通过后再打 v3.0.0 tag、冻结目录、current 转下一版
