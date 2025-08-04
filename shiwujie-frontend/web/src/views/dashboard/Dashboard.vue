<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <a-row :gutter="24" class="stats-row">
      <a-col :span="6">
        <a-card class="stats-card">
          <a-statistic
            title="总社区数"
            :value="0"
            :value-style="{ color: '#1890ff' }"
          >
            <template #prefix>
              🏘️
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stats-card">
          <a-statistic
            title="活跃活动"
            :value="0"
            :value-style="{ color: '#52c41a' }"
          >
            <template #prefix>
              📅
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stats-card">
          <a-statistic
            title="待审核申请"
            :value="0"
            :value-style="{ color: '#faad14' }"
          >
            <template #prefix>
              ⏰
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stats-card">
          <a-statistic
            title="总用户数"
            :value="0"
            :value-style="{ color: '#722ed1' }"
          >
            <template #prefix>
              👥
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="24" class="charts-row">
      <a-col :span="12">
        <a-card title="社区活跃度趋势" class="chart-card">
          <div class="chart-container">
            <a-empty description="暂无数据" />
          </div>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="活动参与情况" class="chart-card">
          <div class="chart-container">
            <a-empty description="暂无数据" />
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 最近活动 -->
    <a-row :gutter="24" class="recent-row">
      <a-col :span="12">
        <a-card title="最近活动" class="recent-card">
          <a-empty description="暂无活动数据" />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="待处理事项" class="recent-card">
          <a-empty description="暂无待处理事项" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script>
export default {
  name: 'Dashboard'
};
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stats-row {
  margin-bottom: 24px;
}

.stats-card {
  text-align: center;
}

.charts-row {
  margin-bottom: 24px;
}

.chart-card {
  height: 400px;
}

.chart-container {
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.recent-row {
  margin-bottom: 24px;
}

.recent-card {
  height: 400px;
}
</style> 