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
          <div class="stat-value">0</div>
          <div class="stat-label">总社区数</div>
          <div class="stat-change up"><ArrowUpOutlined /> 较上月 +12%</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-top">
            <div class="stat-icon tint-teal"><CalendarOutlined /></div>
          </div>
          <div class="stat-value">0</div>
          <div class="stat-label">活跃活动</div>
          <div class="stat-change up"><ArrowUpOutlined /> 较上月 +8%</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-top">
            <div class="stat-icon tint-orange"><ClockCircleOutlined /></div>
          </div>
          <div class="stat-value stat-warn">0</div>
          <div class="stat-label">待审核申请</div>
          <div class="stat-change down"><ArrowDownOutlined /> 较上月 −5%</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-top">
            <div class="stat-icon tint-green"><TeamOutlined /></div>
          </div>
          <div class="stat-value">0</div>
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
          <div class="panel-body chart-empty">
            <LineChartOutlined class="empty-icon" />
            <p>暂无数据</p>
            <a-button type="link" size="small">查看详情</a-button>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-head">
            <h3 class="panel-title"><BarChartOutlined /> 活动参与情况</h3>
          </div>
          <div class="panel-body chart-empty">
            <BarChartOutlined class="empty-icon" />
            <p>暂无数据</p>
            <a-button type="link" size="small">查看详情</a-button>
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
          <div class="panel-body state-empty">
            <CalendarOutlined class="empty-icon" />
            <p>暂无活动数据</p>
            <a-button type="primary" size="small">创建活动</a-button>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-head">
            <h3 class="panel-title"><ExclamationCircleOutlined /> 待处理事项</h3>
            <a-button type="link" size="small">查看全部</a-button>
          </div>
          <div class="panel-body state-empty">
            <CheckCircleOutlined class="empty-icon" />
            <p>暂无待处理事项</p>
            <a-button size="small" @click="refresh">刷新</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { computed } from 'vue'
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
  ExclamationCircleOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue'

export default {
  name: 'Dashboard',
  components: {
    ReloadOutlined, HomeOutlined, CalendarOutlined, ClockCircleOutlined, TeamOutlined,
    ArrowUpOutlined, ArrowDownOutlined, LineChartOutlined, BarChartOutlined,
    FireOutlined, ExclamationCircleOutlined, CheckCircleOutlined
  },
  setup() {
    const authStore = useAuthStore()

    const currentDate = computed(() => {
      const now = new Date()
      return now.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        weekday: 'long'
      })
    })

    const userName = computed(() => authStore.volunteerInfo?.name || authStore.volunteer?.name || '管理员')

    const refresh = () => {
      // 仪表板数据刷新占位
    }

    return { currentDate, userName, refresh }
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

@media (max-width: 576px) {
  .stat-value {
    font-size: 22px;
  }
  .page-head h2 {
    font-size: 19px;
  }
}
</style>
