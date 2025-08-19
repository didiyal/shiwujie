<template>
  <div class="activity-sign">
    <a-card :title="pageTitle" class="management-card">
      <!-- 搜索和操作栏 -->
      <div class="search-section">
        <a-row :gutter="16" align="middle">
          <a-col :span="8">
            <a-input-search
              v-model="searchForm.keyword"
              placeholder="搜索活动名称"
              @search="handleSearch"
              allow-clear
              size="large"
            >
              <template #prefix>
                <SearchOutlined />
              </template>
            </a-input-search>
          </a-col>
          <a-col :span="8">
            <a-select
              v-model="searchForm.status"
              placeholder="活动状态"
              allow-clear
              @change="handleSearch"
              size="large"
            >
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="未开始">未开始</a-select-option>
              <a-select-option value="进行中">进行中</a-select-option>
              <a-select-option value="已结束">已结束</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="4" style="text-align: right">
            <a-button type="default" @click="goBack" size="large">
              <template #icon>
                <ArrowLeftOutlined />
              </template>
              返回
            </a-button>
          </a-col>
          <a-col :span="4" style="text-align: right">
            <a-button type="primary" @click="refreshData" size="large">
              <template #icon>
                <ReloadOutlined />
              </template>
              刷新
            </a-button>
          </a-col>
        </a-row>
      </div>

      <!-- 统计信息 -->
      <div class="stats-section">
        <a-row :gutter="16">
          <a-col :span="6">
            <div class="stat-card total">
              <div class="stat-icon">📊</div>
              <div class="stat-content">
                <div class="stat-value">{{ totalActivities }}</div>
                <div class="stat-title">总活动数</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card pending">
              <div class="stat-icon">📝</div>
              <div class="stat-content">
                <div class="stat-value">{{ pendingActivities }}</div>
                <div class="stat-title">未开始</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card active">
              <div class="stat-icon">✅</div>
              <div class="stat-content">
                <div class="stat-value">{{ activeActivities }}</div>
                <div class="stat-title">进行中</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card finished">
              <div class="stat-icon">🏁</div>
              <div class="stat-content">
                <div class="stat-value">{{ finishedActivities }}</div>
                <div class="stat-title">已结束</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <!-- 活动卡片列表 -->
      <div class="activities-section">
        <a-row :gutter="16">
          <a-col 
            :span="8" 
            v-for="activity in filteredActivities" 
            :key="activity.activityId"
            class="activity-col"
          >
            <div class="activity-card" @click="viewActivitySign(activity)">
              <div class="activity-header">
                <div class="activity-status">
                  <a-tag :color="getActivityStatusColor(activity)">
                    {{ getActivityStatusText(activity) }}
                  </a-tag>
                </div>
                <div class="activity-time">
                  <ClockCircleOutlined />
                  {{ formatTime(activity.startTime) }}
                </div>
              </div>
              
              <div class="activity-title">
                {{ activity.activityName }}
              </div>
              
              <div class="activity-info">
                <div class="info-item">
                  <UserOutlined />
                  <span>报名人数: {{ activity.signUpCount || 0 }}</span>
                </div>
                <div class="info-item">
                  <EnvironmentOutlined />
                  <span>{{ activity.location || '地点待定' }}</span>
                </div>
              </div>
              
              <div class="activity-footer">
                <a-button type="primary" size="small" @click.stop="viewActivitySign(activity)">
                  查看报名
                </a-button>
              </div>
            </div>
          </a-col>
        </a-row>
        
        <!-- 空状态 -->
        <div v-if="filteredActivities.length === 0 && !loading" class="empty-state">
          <Empty description="暂无活动数据" />
        </div>
      </div>
    </a-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  SearchOutlined, 
  ReloadOutlined,
  ArrowLeftOutlined,
  ClockCircleOutlined,
  UserOutlined,
  EnvironmentOutlined
} from '@ant-design/icons-vue'
import { Empty } from 'ant-design-vue'
import { activityApi } from '@/api/activity'
import { useAuthStore } from '@/stores/auth'

export default {
  name: 'ActivitySign',
  components: {
    SearchOutlined,
    ReloadOutlined,
    ArrowLeftOutlined,
    ClockCircleOutlined,
    UserOutlined,
    EnvironmentOutlined,
    Empty
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const authStore = useAuthStore()
    const loading = ref(false)
    const activities = ref([])

    const searchForm = reactive({
      keyword: '',
      status: ''
    })

    // 计算页面标题
    const pageTitle = computed(() => {
      return '活动报名管理'
    })

    // 计算统计信息
    const totalActivities = computed(() => activities.value.length)
    const pendingActivities = computed(() => 
      activities.value.filter(item => getActivityStatus(item) === '未开始').length
    )
    const activeActivities = computed(() => 
      activities.value.filter(item => getActivityStatus(item) === '进行中').length
    )
    const finishedActivities = computed(() => 
      activities.value.filter(item => getActivityStatus(item) === '已结束').length
    )

    // 获取活动列表
    const fetchActivities = async () => {
      try {
        loading.value = true
        
        const response = await activityApi.getActivityList(1, 100, {
          communityId: authStore.volunteerInfo?.communityId
        })
        
        if (response && response.records) {
          activities.value = response.records
        }
      } catch (error) {
        console.error('获取活动列表失败:', error)
        message.error(error.message || '获取活动列表失败')
      } finally {
        loading.value = false
      }
    }

    // 搜索处理
    const handleSearch = () => {
      // 客户端搜索，不需要重新请求
    }

    // 客户端筛选数据
    const filteredActivities = computed(() => {
      let filtered = activities.value
      
      // 按关键词筛选
      if (searchForm.keyword) {
        const keyword = searchForm.keyword.toLowerCase()
        filtered = filtered.filter(item => 
          item.activityName && item.activityName.toLowerCase().includes(keyword)
        )
      }
      
      // 按状态筛选
      if (searchForm.status) {
        filtered = filtered.filter(item => getActivityStatus(item) === searchForm.status)
      }
      
      return filtered
    })

    // 刷新数据
    const refreshData = () => {
      fetchActivities()
    }

    // 返回上一页
    const goBack = () => {
      router.go(-1)
    }

    // 查看活动报名信息
    const viewActivitySign = (activity) => {
      // 跳转到该活动的报名详情页面
      router.push({
        path: '/activity-sign-detail',
        query: {
          activityId: activity.activityId,
          activityName: activity.activityName
        }
      })
    }

    // 根据活动时间和当前时间判断活动状态
    const getActivityStatus = (activity) => {
      if (!activity.startTime || !activity.endTime) {
        return '未知状态'
      }
      
      const now = new Date()
      const startTime = new Date(activity.startTime)
      const endTime = new Date(activity.endTime)
      
      // 如果活动还没开始
      if (now < startTime) {
        return '未开始'
      }
      // 如果活动正在进行中
      else if (now >= startTime && now <= endTime) {
        return '进行中'
      }
      // 如果活动已结束
      else {
        return '已结束'
      }
    }

    // 获取状态颜色
    const getActivityStatusColor = (activity) => {
      const status = getActivityStatus(activity)
      const colorMap = {
        '未开始': 'orange',
        '进行中': 'blue',
        '已结束': 'green'
      }
      return colorMap[status] || 'default'
    }

    // 获取状态文本
    const getActivityStatusText = (activity) => {
      return getActivityStatus(activity)
    }

    // 格式化时间
    const formatTime = (time) => {
      if (!time) return '-'
      return new Date(time).toLocaleDateString('zh-CN')
    }

    // 组件挂载时获取数据
    onMounted(() => {
      fetchActivities()
    })

    return {
      loading,
      activities,
      searchForm,
      pageTitle,
      totalActivities,
      pendingActivities,
      activeActivities,
      finishedActivities,
      filteredActivities,
      handleSearch,
      refreshData,
      goBack,
      viewActivitySign,
      getActivityStatusColor,
      getActivityStatusText,
      formatTime
    }
  }
}
</script>

<style scoped>
.activity-sign {
  padding: 24px;
  background: #f5f5f5;
  min-height: 100vh;
}

.management-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-section {
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}

.search-section :deep(.ant-input),
.search-section :deep(.ant-select-selector) {
  border-radius: 6px;
}

.stats-section {
  margin-bottom: 24px;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  border-left: 4px solid;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.stat-card.total {
  border-left-color: #722ed1;
}

.stat-card.pending {
  border-left-color: #faad14;
}

.stat-card.active {
  border-left-color: #1890ff;
}

.stat-card.finished {
  border-left-color: #52c41a;
}

.stat-icon {
  font-size: 32px;
  margin-right: 16px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #262626;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-title {
  font-size: 14px;
  color: #8c8c8c;
  font-weight: 500;
}

.activities-section {
  margin-top: 24px;
}

.activity-col {
  margin-bottom: 16px;
}

.activity-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid #e8e8e8;
}

.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  border-color: #1890ff;
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.activity-status {
  flex: 1;
}

.activity-time {
  color: #8c8c8c;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.activity-title {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
  line-height: 1.4;
}

.activity-info {
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #595959;
  font-size: 14px;
}

.activity-footer {
  text-align: center;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #8c8c8c;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .activity-sign {
    padding: 16px;
  }
  
  .search-section {
    padding: 16px;
  }
  
  .stats-section {
    padding: 16px;
  }
  
  .activity-col {
    span: 24;
  }
}

/* 动画效果 */
.management-card {
  transition: all 0.3s ease;
}

.management-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.search-section {
  transition: all 0.3s ease;
}

.search-section:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
</style> 