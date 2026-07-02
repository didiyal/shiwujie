# Android 原生 App

> 视障者/志愿者客户端。**原生 Android（Java + ViewBinding，compileSdk 35）**，非 uniapp。按角色分包，集成 anyRTC/讯飞/Camera2/高德，悬浮窗与无障碍 TTS。

## 模块定位

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-frontend/app/shiwujie/app/src/main/java/com/swj/shiwujie/` |
| 语言/UI | Java + ViewBinding |
| 构建 | compileSdk **35**，minSdk 30 |
| 角色 | 视障者（`blind/`）+ 志愿者（`volunteer/`）双端，共享 `common/` 与 `data/model/` |
| 主后端 | `http://47.112.114.139:8100`（明文 HTTP） |
| WS | `ws://47.112.114.139:8100/api/ws/call`（明文，无 token） |

## 目录与角色包结构

```
com/swj/shiwujie/
├── blind/                 # 视障者端
│   ├── LoginActivity / QuickLoginActivity / BlindHomeActivity / VideoCallActivity
│   └── ui/{ai, community, family, home, profile}/  # AiFragment(独有) 等
├── volunteer/             # 志愿者端
│   ├── LoginActivity / QuickLoginActivity / VolunteerHomeActivity / VideoCallActivity
│   └── ui/{community, family, home, message, profile}/  # MessageFragment(独有)
├── common/
│   ├── network/           # RetrofitClient / ApiService / WebSocketManager / WebSocketService
│   │                     # AiChatManager / VideoCallManager / EmergencyHelpManager
│   │                     # ImageRecognitionManager / ObstacleDetectionManager(RetrofitClient)
│   ├── service/           # AIFloatingBallService / FloatingWindowService（悬浮窗）
│   ├── ui/                # EmergencyHelpFloatingWindow / EmergencyHelpIncomingWindow
│   ├── navigation/        # NavigationHelper
│   ├── utils/             # TTSManager / SpeechRecognitionManager / IatResultParser
│   │                     # CameraPreviewManager / AppListManager / PermissionManager / ...
│   └── activity/
├── data/model/            # VO/Request（与后端 model 对应）：BlindVO / VolunteerVO / FamilyVO
│                          # AiChatRequest/Response / ObstacleDetection* / SocketDataV0 ...
├── MyApplication.java / ChooseIdentityActivity.java / SetPasswordActivity.java
```

> 盲人端独有 **AI Tab + AI 悬浮球**；志愿者端独有 **Message Tab**。

## 网络与 WebSocket 层

| 类 | 职责 |
|---|---|
| `RetrofitClient` | 主业务 Retrofit 实例，baseURL 主后端 |
| `ObstacleDetectionRetrofitClient` | 避障服务专用（`https://192.168.100.248:9989/`，内网不可达 + 自签全信任） |
| `ApiService` | 60+ HTTP 端点（用户/家庭/视频/紧急/社区/AI/避障） |
| `WebSocketManager` + `WebSocketService`（前台 Service） | 与后端 `/ws/call` 交互，处理 -1/0/1/2/3/4/5 + 5001-5006 信令 |
| `SocketDataV0` | WS 消息体，`toSendJson()` 只发 4 字段 |

## SDK 集成

| 能力 | SDK | 封装类 | 备注 |
|---|---|---|---|
| 音视频 | anyRTC | `VideoCallManager` | RTC token 为空（风险） |
| TTS | 讯飞 | `TTSManager` / `SegmentedTTSManager`(空文件) / `ObstacleDetectionTTSManager` | appid 硬编码（重复两处） |
| ASR | 讯飞 | `SpeechRecognitionManager` / `IatResultParser` | |
| 相机/避障 | **Camera2**（封装为 `CameraPreviewManager`） | `ObstacleDetectionManager` | 注：依赖声明提及 CameraX，**实际用 Camera2**；避障模型未接（模拟数据） |
| 导航 | 高德 | `NavigationManager` / `NavigationHelper` | **URI 调起**（未集成高德 SDK） |
| 悬浮窗 | 系统 WindowManager | `FloatingWindowService` / `AIFloatingBallService` | |

## 关键数据流

### 登录

```
ChooseIdentityActivity 选身份 → blind/volunteer LoginActivity
  → ApiService.loginAndRegisterQuickly(phone) → 后端返 token + VO
  → SharedPrefsUtil 存 token/用户信息
  → 启动 WebSocketService（建 /ws/call 长连接）
  → 进入 HomeActivity，盲人端启动 AI 悬浮球
```

### AI 多轮对话

```
盲人 AiFragment 语音输入 → SpeechRecognitionManager 转文字
  → AiChatManager → POST /api/ai/ai/doChatByText (SSE)
  → 流式接收 → 打字机效果展示 + 流式 TTS 播报
```

### 视频求助

```
盲人发起 → 后端撮合（Redis 队列 FIFO）→ WS type1 通知志愿者
  → 志愿者就绪 → WS type2 转告盲人 → 双方 VideoCallActivity
  → anyRTC 建立双向音视频 → 挂断上传回放
```

## 配置要点

- 主后端明文 HTTP `47.112.114.139:8100`；避障服务内网 + 自签证书全信任。
- anyRTC appId、讯飞 appid、高德 key 全部**硬编码**。
- 申请了系统级权限（悬浮窗等）。
- token 无统一拦截器（手动加 Header）。

## 功能需求（FR-APP）

| ID | 需求 |
|---|---|
| FR-APP-01 | 双角色选择与一键/密码登录 |
| FR-APP-02 | 登录态持久化（SharedPrefs） |
| FR-APP-03 | 盲人端 AI 多轮对话（SSE 流式 + 流式 TTS） |
| FR-APP-04 | 盲人端 AI 悬浮球（AIFloatingBallService） |
| FR-APP-05 | 视频求助发起/接听（anyRTC 双向） |
| FR-APP-06 | 紧急求助（家属通知 + 来电悬浮窗 + 响铃） |
| FR-APP-07 | WebSocket 信令处理（-1/0/1/2/3/4/5xxx） |
| FR-APP-08 | 社区（求助帖/活动/报名） |
| FR-APP-09 | 家庭（加入/退出/审核） |
| FR-APP-10 | 图片识别（多模态） |
| FR-APP-11 | 避障检测（Camera2 + 模型） |
| FR-APP-12 | 高德导航（URI 调起） |
| FR-APP-13 | 跳转他应用 |
| FR-APP-14 | 讯飞 TTS/ASR |
| FR-APP-15~18 | 资料编辑 / 消息（志愿者）/ 个人中心 / 权限管理 |

## 验收标准（AC-APP）

| ID | 验收点 |
|---|---|
| AC-APP-01 | 选身份 → 一键登录 → 进入对应 Home，token 持久化 |
| AC-APP-02 | 盲人 AI 对话：语音输入 → SSE 流式回复 + 流式 TTS 播报 |
| AC-APP-03 | AI 悬浮球可在任意界面悬浮，点击唤起 AI |
| AC-APP-04 | 视频求助：盲人发起 → 志愿者接听 → 双向音视频建立 |
| AC-APP-05 | 紧急求助：盲人发起 → 家属端悬浮窗 + 响铃提醒 |
| AC-APP-06 | WS 连接保活（心跳），断线重连 |
| AC-APP-07 | 收到 5xxx 推送正确跳转对应功能页 |
| AC-APP-08 | 图片识别返回 100 字内描述 |
| AC-APP-09~18 | 社区/家庭/导航/跳转/TTS 各功能可达 |

## 已知问题

> 共 25 项，按类别列要点：

**安全**：
1. 明文 HTTP/WS（无 TLS）。
2. 避障服务自签证书全信任（`ObstacleDetectionRetrofitClient`）。
3. release 构建未关闭日志（含 token/明文数据泄露）。
4. anyRTC token 为空。
5. SDK Key 全部硬编码。
6. 申请了系统级权限。

**功能**：
7. **心跳频率 bug**：注释 30s，实际间隔 2 小时（WebSocketManager）。
8. **避障模型未接**（用模拟数据），且避障服务内网不可达。
9. `MessageFragment`（志愿者消息）全 mock 数据。
10. `SegmentedTTSManager` 空文件。
11. 紧急求助无超时机制。

**架构**：
12. `CameraPreviewManager` 用 Camera2（非依赖声明的 CameraX，与任务描述不符）。
13. `NavigationManager` 未集成高德 SDK（仅 URI 调起）。
14. 讯飞 appid 重复两处。
15. token 无统一拦截器（各请求手动加 Header）。
16. `VideoCallManager` 信令处理不完整（type 3/4/5 走 default 分支）。
