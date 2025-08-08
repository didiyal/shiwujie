<template>
  <div class="dashboard">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-content">
        <h1 class="welcome-title">欢迎回来，管理员！</h1>
        <p class="welcome-subtitle">今天是 {{ currentDate }}，祝您工作愉快！</p>
      </div>
      <div class="welcome-illustration">
        <div class="illustration-icon">📊</div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="24" class="stats-row">
      <a-col :xs="24" :sm="12" :md="6">
        <div class="stats-card stats-card-primary">
          <div class="stats-icon">
            🏘️
          </div>
          <div class="stats-content">
            <div class="stats-number">0</div>
            <div class="stats-title">总社区数</div>
            <div class="stats-trend">
              <span>+12%</span>
            </div>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="12" :md="6">
        <div class="stats-card stats-card-success">
          <div class="stats-icon">
            📅
          </div>
          <div class="stats-content">
            <div class="stats-number">0</div>
            <div class="stats-title">活跃活动</div>
            <div class="stats-trend">
              <span>+8%</span>
            </div>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="12" :md="6">
        <div class="stats-card stats-card-warning">
          <div class="stats-icon">
            ⏰
          </div>
          <div class="stats-content">
            <div class="stats-number">0</div>
            <div class="stats-title">待审核申请</div>
            <div class="stats-trend">
              <span>-5%</span>
            </div>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="12" :md="6">
        <div class="stats-card stats-card-purple">
          <div class="stats-icon">
            👥
          </div>
          <div class="stats-content">
            <div class="stats-number">0</div>
            <div class="stats-title">总用户数</div>
            <div class="stats-trend">
              <span>+15%</span>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="24" class="charts-row">
      <a-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">
              📊 社区活跃度趋势
            </h3>
          </div>
          <div class="chart-container">
            <div class="chart-placeholder">
              <div class="placeholder-icon">📊</div>
              <p>暂无数据</p>
              <a-button type="primary" size="small">查看详情</a-button>
            </div>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">
              📈 活动参与情况
            </h3>
          </div>
          <div class="chart-container">
            <div class="chart-placeholder">
              <div class="placeholder-icon">📈</div>
              <p>暂无数据</p>
              <a-button type="primary" size="small">查看详情</a-button>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 最近活动和待处理事项 -->
    <a-row :gutter="24" class="recent-row">
      <a-col :xs="24" :lg="12">
        <div class="recent-card">
          <div class="card-header">
            <h3 class="card-title">
              🔥 最近活动
            </h3>
            <a-button type="link" size="small">查看全部</a-button>
          </div>
          <div class="card-content">
            <div class="empty-state">
              <div class="empty-icon">📅</div>
              <p>暂无活动数据</p>
              <a-button type="primary" size="small">创建活动</a-button>
            </div>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="12">
        <div class="recent-card">
          <div class="card-header">
            <h3 class="card-title">
              ⚠️ 待处理事项
            </h3>
            <a-button type="link" size="small">查看全部</a-button>
          </div>
          <div class="card-content">
            <div class="empty-state">
              <div class="empty-icon">✅</div>
              <p>暂无待处理事项</p>
              <a-button type="primary" size="small">刷新</a-button>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { computed } from 'vue';

export default {
  name: 'Dashboard',
  setup() {
    const currentDate = computed(() => {
      const now = new Date();
      return now.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        weekday: 'long'
      });
    });

    return {
      currentDate
    };
  }
};
</script>

<style scoped>
.dashboard {
  padding: 0;
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.welcome-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  padding: 32px;
  margin-bottom: 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.3);
  position: relative;
  overflow: hidden;
}

.welcome-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);
  pointer-events: none;
}

.welcome-content {
  position: relative;
  z-index: 1;
}

.welcome-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.welcome-subtitle {
  font-size: 16px;
  margin: 0;
  opacity: 0.9;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.welcome-illustration {
  position: relative;
  z-index: 1;
}

.illustration-icon {
  font-size: 64px;
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

.stats-row {
  margin-bottom: 32px;
}

.stats-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stats-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--card-color), var(--card-color-light));
}

.stats-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.stats-card-primary {
  --card-color: #667eea;
  --card-color-light: #764ba2;
}

.stats-card-success {
  --card-color: #52c41a;
  --card-color-light: #73d13d;
}

.stats-card-warning {
  --card-color: #faad14;
  --card-color-light: #ffc53d;
}

.stats-card-purple {
  --card-color: #722ed1;
  --card-color-light: #9254de;
}

.stats-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  background: linear-gradient(135deg, var(--card-color), var(--card-color-light));
  flex-shrink: 0;
}

.stats-content {
  flex: 1;
}

.stats-number {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1;
  margin-bottom: 4px;
}

.stats-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
  font-weight: 500;
}

.stats-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
}

.stats-card-primary .stats-trend {
  color: #52c41a;
}

.stats-card-success .stats-trend {
  color: #52c41a;
}

.stats-card-warning .stats-trend {
  color: #faad14;
}

.stats-card-purple .stats-trend {
  color: #722ed1;
}

.charts-row {
  margin-bottom: 32px;
}

.chart-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  height: 400px;
}

.chart-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-container {
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-placeholder {
  text-align: center;
  color: #999;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.recent-row {
  margin-bottom: 32px;
}

.recent-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  height: 400px;
}

.recent-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-content {
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-state {
  text-align: center;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.empty-state p {
  margin-bottom: 16px;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .welcome-section {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }
  
  .welcome-title {
    font-size: 24px;
  }
  
  .stats-card {
    margin-bottom: 16px;
  }
  
  .chart-card,
  .recent-card {
    margin-bottom: 16px;
    height: 300px;
  }
  
  .chart-container {
    height: 220px;
  }
  
  .card-content {
    height: 220px;
  }
}

/* 深色主题适配 */
@media (prefers-color-scheme: dark) {
  .stats-card,
  .chart-card,
  .recent-card {
    background: #1f1f1f;
    color: white;
    border-color: #333;
  }
  
  .chart-header,
  .card-header {
    border-bottom-color: #333;
  }
  
  .chart-title,
  .card-title {
    color: white;
  }
  
  .stats-number {
    color: white;
  }
  
  .stats-title {
    color: #ccc;
  }
}
</style> 