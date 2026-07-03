# Web 管理后台文档（development 细化）

> Vue3 社区管理后台技术实现文档。**不含用户契约**——FR-WEB / AC-WEB 在根 [product/current/](../../../docs/product/current/)；跨切面（鉴权/路由/选型）在根 [architecture/](../../../docs/architecture/)；规范见 [docs/CONTRIBUTING.md](../../../docs/CONTRIBUTING.md)。

## 目录

| 文档 | 内容 |
|---|---|
| [vue-admin.md](vue-admin.md) | 模块定位、目录结构、路由与菜单、页面实现度、API 层与鉴权、关键数据流 |
| [known-issues.md](known-issues.md) | 缺陷与技术债（★ 运行时错误 / 统计缺失 / 调试日志等） |

## 开发命令

```bash
npm install        # 安装依赖
npm run dev        # 启动开发服务器（http://localhost:9090，代理 /api → 网关 8100）
npm run build      # 构建生产版本
```

> 技术栈：Vue 3.3 + Vue Router 4 + Pinia 2 + Ant Design Vue 4 + Axios 1.4 + Vite 4。详见 [vue-admin.md](vue-admin.md)。
