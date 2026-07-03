# Android 功能实现备注

>development 细化：合并自原 `app/shiwujie/README_*.md` 与 `应用列表管理器实现说明.md`（已并入此处，原件移除）。记录若干 Fragment / 工具类的实现细节、文件清单与数据流，供二次开发参照。功能契约见 [product/current/functional-requirements.md](../../../docs/product/current/functional-requirements.md) FR-APP；缺陷见 [known-issues.md](known-issues.md)。

## 盲人端社区模块

`blind/ui/community/CommunityFragment` 按用户 `communityId` 状态切换两套视图：未加入（加入引导 + 搜索社区 ID）与已加入（社区信息 + 求助/活动/成员入口）。

- **布局**：`fragment_blind_community_no_join.xml`（未加入）/ `fragment_blind_community_joined.xml`（已加入）。
- **请求模型**：`BlindCommunityJoinRequest`（社区 ID，须纯数字）。
- **接口**：加入 `POST /api/user/blind/community/join`；取社区信息 `GET /api/community/community/get/id/vo`。
- **状态管理**：`SharedPrefsUtil` 管登录态、`UserInfoManager` 取用户信息（按 `communityId` 是否 > 0 切视图）、`RetrofitClient` 发请求。

> 无障碍侧重：大按钮、清晰文字标签、蓝色主题；网络/输入/登录态错误均有 Toast 反馈。

## 盲人端社区活动

`CommunityFragment` 内的活动列表区，集成下拉刷新、分页（每页 10）、本地关键词过滤、详情对话框与报名。

- **布局**：`fragment_blind_community_main.xml`（含活动列表区）、`dialog_activity_detail.xml`（详情对话框）、`item_activity.xml`（列表项）。
- **核心类**：`CommunityFragment`（主逻辑）、`ActivityAdapter`（列表适配器）、`ActivityVO`（数据模型）、`ActivitySignAddRequest`（报名请求）。
- **接口**：`getActivityList()`（列表，支持分页）、`addActivitySign()`（报名）。
- **报名前置校验**：活动状态（已结束/已取消不可报名）+ 用户登录态与信息。
- **交互**：详情对话框 → 确认报名 → 调接口 → 结果 Toast；`SwipeRefreshLayout` 下拉刷新。

## 应用列表管理器

`common/utils/AppListManager`：获取并缓存本机已安装应用列表，为「跳转他应用 / 需求复述」AI 工具提供应用名→包名映射基础。

- **主要方法**：`loadInstalledApps()`（异步加载）、`isAppInstalled(name)`、`launchApp(name)`、`getPackageName(name)`、`getAppStoreLink(packageName)`。
- **匹配**：应用名精确 / 小写 / 模糊三级匹配。
- **集成**：`BlindHomeActivity.onCreate` 初始化，独立 `ExecutorService`（与 WS/视频等隔离），`onDestroy` 清理线程池与缓存。
- **容错**：加载失败不影响其它功能；`testAppListManager()` 供 Logcat 验证。
- **权限**：`READ_PHONE_STATE` 等（已在 `AndroidManifest.xml` 配置）。

> 演进备注：盲人社区早期仅实现「加入」引导（求助/活动/成员入口当时为占位），后续补齐活动列表与报名（见上节）。求助帖、成员列表等其余入口的实现度以 FR-APP 状态列为准。
