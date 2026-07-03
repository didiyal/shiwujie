# Android 原生 App

> 视障者/志愿者客户端。**原生 Android（Java + ViewBinding，compileSdk 35）**，非 uniapp。按角色分包，集成 anyRTC/讯飞/Camera2/高德，悬浮窗与无障碍 TTS。本文为 development 细化（结构/网络/SDK/数据流/配置）；用户可见契约（FR-APP / AC-APP）见 [product/current.md](../../../docs/product/current.md)。

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

---

> **延伸阅读**
>
> - 用户可见契约（FR-APP / AC-APP）：[../../../docs/product/v2.0.0/functional-requirements.md](../../../docs/product/v2.0.0/functional-requirements.md) · [../../../docs/product/v2.0.0/acceptance-criteria.md](../../../docs/product/v2.0.0/acceptance-criteria.md)
> - 功能实现备注（盲人社区 / 社区活动 / 应用列表管理器）：[features.md](features.md)
> - 缺陷与技术债（安全/功能/架构 + 已修历史坑）：[known-issues.md](known-issues.md)
