<template>
  <div class="changelog-page">
    <!-- ==================== PAGE HEADER ==================== -->
    <header class="page-header">
      <h1>更新<span>日志</span></h1>
      <p>视无界的成长记录</p>
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
        version: 'v3.0',
        date: '2026-08-16',
        badge: '最新',
        badgeClass: 'new',
        title: 'AI 助手全面升级：说话即操作，拍照即识别',
        desc: '这是现在的视无界：打开软件就是 AI 助手，开口就能用。',
        sections: [
          {
            heading: '🤖 AI 语音对话',
            items: [
              { tag: '功能', tagClass: 'feature', text: '像聊天一样说出需求，AI 语音回答，还能联网搜索最新信息' },
              { tag: '功能', tagClass: 'feature', text: '说"帮我打开微信"，AI 帮您打开手机里的应用' },
              { tag: '功能', tagClass: 'feature', text: '说"我要去哪里"，AI 帮您快捷规划导航路线' },
              { tag: '功能', tagClass: 'feature', text: '说"帮我紧急求助"，AI 帮您操控软件功能、发起求助' }
            ]
          },
          {
            heading: '📷 AI 拍照识别',
            items: [
              { tag: '功能', tagClass: 'feature', text: '说"帮我识别前面"，AI 自动拍照并语音告诉您眼前是什么' },
              { tag: '功能', tagClass: 'feature', text: '识别结果可以继续追问，像随身带了一位讲解员' }
            ]
          },
          {
            heading: '🆘 一键求助',
            items: [
              { tag: '功能', tagClass: 'feature', text: '紧急求助：一键向家属发起视频通话求助' },
              { tag: '功能', tagClass: 'feature', text: '志愿者求助：视频连线志愿者，远程做您的眼睛' }
            ]
          },
          {
            heading: '🔮 AI 悬浮球',
            items: [
              { tag: '功能', tagClass: 'feature', text: '退出软件后屏幕上保留 AI 悬浮球，点击随时回到 AI 助手' },
              { tag: '功能', tagClass: 'feature', text: '退到后台有语音提示，界面改为 AI 优先的新版布局' }
            ]
          }
        ]
      },
      {
        version: 'v2.0',
        date: '2025-08-23',
        badge: '里程碑',
        badgeClass: 'improve',
        title: 'AI 助手加入 + 社区家园',
        desc: '这一时期的主页：首页汇集各功能入口，AI 助手首次登场，社区与家庭体系成型。',
        sections: [
          {
            heading: '🤖 AI 助手',
            items: [
              { tag: '功能', tagClass: 'feature', text: 'AI 语音助手上线：语音提问、语音回答' },
              { tag: '功能', tagClass: 'feature', text: '拍照识别：拍下眼前的事物，AI 告诉您它是什么' }
            ]
          },
          {
            heading: '🏠 社区与家庭',
            items: [
              { tag: '功能', tagClass: 'feature', text: '家庭体系：与家人绑定，紧急求助一键通知家属' },
              { tag: '功能', tagClass: 'feature', text: '社区体系：加入社区、参与活动、发布互助求助帖' },
              { tag: '功能', tagClass: 'feature', text: '主页一站式入口：AI、家庭、社区、我的汇聚一页' }
            ]
          },
          {
            heading: '🆘 求助与通话',
            items: [
              { tag: '功能', tagClass: 'feature', text: '志愿者视频帮扶：一键连线志愿者远程协助' },
              { tag: '功能', tagClass: 'feature', text: '紧急求助：家庭内群发通知，家属视频接入' }
            ]
          }
        ]
      },
      {
        version: 'v1.0',
        date: '2025-05',
        badge: '起点',
        badgeClass: 'improve',
        title: '视无界诞生：第一个可用版本',
        desc: '项目起点（uniapp 时期）：视障者、志愿者、家属三端互通的平台雏形。',
        sections: [
          {
            heading: '👥 三类角色',
            items: [
              { tag: '功能', tagClass: 'feature', text: '视障人士 / 志愿者 / 家属三类账号注册登录' },
              { tag: '功能', tagClass: 'feature', text: '家庭创建与加入，家属与视障者绑定' }
            ]
          },
          {
            heading: '🆘 求助基础能力',
            items: [
              { tag: '功能', tagClass: 'feature', text: '紧急求助：向家人发起求助通知' },
              { tag: '功能', tagClass: 'feature', text: '志愿者协助：发起求助等待志愿者响应' }
            ]
          }
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
