<template>
  <div class="activity-stats">
    <div class="page-head">
      <div>
        <h2>活动统计</h2>
        <p>活动开展数据概览</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stats-row">
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon tint-teal"><CalendarOutlined /></div>
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">累计活动数</div>
          <div class="stat-change up"><ArrowUpOutlined /> 本月新增 12 场</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon tint-green"><CheckCircleOutlined /></div>
          <div class="stat-value">{{ stats.checkinRate }}%</div>
          <div class="stat-label">平均签到率</div>
          <div class="stat-change up"><ArrowUpOutlined /> 较上月 +3.2%</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon tint-blue"><TeamOutlined /></div>
          <div class="stat-value">{{ stats.participants }}</div>
          <div class="stat-label">累计参与人次</div>
          <div class="stat-change up"><ArrowUpOutlined /> 较上月 +25%</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon tint-purple"><StarOutlined /></div>
          <div class="stat-value">{{ stats.avgRating }}</div>
          <div class="stat-label">活动平均评分</div>
          <div class="stat-change up"><ArrowUpOutlined /> 较上月 +0.2</div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表行 -->
    <a-row :gutter="16" class="block-row">
      <a-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-head">
            <h3 class="panel-title"><LineChartOutlined /> 月度活动举办趋势</h3>
            <span class="chart-subtitle">近 6 个月</span>
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
                <text x="44" y="24">20</text><text x="44" y="74">15</text><text x="44" y="124">10</text><text x="44" y="174">5</text><text x="44" y="218">0</text>
              </g>
              <defs>
                <linearGradient id="areaGradAs" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#30b0c7" stop-opacity="0.15"/>
                  <stop offset="100%" stop-color="#30b0c7" stop-opacity="0.0"/>
                </linearGradient>
              </defs>
              <polygon fill="url(#areaGradAs)" :points="trendChart.points"/>
              <polyline fill="none" stroke="#30b0c7" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" :points="trendChart.points"/>
              <g fill="#fff" stroke="#30b0c7" stroke-width="2">
                <circle v-for="(p, i) in trendChart.dots" :key="i" :cx="p.x" :cy="p.y" r="4.5"/>
              </g>
              <g fill="#aeaeb2" font-size="10" text-anchor="middle">
                <text v-for="(l, i) in trendChart.labels" :key="i" :x="l.x" y="210">{{ l.text }}</text>
              </g>
            </svg>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-head">
            <h3 class="panel-title"><BarChartOutlined /> 各活动签到率对比（近 6 期）</h3>
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
                <text x="44" y="24">100%</text><text x="44" y="74">75%</text><text x="44" y="124">50%</text><text x="44" y="174">25%</text><text x="44" y="218">0%</text>
              </g>
              <polyline fill="none" stroke="#ff9500" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" :points="rateChart.points"/>
              <g fill="#fff" stroke="#ff9500" stroke-width="2">
                <circle v-for="(p, i) in rateChart.dots" :key="i" :cx="p.x" :cy="p.y" r="4.5"/>
              </g>
              <g fill="var(--text)" font-size="10" font-weight="700" font-family="SF Mono, monospace" text-anchor="middle">
                <text v-for="(l, i) in rateChart.vals" :key="i" :x="l.x" :y="l.y">{{ l.text }}</text>
              </g>
              <g fill="#aeaeb2" font-size="10" text-anchor="middle">
                <text v-for="(l, i) in rateChart.labels" :key="i" :x="l.x" y="210">{{ l.text }}</text>
              </g>
            </svg>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 近期活动明细 -->
    <div class="panel" style="margin-top:0;">
      <div class="panel-head">
        <h3 class="panel-title"><OrderedListOutlined /> 近期活动明细</h3>
      </div>
      <table class="info-table">
        <thead><tr><th>活动名称</th><th>所属社区</th><th>报名人数</th><th>签到人数</th><th>签到率</th><th>评分</th><th>日期</th></tr></thead>
        <tbody>
          <tr v-for="a in activityList" :key="a.name">
            <td class="fw-600">{{ a.name }}</td>
            <td>{{ a.community }}</td>
            <td class="mono">{{ a.registered }}</td>
            <td class="mono">{{ a.checkedIn }}</td>
            <td><span class="tag-sm" :class="a.rateClass">{{ a.rate }}%</span></td>
            <td class="mono">⭐ {{ a.rating }}</td>
            <td class="mono">{{ a.date }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import { computed, reactive } from 'vue'
import {
  CalendarOutlined, CheckCircleOutlined, TeamOutlined, StarOutlined,
  ArrowUpOutlined, ArrowDownOutlined, LineChartOutlined,
  BarChartOutlined, OrderedListOutlined
} from '@ant-design/icons-vue'

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

// rate = 100% → top(20), 0% → bottom(190), scale=1.7
function makeRateChart(rates, names) {
  const scale = 1.7
  const top = 20
  return {
    points: rates.map((v, i) => `${CHART_X[i]},${top + (100 - v) * scale}`).join(' '),
    dots: rates.map((v, i) => ({ x: CHART_X[i], y: top + (100 - v) * scale })),
    vals: rates.map((v, i) => ({ x: CHART_X[i], y: top + (100 - v) * scale - 10, text: v + '%' })),
    labels: names.map((n, i) => ({ x: CHART_X[i], text: n }))
  }
}

export default {
  name: 'ActivityStats',
  components: {
    CalendarOutlined, CheckCircleOutlined, TeamOutlined, StarOutlined,
    ArrowUpOutlined, ArrowDownOutlined, LineChartOutlined,
    BarChartOutlined, OrderedListOutlined
  },
  setup() {
    const stats = reactive({
      total: 156,
      checkinRate: 87.3,
      participants: 2340,
      avgRating: 4.8
    })

    const trendChart = computed(() => makeArea([9, 12, 15, 13, 16, 18], 20))

    const rateChart = computed(() => makeRateChart(
      [89, 84, 68, 94, 82, 91],
      ['智能手机', '趣味运动', '出行体验', '阅读分享', '心理辅导', '健康讲座']
    ))

    const activityList = [
      { name: '视障人士智能手机培训', community: '阳光家园社区', registered: 55, checkedIn: 50, rate: 91, rateClass: 'green', rating: 4.9, date: '07-20' },
      { name: '夏季趣味运动会', community: '彩虹桥社区', registered: 38, checkedIn: 35, rate: 92, rateClass: 'green', rating: 4.8, date: '07-15' },
      { name: '无障碍出行体验日', community: '星光里社区', registered: 28, checkedIn: 19, rate: 68, rateClass: 'orange', rating: 4.3, date: '07-10' },
      { name: '盲文阅读分享会', community: '阳光家园社区', registered: 52, checkedIn: 49, rate: 94, rateClass: 'green', rating: 5.0, date: '07-05' },
      { name: '志愿者心理辅导培训', community: '爱心港湾社区', registered: 45, checkedIn: 37, rate: 82, rateClass: 'green', rating: 4.6, date: '06-28' },
      { name: '视障人士健康知识讲座', community: '暖心坊社区', registered: 32, checkedIn: 26, rate: 81, rateClass: 'green', rating: 4.7, date: '06-22' }
    ]

    return { stats, trendChart, rateChart, activityList }
  }
}
</script>

<style scoped>
.activity-stats {
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-head { margin-bottom: 20px; }
.page-head h2 {
  margin: 0 0 2px 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text);
}
.page-head p {
  margin: 0;
  font-size: 13px;
  color: var(--text-2);
}

/* Stat cards */
.stats-row { margin-bottom: 16px !important; }
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
.stat-card:hover { border-color: var(--border); }
.stat-icon {
  width: 30px; height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
}
.tint-blue   { background: rgba(0,113,227,.1);   color: var(--primary); }
.tint-teal   { background: rgba(48,176,199,.12);  color: var(--teal); }
.tint-green  { background: rgba(52,199,89,.12);   color: #1a9e3f; }
.tint-purple { background: rgba(175,82,222,.12);  color: #8944ab; }
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text);
  line-height: 1;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
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
.stat-change.up   { color: var(--success); }
.stat-change.down { color: var(--danger); }

/* Panels */
.block-row { margin-bottom: 16px !important; }
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
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
}

/* Chart */
.chart-body { padding: 4px 8px; }
.chart-svg { width: 100%; height: auto; max-height: 240px; }
.chart-subtitle { font-size: 11px; color: var(--text-3); }

/* Table */
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

/* Tags */
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
.tag-sm.orange { background: rgba(255,149,0,.1);   color: #ff9500; }

@media (max-width: 576px) {
  .stat-value { font-size: 22px; }
  .page-head h2 { font-size: 19px; }
}
</style>
