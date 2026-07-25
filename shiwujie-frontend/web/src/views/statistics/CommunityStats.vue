<template>
  <div class="community-stats">
    <div class="page-head">
      <div>
        <h2>社区统计</h2>
        <p>社区运营数据概览</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stats-row">
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon tint-blue"><HomeOutlined /></div>
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总社区数</div>
          <div class="stat-change up"><ArrowUpOutlined /> 本月新增 3 个</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon tint-green"><TeamOutlined /></div>
          <div class="stat-value">{{ stats.members }}</div>
          <div class="stat-label">社区成员总数</div>
          <div class="stat-change up"><ArrowUpOutlined /> 较上月 +18%</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon tint-teal"><FileTextOutlined /></div>
          <div class="stat-value">{{ stats.posts }}</div>
          <div class="stat-label">累计求助帖</div>
          <div class="stat-change up"><ArrowUpOutlined /> 较上月 +22%</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon tint-orange"><ClockCircleOutlined /></div>
          <div class="stat-value stat-warn">{{ stats.pending }}</div>
          <div class="stat-label">待审核社区申请</div>
          <div class="stat-change down"><ArrowDownOutlined /> 较上月 −22%</div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表行 -->
    <a-row :gutter="16" class="block-row">
      <a-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-head">
            <h3 class="panel-title"><LineChartOutlined /> 社区成员增长趋势</h3>
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
                <text x="44" y="24">1,000</text><text x="44" y="74">750</text><text x="44" y="124">500</text><text x="44" y="174">250</text><text x="44" y="218">0</text>
              </g>
              <defs>
                <linearGradient id="areaGradCs" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#34c759" stop-opacity="0.15"/>
                  <stop offset="100%" stop-color="#34c759" stop-opacity="0.0"/>
                </linearGradient>
              </defs>
              <polygon fill="url(#areaGradCs)" :points="memberChart.points"/>
              <polyline fill="none" stroke="#34c759" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" :points="memberChart.points"/>
              <g fill="#fff" stroke="#34c759" stroke-width="2">
                <circle v-for="(p, i) in memberChart.dots" :key="i" :cx="p.x" :cy="p.y" r="4.5"/>
              </g>
              <g fill="#aeaeb2" font-size="10" text-anchor="middle">
                <text v-for="(l, i) in memberChart.labels" :key="i" :x="l.x" y="210">{{ l.text }}</text>
              </g>
            </svg>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-head">
            <h3 class="panel-title"><EnvironmentOutlined /> 社区地域分布（TOP 6）</h3>
          </div>
          <div class="panel-body" style="flex-direction:column;align-items:stretch;padding:4px 0;">
            <div class="rank-item" v-for="(r, i) in regions" :key="r.name">
              <div class="rank-num" :class="'rank-' + (i + 1)">{{ i + 1 }}</div>
              <span class="rank-name">{{ r.name }}</span>
              <div class="rank-bar-wrap"><div class="rank-bar-fill" :style="{ width: r.pct + '%' }"></div></div>
              <span class="rank-val">{{ r.count }}</span>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 社区活跃度排行 -->
    <div class="panel" style="margin-top:0;">
      <div class="panel-head">
        <h3 class="panel-title"><OrderedListOutlined /> 社区活跃度排行</h3>
      </div>
      <table class="info-table">
        <thead><tr><th>排名</th><th>社区名称</th><th>所属区域</th><th>成员数</th><th>本月活动</th><th>活跃度</th></tr></thead>
        <tbody>
          <tr v-for="(c, i) in communityRank" :key="c.name">
            <td class="mono" :class="{ 'rank-highlight': i < 3 }">{{ c.rank }}</td>
            <td class="fw-600">{{ c.name }}</td>
            <td>{{ c.district }}</td>
            <td class="mono">{{ c.members }}</td>
            <td class="mono">{{ c.events }}</td>
            <td><span class="tag-sm" :class="c.activityClass">{{ c.activity }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import { computed, reactive } from 'vue'
import {
  HomeOutlined, TeamOutlined, FileTextOutlined, ClockCircleOutlined,
  ArrowUpOutlined, ArrowDownOutlined, LineChartOutlined,
  EnvironmentOutlined, OrderedListOutlined
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

export default {
  name: 'CommunityStats',
  components: {
    HomeOutlined, TeamOutlined, FileTextOutlined, ClockCircleOutlined,
    ArrowUpOutlined, ArrowDownOutlined, LineChartOutlined,
    EnvironmentOutlined, OrderedListOutlined
  },
  setup() {
    const stats = reactive({
      total: 48,
      members: 856,
      posts: 326,
      pending: 7
    })

    const memberChart = computed(() => makeArea([520, 580, 650, 720, 790, 856], 1000))

    const regions = [
      { name: '北京市朝阳区', count: 12, pct: 100 },
      { name: '上海市浦东新区', count: 10, pct: 83 },
      { name: '广州市天河区', count: 8, pct: 67 },
      { name: '深圳市南山区', count: 6, pct: 50 },
      { name: '杭州市西湖区', count: 5, pct: 42 },
      { name: '成都市武侯区', count: 4, pct: 33 }
    ]

    const communityRank = [
      { rank: '#1', name: '阳光家园社区', district: '北京市朝阳区', members: 128, events: 5, activity: '高', activityClass: 'green' },
      { rank: '#2', name: '彩虹桥社区', district: '上海市浦东新区', members: 96, events: 4, activity: '高', activityClass: 'green' },
      { rank: '#3', name: '星光里社区', district: '广州市天河区', members: 84, events: 3, activity: '中', activityClass: 'blue' },
      { rank: '#4', name: '爱心港湾社区', district: '深圳市南山区', members: 72, events: 3, activity: '中', activityClass: 'blue' },
      { rank: '#5', name: '暖心坊社区', district: '杭州市西湖区', members: 65, events: 2, activity: '低', activityClass: 'orange' },
      { rank: '#6', name: '同路人社区', district: '成都市武侯区', members: 58, events: 2, activity: '低', activityClass: 'orange' }
    ]

    return { stats, memberChart, regions, communityRank }
  }
}
</script>

<style scoped>
.community-stats {
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
.tint-orange { background: rgba(255,149,0,.12);   color: #cc7000; }
.tint-green  { background: rgba(52,199,89,.12);   color: #1a9e3f; }
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text);
  line-height: 1;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
}
.stat-warn { color: var(--warning); }
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

/* Ranking bars */
.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}
.rank-item + .rank-item { border-top: 1px solid var(--border-l); }
.rank-num {
  width: 22px; height: 22px;
  border-radius: 6px;
  background: var(--border-l);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-2);
  flex-shrink: 0;
}
.rank-1 { background: #ff9500; color: #fff; }
.rank-2 { background: var(--text-3); color: #fff; }
.rank-3 { background: #cd7f32; color: #fff; }
.rank-name { flex: 1; font-size: 13px; font-weight: 500; }
.rank-bar-wrap {
  width: 100px; height: 6px;
  background: var(--border-l);
  border-radius: 3px;
  flex-shrink: 0;
}
.rank-bar-fill {
  height: 100%;
  border-radius: 3px;
  background: var(--primary);
}
.rank-val {
  width: 28px;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  font-family: var(--font-mono);
  flex-shrink: 0;
}

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
.rank-highlight { font-weight: 700; color: var(--primary); }

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
.tag-sm.blue   { background: rgba(0,113,227,.1);  color: var(--primary); }
.tag-sm.orange { background: rgba(255,149,0,.1);   color: #ff9500; }

@media (max-width: 576px) {
  .stat-value { font-size: 22px; }
  .page-head h2 { font-size: 19px; }
}
</style>
