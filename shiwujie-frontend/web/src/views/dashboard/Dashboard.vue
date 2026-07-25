<template>
  <div class="dashboard">
    <!-- 页面头 -->
    <div class="page-head">
      <div>
        <h2>仪表板</h2>
        <p>{{ currentDate }} · 欢迎回来，{{ userName }}</p>
      </div>
      <a-button @click="refresh" class="refresh-btn">
        <template #icon><ReloadOutlined /></template>
        刷新数据
      </a-button>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stats-row">
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-top">
            <div class="stat-icon tint-blue"><HomeOutlined /></div>
          </div>
          <div class="stat-value">{{ stats.communities }}</div>
          <div class="stat-label">总社区数</div>
          <div class="stat-change up"><ArrowUpOutlined /> 较上月 +12%</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-top">
            <div class="stat-icon tint-teal"><CalendarOutlined /></div>
          </div>
          <div class="stat-value">{{ stats.activeEvents }}</div>
          <div class="stat-label">活跃活动</div>
          <div class="stat-change up"><ArrowUpOutlined /> 较上月 +8%</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-top">
            <div class="stat-icon tint-orange"><ClockCircleOutlined /></div>
          </div>
          <div class="stat-value stat-warn">{{ stats.pending }}</div>
          <div class="stat-label">待审核申请</div>
          <div class="stat-change down"><ArrowDownOutlined /> 较上月 −22%</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-top">
            <div class="stat-icon tint-green"><TeamOutlined /></div>
          </div>
          <div class="stat-value">{{ stats.users }}</div>
          <div class="stat-label">总用户数</div>
          <div class="stat-change up"><ArrowUpOutlined /> 较上月 +15%</div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表 -->
    <a-row :gutter="16" class="block-row">
      <a-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-head">
            <h3 class="panel-title"><LineChartOutlined /> 社区活跃度趋势</h3>
          </div>
          <div class="panel-body chart-body">
            <svg viewBox="0 0 500 220" class="chart-svg">
              <g stroke="#e8e8ed" stroke-width="1">
                <line x1="50" y1="20" x2="470" y2="20"/>
                <line x1="50" y1="70" x2="470" y2="70"/>
                <line x1="50" y1="120" x2="470" y2="120"/>
                <line x1="50" y1="170" x2="470" y2="170"/>
              </g>
              <g fill="#aeaeb2" font-size="10" font-family="SF Mono, Menlo, monospace" text-anchor="end">
                <text x="44" y="24">80</text><text x="44" y="74">60</text><text x="44" y="124">40</text><text x="44" y="174">20</text><text x="44" y="218">0</text>
              </g>
              <defs>
                <linearGradient id="areaGrad1" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#0071e3" stop-opacity="0.15"/>
                  <stop offset="100%" stop-color="#0071e3" stop-opacity="0.0"/>
                </linearGradient>
              </defs>
              <polygon fill="url(#areaGrad1)" :points="chartArea.points"/>
              <polyline fill="none" stroke="#0071e3" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" :points="chartArea.points"/>
              <g fill="#fff" stroke="#0071e3" stroke-width="2">
                <circle v-for="(p, i) in chartArea.dots" :key="i" :cx="p.x" :cy="p.y" r="4.5"/>
              </g>
              <g fill="#aeaeb2" font-size="10" text-anchor="middle">
                <text v-for="(l, i) in chartArea.labels" :key="i" :x="l.x" y="210">{{ l.text }}</text>
              </g>
            </svg>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-head">
            <h3 class="panel-title"><BarChartOutlined /> 活动参与情况（近 6 期）</h3>
            <span class="chart-subtitle">报名 / 签到</span>
          </div>
          <div class="panel-body chart-body">
            <svg viewBox="0 0 500 220" class="chart-svg">
              <g stroke="#e8e8ed" stroke-width="1">
                <line x1="50" y1="20" x2="470" y2="20"/>
                <line x1="50" y1="70" x2="470" y2="70"/>
                <line x1="50" y1="120" x2="470" y2="120"/>
                <line x1="50" y1="170" x2="470" y2="170"/>
              </g>
              <g fill="#aeaeb2" font-size="10" font-family="SF Mono, Menlo, monospace" text-anchor="end">
                <text x="44" y="24">60</text><text x="44" y="74">45</text><text x="44" y="124">30</text><text x="44" y="174">15</text><text x="44" y="218">0</text>
              </g>
              <rect v-for="(b, i) in barChart.bars" :key="'reg'+i" :x="b.x1" :y="b.y1" width="26" :height="b.h1" rx="4" fill="#0071e3" opacity="0.85"/>
              <rect v-for="(b, i) in barChart.bars" :key="'chk'+i" :x="b.x2" :y="b.y2" width="26" :height="b.h2" rx="4" fill="#34c759" opacity="0.85"/>
              <rect x="52" y="200" width="10" height="10" rx="2" fill="#0071e3" opacity="0.85"/>
              <text x="66" y="210" font-size="10" fill="#6e6e73">报名</text>
              <rect x="100" y="200" width="10" height="10" rx="2" fill="#34c759" opacity="0.85"/>
              <text x="114" y="210" font-size="10" fill="#6e6e73">签到</text>
            </svg>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 最近活动 / 待处理 -->
    <a-row :gutter="16" class="block-row">
      <a-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-head">
            <h3 class="panel-title"><FireOutlined /> 最近活动</h3>
            <a-button type="link" size="small">查看全部</a-button>
          </div>
          <table class="info-table">
            <thead><tr><th>活动名称</th><th>社区</th><th>状态</th><th>日期</th></tr></thead>
            <tbody>
              <tr v-for="a in recentActivities" :key="a.name">
                <td class="fw-600">{{ a.name }}</td>
                <td>{{ a.community }}</td>
                <td><span class="tag-sm" :class="a.statusClass">{{ a.status }}</span></td>
                <td class="mono">{{ a.date }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </a-col>
      <a-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-head">
            <h3 class="panel-title"><ExclamationCircleOutlined /> 待处理事项</h3>
            <a-button type="link" size="small">查看全部</a-button>
          </div>
          <table class="info-table">
            <thead><tr><th>事项</th><th>类型</th><th>优先级</th><th>时间</th></tr></thead>
            <tbody>
              <tr v-for="p in pendingItems" :key="p.title">
                <td class="fw-600">{{ p.title }}</td>
                <td>{{ p.type }}</td>
                <td><span class="tag-sm" :class="p.priorityClass">{{ p.priority }}</span></td>
                <td class="mono">{{ p.time }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { computed, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  ReloadOutlined,
  HomeOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  TeamOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  LineChartOutlined,
  BarChartOutlined,
  FireOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue'

// ---- sample data generators ----

const CHART_X = [90, 166, 242, 318, 394, 470]
const CHART_LABELS = ['2月', '3月', '4月', '5月', '6月', '7月']

function makeArea(data, max) {
  const scale = 170 / max
  return {
    points: data.map((v, i) => `${CHART_X[i]},${190 - v * scale}`).join(' '),
    dots: data.map((v, i) => ({ x: CHART_X[i], y: 190 - v * scale })),
    labels: data.map((v, i) => ({ x: CHART_X[i], text: CHART_LABELS[i] }))
  }
}

function makeBars(data1, data2, max) {
  const scale = 170 / max
  const step = 80
  const base = CHART_X[0] - 28
  return {
    bars: data1.map((v, i) => ({
      x1: base + step * i,
      y1: 190 - v * scale,
      h1: v * scale,
      x2: base + step * i + 28,
      y2: 190 - data2[i] * scale,
      h2: data2[i] * scale
    }))
  }
}

export default {
  name: 'Dashboard',
  components: {
    ReloadOutlined, HomeOutlined, CalendarOutlined, ClockCircleOutlined, TeamOutlined,
    ArrowUpOutlined, ArrowDownOutlined, LineChartOutlined, BarChartOutlined,
    FireOutlined, ExclamationCircleOutlined
  },
  setup() {
    const authStore = useAuthStore()

    const currentDate = computed(() => {
      const now = new Date()
      return now.toLocaleDateString('zh-CN', {
        year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
      })
    })

    const userName = computed(() => authStore.volunteerInfo?.name || authStore.volunteer?.name || '管理员')

    const stats = reactive({
      communities: 48,
      activeEvents: 12,
      pending: 7,
      users: 1284
    })

    const chartArea = computed(() => makeArea([35, 42, 55, 48, 62, 71], 80))

    const barChart = computed(() => makeBars(
      [32, 45, 28, 52, 38, 55],
      [28, 38, 22, 48, 35, 50],
      60
    ))

    const recentActivities = [
      { name: '视障人士智能手机培训', community: '阳光家园社区', status: '进行中', statusClass: 'green', date: '07-20' },
      { name: '夏季趣味运动会', community: '彩虹桥社区', status: '即将开始', statusClass: 'blue', date: '07-22' },
      { name: '无障碍出行体验日', community: '星光里社区', status: '招募中', statusClass: 'blue', date: '07-25' },
      { name: '盲文阅读分享会', community: '阳光家园社区', status: '招募中', statusClass: 'blue', date: '07-28' },
      { name: '志愿者心理辅导培训', community: '爱心港湾社区', status: '进行中', statusClass: 'green', date: '08-01' }
    ]

    const pendingItems = [
      { title: '张明的社区加入申请', type: '社区审核', priority: '高', priorityClass: 'orange', time: '2 小时前' },
      { title: '李华的志愿者认证审核', type: '身份审核', priority: '高', priorityClass: 'orange', time: '3 小时前' },
      { title: '王芳的家庭绑定申请', type: '家庭审核', priority: '中', priorityClass: 'blue', time: '5 小时前' },
      { title: '「无障碍出行」活动报名异常', type: '活动处理', priority: '中', priorityClass: 'blue', time: '1 天前' },
      { title: '阳光家园社区信息更新', type: '社区管理', priority: '低', priorityClass: 'red', time: '2 天前' }
    ]

    const refresh = () => { /* placeholder */ }

    return { currentDate, userName, stats, chartArea, barChart, recentActivities, pendingItems, refresh }
  }
}
</script>

<style scoped>
.dashboard {
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  gap: 12px;
}
.page-head h2 {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.page-head p {
  font-size: 13px;
  color: var(--text-2);
  margin-top: 2px;
}
.refresh-btn {
  border: 1px solid var(--border) !important;
}

/* 统计卡 */
.stats-row {
  margin-bottom: 16px !important;
}
.stat-card {
  background: var(--surface);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  transition: var(--tr);
}
.stat-card:hover {
  border-color: var(--border);
}
.stat-top {
  display: flex;
  align-items: center;
}
.stat-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
}
.tint-blue { background: rgba(0, 113, 227, 0.1); color: var(--primary); }
.tint-teal { background: rgba(48, 176, 199, 0.12); color: var(--teal); }
.tint-orange { background: rgba(255, 149, 0, 0.12); color: #cc7000; }
.tint-green { background: rgba(52, 199, 89, 0.12); color: #1a9e3f; }
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text);
  line-height: 1;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
}
.stat-warn {
  color: var(--warning);
}
.stat-label {
  font-size: 12px;
  color: var(--text-2);
  font-weight: 500;
}
.stat-change {
  font-size: 11px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 2px;
}
.stat-change.up { color: var(--success); }
.stat-change.down { color: var(--danger); }

/* 面板 */
.block-row {
  margin-bottom: 16px !important;
}
.panel {
  background: var(--surface);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
  padding: 18px 20px;
  height: 100%;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-l);
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.panel-body {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chart-empty,
.state-empty {
  text-align: center;
  color: var(--text-3);
  flex-direction: column;
}
.empty-icon {
  font-size: 36px;
  opacity: 0.3;
  margin-bottom: 10px;
}
.chart-empty p,
.state-empty p {
  margin-bottom: 12px;
  font-size: 13px;
}

/* Chart panels */
.chart-body {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  padding: 4px 8px;
}
.chart-svg {
  width: 100%;
  height: auto;
  max-height: 240px;
}
.chart-subtitle {
  font-size: 11px;
  color: var(--text-3);
}

/* Info tables */
.info-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.info-table th {
  text-align: left;
  font-weight: 600;
  color: var(--text-2);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-l);
}
.info-table td {
  padding: 10px 0;
  border-bottom: 1px solid var(--border-l);
  color: var(--text);
}
.info-table tr:last-child td { border-bottom: none; }
.fw-600 { font-weight: 600; }
.mono { font-family: var(--font-mono); font-size: 12px; }

/* Tag pills */
.tag-sm {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: 5px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.03em;
}
.tag-sm.green  { background: rgba(52,199,89,.12); color: #34c759; }
.tag-sm.blue   { background: rgba(0,113,227,.1);  color: var(--primary); }
.tag-sm.orange { background: rgba(255,149,0,.1);   color: #ff9500; }
.tag-sm.red    { background: rgba(255,59,48,.1);   color: #ff3b30; }

@media (max-width: 576px) {
  .stat-value {
    font-size: 22px;
  }
  .page-head h2 {
    font-size: 19px;
  }
}
</style>
