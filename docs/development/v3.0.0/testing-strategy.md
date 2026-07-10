# v3.0.0 测试策略

> 本文件属 `development/v3.0.0/`（单体化改造·进行中）。测试基线**继承 v2.0.0**（见 [../v2.0.0/testing-strategy.md](../v2.0.0/testing-strategy.md)，已封版 2026-07-11），本版本补充单体化特有的验证点。版本指针 [../current.md](../current.md)。

## 继承基线

v2.0.0 的测试现状 / 环境表 / 验证点 / 缺口继续适用，见 [../v2.0.0/testing-strategy.md](../v2.0.0/testing-strategy.md)。

## v3.0.0 新增验证点（随单体化落地滚动补充）

- 单端口入口回归：原经 gateway 分发到 user/call/community/ai 的链路，合并为单体统一入口后功能不变。
- 跨模块调用改本地调用回归：原 Dubbo Inner 调用改为同进程方法调用，幂等性与异常透传一致。
- 统一 Spring Boot 版本后 `contextLoads` 全绿（消除 SB2.7/Java17 与 SB3.4.5/Java21 双轨）。
- 4 处 `LoginCheckInterceptor` 收敛为 1 处后，鉴权行为（含 v2.0.0 修复的滑动会话续期）不回归。
