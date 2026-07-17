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

## 3. AI 对话客户端：SSE HTTP → WS AI-turn（图片仍 HTTP）

### 现状

AI 文本对话走独立 SSE HTTP 通道，与 WS 信令通道**物理分离**：

- `ApiService.sendAiTextMessage`（`ApiService.java:672`，`@POST("/api/ai/ai/doChatByText")`，返 `ResponseBody`）+ `sendAiImageMessage`（`ApiService.java:685`，`/api/ai/ai/doChatByImage`）。
- `AiChatManager`（`common/network/AiChatManager.java`）用 `retrofit2` 阻塞读 SSE 流（`handleStreamingResponse` 第 160 行：`BufferedReader` 逐行读 `data: ` 前缀、字符级 `Thread.sleep(typingSpeed=50ms)` 模拟打字机、主线程 `Handler.post` 回 UI），线程池 `Executors.newCachedThreadPool()`。
- 数据流见 [android.md](android.md)「AI 多轮对话」段。

### 重写后（设计敲定 · 待 Phase 5）

**文本 turn 迁入 WS**（缝 A 单双向通道承载文本/语音/位置/图片/流式回/5001-6 信令/未来主动推送），**图片仍走 HTTP multipart**：

- **入站（App→Java）**：WS 新增 AI-turn 消息类型（新 `requestType` + body 携带对话文本/位置）；不再 `POST /api/ai/ai/doChatByText`。
- **出站（Java→App，流式回）**：WS 推流式 token 帧（token-by-token），替代 SSE 的 `data: ` 文本流。`AiChatManager` 的字符级 `Thread.sleep` 打字机逻辑删除——流式感由后端真实 token 到达节奏提供，App 只负责按帧追加展示 + 流式 TTS 播报。
- **图片**：仍 `POST /api/ai/ai/doChatByImage` multipart 上传（大二进制不走文本 WS），上传后触发的新 turn 走 WS。
- **废弃端点**：`/api/ai/ai/doChatByText`、`/api/ai/ai/doChatByImage`、`/NewApp` 三个 SSE 端点随本次重写清除（属重写范围、App 同步改、非违约——AI 通道本就重写）。

**实现要点（待 Phase 5）**：

- `AiChatManager` 改为 WS 帧监听器（注册到 `WebSocketManager` 的 AI-turn 出站帧回调），删 Retrofit SSE 路径与 `typingSpeed`/`isStreaming` 状态机。
- 复用既有 WS 长连接（不另开连接）；AI-turn 帧与 5001-6 信令帧共用一条 WS，按 `requestType` 分流。
- WS 断线重连（已修，见 [known-issues.md](known-issues.md)「AI 页 WS 断线不重连」）现在同样护住 AI 对话连续性。

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
