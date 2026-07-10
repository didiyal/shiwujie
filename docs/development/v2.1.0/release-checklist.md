# v2.1.0 发布检查清单

> 本文件属 `development/v2.1.0/`（二期·已封版 2026-07-11）。以下为封版前核对清单（tag `v2.1.0` 已于 2026-07-11 打）。版本指针 [../current.md](../current.md)。

## 文档体系

- [ ] `product/v2.1.0/` 4 文件内容定稿（overview / functional-requirements / acceptance-criteria / changelog）
- [ ] `development/v2.1.0/` 三件套定稿（task-breakdown / testing-strategy / 本文件）
- [ ] 内容边界自检零命中：`grep -rniE 'src/main|com/swj|\.java:|@DubboService|@DubboReference|java -jar|DUBBO_IP_TO_REGISTRY|mvn |\.yml' docs/product/`
- [ ] FR/AC 去重自检零命中：`grep -rniE 'FR-[A-Z]+-[0-9]+|AC-[A-Z]+-[0-9]+' docs/ | grep -v 'docs/product/'`
- [ ] 全仓相对链接可达（`git mv` / 改名后逐一核）
- [ ] ROADMAP 待实现项与 task-breakdown 收尾任务一致

## 版本号三处同步

- [ ] `product/current.md` 写 `v2.1.0`
- [ ] `development/current.md` 写 `v2.1.0`
- [ ] `docs/README.md` 当前工作版本段写 `v2.1.0`

## 🔴 安全加固（逐条，详见 [task-breakdown.md](task-breakdown.md)）

- [ ] 关闭 ai 默认用户兜底
- [ ] 恢复 Helppost / Community 删除/更新权限检查
- [ ] `/ws/call` 与社区/家庭审核补鉴权
- [ ] 密码 MD5 → BCrypt/Argon2；TOKEN_SECRETKEY 走环境变量
- [ ] 前端 TLS；移除硬编码 SDK Key / token；release 关调试日志
- [x] 修复续期 key 拼接 bug（2026-07-10）

## 能力补全 · 工程化（详见 [task-breakdown.md](task-breakdown.md)）

- [ ] 真实避障模型接入
- [ ] App 高德 SDK 集成
- [ ] Web 统计模块落地 + 多社区切换
- [ ] Activitysign 签到/签退
- [ ] 分布式事务 / 网关统一鉴权 / Knife4j 聚合 ai / 压测 / Docker（按 ROADMAP 优先级，可延后至 v2.x）

## 部署回归

- [ ] Dubbo provider 端口 21200–21500（已迁出 Hyper-V/WSL 保留段）
- [ ] 凭据 `${ENV:default}` 占位符齐全（MYSQL/REDIS/NACOS/DASHSCOPE/SEARCH）
- [ ] prod profile 公网 IP `47.112.114.139` 硬编码生效
- [ ] `contextLoads` 在 JDK17（业务模块）/ JDK21（ai）全绿
- [ ] 手动联调：App + Web + 全后端起栈，核心链路（登录 / 视频求助 / AI 对话 / 社区审核）跑通

## 打 tag 动作（已于 2026-07-11 执行）

- [x] `git tag v2.1.0`
- [x] CHANGELOG `## v2.1.0（进行中，未发布）` → `## v2.1.0 - 2026-07-11`
- [x] 新建 `product/v3.0.0/` + `development/v3.0.0/`（立项骨架），`current.md` ×2 + README 改指 v3.0.0
