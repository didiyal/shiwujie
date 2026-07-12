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
          <ul class="cl-items">
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
        version: 'v3.0.0',
        date: '2026-07-08',
        badge: 'Major',
        badgeClass: 'new',
        title: '官网全新上线 🎉',
        desc: '视无界正式推出品牌官网，提供产品介绍、下载入口与更新日志，管理端同步升级至全新 Apple 风格设计系统。',
        items: [
          { tag: '新增', tagClass: 'feature', text: '全新品牌官网首页，科技概念视觉设计' },
          { tag: '新增', tagClass: 'feature', text: '官网导航栏：产品介绍、更新日志、GitHub、管理端入口' },
          { tag: '优化', tagClass: 'improve', text: '管理端整体 UI 升级，采用 Apple 设计语言，统一圆角/间距/字体体系' },
          { tag: '优化', tagClass: 'improve', text: '暗色侧栏 + 毛玻璃顶栏，提升视觉层次' }
        ]
      },
      {
        version: 'v2.5.0',
        date: '2026-06-20',
        badge: '改进',
        badgeClass: 'improve',
        title: '社区与活动功能增强',
        desc: '完善社区管理流程，新增活动签到管理功能。',
        items: [
          { tag: '新增', tagClass: 'feature', text: '活动签到管理：支持签到列表查看、签到详情追踪' },
          { tag: '优化', tagClass: 'improve', text: '社区编辑表单重构，提升数据录入效率' },
          { tag: '修复', tagClass: 'fix', text: '修复社区列表分页在移动端的显示问题' }
        ]
      },
      {
        version: 'v2.4.0',
        date: '2026-05-15',
        badge: '改进',
        badgeClass: 'improve',
        title: '数据统计模块上线',
        desc: '新增社区与活动数据统计分析功能，支持多维度可视化。',
        items: [
          { tag: '新增', tagClass: 'feature', text: '社区统计看板：成员增长、活跃度、地域分布' },
          { tag: '新增', tagClass: 'feature', text: '活动统计看板：参与率、签到率、活动趋势' },
          { tag: '优化', tagClass: 'improve', text: '仪表板首页改版，关键指标卡片重新设计' }
        ]
      },
      {
        version: 'v2.3.0',
        date: '2026-04-10',
        badge: '新增',
        badgeClass: 'new',
        title: '用户管理与审核系统',
        desc: '完善用户管理功能，新增社区加入审核流程。',
        items: [
          { tag: '新增', tagClass: 'feature', text: '志愿者、员工、视障人士三类用户分页管理' },
          { tag: '新增', tagClass: 'feature', text: '社区加入审核：审批志愿者入群申请' },
          { tag: '修复', tagClass: 'fix', text: '修复登录状态在页面刷新后丢失的问题' }
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
