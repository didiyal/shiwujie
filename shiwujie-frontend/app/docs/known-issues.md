# Android App 已知问题与技术债

> App 端技术债 / 缺陷登记。development 层（允许源码引用与文件名）。🔴 = 安全或高危。统计/占位类缺口同步进 [ROADMAP.md](../../../docs/ROADMAP.md) 待实现「能力补全」。功能契约状态见 [product/v2.1.0/functional-requirements.md](../../../docs/product/v2.1.0/functional-requirements.md) FR-APP。

## 已修复（P0 快速止血，2026-07-12）

> 结合 [`问题.md`](../../../问题.md) 与本轮代码审查，对核实仍存在的前端阻断/高危项做最小修复，不触碰结构。对应 [CHANGELOG](../../../docs/CHANGELOG.md) v3.0.0「App 前端 P0 快速止血」。`assembleDebug` + `assembleRelease`（R8 混淆+资源压缩+lintVital）均已通过。

- ✅ **WS 心跳间隔 2h→30s**（原 #7）：`WebSocketService` 心跳 `scheduleAtFixedRate` 实为 `HOURS`、注释却写 30s，长连接被 NAT 静默掐断；对齐为 30s。
- ✅ **视频通话监听器泄漏**（[`问题.md`](../../../问题.md) #6）：blind/volunteer 两份 `VideoCallActivity.onDestroy` 误调 `removeXxxListener(null)`（no-op），匿名监听器永不移除；改为存字段、按真实引用移除（volunteer 侧用 `setMessageListener(null)` 清单主监听器）。
- ✅ **紧急求助"无法再次求助"死锁**（原 #11 / [`问题.md`](../../../问题.md) #5）：`EmergencyHelpManager` 求助成功后 `isInEmergencyHelp` 仅在失败/取消/挂断复位，家属无响应时永真；新增 60s 超时兜底（复用主线程 Handler，取消/挂断/响应/通话建立时取消），超时自动复位，接口加 `default onHelpTimeout()`。
- ✅ **AI 页 WS 断线不重连**（[`问题.md`](../../../问题.md) #12）：`AiFragment.attemptWebSocketReconnect` 仅打日志从不调 `connect`、却谎报"重连完成"，AI 推送 5001~5006 断线后永不恢复；改为真正调 `WebSocketManager.connectWebSocket`。
- ✅ **统一 token 注入拦截器**（原 #15）：`RetrofitClient` 加 OkHttp 拦截器，仅当请求未自带 `Authorization` 头时注入 `Bearer <token>`，兜住历史漏带；既有手拼 `"Bearer "+token` 调用不受影响。HTTP BODY 日志改仅 DEBUG 打印。
- ✅ **release 构建加固**（原 #3）：开 R8 混淆+资源压缩（补 Gson 泛型/`AiFragment` 内部数据类/anyRTC/讯飞/Retrofit keep）、`allowBackup=false`、讯飞 SDK 日志按 `BuildConfig.DEBUG` 守卫、`buildFeatures.buildConfig=true`。
- ✅ **`network_security_config` 清理**（原 #1 子项）：移除把 CIDR 当 `<domain>` 的无效条目；明文 HTTP/WS 本身仍保留，待后端 TLS。
- ✅ **`gradle-wrapper.properties` 修正**：失效的 macOS 本地分发路径 `file:/Users/luna/...`（仅该开发机可用）切回腾讯镜像远程 URL（已本地缓存）。

## 🔴 安全

1. **明文 HTTP/WS（无 TLS）**：主后端 `http://47.112.114.139:8100`、WS `ws://.../api/ws/call` 均明文，token 与信令可被中间人窃取。（子项「`network_security_config` 误把 CIDR 当 domain」已于 2026-07-12 清理；明文本体待后端启用 TLS。）
2. **避障服务自签证书全信任**：`ObstacleDetectionRetrofitClient` 信任所有证书（`https://192.168.100.248:9989/`，内网不可达）。
3. ~~**release 构建未关闭日志**~~ ✅（2026-07-12 已修，见上「已修复」）：R8 混淆+资源压缩已开、HTTP BODY/讯飞 SDK 日志按 `BuildConfig.DEBUG` 守卫、`allowBackup=false`。
4. **anyRTC token 为空**：`VideoCallManager` 的 RTC token 恒空，房间安全性依赖 anyRTC 服务端。
5. **SDK Key 全部硬编码**：anyRTC appId、讯飞 appid、高德 key 明文在源码。
6. **申请了系统级权限**：悬浮窗等系统级权限，权限范围偏大。

## 功能

7. ~~**心跳频率 bug**~~ ✅（2026-07-12 已修，见上「已修复」）：`WebSocketService` 心跳间隔已对齐为 30s。
8. **避障模型未接**：`ObstacleDetectionManager` 用模拟数据，且避障服务内网不可达（见 #2）。
9. **`MessageFragment` 全 mock 数据**：志愿者消息 Tab 无真实接口。
10. **`SegmentedTTSManager` 空文件**：占位类，未实现。
11. ~~**紧急求助无超时机制**~~ ✅（2026-07-12 已修，见上「已修复」）：`EmergencyHelpManager` 已加 60s "家属无响应"超时兜底。

## 架构

12. **`CameraPreviewManager` 用 Camera2**：依赖声明提及 CameraX，**实际用 Camera2**，与任务/依赖描述不符。
13. **`NavigationManager` 未集成高德 SDK**：仅 URI 调起高德地图 App，非 SDK 集成。
14. **讯飞 appid 重复两处**：硬编码于两个文件，改一处易漏。
15. ~~**token 无统一拦截器**~~ ✅（2026-07-12 已修，见上「已修复」）：`RetrofitClient` 已加拦截器，未带 `Authorization` 头时自动注入 `Bearer <token>`。各调用点手拼 header 的清理属结构重构批，待后续。
16. **`VideoCallManager` 信令处理不完整**：WS type 3/4/5 落入 default 分支（紧急求助通知/取消/跳转在视频侧未处理）。

## 已修复的历史坑（保留备查）

- **社区页 NPE（已修）**：`VolunteerUserInfoManager` 的 `apiService` 静态变量与 `SharedPrefsUtil` 在使用前未初始化，叠加 `CommunityFragment` 复杂视图切换时 `getView()` 为 null → 进入社区页崩溃。修法：`getApiService()` 懒加载 + `SharedPrefsUtil.init(context)` 前置 + 简化 Fragment 视图切换（默认返回 noJoinView，按 `communityId` 异步切换）。详见 [features.md](features.md) 社区模块。
