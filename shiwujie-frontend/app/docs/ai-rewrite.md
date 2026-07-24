# AI 重写：Android 前端侧（设计敲定 · 待 Phase 5）

> v3.0.0（进行中，未发布）。本文登记「AI 模块重写」（现有 Java AI 模块整体替换为 Python 智能体驱动）在 **Android 客户端侧** 的设计变更。**全部内容均为「设计敲定（Phase 1-4）· 实现待 Phase 5」**——本仓尚无任何一行对应代码落地，下文「现状」指 v2.1.0 封版态（现有源码），「重写后」指 Phase 5 待实现的目标态。
>
> 跨切面总图与 polyglot 双进程架构见 [../../../docs/architecture/ai-rewrite.md](../../../docs/architecture/ai-rewrite.md)；后端两进程部署见 [../../../shiwujie-backend/docs/deployment.md](../../../shiwujie-backend/docs/deployment.md)。用户可见契约（对话通道 / 求助信令 / 导航分步确认等用户面措辞）见根 [product/current.md](../../../docs/product/current.md)，本文不重复 FR/AC。
>
> 规范见 [docs/CONTRIBUTING.md](../../../docs/CONTRIBUTING.md)。本文为 development 层（允许源码符号 / `file:line`）。

## 一句话定位

AI 重写在 App 侧的影响面集中在 **5 处**：① `SocketDataV0` 加可选 `destination` 载荷；② 5006 导航 handler 从误读 `volunteerPhone` 改读 `destination`；③ AI 对话客户端由 SSE HTTP 改 WS AI-turn 消息（图片仍走 HTTP multipart）；④ AI 主界面从「语音+拍照」2 按钮重写为「进页即摄像头 + 4 按钮」；⑤ WS 登录由自报 `phone` 改短时 `ticket`。业务端（user/call/community）零变更。

---

## 1. SocketDataV0 加可选 destination（加字段不改名）

### 现状

`SocketDataV0`（`app/src/main/java/com/swj/shiwujie/data/model/SocketDataV0.java`）是 App 端 WS 消息体，承载 -1/0/1-5/5001-5006 全部信令。其中 5006（`REQUEST_TYPE_NAVIGATION_REQUEST`，导航请求，第 109 行常量）**首次需要携带「导航终点」结构化载荷**，但当前模型只有 `blindPhone` / `volunteerPhone` / `channelId` / `blindId` / `callStatus` / `callId` / `message` 等扁平字段，没有目的地{name,lat,lng,address}。

### 重写后（设计敲定 · 待 Phase 5）

新增**可选**嵌套字段 `destination`，**加字段不改名、不删既有字段**（与 Java 侧 `SocketData` / `SocketVO` 三处对齐）：

```java
// 新增：5006 导航终点载荷（可选；仅 5006 携带，其余信令缺省）
@SerializedName("destination")
private Destination destination;

public static class Destination {
    @SerializedName("name")    private String name;     // 地点名（朗读友好）
    @SerializedName("lat")     private double lat;      // 纬度
    @SerializedName("lng")     private double lng;      // 经度
    @SerializedName("address") private String address;  // 完整地址
    // getter/setter 略
}
public Destination getDestination() { return destination; }
public void setDestination(Destination d) { this.destination = d; }
```

- **可选性**：`destination == null` 时 5006 行为回退到旧态（见第 2 节迁移期），平滑灰度。
- **契约对齐**：此为本次重写范围内的字段扩展，码值 5006 不变；WS 12 信令码（-1/0/1/2/3/4/5001-5006）框架不变。详见 product 契约「求助与导航信令」段。
- **序列化**：`toSendJson()`（第 223 行）只发后端期望的 4 字段，App **只收不发** 5006，故 `destination` 不进 `toSendJson()`，仅作反序列化入站载荷。

---

## 2. AiFragment 5006 handler 迁移：volunteerPhone → destination

### 现状（已知问题）

`AiFragment.handleNavigationRequest(SocketDataV0 data)`（`app/src/main/java/com/swj/shiwujie/blind/ui/ai/AiFragment.java:4500`）当前**误把 `volunteerPhone` 当导航终点**读：

```java
// AiFragment.java:4504-4505（现状，已知问题）
// 从volunteerPhone字段获取目的地信息
String destination = data.getVolunteerPhone();
```

`volunteerPhone` 语义是「志愿者手机号」，根本不是目的地。这是 v2.1.0 封版态遗留：5006 当时没有专用终点字段，临时复用了 `volunteerPhone` 字符串位塞目的地名，导致 5006 只能传一个名字、无经纬度、且语义错乱。

### 重写后（设计敲定 · 待 Phase 5）

读 `destination` 结构化载荷，迁 `showNavigationConfirmDialog(String)` → 接 `Destination`：

```java
// 重写后（待 Phase 5）
SocketDataV0.Destination dest = data.getDestination();
if (dest == null) {
    // 迁移期兜底：旧后端仍只塞名字进 volunteerPhone 时，回退读名字（灰度配对期保留）
    dest = SocketDataV0.Destination.fromName(data.getVolunteerPhone());
}
// 起导航走高德 SDK（launch_navigation 信令），终点用 dest.lat/lng/name
```

- **迁移期**：灰度 = 硬切换（后端镜像 + APK 同批发，SSE↔WS 不兼容须版本配对），但 5006 的 `volunteerPhone`→`destination` 切换加一个 `dest==null` 回退分支，降低联调期对齐成本；正式版可删回退。
- **经纬度落点**：现状 `executeNavigation(destination)` 仅以名字做高德 URI 调起（见 [android.md](android.md)「`NavigationManager` 未集成高德 SDK」），重写后终点优先用 `lat/lng` 精确定位，名字仅作 TTS 朗读。

---

## 3. AI 对话客户端：WS AI-turn（文本 + 图片，老 SSE 已清）

> ✅ **已落地**（chunk-2e-2 文本 turn WS + chunk-2e-3 图片 turn HTTP+WS + chunk-2f-1 老 SSE 死代码全清）。本节为 as-built 记录。

### 通道结构（as-built）

缝 A 单双向 WS 通道承载文本/语音/位置/流式回/5001-6 信令；图片大二进制走 HTTP multipart，流式答复骑同一 WS 通路：

- **文本 turn**：经既有 WS `/api/ws/call`（`WebSocketManager` 复用不开新连接）发 `requestType=100`（裸 socketData，含 `text` + `position`），收 `110`/`111`/`112`/`113`（delta/progress/turn_end/error）由 `AiTurnManager`（`common/network/AiTurnManager.java`，chunk-2e-2 新增）路由到 UI + 讯飞 TTS 按句切喂（`feedTtsIncremental`/`flushRemainingTts`）。
- **图片 turn**（chunk-2e-3）：multipart 上传 `POST /api/call/ai/image-turn`（`ApiService.sendAiImageTurn` + `ImageRecognitionManager.sendImageTurn`），Java 收图转 base64 data URL 调 Python `/ai/turn {image}`，**流式答复骑同一套 WS 中继**（`AiTurnManager` 现成路由，零 Android 重复）。图片 base64 MB 级超 WS 帧上限故走 HTTP。
- WS 断线重连已修（见 [known-issues.md](known-issues.md)「AI 页 WS 断线不重连」）；`AiTurnManager` turn 中途 `setMatchingStatus(true)` 压制重连防丢 turn_end，45s watchdog 兜底解锁麦克风。

### 老 SSE 通道（已删 · chunk-2f-1）

> 2026-07-20 全清。后端 SSE 端点 `/api/ai/ai/doChatByText`/`doChatByImage`/`/NewApp` 早于 chunk-2b-5 删除，App 侧老 SSE 客户端调 404 成死代码，chunk-2f-1 删净：`AiChatManager.java`（整文件）+ `ImageRecognitionManager` 老 `sendImage`/`OnStreamingListener`/`handleStreamingResponse` + `ApiService.sendAiTextMessage`/`sendAiImageMessage` + `AiFragment` 智能播报系统（11 方法 + `StreamingState` 状态机 + handler/buffer 字段）+ 图片专属 UI 4 方法。chunk-2e-2「AiChatManager 保留作降级」同期撤销（后端已删、降级是 fiction）。详见根 [CHANGELOG](../../../docs/CHANGELOG.md) chunk-2f-1。

---

## 4. AI 主界面 4-button 重写（进页即摄像头 + 4 按钮）

### 现状

`AiFragment.initViews`（`AiFragment.java:426`）当前绑定 **2 个主按钮**：`btnVoice`（语音输入，第 427 行）+ `btnCamera`（拍照，第 428 行），外加 `btnCollapseMessage`/`btnExpandMessage`（消息面板折叠，第 429-430 行）与 `btnBack`/`btnAiAssist`。进页不自动开摄像头，拍照靠点 `btnCamera`。

### 重写后（设计敲定 · 待 Phase 5）

AI 主界面重构为 **「进页即摄像头预览 + 4 按钮」**：

| 按钮 | 行为 | 备注 |
|---|---|---|
| **拍照识别** | 直连 `recognize_photo`（VLM 识图），不走对话 turn | 复用 `btnCamera` 路径，但不触发对话，单图直识 |
| **语音输入** | 进入对话（WS AI-turn），承载多轮语音交互 | 复用 `btnVoice` + `SpeechRecognitionManager` |
| **我的位置** | 仅 TTS 朗读当前位置（调 `get_weather` 同源的 position 获取），不发起对话 | 新按钮，纯本地播报 |
| **操作说明** | 静态朗读固定帮助文本（本设备能力速述），不联网 | 新按钮，无网络依赖 |

- **进页即摄像头**：进 AI Tab 即激活 `CameraPreviewManager` 预览（盲人虽不看画面，但预览流供拍照识别与未来避障复用），降低「拍照」操作步数。
- **静态帮助文本**：「操作说明」朗读内容为 App 内固定字符串，描述 4 按钮各自能力，**不**走 KB/search（那是后端智能体能力），保证无网可用。
- 此为 UI 重构，落 product 的用户面契约（FR-APP「AI 助手主界面」段），本文只记实现侧锚点。

---

## 5. WS 登录：自报 phone → 短时 ticket

### 现状

WS 登录靠客户端**自报手机号**，无服务端校验——任何人填一个 `phone` 即可冒充该号盲人收其 5001-6 信令（高危，对应后端 [known-issues.md](../../../shiwujie-backend/docs/known-issues.md) #1「AI 拦截器 dev 后门」/根 architecture auth「风险 #6」一族）：

```java
// WebSocketManager.java:250-251（现状）
private void sendLoginMessage(String phone, boolean isVolunteer) {
    SocketDataV0 loginData = SocketDataV0.createLoginMessage(phone, isVolunteer);
```

`SocketDataV0.createLoginMessage(phone, isVolunteer)`（`SocketDataV0.java:65`）把 `phone` 塞进 `blindPhone`/`volunteerPhone`，`requestType=0` 登录。

### 重写后（设计敲定 · 待 Phase 5）

盲人经**已鉴权 HTTP**（带 `Bearer token`）先换一张**短时 WS ticket**，再以 `requestType=0` 登录消息携带 `ticket`（替代自报 `phone`）：

```
1. App 登录拿 JWT（既有，走 ApiService）
2. GET /api/.../ws-ticket（新 HTTP 端点，Authorization: Bearer <jwt>）
   → 后端 Redis(db=2) 存 ticket→{blindId, phone}，短时 TTL
3. WS 连接后 sendLoginMessage 带 ticket（非 phone）
   → 后端按 ticket 取 blindId/phone 鉴权，type=0 登录态
```

**实现要点（待 Phase 5）**：

- `SocketDataV0` 加可选 `ticket` 字段（与 `destination` 同式，加字段不改名）；`createLoginMessage` 签名扩参或新增 `createLoginMessageWithTicket(...)`，旧 `phone` 重载暂留迁移期。
- `WebSocketManager.connect(phone, isVolunteer)`（第 127 行）签名改为 `connect(ticket, ...)`；自动重连（第 576-580 行现读 `SharedPrefsUtil.getPhone()`）改为 ticket 过期时重新走步骤 2 换票。
- **图片/位置等既有 multipart 端点鉴权不变**（仍 `Bearer token`，HTTP 侧）；ticket 仅用于 WS 登录这一跳。
- 此项堵 phone 冒充，是后端 WS 必修改造（缝 A）的前端配套——后端删 dev 后门、删「回显收到的数据」、HashMap→`ConcurrentHashMap` 等见 architecture/ai-rewrite.md。

---

## 与 v2.1.0 封版态的差异总览

| 维度 | v2.1.0 现状 | v3.0.0 重写后（待 Phase 5） |
|---|---|---|
| 5006 终点载荷 | 复用 `volunteerPhone` 字符串（语义错） | 专用 `destination{name,lat,lng,address}`（可选） |
| AI 文本通道 | SSE `POST /api/ai/ai/doChatByText` | WS AI-turn 消息（流式 token 帧） |
| AI 图片通道 | SSE `POST /api/ai/ai/doChatByImage` | 仍 HTTP multipart（上传），turn 走 WS |
| AI 打字机 | `AiChatManager` 字符级 `Thread.sleep(50ms)` | 删，由后端真实 token 节奏提供流式感 |
| AI 主界面 | 2 按钮（语音+拍照） | 进页即摄像头 + 4 按钮（拍照识别/语音输入/我的位置/操作说明） |
| WS 登录 | 自报 `phone`（可冒充） | 短时 `ticket`（已鉴权 HTTP 换取） |
| 业务端（user/call/community） | — | **零变更** |

## 风险与前置

- **硬切换灰度**：SSE↔WS 不兼容，后端镜像 + APK **必须同批发**、版本配对，不存在渐进混跑期；5006 的 `dest==null` 回退分支只是联调期便利。
- **依赖后端先落地**：本文件所有「重写后」均待后端 WS 改造（缝 A：ticket 鉴权 / 流式中继 / 删后门 / ConcurrentHashMap / 删回显）与 Python 智能体（缝 A Java→Python 内部 HTTP、缝 C MCP）就绪后方可联调。后端改造与 Python 设计见 architecture/ai-rewrite.md。
- **前置 spike**：qwen function-calling 稳定性 spike 须先过（影响导航分步确认等强工具路径），见 architecture/ai-rewrite.md 硬修正 #3。

---

> **延伸阅读**
>
> - 跨切面总图（polyglot 双进程 / 缝 A·C / 两层记忆 / 工具技能 KB）：[../../../docs/architecture/ai-rewrite.md](../../../docs/architecture/ai-rewrite.md)
> - 后端两进程部署（Docker compose / 端口 / .env）：[../../../shiwujie-backend/docs/deployment.md](../../../shiwujie-backend/docs/deployment.md)
> - 用户可见契约（FR-APP / AC-APP）：[../../../docs/product/current.md](../../../docs/product/current.md)
> - App 现状结构/网络/SDK/数据流：[android.md](android.md) · 缺陷与技术债：[known-issues.md](known-issues.md)
