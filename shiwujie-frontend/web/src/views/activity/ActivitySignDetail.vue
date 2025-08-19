<template>
  <div class="activity-sign-detail">
    <a-card :title="pageTitle" class="management-card">
      <!-- 活动信息头部 -->
      <div class="activity-header-section">
        <a-row :gutter="16">
          <a-col :span="16">
            <div class="activity-info">
              <h2>{{ activityInfo.activityName }}</h2>
              <div class="activity-meta">
                <span class="meta-item">
                  <ClockCircleOutlined />
                  {{ formatTime(activityInfo.startTime) }} - {{ formatTime(activityInfo.endTime) }}
                </span>
                <span class="meta-item">
                  <EnvironmentOutlined />
                  {{ activityInfo.location || '地点待定' }}
                </span>
                <span class="meta-item">
                  <UserOutlined />
                  报名人数: {{ signList.length }}
                </span>
              </div>
            </div>
          </a-col>
          <a-col :span="8" style="text-align: right">
            <a-button type="default" @click="goBack" size="large">
              <template #icon>
                <ArrowLeftOutlined />
              </template>
              返回
            </a-button>
            <a-button type="primary" @click="refreshData" size="large" style="margin-left: 8px;">
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
                <div class="stat-value">{{ totalCount }}</div>
                <div class="stat-title">总报名数</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card pending">
              <div class="stat-icon">📝</div>
              <div class="stat-content">
                <div class="stat-value">{{ pendingCount }}</div>
                <div class="stat-title">已报名</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card checked-in">
              <div class="stat-icon">✅</div>
              <div class="stat-content">
                <div class="stat-value">{{ checkedInCount }}</div>
                <div class="stat-title">已签到</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card checked-out">
              <div class="stat-icon">🏁</div>
              <div class="stat-content">
                <div class="stat-value">{{ checkedOutCount }}</div>
                <div class="stat-title">已签退</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <!-- 搜索和筛选 -->
      <div class="search-section">
        <a-row :gutter="16" align="middle">
          <a-col :span="8">
            <a-input-search
              v-model="searchForm.keyword"
              placeholder="搜索报名人姓名"
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
              placeholder="签到状态"
              allow-clear
              @change="handleSearch"
              size="large"
            >
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="已报名">已报名</a-select-option>
              <a-select-option value="已签到">已签到</a-select-option>
              <a-select-option value="已签退">已签退</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="8">
            <a-range-picker
              v-model="searchForm.dateRange"
              @change="handleSearch"
              placeholder="['开始日期', '结束日期']"
              size="large"
            />
          </a-col>
        </a-row>
      </div>

      <!-- 报名列表 -->
      <a-table
        :columns="columns"
        :data-source="filteredSignList"
        :loading="loading"
        :pagination="false"
        row-key="signId"
        class="management-table"
      >
        <template #bodyCell="{ column, record }">
          <!-- 报名时间列 -->
          <template v-if="column.key === 'signUpTime'">
            <div class="time-cell">
              {{ formatTime(record.signUpTime) }}
            </div>
          </template>

          <!-- 签到时间列 -->
          <template v-if="column.key === 'checkInTime'">
            <div class="time-cell">
              {{ record.checkInTime ? formatTime(record.checkInTime) : '-' }}
            </div>
          </template>

          <!-- 签到状态列 -->
          <template v-if="column.key === 'checkInStatus'">
            <a-tag :color="getStatusColor(getCheckInStatus(record))">
              {{ getStatusText(getCheckInStatus(record)) }}
            </a-tag>
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="viewDetail(record)">
                <template #icon>
                  <EyeOutlined />
                </template>
                查看
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
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
  EyeOutlined,
  ArrowLeftOutlined,
  ClockCircleOutlined,
  EnvironmentOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { activityApi } from '@/api/activity'
import { useAuthStore } from '@/stores/auth'

export default {
  name: 'ActivitySignDetail',
  components: {
    SearchOutlined,
    ReloadOutlined,
    EyeOutlined,
    ArrowLeftOutlined,
    ClockCircleOutlined,
    EnvironmentOutlined,
    UserOutlined
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const authStore = useAuthStore()
    const loading = ref(false)
    const signList = ref([])
    const activityInfo = ref({})

    const searchForm = reactive({
      keyword: '',
      status: '',
      dateRange: []
    })

    const columns = [
      {
        title: '报名人',
        dataIndex: 'userName',
        key: 'userName',
        width: 120
      },
      {
        title: '报名时间',
        key: 'signUpTime',
        width: 180
      },
      {
        title: '签到时间',
        key: 'checkInTime',
        width: 180
      },
      {
        title: '签到状态',
        dataIndex: 'checkInStatus',
        key: 'checkInStatus',
        width: 100
      },
      {
        title: '操作',
        key: 'action',
        width: 150,
        fixed: 'right'
      }
    ]

    // 计算页面标题
    const pageTitle = computed(() => {
      if (route.query.activityName) {
        return `报名管理 - ${route.query.activityName}`
      }
      return '报名管理'
    })

    // 计算统计信息
    const totalCount = computed(() => signList.value.length)
    const pendingCount = computed(() => 
      signList.value.filter(item => getCheckInStatus(item) === '已报名').length
    )
    const checkedInCount = computed(() => 
      signList.value.filter(item => getCheckInStatus(item) === '已签到').length
    )
    const checkedOutCount = computed(() => 
      signList.value.filter(item => getCheckInStatus(item) === '已签退').length
    )

    // 获取活动信息
    const fetchActivityInfo = async () => {
      try {
        const activityId = route.query.activityId
        if (!activityId) return

        const response = await activityApi.getActivityById(activityId)
        if (response) {
          activityInfo.value = response
        }
      } catch (error) {
        console.error('获取活动信息失败:', error)
      }
    }

    // 获取报名列表
    const fetchSignList = async () => {
      try {
        loading.value = true
        
        const activityId = route.query.activityId
        if (!activityId) return

        const params = {
          current: 1,
          pageSize: 1000, // 获取所有报名记录
          communityId: authStore.volunteerInfo?.communityId,
          activityId: activityId
        }
        
        const response = await activityApi.getActivitySignListPageVO(params)
        
        if (response && response.records) {
          // 为每个报名记录添加用户信息
          const enrichedRecords = response.records.map(record => ({
            ...record,
            userName: `用户${record.volunteerId || record.blindId}` // 临时用户名
          }))
          
          signList.value = enrichedRecords
        }
      } catch (error) {
        console.error('获取报名列表失败:', error)
        message.error(error.message || '获取报名列表失败')
      } finally {
        loading.value = false
      }
    }

    // 搜索处理
    const handleSearch = () => {
      // 客户端搜索，不需要重新请求
    }

    // 客户端筛选数据
    const filteredSignList = computed(() => {
      let filtered = signList.value
      
      // 按关键词筛选
      if (searchForm.keyword) {
        const keyword = searchForm.keyword.toLowerCase()
        filtered = filtered.filter(item => 
          item.userName && item.userName.toLowerCase().includes(keyword)
        )
      }
      
      // 按状态筛选
      if (searchForm.status) {
        filtered = filtered.filter(item => getCheckInStatus(item) === searchForm.status)
      }
      
      return filtered
    })

    // 刷新数据
    const refreshData = () => {
      fetchActivityInfo()
      fetchSignList()
    }

    // 返回上一页
    const goBack = () => {
      router.go(-1)
    }

    // 查看详情
    const viewDetail = (record) => {
      console.log('查看报名详情:', record)
      // 这里可以显示详情模态框
    }

    // 根据活动时间和当前时间判断签到状态
    const getCheckInStatus = (record) => {
      if (!activityInfo.value.startTime || !activityInfo.value.endTime) {
        return '未知状态'
      }
      
      const now = new Date()
      const startTime = new Date(activityInfo.value.startTime)
      const endTime = new Date(activityInfo.value.endTime)
      
      // 如果活动还没开始
      if (now < startTime) {
        return '已报名'
      }
      // 如果活动正在进行中
      else if (now >= startTime && now <= endTime) {
        return '已签到'
      }
      // 如果活动已结束
      else {
        return '已签退'
      }
    }

    // 获取状态颜色
    const getStatusColor = (status) => {
      const colorMap = {
        '已报名': 'orange',
        '已签到': 'blue',
        '已签退': 'green'
      }
      return colorMap[status] || 'default'
    }

    // 获取状态文本
    const getStatusText = (status) => {
      return status || '未知状态'
    }

    // 格式化时间
    const formatTime = (time) => {
      if (!time) return '-'
      return new Date(time).toLocaleString('zh-CN')
    }

    // 组件挂载时获取数据
    onMounted(() => {
      fetchActivityInfo()
      fetchSignList()
    })

    return {
      loading,
      signList,
      activityInfo,
      searchForm,
      columns,
      pageTitle,
      totalCount,
      pendingCount,
      checkedInCount,
      checkedOutCount,
      filteredSignList,
      handleSearch,
      refreshData,
      goBack,
      viewDetail,
      getCheckInStatus,
      getStatusColor,
      getStatusText,
      formatTime
    }
  }
}
</script>

<style scoped>
.activity-sign-detail {
  padding: 24px;
  background: #f5f5f5;
  min-height: 100vh;
}

.management-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.activity-header-section {
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}

.activity-info h2 {
  color: white;
  margin: 0 0 16px 0;
  font-size: 24px;
}

.activity-meta {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
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

.stat-card.checked-in {
  border-left-color: #1890ff;
}

.stat-card.checked-out {
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

.search-section {
  margin-bottom: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.search-section :deep(.ant-input),
.search-section :deep(.ant-select-selector),
.search-section :deep(.ant-picker) {
  border-radius: 6px;
}

.management-table {
  border-radius: 8px;
  overflow: hidden;
}

.management-table :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
  color: #262626;
}

.management-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f0f8ff;
}

.time-cell {
  font-size: 12px;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .activity-sign-detail {
    padding: 16px;
  }
  
  .activity-header-section {
    padding: 16px;
  }
  
  .stats-section {
    padding: 16px;
  }
  
  .search-section {
    padding: 16px;
  }
  
  .activity-meta {
    flex-direction: column;
    gap: 12px;
  }
}

/* 动画效果 */
.management-card {
  transition: all 0.3s ease;
}

.management-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.activity-header-section {
  transition: all 0.3s ease;
}

.activity-header-section:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
</style>
