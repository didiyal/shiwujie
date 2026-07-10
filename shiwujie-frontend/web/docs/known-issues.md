# Web 管理后台已知问题与技术债

> Web 后台技术债 / 缺陷登记。development 层（允许源码引用 `file:line`）。★ = 影响功能正确性。统计占位类缺口同步进 [ROADMAP.md](../../../docs/ROADMAP.md) 待实现「能力补全」。功能契约状态见 [product/v2.1.0/functional-requirements.md](../../../docs/product/v2.1.0/functional-requirements.md) FR-WEB。

1. **★ 子社区加载方法名不一致（运行时错误）**：`CommunityList.vue:340` 调 `getSubCommunityList(...)`，但 `api/community.js:86` 定义的是 `getSubCommunities(...)` → 点击展开子社区抛 `TypeError: ... is not a function`。
2. **★ 统计模块完全缺失**：Dashboard / CommunityStats / ActivityStats 全占位，无图表库（echarts/antv 不在 `package.json`），后端 `/activity/stats` 未对接。
3. **★ 生产环境调试日志未剥离**：`request.js` / `auth.js` / 各 view 遍布 `console.log('🔍 ...')`（含 URL、token 前 20 字符、请求响应明文）。建议按 `import.meta.env.DEV` 守卫。
4. **后端地址不一致**：vite proxy `47.112.114.139:8100` vs `Login.vue` 调试 log `43.139.38.62:8081`（旧，仅 console）。
5. **BaseResponse 业务码自相矛盾**：`models/base.js:18` `isSuccess` 判 `code===0`，拦截器实际判 `code===1`（`BaseResponse` 类未被运行时使用）。
6. **残留 `stores/user.js` 未被引用**：实际鉴权走 `stores/auth.js`，`user.js` 是早期遗留。
7. **鉴权依赖 localStorage 直读**：router / `request.js` 直接 `localStorage.getItem('token')` 绕过 Pinia，可能与 `authStore` 短时不一致。
8. **社区 ID 来源依赖 localStorage 列表首项**：`BlindList` / `HelpPostList` 从 `userCommunities[0]` 取 communityId，多社区管理员只能操作第一个（无法切换）。
9. **`HelpPostList` 列定义与数据不匹配**：定义了 `userName` 列但后端 VO 未必返回，「发布人」列可能恒空。
10. **`CommunityReview` 数据结构脆弱**：对返回做三段兜底，`userName` / `communityName` 用 `${id}` 拼接（后端 VO 未携带）。
11. **依赖版本略旧 / 冗余**：Vite 4 / Vue 3.3 / Axios 1.4；`terser` 在 devDependencies 但未启用。
