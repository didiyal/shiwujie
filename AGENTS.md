# AGENTS.md

> ZCode Agent 工作向导。规则真值在 [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md)——本文件只做导航与速查；根目录 [`CLAUDE.md`](CLAUDE.md) 是平行手维护的同类向导，两者冲突时以 CONTRIBUTING 为准。

## 仓库是什么

「视无界」——面向视障人士的无障碍平台：**单体后端 + 原生 Android 客户端（盲人/志愿者双端）+ Vue3 官网与管理后台**，核心是 AI 助手「小界」（对话/拍照识别）+ 远程人工协助（视频求助）+ 社区互助。`v3.0.0` 已封版打 tag（2026-09-12），当前为封版后维护期：App 以 3.1.x 补丁线推进，文档指针暂仍指 `v3.0.0/`。

```
Phase2/
├── docs/                      # ★ 平台层文档（CONTRIBUTING / product / architecture / development / CHANGELOG）——先读这里
├── shiwujie-backend/          # 后端单体：shiwujie-model（契约层，无 Spring）+ shiwujie-bootstrap（唯一 app）
├── shiwujie-frontend/
│   ├── app/shiwujie/          # Android 原生（Java + ViewBinding，Gradle Kotlin DSL）
│   └── web/                   # Vue3 官网 + 管理后台
├── shiwujie-ai/               # LangGraph Python 智能体服务（休眠中，docker profiles=["ai"] 才启动）
└── scripts/  docker/  config/ # 部署脚本与配置
```

## 构建 / 测试 / 运行

**后端**（Maven 2 模块 reactor，SB 3.4.5 / Java 21，单 fat jar；运行仅需 MySQL 库 `shiwujie` + Redis db=2，端口 8100，无 Nacos/Dubbo）：

```bash
mvn -f shiwujie-backend/pom.xml install -DskipTests   # 构建
mvn -f shiwujie-backend/pom.xml test                  # 纯 Mockito 单测（不起 Spring/DB）
```

**Web**（`shiwujie-frontend/web/`，dev 端口 9090）：`npm run dev`（`dev:clean` 先 kill 9090）/ `npm run build` / `npm run lint`。

**Android**（`shiwujie-frontend/app/shiwujie/`）：`./gradlew assembleDebug` / `./gradlew :app:testDebugUnitTest`。

## 工作约定

- **禁止本地 `git merge`**：合并走远程 PR；日常单人开发可直接提交 master。推送只推 `git push origin master`（origin=Gitee），**pre-push 钩子（`.githooks/pre-push`）自动把 master 镜像到 github 远程**，不要手动双推。
- **提交格式**：Conventional Commits，**不带署名**（无 Co-Authored-By / Generated with）。
- **凭据**：留在 yml 里 `${ENV:default}` 占位符 + 内联默认值；公网 IP `47.112.114.139` 硬编码是有意为之，不抽环境变量。
- **文档同步**：行为变更进 CHANGELOG；内部实现变更更新随代码就近的 development 文档（各子项目 `docs/`，含 `known-issues.md` 登记坑）。`docs/product/` 内禁止出现源码路径/启动命令。

## 服务器与部署（运维速查）

- **SSH**：`ssh -p 2222 root@47.112.114.139`（本机网络封出站 22，**必须用 2222**；宝塔面板已关防火墙）。部署目录 `/www/wwwroot/shiwujie/`。
- **后端发版**：`mvn install` → 上传 `shiwujie-bootstrap/target/shiwujieBootstrap-0.0.1-SNAPSHOT.jar` → 服务器重启（端口 8100）。
- **APK 发版（顺序敏感）**：**先上传签名 APK 到 `/www/wwwroot/shiwujie/shiwujie-frontend/`（任意文件名，服务端自动选最新），再更新版本配置并重启后端**——`/api/download/version` 依赖「目录里有包 + 版本配置」两者，顺序反了用户会拉到旧包。
- **强更版本链（三处必须同步）**：`app/build.gradle.kts` 的 versionCode/versionName ↔ `shiwujie-backend/.../application.yml` 的 `APP_VERSION_CODE/NAME`（env 可覆盖，当前 20 / 3.1.15）。客户端强更判定：本地 code < 服务端 code 即弹不可取消更新。测强更时客户端本地版本必须低于服务端，否则静默不弹（这是设计行为）。
- **Android 签名**：`../apk/release.jks`，alias `shiwujie`；发版必须走签名 release 包。

## 平台坑位（易踩，先看再改）

- **Android / release 构建**：R8 会剥掉 `Log.d/v`——**真机调 release 包只信 `Log.e`**。
- **Android / 无障碍（TalkBack 是硬性验收）**：API 33+ 注册 Receiver 必须 `RECEIVER_NOT_EXPORTED`；给 NOT_EXPORTED 接收器发隐式广播需 `setPackage(getPackageName())`（vivo ROM 实测隐式广播丢失）；播报文案放单个 TextView 一次读完，别拆多控件。
- **Android / 强更安装**：安装页不弹多半是 FileProvider 路径没覆盖下载目录（`file_paths.xml` 需 external-files-path 等）或下载未完成就拉起安装。
- **后端 / 鉴权**：`/api/ai/**` 已并入业务 `LoginCheckInterceptor`（与 user/call/community 完全一致），身份取 `LoginUtils.getLoginBlindId()`；不要再建 AI 专用拦截器（原 AI 后门已删，见 `shiwujie-backend/docs/known-issues.md`）。
- **后端 / AI 提示词**：拍照识别文案面向视障人士——先整体后细节、≤60 字、用方位语言；追问轮可放宽。AI 对话上下文按 blindId 隔离（conversation id = blindId）。
- **Docker**：默认 `docker compose up` 只起 Java 后端；Python AI 服务在 `profiles:["ai"]`，需 `--profile ai` 显式启用。

## 改敏感区前先读

| 区域 | 读什么 |
|---|---|
| 任何文档/提交前 | [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md)（四层文档规范，**必读**） |
| 鉴权/拦截器/WS | [`docs/architecture/auth.md`](docs/architecture/auth.md) + [`shiwujie-backend/docs/known-issues.md`](shiwujie-backend/docs/known-issues.md)（未修安全债清单） |
| 匹配/求助链路 | [`shiwujie-backend/docs/modules/`](shiwujie-backend/docs/modules/)（joinVideohelp 原子匹配、WAITING/HELPING 自动过期、挂断 type=5 对端通知） |
| App 结构与 AI 页 | [`shiwujie-frontend/app/docs/`](shiwujie-frontend/app/docs/)（AiFragment 四按钮、悬浮球后台才显示、句子级 TTS 队列） |
| Web/官网 | [`shiwujie-frontend/web/docs/`](shiwujie-frontend/web/docs/) |
