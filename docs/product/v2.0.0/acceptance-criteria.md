# 验收标准（v2.0.0 · 已封版 2026-07-11）

> 全部模块的验收标准合并去重，编号 `AC-<MODULE>-<NN>`，与 [functional-requirements.md](functional-requirements.md) 的 `FR-<MODULE>-<NN>` 一一对应。⚠ 标注当前不满足项。本文件属 `product/v2.0.0/`（已封版 2026-07-11）。

## 网关与共享层

### AC-GATEWAY

| ID | 验收点 |
|---|---|
| AC-GATEWAY-01 | 请求 `http://<网关>:8100/api/user/...` 经路由可达 user 服务 |
| AC-GATEWAY-02 | 建立 WebSocket 到 `/api/ws/call` 握手成功并保持长连接 |
| AC-GATEWAY-03 | 访问 `/doc.html` 展示聚合的 user/call/community 接口文档 |
| AC-GATEWAY-04 | 停掉某实例后负载均衡不再路由到它 |
| AC-GATEWAY-05 | prod 环境启动后注册中心控制台注册 IP=服务器公网 IP |

### AC-MODEL

| ID | 验收点 |
|---|---|
| AC-MODEL-01 | 契约层可独立构建、被其余模块依赖 |
| AC-MODEL-02 | `Inner*Service` 签名变更后，编译期所有提供者/消费者同步报错 |
| AC-MODEL-03 | 各类异常统一转为统一返回结构 |

## 鉴权

### AC-AUTH

| ID | 验收点 |
|---|---|
| AC-AUTH-01 | 登录返回的 token 可用于受保护接口（成功 + 业务数据） |
| AC-AUTH-02 | 删除 Redis 对应 key 后，同 token 再请求返回 `40010 NOT_LOGIN` |
| AC-AUTH-03 | 篡改 token 签名 → `NOT_LOGIN`（HS256 校验生效） |
| AC-AUTH-04 | JWT 过期但 Redis key 仍存在时，请求仍成功（以 Redis TTL 为准） |
| AC-AUTH-05 | ⚠ **当前不满足**——合法请求后 Redis token key 的 TTL 未被刷新（续期 key 拼接 bug，见 [architecture/auth.md](../../architecture/auth.md)） |

## 用户与家庭

### AC-USER

| ID | 验收点 |
|---|---|
| AC-USER-01 | 合法手机号：未注册→自动建号返回 token；已注册→直接返回 token |
| AC-USER-02 | 密码不符合「字母+数字」→报错 |
| AC-USER-03 | 同一手机号 Blind/Volunteer 只能一处→第二处报「已被注册」 |
| AC-USER-04 | 再次登录覆盖旧 token，旧 token 立即失效（单点） |
| AC-USER-05 | 错误/无 Authorization → `NOT_LOGIN` |
| AC-USER-06 | 非本人改他人资料 →「操作用户错误」 |
| AC-USER-07 | 身份证/残疾证存库为 MD5；返回仅 `isIdCard`/`isDisabilityCard` 布尔 |
| AC-USER-08 | 仅志愿者可创建家庭 |
| AC-USER-09 | 家主不能加入自己的家庭 |
| AC-USER-10 | 审核通过后申请人 familyId 更新 |
| AC-USER-11 | 非家主审核报错 |
| AC-USER-12 | 删家庭级联清成员 |
| AC-USER-13 | 社区管理员（非员工）可踢人 |
| AC-USER-14 | `Inner*Service` 经注册中心注册，消费者可调用 |

## 实时通信与求助

### AC-CALL

| ID | 验收点 |
|---|---|
| AC-CALL-01 | 志愿者重复加入返回「您已经在匹配中了」 |
| AC-CALL-02 | 无空闲志愿者返回「没有空闲的志愿者」 |
| AC-CALL-03 | 匹配成功志愿者端收到 type1（含盲人手机号/频道号） |
| AC-CALL-04 | 未加入家庭发起紧急求助返回「您没有加入家庭,无法紧急求助」 |
| AC-CALL-05 | 心跳超时/断连后会话表清理对应手机号 |
| AC-CALL-06 | 挂断时长=round((结束时间 − 响应时间) / 分钟) |
| AC-CALL-07 | ai 启动后发现 `InnerSocket` 提供方 |
| AC-CALL-08 | dev/prod 环境切换注册 IP |
| AC-CALL-09 | `/call/*` 接口需有效 Bearer token |

## 社区治理

### AC-COMMUNITY

| ID | 验收点 |
|---|---|
| AC-COMMUNITY-01 | Register 全填 → 返回 VO + token；DB 出现三级社区，默认标识 0/0/1，状态=已通过 |
| AC-COMMUNITY-02 | Login 密码错误→参数错误；非管理员→无权限 |
| AC-COMMUNITY-03 | 删社区后成员 communityId 清空、管理记录删除 |
| AC-COMMUNITY-04 | 审核通过后申请人 communityId 回写；并发同手机号被串行 |
| AC-COMMUNITY-05 | 未加入社区的视障发帖 → 无权限 |
| AC-COMMUNITY-06 | 同活动同人重复报名 →「活动已报名」 |
| AC-COMMUNITY-07 | 求助帖状态仅接受四个中文名，非法 → 参数错误 |
| AC-COMMUNITY-08 | 无/错 token → `NOT_LOGIN` |
| AC-COMMUNITY-09 | URL 含 Login/Register 放行 |
| AC-COMMUNITY-10 | prod 注册 IP=服务器，dev=本机 |

## AI 助手

### AC-AI

| ID | 验收点 |
|---|---|
| AC-AI-01 | 文本对话返回 SSE 流 |
| AC-AI-02 | 图片对话返回 SSE 流且 100 字内 |
| AC-AI-03 | 上下文按盲人 ID 隔离 |
| AC-AI-04 | 文本上下文窗口=10 轮 |
| AC-AI-05 | 图片上下文窗口=6 轮 |
| AC-AI-06 | 记忆存储键命名统一 |
| AC-AI-07 | 工具调用走约定 JSON（非原生 function-calling） |
| AC-AI-08 | 工具索引覆盖 1-9 且无 community |
| AC-AI-09 | 非末条历史图片在上下文中被脱敏 |
| AC-AI-10 | 末条图片以占位符替代、按需回放 |
| AC-AI-11 | Dubbo 消费仅 family/socket/blind（3 处） |
| AC-AI-12 | 模块无 RPC 暴露 |
| AC-AI-13 | 记录每次调用的 token 消耗 |
| AC-AI-14 | 默认用户兜底（调试便利，生产风险） |
| AC-AI-15 | 诚实缺口：无压测 / 无索引优化 / 无 Docker |

## Android 客户端

### AC-APP

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
| AC-APP-09~18 | 社区 / 家庭 / 导航 / 跳转 / TTS / 资料 / 消息 / 个人中心 / 权限各功能可达 |

## Web 管理后台

### AC-WEB

| ID | 验收点 |
|---|---|
| AC-WEB-01 | `npm run dev` 自动开 `http://localhost:9090`，未登录重定向 `/login` |
| AC-WEB-02 | 登录后 token 存 localStorage，userCommunities 存 communityId 字符串数组 |
| AC-WEB-03 | 刷新后侧边栏用户名仍正确（依赖 init → checkLogin） |
| AC-WEB-04 | token 失效（code 40010/HTTP 401）→ 清登录态跳 `/login`，提示「登录已过期」 |
| AC-WEB-05 | 盲人列表分页正确，blindId 在表格/详情/踢出中始终为字符串 |
| AC-WEB-06 | 仅 注册人/管理员 角色显示「踢出/通过拒绝」按钮 |
| AC-WEB-07 | 任意接口返回 19 位 ID → 三层字符串转换防护：(a) 响应拦截器预替换关键字段；(b) 解析时检查安全整数上限；(c) 递归处理 |
| AC-WEB-08 | 社区入驻注册成功自动登录跳 `/` |
| AC-WEB-09 | 退出登录清空 token/userCommunities/userInfo，跳 `/login` |
| AC-WEB-10 | 活动报名点查看跳详情页，正确加载活动 + 报名分页 |
