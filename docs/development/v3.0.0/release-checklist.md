# v3.0.0 发布检查清单

> 本文件属 `development/v3.0.0/`（单体化改造·进行中）。打 **v3.0.0** tag 前逐项核对。版本指针 [../current.md](../current.md)。

## 文档体系

- [ ] `product/v3.0.0/` 4 文件定稿（单体化契约变更补齐 FR/AC）
- [ ] `development/v3.0.0/` 三件套定稿（task-breakdown / testing-strategy / 本文件）
- [ ] 内容边界自检零命中：`grep -rniE 'src/main|com/swj|\.java:|@DubboService|@DubboReference|java -jar|DUBBO_IP_TO_REGISTRY|mvn |\.yml' docs/product/`
- [ ] FR/AC 去重自检零命中：`grep -rniE 'FR-[A-Z]+-[0-9]+|AC-[A-Z]+-[0-9]+' docs/ | grep -v 'docs/product/'`
- [ ] 全仓相对链接可达
- [ ] ROADMAP v3.0.0 项与 task-breakdown 一致

## 版本号三处同步

- [ ] `product/current.md` 写 `v3.0.0`
- [ ] `development/current.md` 写 `v3.0.0`
- [ ] `docs/README.md` 当前工作版本段写 `v3.0.0`

## 单体化交付（单体重写两阶段，详见 [task-breakdown](task-breakdown.md)）

- [ ] 阶段 1：业务模块统一 SB 3.4.5/Java21（jakarta 迁移 + MP 3.5.9 + knife4j openapi3 + nacos 2023.0.1.0），逐模块 `contextLoads` 全绿
- [ ] 阶段 2：合并单体（bootstrap 唯一入口 + 删 gateway + Dubbo→本地 + 路径内化 + 4 拦截器收敛 + ai 副本/重复类清理）
- [ ] 阶段 2：合库 `shiwujie`（mysqldump 旧 4 库导入 + 单 datasource + call snake→camel + 跨库写单事务）
- [ ] 契约回归零变更：HTTP 路径 + WS `/api/ws/call` 12 信令 + 业务码 + 返回字段（前端 App/Web 不改可对接，详见 [testing-strategy](testing-strategy.md) 契约保护铁律）
- [ ] 🔴 安全加固全部清零（ai 后门 / 删改权限 / WS 鉴权 / 密码哈希 / 弱密钥 / 前端 TLS）——独立项
- [ ] App 高德 SDK 集成 ——独立项
- [ ] Docker 化 + 压测 + AiLogs 索引调优 ——独立项

## 合库执行步骤（mysqldump 旧 4 库 → 单库 `shiwujie`）

> 旧 4 库 `shiwujieuser` / `shiwujiecall` / `shiwujiecommunity` / `shiwujieai` 共 13 表（表名 PascalCase，无冲突）合并导入空库 `shiwujie`（`47.112.114.139:3306`，user=`shiwujie`，密码见 [`shiwujie-bootstrap/src/main/resources/application.yml`](../../../shiwujie-backend/shiwujie-bootstrap/src/main/resources/application.yml)）。由具备远程访问权限的连接执行（应用账号默认仅授权服务端本机/白名单 IP）。

```bash
# 0) 凭据（PWD 取自 bootstrap application.yml 的 ${MYSQL_PASSWORD:默认}）
export MPWD='<'密码'>'

# 1) 逐库导出（仅表级，不含 CREATE DATABASE/USE；--single-transaction 一致性快照）
for db in shiwujieuser shiwujiecall shiwujiecommunity shiwujieai; do
  mysqldump -h47.112.114.139 -P3306 -ushiwujie -p"$MPWD" \
    --single-transaction --default-character-set=utf8mb4 \
    "$db" > "${db}.sql"
done

# 2) 导入空库 shiwujie（临时关 FK 检查，避免表创建/外键顺序依赖；mysqldump 自带 DROP+CREATE+INSERT，可重跑）
for db in shiwujieuser shiwujiecall shiwujiecommunity shiwujieai; do
  ( echo "SET FOREIGN_KEY_CHECKS=0;"; cat "${db}.sql"; echo "SET FOREIGN_KEY_CHECKS=1;" ) \
    | mysql -h47.112.114.139 -P3306 -ushiwujie -p"$MPWD" shiwujie
done

# 3) 校验：目标库表数应 = 13（user4 + call2 + community6 + ai1）
mysql -h47.112.114.139 -P3306 -ushiwujie -p"$MPWD" shiwujie -e \
  "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='shiwujie';"
```

> 注意：表名大小写由服务端 `lower_case_table_names` 决定——旧库既已按现名工作，导入后行为一致，无需额外处理。导入完成后保留 4 份 `.sql` 作为回滚源，直到 2.7 验证通过。

## 部署回归

- [ ] 单体统一端口 / 单库 / 统一 Spring Boot 版本 `contextLoads` 全绿
- [ ] 凭据 `${ENV:default}` 占位符齐全 + prod 公网 IP `47.112.114.139` 硬编码生效
- [ ] 手动联调：App + Web + 单体后端起栈，核心链路（登录 / 视频求助 / AI 对话 / 社区审核）跑通

## 打 tag 动作

- [ ] `git tag v3.0.0`
- [ ] CHANGELOG `## v3.0.0（进行中，未发布）` → `## v3.0.0 - <tag 日期>`
- [ ] 新建下一版目录，`current.md` ×2 + README 改指下一版
