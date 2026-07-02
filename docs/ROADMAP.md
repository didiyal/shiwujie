# 路线图（ROADMAP）

> 复选框清单（`[x]` 已完成 / `[ ]` 规划中 / `[?]` 待评估）。反映当前代码现状与已知缺口。

## 后端

```text
[x] 微服务脚手架（gateway + user + call + community + ai + model + common-web）
[x] Nacos 服务发现 + Dubbo 3.3.0 RPC
[x] JWT(Hutool HS256) + Redis 单点鉴权
[x] Spring Cloud Gateway 路由 + 轮询负载均衡
[x] 分库设计（user/call/community/ai 各自独立库）
[x] Knife4j 文档聚合（user/call/community）
[x] dev/prod 多环境 profile + 凭据占位符化
[x] 后端模块扁平化（6 模块移出 gateway 嵌套，同级摆放）
[x] 仓库卫生（.idea/日志移出跟踪 + 分层 .gitignore）
[ ] Knife4j 聚合 ai 服务（SB2/SB3 文档协议不兼容，未做）
[ ] 网关统一鉴权（当前鉴权下沉到各服务，4 处拦截器重复）
[ ] 引入分布式事务（Seata，当前靠 synchronized + 级联 update）
[ ] 抽取统一 LoginCheckInterceptor（被 SB2/SB3 割裂阻挡）
```

## user 模块

```text
[x] 三类用户（Blind/Volunteer/Family）注册登录
[x] JWT 签发 + Redis 单点
[x] 家庭创建/加入/审核/踢人/退出
[x] 加入/退出社区（Dubbo 调 community）
[x] Inner*Service 供 ai/call/community 消费
[ ] 修复续期 key 拼接 bug（漏 -blind-/-volunteer- 前缀，滑动会话失效）
[ ] 修复审核权限校验（未校验是否为家庭 creator）
[ ] 密码改 BCrypt/Argon2 + 加盐（当前 MD5 无 salt）
[ ] TOKEN_SECRETKEY 走环境变量（当前硬编码）
[ ] deleteBlind/deleteVolunteer 权限校验
```

## call 模块

```text
[x] WebSocket 信令中枢（@ServerEndpoint /ws/call，Spring WebSocket）
[x] 视频求助匹配（Redis 队列 FIFO）
[x] 紧急求助（家庭群发）
[x] InnerSocket 供 ai 触发 6 类前端推送（5001~5006）
[?] 删除冗余 netty-all 依赖（pom 残留，无代码引用）
[ ] sessionMap/sessionPhoneMap 改 ConcurrentHashMap（当前 HashMap，并发隐患）
[ ] 5xxx 失败分支向 ai 透传异常（当前静默丢失）
[ ] /ws/call 鉴权（当前 WS 绕过 JWT）
[ ] 5005 noticeJumpToUserUpdate 孤儿方法定论
```

## community 模块

```text
[x] 六大主体（社区/管理者/审核/求助帖/活动/报名）
[x] 省/市/街道三级社区自动补齐
[x] 成员加入审核
[x] 6 个 Inner 服务暴露
[ ] 权限模型按 roleId 落地（当前枚举存在但无运行时鉴权）
[ ] 修复 Helppost/Community 权限检查被注释（安全漏洞）
[ ] CommunitymanagerController 的 Dubbo 调用下沉到 Service 层
[ ] InnerCommunityjoinreviewService 去掉 QueryWrapper 跨 Dubbo 传递
[ ] Activitysign 补签到/签退接口 + 校验人数上限/活动状态
[ ] 清理无消费方的 Inner(Activity/Activitysign/Helppost)
```

## ai 模块

```text
[x] Spring AI Alibaba 多模型（qwen3-max 文本 + qwen3-vl-flash 图像）
[x] 自研 ChatMemory（Redis 精简 + MySQL 全量 + kryo）
[x] 工作流式工具路由（ToolChoiceApp，约定 JSON）
[x] 9 类工具（ToolChoiceCenter case 1-9）
[x] 图片瘦身上下文工程（占位符 + MySQL 回放）
[x] Web 搜索（searchapi.io + jsoup）
[x] Dubbo 消费（InnerFamily/InnerSocket/InnerBlind）
[?] 清理 pom 残留 paho（mqtt，已取消）
[?] 清理 MyRagAdvisor 半残留 Bean（RAG 已弃用）
[?] 清理提示词 community 残留文本（无对应工具）
[ ] 关闭默认用户兜底（生产后门）
[ ] TextApp Redis TTL 与注释对齐（10 分钟 vs 注释 5 天）
[ ] 压测 / Docker / AiLogs 索引调优
```

## 前端 App（Android）

```text
[x] 双角色（盲人/志愿者）登录与首页
[x] AI 多轮对话（SSE 流式 + 流式 TTS）
[x] AI 悬浮球 + 自由跳转
[x] 视频求助（anyRTC 双向）
[x] 紧急求助（悬浮窗 + 响铃）
[x] WebSocket 信令处理
[x] 社区/家庭/图片识别/导航/跳转应用
[ ] 修复心跳频率 bug（注释 30s 实际 2 小时）
[ ] 接入真实避障模型（当前模拟数据）
[ ] 明文 HTTP/WS 改 TLS
[ ] 避障服务自签证书改正规 CA
[ ] release 关闭调试日志
[ ] token 统一拦截器
[ ] CameraPreviewManager 与 CameraX 声明对齐
[ ] NavigationManager 集成高德 SDK（当前 URI 调起）
```

## 前端 Web（Vue3）

```text
[x] 社区管理员登录 + 路由守卫
[x] 社区/活动/求助帖/用户管理（列表 + 审核）
[x] 大数字 ID 精度保护（三层字符串转换）
[ ] 修复子社区加载方法名不一致（getSubCommunityList vs getSubCommunities）
[ ] 实现统计模块（Dashboard/CommunityStats/ActivityStats 当前全占位）
[ ] 引入图表库（echarts/antv）
[ ] 剥离生产调试日志（含 token 片段泄露）
[ ] 删除残留 stores/user.js
[ ] 多社区切换（当前只能操作 userCommunities[0]）
```

## 跨切面

```text
[?] 统一前后端 BaseResponse 业务码（web isSuccess 判 0，拦截器判 1）
[ ] 前后端地址统一（web vite proxy vs Login.vue 调试 log 旧地址）
[ ] 移除测试用户后门、硬编码 SDK Key、弱密码默认值
[ ] 压力测试 + 性能基线
```
