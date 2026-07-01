# Vue3 管理后台

> 面向**社区管理员/注册人**的 Web 后台。Vue3 + Ant Design Vue 4 + Pinia，与 App（盲人/志愿者端）互补。

## 模块定位

| 项 | 值 |
|---|---|
| 路径 | `shiwujie-frontend/web/src/`（**排除 node_modules**） |
| 包名 | `shiwujie-web-admin` v1.0.0（"视无界社区管理端"） |
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

## 配置要点

- vite proxy `/api` → `http://47.112.114.139:8100`（**硬编码公网 IP**），`changeOrigin:true`，保留 `/api` 前缀。
- 端口 9090（`strictPort:true`）。
- Login.vue 调试 log 残留旧地址 `http://43.139.38.62:8081`（仅 console，未用）。

## 功能需求（FR-WEB）

| ID | 需求 | 状态 |
|---|---|---|
| FR-WEB-01 | 社区管理员密码登录 | ✅ |
| FR-WEB-02 | 社区入驻注册（自动登录） | ✅ |
| FR-WEB-03 | 登录态持久化 + 路由守卫 | ✅ |
| FR-WEB-04 | 社区列表（子社区树/分页/删除） | ✅（子社区分页有 bug） |
| FR-WEB-05 | 社区信息编辑 | ✅ |
| FR-WEB-06 | 加入申请审核 | ✅ |
| FR-WEB-07 | 活动 CRUD | ✅ |
| FR-WEB-08 | 活动报名管理 + 审核 | ✅ |
| FR-WEB-09 | 求助帖列表 + 详情 | ✅（只读） |
| FR-WEB-10 | 视障列表 + 踢出 | ✅（添加/编辑"开发中"） |
| FR-WEB-11 | 志愿者列表 + 设为管理员 + 踢出 | ✅ |
| FR-WEB-12 | 员工列表 + 角色更新 | ✅ |
| FR-WEB-13 | 大数字（19 位雪花 ID）精度保护 | ✅（多层防护） |
| FR-WEB-14 | 仪表板统计概览 | ❌ **占位** |
| FR-WEB-15 | 社区统计图表 | ❌ **占位** |
| FR-WEB-16 | 活动统计图表 | ❌ **占位**（后端接口已就绪） |

## 验收标准（AC-WEB）

| ID | 验收点 |
|---|---|
| AC-WEB-01 | `npm run dev` 自动开 `http://localhost:9090`，未登录重定向 `/login` |
| AC-WEB-02 | 登录后 token 存 localStorage，userCommunities 存 communityId 字符串数组 |
| AC-WEB-03 | 刷新后侧边栏用户名仍正确（依赖 init → checkLogin） |
| AC-WEB-04 | token 失效（code 40010/HTTP 401）→ 清登录态跳 `/login`，提示"登录已过期" |
| AC-WEB-05 | 盲人列表分页正确，blindId 在表格/详情/踢出中始终为字符串 |
| AC-WEB-06 | 仅 注册人/管理员 角色显示"踢出/通过拒绝"按钮 |
| AC-WEB-07 | 任意接口返回 19 位 ID → request.js 三层防护转字符串：(a) 拦截器正则预替换 8 字段；(b) JSON.parse reviver 检查 MAX_SAFE_INTEGER；(c) processBigNumbers 递归 |
| AC-WEB-08 | 社区入驻注册成功自动登录跳 `/` |
| AC-WEB-09 | 退出登录清空 token/userCommunities/userInfo，跳 `/login` |
| AC-WEB-10 | ActivitySign 点查看报名跳详情页，正确加载活动 + 报名分页 |

## 已知问题

1. **★ 子社区加载方法名不一致（运行时错误）**：`CommunityList.vue:340` 调 `getSubCommunityList(...)`，但 `api/community.js:86` 定义的是 `getSubCommunities(...)` → 点击展开子社区抛 `TypeError: ... is not a function`。
2. **★ 统计模块完全缺失**：Dashboard/CommunityStats/ActivityStats 全占位，无图表库，后端 `/activity/stats` 未对接。
3. **★ 生产环境调试日志未剥离**：request.js/auth.js/各 view 遍布 `console.log('🔍 ...')`（含 URL、token 前 20 字符、请求响应明文）。建议按 `import.meta.env.DEV` 守卫。
4. **后端地址不一致**：vite proxy `47.112.114.139:8100` vs Login.vue 调试 log `43.139.38.62:8081`（旧）。
5. **BaseResponse 业务码自相矛盾**：`models/base.js:18` isSuccess 判 `code===0`，拦截器实际判 `code===1`（BaseResponse 类未被运行时使用）。
6. **残留 `stores/user.js` 未被引用**：实际鉴权走 `stores/auth.js`，user.js 是早期遗留。
7. **鉴权依赖 localStorage 直读**：router/request.js 直接 `localStorage.getItem('token')` 绕过 Pinia，可能与 authStore 短时不一致。
8. **社区 ID 来源依赖 localStorage 列表首项**：BlindList/HelpPostList 从 `userCommunities[0]` 取 communityId，多社区管理员只能操作第一个（无法切换）。
9. **HelpPostList 列定义与数据不匹配**：定义了 `userName` 列但后端 VO 未必返回，"发布人"列可能恒空。
10. **CommunityReview 数据结构脆弱**：对返回做三段兜底，userName/communityName 用 `${id}` 拼接（后端 VO 未携带）。
11. **依赖版本略旧**：Vite 4 / Vue 3.3 / Axios 1.4；`terser` 在 devDependencies 但未启用（冗余）。
