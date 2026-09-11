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
  }
}
</script>
