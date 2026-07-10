# Vue3 管理后台

> 面向**社区管理员/注册人**的 Web 后台。Vue3 + Ant Design Vue 4 + Pinia，与 App（盲人/志愿者端）互补。本文为 development 细化（结构/路由/页面实现度/API/数据流/配置）；用户可见契约（FR-WEB / AC-WEB）见 [product/current.md](../../../docs/product/current.md)。

## 模块定位

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-frontend/web/src/`（**排除 node_modules**） |
| 包名 | `shiwujie-web-admin` v2.0.0（"视无界社区管理端"） |
| 技术栈 | Vue **3.3** + Vue Router 4 + Pinia 2 + Ant Design Vue **4** + Axios 1.4 + Vite **4** |
| dev 端口 | 9090，代理 `/api` → 网关 8100 |
| 鉴权主体 | 社区志愿者（`VolunteerVO`，带 communityManager 角色：注册人/管理员/空） |

## 目录结构

```
web/src/
├── main.js                    # Pinia + Router + Antd 全量注册
├── App.vue                    # 仅 <router-view/>
├── router/index.js            # 路由表 + beforeEach 守卫
├── api/{base, user, community, activity, index}.js
├── stores/{auth.js ★, user.js △(残留未引用)}
├── models/{base, volunteer, community, blind}.js
├── utils/{request.js ★, bigIntUtils, debugUtils}.js
└── views/
    ├── login/Login.vue
    ├── layout/MainLayout.vue              # 侧边栏菜单 + 顶部栏
    ├── dashboard/Dashboard.vue            # ★ 占位（统计写死 0）
    ├── statistics/{CommunityStats, ActivityStats}.vue   # ★ 占位（a-empty）
    ├── community/{CommunityList, CommunityEdit, CommunityReview}.vue
    ├── activity/{ActivityList, ActivitySign, ActivitySignDetail}.vue
    ├── helppost/HelpPostList.vue
    └── user/{BlindList, VolunteerList, EmployeeList}.vue
```

## 路由与菜单

历史模式 `createWebHistory`，`@`→`src`。布局路由下嵌套各业务页（community-list / community-review / activity-list / activity-sign / helppost-list / volunteer-list / employee-list / blind-list / community-stats / activity-stats）。`beforeEach` 守卫读 `localStorage.token`，无则跳 `/login`。

侧边栏菜单（MainLayout.vue）：仪表板 / 社区管理（列表·审核）/ 活动管理（列表·报名）/ 求助管理 / 用户管理（志愿者·员工·视障）/ 数据统计（社区·活动）/ 退出登录。

## 页面实现度

| 页面 | 实现度 | 说明 |
|---|---|---|
| Login | 完整 | 登录 + 社区入驻注册双表单 |
| CommunityList | 完整（含 bug） | 树形社区列表、子社区分页、删除 |
| CommunityEdit / CommunityReview | 完整 | 编辑 / 加入审核通过拒绝 |
| ActivityList / ActivitySign / ActivitySignDetail | 完整 | 活动 CRUD / 报名管理 / 审核 |
| HelpPostList | 部分 | 列表 + 详情（删除已注释禁用） |
| BlindList | 部分 | 列表 + 踢出（添加/编辑/求助记录"开发中"） |
| VolunteerList / EmployeeList | 完整 | 列表 + 设为管理员 / 角色更新 |
| **Dashboard** | **占位** | 4 卡片数字写死 0，趋势写死 |
| **CommunityStats / ActivityStats** | **占位** | 仅 `<a-empty/>` |

## API 层与鉴权

| 文件 | 职责 |
|---|---|
| `utils/request.js` ★ | axios 实例：baseURL `/api`、timeout 30s、禁用自动 JSON.parse、**大数字 ID 三层字符串转换防护** |
| `api/base.js` | BaseApiService（get/post/put/delete/upload） |
| `api/community.js` | 社区/求助帖接口 + 登录注册 |
| `api/activity.js` | 活动接口（不继承 Base，直接用 http） |
| `api/user.js` | 用户接口（仅 getBlindList / removeBlindFromCommunity 被 BlindList 用到） |
| `stores/auth.js` ★ | 鉴权 store：token / volunteer / isLoggedIn / login / checkLogin / clearLoginInfo |

**业务码约定**：`code===1` 成功返回 `data.data`；`40010`/HTTP 401 → 清登录态跳 `/login`；`40000` → 重新选身份。

**后端端点**（经 vite proxy `/api`）：社区登录 `/community/community/Login`、注册 `/Register`、登录校验 `/login/check`；社区/求助帖/活动/用户各 `/community/*` 与 `/user/*` 端点（详见后端各模块文档）。

## 关键数据流

### 登录

```
Login.vue handleLogin → authStore.login(phone,password)
  → communityApi.login → POST /api/community/community/Login
  → request.js 响应拦截器：正则把 19 位 ID 包成字符串 → JSON.parse(reviver) → processBigNumbers
  → code===1 返回 data.data → new CommunityLoginModel(response)
  → authStore.token/volunteer/isLoggedIn 赋值 + localStorage.setItem('token', token)
  → addSafeIdToList('userCommunities', communityId) → router.push('/')
```

刷新：`beforeEach` 读 token → `authStore.init()` → `checkLogin()` → `GET /login/check` 恢复 volunteer。

### 查看盲人列表

```
BlindList.vue onMounted → fetchBlindList
  → userApi.getBlindList({current, pageSize, communityId}) → GET /api/user/blind/community/blinds/vo
  → 拦截器大数字处理 → records.map(b => new BlindModel(b))（blindId/communityId 强制 String）
  → 表格展示；统计卡片由 computed 实时算
```

### 查看统计图表

**当前为空实现**：Dashboard/CommunityStats/ActivityStats 全静态占位，无图表库（echarts/antv 不在 package.json），`activityApi.getActivityStats` 接口已定义但无 view 调用。

### 社区加入审核（CommunityReview）

合并自原 `web/README_Community_Review.md`（已并入此处，原件移除）。

- **接口**：`getCommunityJoinReviewList()` → `GET /communityjoinreview/get/list/vo`；`updateCommunityJoinReview(data)` → `PUT /communityjoinreview/update`。
- **列表展示**：区分志愿者/盲人申请（标签），显示申请人名+ID、申请社区名+ID、格式化申请时间，操作列 通过/拒绝。
- **流程**：页面加载自动取列表 → 为每条记录补用户/社区信息 → 点 通过/拒绝 → 确认对话框 → 调 `update` → 自动刷新。
- **健壮性**：操作中加载态防重复；网络/空数据/操作失败均有提示；大数字 ID 经 `request.js` 三层防护转字符串（见 [known-issues.md](known-issues.md) #10 数据结构脆弱的兜底）。

> 注：仅社区管理员（注册人/管理员角色）可见审核入口；审核不可撤销。

## 配置要点

- vite proxy `/api` → `http://47.112.114.139:8100`（**硬编码公网 IP**），`changeOrigin:true`，保留 `/api` 前缀。
- 端口 9090（`strictPort:true`）。
- Login.vue 调试 log 残留旧地址 `http://43.139.38.62:8081`（仅 console，未用）。

---

> **延伸阅读**
>
> - 用户可见契约（FR-WEB / AC-WEB）：[../../../docs/product/v2.1.0/functional-requirements.md](../../../docs/product/v2.1.0/functional-requirements.md) · [../../../docs/product/v2.1.0/acceptance-criteria.md](../../../docs/product/v2.1.0/acceptance-criteria.md)
> - 缺陷与技术债（★ 运行时错误 / 统计缺失 / 调试日志等 11 项）：[known-issues.md](known-issues.md)
> - 开发命令（`npm install` / `npm run dev` / `npm run build`）：见 [README.md](README.md)
