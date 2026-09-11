<template>
  <div class="changelog-page">
    <!-- ==================== PAGE HEADER ==================== -->
    <header class="page-header">
      <h1>更新<span>日志</span></h1>
      <p>追踪视无界每一次迭代与改进</p>
    </header>

    <!-- ==================== TIMELINE ==================== -->
    <div class="changelog">
      <div class="version" v-for="(v, idx) in versions" :key="v.version" :class="{ first: idx === 0 }">
        <div class="version-dot"></div>
        <div class="version-header">
          <span class="version-tag">{{ v.version }}</span>
          <span class="version-date">{{ v.date }}</span>
          <span class="version-badge" :class="v.badgeClass">{{ v.badge }}</span>
        </div>
        <div class="version-body">
          <h4>{{ v.title }}</h4>
          <p>{{ v.desc }}</p>

          <!-- Sections mode (with h5 sub-headings) -->
          <template v-if="v.sections">
            <div v-for="(sec, si) in v.sections" :key="si">
              <h5 class="version-section-title">{{ sec.heading }}</h5>
              <ul class="cl-items">
                <li class="cl-item" v-for="item in sec.items" :key="item.text">
                  <span class="tag" :class="item.tagClass">{{ item.tag }}</span>
                  <span>{{ item.text }}</span>
                </li>
              </ul>
            </div>
          </template>

          <!-- Flat items mode (backward compatible) -->
          <ul v-else class="cl-items">
            <li class="cl-item" v-for="item in v.items" :key="item.text">
              <span class="tag" :class="item.tagClass">{{ item.tag }}</span>
              <span>{{ item.text }}</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ChangelogPage',
  setup() {
    const versions = [
      {
        version: 'v1.3',
        date: '2026-09-12',
        badge: 'Latest',
        badgeClass: 'new',
        title: 'AI 全面升级：说话即操作，拍照即识别 🎙️',
        desc: 'App 核心体验重做：界面收敛为 AI 优先，语音能力全面加强——句子级流式播报不再久等，语音即可操控应用、拍照识别、一键求助；新增强制更新与新手引导。',
        sections: [
          {
            heading: '🤖 AI 助手',
            items: [
              { tag: '新增', tagClass: 'feature', text: 'AI 语音对话：说出需求即可联网搜索、打开应用（如"帮我打开微信"）、规划导航、操控软件内部功能' },
              { tag: '新增', tagClass: 'feature', text: 'AI 拍照识别闭环：说"帮我识别前面"自动拍照，识别结果可继续追问' },
              { tag: '优化', tagClass: 'improve', text: '句子级流式语音播报：AI 边生成边朗读，长回答不再久等，也不从头重读' },
              { tag: '新增', tagClass: 'feature', text: 'AI 页功能介绍弹窗：首次进入引导使用，可选"不再显示"' }
            ]
          },
          {
            heading: '📱 界面与交互',
            items: [
              { tag: '优化', tagClass: 'improve', text: '首页改版：进入软件直达 AI 页，底栏为 AI / 家庭 / 社区 / 我的' },
              { tag: '优化', tagClass: 'improve', text: 'AI 页功能键重排：对话、拍照识别、紧急求助（红）、志愿者求助，语音直达' },
              { tag: '新增', tagClass: 'feature', text: 'AI 悬浮球：退出应用后常驻屏幕，点击随时回到 AI；退到后台有语音提示' }
            ]
          },
          {
            heading: '🚨 求助与安全',
            items: [
              { tag: '优化', tagClass: 'improve', text: '紧急求助一键向家属发起视频通话求助，志愿者求助视频连线帮扶，均可在 AI 页语音触发' },
              { tag: '优化', tagClass: 'improve', text: '视频通话结束后回到 AI 页，流程闭环' }
            ]
          },
          {
            heading: '⚙️ 稳定性与安全',
            items: [
              { tag: '修复', tagClass: 'fix', text: '修复 AI 跳转其它应用后点悬浮球返回必闪退的问题（相机被抢占竞态）' },
              { tag: '新增', tagClass: 'feature', text: '应用内强制更新：新版本发布后自动弹窗引导下载安装' },
              { tag: '优化', tagClass: 'improve', text: '后端 AI 接口纳入统一登录鉴权，与业务接口同一套安全标准' }
            ]
          }
        ]
      },
      {
        version: 'v3.0.0',
        date: '2026-07',
        badge: 'Major',
        badgeClass: 'new',
        title: '官网全新上线 & 架构升级 🎉',
        desc: '视无界正式推出品牌官网；后端反思微服务过度设计，完成单体化改造（7 模块 → 2 模块），全面加固安全体系并补齐单元测试；App 修复多项稳定性与安全问题。',
        sections: [
          {
            heading: '🖥️ 官网 & Web 管理端',
            items: [
              { tag: '新增', tagClass: 'feature', text: '品牌官网首页上线，Apple 风格设计系统' },
              { tag: '新增', tagClass: 'feature', text: '官网导航：产品介绍、更新日志、GitHub、管理端入口' },
              { tag: '新增', tagClass: 'feature', text: 'App 软件下载功能，支持 Android APK 直接下载' },
              { tag: '优化', tagClass: 'improve', text: '管理端整体 UI 升级：暗色侧栏 + 毛玻璃顶栏，统一圆角/间距/字体体系' }
            ]
          },
          {
            heading: '⚙️ 后端：单体化改造',
            items: [
              { tag: '优化', tagClass: 'improve', text: '模块合并 7→2（model 契约层 + bootstrap 唯一应用），删除 gateway 模块' },
              { tag: '优化', tagClass: 'improve', text: '去 Spring Cloud / Nacos / Dubbo，统一 Spring Boot 3.4.5 / Java 21' },
              { tag: '优化', tagClass: 'improve', text: '4 分库合并为单库 shiwujie，跨库写操作升级为单事务保证一致性' },
              { tag: '优化', tagClass: 'improve', text: '合并 4 份重复 LoginCheckInterceptor / WebConfig 为公共各 1 份' },
              { tag: '优化', tagClass: 'improve', text: '对外 HTTP 路径 / WS 12 信令 / 状态码 / 返回字段零变更，前端无需改动' }
            ]
          },
          {
            heading: '🔒 安全加固',
            items: [
              { tag: '优化', tagClass: 'improve', text: '密码存储 MD5 → BCrypt（cost=10，盐内嵌），存量 MD5 首次登录时懒升级' },
              { tag: '修复', tagClass: 'fix', text: '恢复社区求助帖/活动/管理员增删改权限检查，此前任意登录用户可操作' },
              { tag: '修复', tagClass: 'fix', text: '改密接口补 ownership 校验 + 原密码必填校验，修复账户接管漏洞' },
              { tag: '修复', tagClass: 'fix', text: '修正 deleteCommunityManager 忽略请求体、恒删调用者自己的 bug' }
            ]
          },
          {
            heading: '🤖 AI 模块',
            items: [
              { tag: '优化', tagClass: 'improve', text: '文本模型 qwen3-max → qwen3.6-flash，路径改 OpenAI 兼容直连止血' },
              { tag: '新增', tagClass: 'feature', text: 'AI 集成冒烟测试，文本/图像模型回归可用性自动验证' }
            ]
          },
          {
            heading: '📱 App 修复（P0 + 批次 A/B）',
            items: [
              { tag: '修复', tagClass: 'fix', text: 'WebSocket 心跳实际间隔 2h → 30s，长连接不再被 NAT 静默掐断' },
              { tag: '修复', tagClass: 'fix', text: '视频通话监听器泄漏：onDestroy 误调 remove(null) 致 Activity 销毁后仍收回调' },
              { tag: '修复', tagClass: 'fix', text: '紧急求助「无法再次求助」死锁：新增 60s 家属无响应超时自动复位' },
              { tag: '修复', tagClass: 'fix', text: 'AI 页面 WebSocket 断线不重连：attemptReconnect 只打日志从不调 connect' },
              { tag: '修复', tagClass: 'fix', text: '紧急求助超时在通话进行中误触发——WebSocketManager 补发消息处理' },
              { tag: '修复', tagClass: 'fix', text: 'WS 重连 5 次用尽后永久失活：改为快速窗口（3s）+ 慢速持续重试（60s）' },
              { tag: '修复', tagClass: 'fix', text: 'VideoCallManager 回调跑在子线程，切回主线程防 UI 崩溃' },
              { tag: '优化', tagClass: 'improve', text: '统一 token 注入拦截器，补漏历史裸调；HTTP BODY 日志仅 DEBUG 打印' },
              { tag: '优化', tagClass: 'improve', text: 'Release 加固：开启 R8 混淆 + 资源压缩，allowBackup=false' },
              { tag: '优化', tagClass: 'improve', text: '前台通知按角色跳首页：盲人不再被带到志愿者首页' },
              { tag: '优化', tagClass: 'improve', text: '信令码常量化 + 固话真值表，散落魔数统一替换' }
            ]
          },
          {
            heading: '🧹 App 清理（批次 B）',
            items: [
              { tag: '移除', tagClass: 'fix', text: '删除未用类 15 个（Compose 模板残留、空 POJO、避障 mock 脚手架）' },
              { tag: '移除', tagClass: 'fix', text: '删除死资源 57 文件 + strings 12 项 + 2 数组（旧布局/菜单/图标）' },
              { tag: '移除', tagClass: 'fix', text: '删除死依赖：lifecycle livedata/viewmodel ktx + 整组 CameraX（实用 Camera2）' },
              { tag: '移除', tagClass: 'fix', text: '删除无效权限 READ_PRIVILEGED_PHONE_STATE（第三方拿不到）' }
            ]
          },
          {
            heading: '🧪 后端测试 & 审查',
            items: [
              { tag: '新增', tagClass: 'feature', text: '20 个单元测试类 / 286 例（纯 Mockito，mvn test 全绿），覆盖 user/community/call/utils' },
              { tag: '修复', tagClass: 'fix', text: '视频求助匹配队列序列化断裂：RedisUtils 双注入致 ClassCastException' },
              { tag: '修复', tagClass: 'fix', text: '匹配队列 TTL 单位错：硬编码 30 天 → 30 秒，僵尸志愿者不再滞留队头' },
              { tag: '修复', tagClass: 'fix', text: 'NPE 簇加固：removeVolunteerFromVideohelp 对 null 调 contains 必崩，统一判空' }
            ]
          }
        ]
      },
      {
        version: 'v2.1.0',
        date: '2026-07-11',
        badge: '封版',
        badgeClass: 'improve',
        title: '二期微服务封版 🏷️',
        desc: '二期微服务架构（Spring Cloud + Nacos + Dubbo）能力整合封版（tag v2.1.0），修复关键 bug，四层文档体系规范化落地。阶段 0–9 累积现状为起点，未完成收尾项平移至 v3.0.0。',
        sections: [
          {
            heading: '🐛 关键修复 & 文档',
            items: [
              { tag: '修复', tagClass: 'fix', text: 'Token 续期 key 漏身份前缀：续期/删用户拼的 Redis key 与登录/拦截器不一致，续期静默失效、活跃用户 90 天后被踢、删用户旧 token 残留。提取共享 redisKey 杜绝拼接分叉，对齐 90 天滑动会话' },
              { tag: '新增', tagClass: 'feature', text: '文档体系落地：product / architecture / development / ROADMAP+CHANGELOG 四层规范 + 版本分级模型（current 指针 + vX.Y.Z/ 目录）' }
            ]
          },
          {
            heading: '🔧 阶段 9 · 工程化收尾（约 2026-07）',
            items: [
              { tag: '新增', tagClass: 'feature', text: 'dev/prod 多环境 profile 拆分，凭据占位符化（MYSQL/REDIS/NACOS/DASHSCOPE 等走 ${ENV:default}）' },
              { tag: '新增', tagClass: 'feature', text: '引入 shiwujie-backend 父 pom：7 模块聚合 + 版本统一管理' },
              { tag: '优化', tagClass: 'improve', text: '后端模块扁平化：六模块从 gateway 子目录移至 backend 同级' },
              { tag: '修复', tagClass: 'fix', text: 'Dubbo provider 端口迁出 Hyper-V/WSL 保留段（50200→21200），解决 bind Address already in use' },
              { tag: '修复', tagClass: 'fix', text: '仓库卫生：.idea/、*.iml、logs/*.log 移出 git 跟踪' }
            ]
          },
          {
            heading: '🌐 阶段 8 · 分布式与生产化（约 2026-01）',
            items: [
              { tag: '优化', tagClass: 'improve', text: 'Call 模块 WebSocket 从 Netty 改为 Spring WebSocket（@ServerEndpoint + javax.websocket）' },
              { tag: '新增', tagClass: 'feature', text: 'Gateway 基于 Nacos 服务发现 + Spring Cloud LoadBalancer 轮询负载均衡' },
              { tag: '新增', tagClass: 'feature', text: '多服务器间 Nacos + Dubbo 通信配置，支持分布式部署' }
            ]
          }
        ]
      },
      {
        version: 'v2.0.0',
        date: '2025-11-12',
        badge: '里程碑',
        badgeClass: 'new',
        title: '二期初步稳定版 🚀',
        desc: '二期开发首个 semver 版本（tag v2.0.0），Spring AI Alibaba M6.1 时期的稳定里程碑。至此 AI 大脑、视频通话、社区治理三大核心能力体系建成。',
        sections: [
          {
            heading: '🤖 阶段 5–7 · AI 模块：从零到能用 → 能力跃升 → 引擎升级',
            items: [
              { tag: '新增', tagClass: 'feature', text: 'AI 模块初始化：Spring AI Alibaba 框架、Redis 存储 advisor、内存→阿里云向量数据库' },
              { tag: '新增', tagClass: 'feature', text: '多模型支持：deepseek + 千问（Qwen），文字与图像模型同时回答' },
              { tag: '新增', tagClass: 'feature', text: 'Dubbo Inner 服务接入 AI，工具调用执行真实设备动作（社区查询等）' },
              { tag: '新增', tagClass: 'feature', text: '流式输出 + 讯飞 TTS 自动语音播报 + 拍照识别' },
              { tag: '新增', tagClass: 'feature', text: 'Redis 持久化 + MySQL 异步存储（自研 ChatMemory 双写），调用速度提升约 50%' },
              { tag: '新增', tagClass: 'feature', text: 'AI 避障功能 + 高德导航（自动开启步行导航）+ 跳转外部应用' },
              { tag: '新增', tagClass: 'feature', text: '独立图片处理 App，图片追问不占 Redis 空间' },
              { tag: '优化', tagClass: 'improve', text: 'Spring AI Alibaba M6 → 1.0 引擎重构，动态配置双模型持久化策略，性能调优' },
              { tag: '优化', tagClass: 'improve', text: '提示词改用文档引入，图片追问功能，取消 Redis 图片上下文存储' },
              { tag: '修复', tagClass: 'fix', text: 'AI 悬浮窗在志愿者端误出现、APP 崩溃、紧急求助重复点击等多处 bug' },
              { tag: '移除', tagClass: 'fix', text: 'Mqtt 硬件 IoT 通道（硬件成本取消）、自研 ReAct Agent（改用代码工作流）、RAG 知识库（效果不及工作流）' }
            ]
          },
          {
            heading: '👥 阶段 4 · 社区治理（约 2025-07 ~ 08）',
            items: [
              { tag: '新增', tagClass: 'feature', text: '社区 CRUD + 加入审核 + 管理员设置，省市街道三级社区体系' },
              { tag: '新增', tagClass: 'feature', text: '求助帖发布与管理 + 活动发布/报名/签到全流程' },
              { tag: '新增', tagClass: 'feature', text: 'Web 端社区管理：审核、用户管理、活动管理完整后台' }
            ]
          },
          {
            heading: '📹 阶段 3 · 视频通话与紧急求助（约 2025-07 ~ 08）',
            items: [
              { tag: '新增', tagClass: 'feature', text: 'Call 模块诞生：视频通话 + 紧急求助，早期 Netty Socket 实现' },
              { tag: '新增', tagClass: 'feature', text: 'FIFO 匹配队列：盲人发起求助 → 按序匹配在线志愿者' },
              { tag: '新增', tagClass: 'feature', text: '家庭紧急求助：家属一键呼叫，与视频通话共用信令通道' },
              { tag: '新增', tagClass: 'feature', text: '心跳包 + App 自启动，长连接保活' }
            ]
          },
          {
            heading: '👤 阶段 2 · 用户与家庭模块（约 2025-07 ~ 08）',
            items: [
              { tag: '新增', tagClass: 'feature', text: '三类用户体系：视障人士 / 志愿者 / 员工，注册登录 + JWT + Redis token' },
              { tag: '新增', tagClass: 'feature', text: '家庭关系管理：家属绑定与审核，紧急求助通知链' },
              { tag: '优化', tagClass: 'improve', text: '抽取公共拦截器代码（common-web），统一鉴权逻辑' },
              { tag: '优化', tagClass: 'improve', text: '盲人端移动端适配优化' }
            ]
          },
          {
            heading: '🏗️ 阶段 0–1 · 一期封版 & 二期脚手架',
            items: [
              { tag: '新增', tagClass: 'feature', text: '一期单体封版（uniapp + Spring Boot + 单库 4 表），git tag v1.0' },
              { tag: '新增', tagClass: 'feature', text: '二期微服务脚手架：多模块切分 + Nacos 注册中心 + Dubbo RPC + JWT/Redis 骨架' }
            ]
          }
        ]
      },
      {
        version: 'v1.0',
        date: '2025-06-30',
        badge: '起点',
        badgeClass: 'fix',
        title: '视无界诞生 🌱',
        desc: '一期平台（git tag v1.0，独立根提交）正式封版，奠定面向视障人士的无障碍服务基石。作为二期微服务演进的对照基线保留在 git 历史，不再迭代。',
        items: [
          { tag: '新增', tagClass: 'feature', text: '视障人士与志愿者注册登录，基础身份管理体系' },
          { tag: '新增', tagClass: 'feature', text: '远程无障碍协助服务雏形' },
          { tag: '新增', tagClass: 'feature', text: '技术栈：uniapp 跨端客户端 + Spring Boot 单体后端 + 单库 4 表' }
        ]
      }
    ]

    return { versions }
  }
}
</script>

<style scoped>
/* ============================================================
   PAGE HEADER
   ============================================================ */
.page-header {
  padding: 120px 40px 60px;
  text-align: center;
  position: relative;
  overflow: hidden;
}
.page-header::before {
  content: '';
  position: absolute;
  top: -40%;
  left: 50%;
  transform: translateX(-50%);
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0,113,227,.06) 0%, transparent 70%);
  pointer-events: none;
}
.page-header h1 {
  font-size: clamp(32px, 5vw, 48px);
  font-weight: 800;
  letter-spacing: -.03em;
  position: relative;
}
.page-header h1 span {
  background: linear-gradient(135deg, #0071e3 0%, #40a9ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.page-header p {
  font-size: 16px;
  color: var(--text-2);
  margin-top: 12px;
  position: relative;
}

/* ============================================================
   TIMELINE
   ============================================================ */
.changelog {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 40px 100px;
  position: relative;
}
.changelog::before {
  content: '';
  position: absolute;
  left: calc(40px + 19px);
  top: 0;
  bottom: 0;
  width: 1px;
  background: var(--border-l);
}

.version {
  position: relative;
  padding-left: 64px;
  margin-bottom: 48px;
}
.version:last-child { margin-bottom: 0; }

/* Timeline dot */
.version-dot {
  position: absolute;
  left: calc(40px + 12px);
  top: 4px;
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background: var(--primary);
  border: 3px solid var(--bg);
  box-shadow: 0 0 0 3px rgba(0,113,227,.15);
  z-index: 1;
}
.version.first .version-dot {
  width: 19px;
  height: 19px;
  left: calc(40px + 10px);
  top: 2px;
  box-shadow: 0 0 0 6px rgba(0,113,227,.1);
}

/* Version header */
.version-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.version-tag {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 13px;
  background: var(--primary);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  font-family: var(--font-mono);
  letter-spacing: .02em;
}
.version-date {
  font-size: 13px;
  color: var(--text-3);
  font-family: var(--font-mono);
}
.version-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .04em;
}
.version-badge.new { background: #34c759; color: #fff; }
.version-badge.improve { background: rgba(0,113,227,.1); color: var(--primary); }
.version-badge.fix { background: rgba(255,149,0,.12); color: #ff9500; }

/* Version body */
.version-body {
  background: var(--surface);
  border: 1px solid var(--border-l);
  border-radius: 14px;
  padding: 24px;
}
.version-body h4 {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -.01em;
  margin-bottom: 6px;
}
.version-body > p {
  font-size: 14px;
  color: var(--text-2);
  margin-bottom: 16px;
  line-height: 1.6;
}

/* Section sub-headings */
.version-section-title {
  font-size: 14px;
  font-weight: 700;
  margin: 16px 0 8px;
  color: var(--text);
}

/* Changelog items */
.cl-items {
  list-style: none;
}
.cl-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 0;
  font-size: 14px;
  color: var(--text);
  line-height: 1.5;
}
.cl-item + .cl-item { border-top: 1px solid var(--border-l); }
.cl-item .tag {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: 5px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .03em;
  margin-top: 1px;
}
.tag.feature { background: rgba(52,199,89,.12); color: #34c759; }
.tag.improve { background: rgba(0,113,227,.1); color: var(--primary); }
.tag.fix { background: rgba(255,149,0,.1); color: #ff9500; }

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 768px) {
  .page-header { padding: 100px 20px 40px; }
  .changelog { padding: 0 20px 60px; }
  .changelog::before { left: 19px; }
  .version { padding-left: 48px; }
  .version-dot { left: 12px; }
  .version.first .version-dot { left: 10px; }
}
</style>
