# Android App 已知问题与技术债

> App 端技术债 / 缺陷登记。development 层（允许源码引用与文件名）。🔴 = 安全或高危。统计/占位类缺口同步进 [ROADMAP.md](../../../docs/ROADMAP.md) 待实现「能力补全」。功能契约状态见 [product/v2.0.0/functional-requirements.md](../../../docs/product/v2.0.0/functional-requirements.md) FR-APP。

## 🔴 安全

1. **明文 HTTP/WS（无 TLS）**：主后端 `http://47.112.114.139:8100`、WS `ws://.../api/ws/call` 均明文，token 与信令可被中间人窃取。
2. **避障服务自签证书全信任**：`ObstacleDetectionRetrofitClient` 信任所有证书（`https://192.168.100.248:9989/`，内网不可达）。
3. **release 构建未关闭日志**：含 token 与明文数据，泄露风险。
4. **anyRTC token 为空**：`VideoCallManager` 的 RTC token 恒空，房间安全性依赖 anyRTC 服务端。
5. **SDK Key 全部硬编码**：anyRTC appId、讯飞 appid、高德 key 明文在源码。
6. **申请了系统级权限**：悬浮窗等系统级权限，权限范围偏大。

## 功能

7. **心跳频率 bug**：`WebSocketManager` 注释写 30s，实际间隔 2 小时——保活几乎失效，长连接易被中间网络设备断开。
8. **避障模型未接**：`ObstacleDetectionManager` 用模拟数据，且避障服务内网不可达（见 #2）。
9. **`MessageFragment` 全 mock 数据**：志愿者消息 Tab 无真实接口。
10. **`SegmentedTTSManager` 空文件**：占位类，未实现。
11. **紧急求助无超时机制**：发起后家属无响应时无超时兜底。

## 架构

12. **`CameraPreviewManager` 用 Camera2**：依赖声明提及 CameraX，**实际用 Camera2**，与任务/依赖描述不符。
13. **`NavigationManager` 未集成高德 SDK**：仅 URI 调起高德地图 App，非 SDK 集成。
14. **讯飞 appid 重复两处**：硬编码于两个文件，改一处易漏。
15. **token 无统一拦截器**：各请求手动加 Header，易遗漏。
16. **`VideoCallManager` 信令处理不完整**：WS type 3/4/5 落入 default 分支（紧急求助通知/取消/跳转在视频侧未处理）。

## 已修复的历史坑（保留备查）

- **社区页 NPE（已修）**：`VolunteerUserInfoManager` 的 `apiService` 静态变量与 `SharedPrefsUtil` 在使用前未初始化，叠加 `CommunityFragment` 复杂视图切换时 `getView()` 为 null → 进入社区页崩溃。修法：`getApiService()` 懒加载 + `SharedPrefsUtil.init(context)` 前置 + 简化 Fragment 视图切换（默认返回 noJoinView，按 `communityId` 异步切换）。详见 [features.md](features.md) 社区模块。
