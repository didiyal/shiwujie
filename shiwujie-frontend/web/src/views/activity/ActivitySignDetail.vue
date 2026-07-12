<template>
  <div class="activity-sign-detail">
    <a-card class="management-card" :bordered="false">
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
            <a-statistic title="总报名数" :value="totalCount" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="已报名" :value="pendingCount" :value-style="{ color: '#ff9500' }" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="已签到" :value="checkedInCount" :value-style="{ color: '#0071e3' }" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="已签退" :value="checkedOutCount" :value-style="{ color: '#34c759' }" />
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
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.management-card {
  background: var(--surface);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
}

/* 活动信息头部：清爽表面，去紫色渐变 */
.activity-header-section {
  margin-bottom: 18px;
  padding: 18px 20px;
  background: var(--bg);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
}
.activity-info h2 {
  color: var(--text);
  margin: 0 0 12px 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.activity-meta {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-2);
}

.stats-section {
  margin-bottom: 18px;
  padding: 16px 20px;
  background: var(--surface);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
}

.search-section {
  margin-bottom: 18px;
  padding: 16px;
  background: var(--bg);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
}

.time-cell {
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-2);
}

@media (max-width: 768px) {
  .activity-meta {
    flex-direction: column;
    gap: 8px;
  }
}
</style>

