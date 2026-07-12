<template>
  <div class="volunteer-list">
    <a-card class="management-card" :bordered="false">
      <!-- 搜索和操作栏 -->
      <div class="search-section">
        <a-row :gutter="[16, 12]" align="middle">
          <a-col :xs="24" :sm="12" :md="8">
            <a-input-search
              v-model="searchForm.keyword"
              placeholder="搜索姓名或手机号"
              @search="handleSearch"
              allow-clear
              size="large"
            >
              <template #prefix>
                <SearchOutlined />
              </template>
            </a-input-search>
          </a-col>
          <a-col :xs="12" :sm="6" :md="4">
            <a-select
              v-model="searchForm.gender"
              placeholder="性别"
              allow-clear
              @change="handleSearch"
              size="large"
            >
              <a-select-option value="">全部性别</a-select-option>
              <a-select-option :value="1">男</a-select-option>
              <a-select-option :value="2">女</a-select-option>
            </a-select>
          </a-col>
          <a-col :xs="12" :sm="6" :md="4">
            <a-select
              v-model="searchForm.onlineStatus"
              placeholder="在线状态"
              allow-clear
              @change="handleSearch"
              size="large"
            >
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option :value="1">在线</a-select-option>
              <a-select-option :value="0">离线</a-select-option>
              <a-select-option :value="2">忙碌</a-select-option>
            </a-select>
          </a-col>
          <a-col :xs="12" :sm="6" :md="4">
            <a-select
              v-model="searchForm.joinStatus"
              placeholder="加入状态"
              allow-clear
              @change="handleSearch"
              size="large"
            >
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option :value="1">已加入</a-select-option>
              <a-select-option :value="0">未加入</a-select-option>
            </a-select>
          </a-col>
          <a-col :xs="12" :sm="6" :md="4" style="text-align: right">
            <a-button type="primary" @click="handleRefresh" size="large">
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
        <a-row :gutter="[16, 12]">
          <a-col :xs="12" :sm="6">
            <a-statistic title="总人数" :value="totalCount" />
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-statistic title="在线" :value="onlineCount" :value-style="{ color: '#52c41a' }" />
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-statistic title="已加入" :value="joinedCount" :value-style="{ color: '#1890ff' }" />
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-statistic title="平均评分" :value="averageRating" :precision="1" :value-style="{ color: '#faad14' }" />
          </a-col>
        </a-row>
      </div>

      <!-- 志愿者列表 -->
      <a-table
        :columns="columns"
        :data-source="volunteerList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="volunteerId"
        class="management-table"
        :scroll="{ x: 800 }"
      >
        <template #bodyCell="{ column, record }">
          <!-- 性别列 -->
          <template v-if="column.key === 'gender'">
            <a-tag :color="record.gender === 1 ? 'blue' : 'pink'">
              {{ getGenderText(record.gender) }}
            </a-tag>
          </template>

          <!-- 在线状态列 -->
          <template v-if="column.key === 'onlineStatus'">
            <a-tag :color="getOnlineStatusColor(record.onlineStatus)">
              {{ getOnlineStatusText(record.onlineStatus) }}
            </a-tag>
          </template>

          <!-- 加入状态列 -->
          <template v-if="column.key === 'joinStatus'">
            <a-tag :color="record.isActivelyJoined === 1 ? 'green' : 'orange'">
              {{ getJoinStatusText(record.isActivelyJoined) }}
            </a-tag>
          </template>

          <!-- 管理员状态列 -->
          <template v-if="column.key === 'managerStatus'">
            <a-tag :color="(record.communityManager === '管理员' || record.communityManager === '注册人') ? 'gold' : 'default'">
              {{ (record.communityManager === '管理员' || record.communityManager === '注册人') ? record.communityManager : '员工' }}
            </a-tag>
          </template>

          <!-- 评分列 -->
          <template v-if="column.key === 'rating'">
            <a-rate :value="record.rating || 0" disabled :count="5" />
            <span style="margin-left: 8px; color: #666;">{{ record.rating || 0 }}</span>
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
              <a-button type="link" size="small" @click="editVolunteer(record)">
                <template #icon>
                  <EditOutlined />
                </template>
                编辑
              </a-button>
              <!-- 根据管理员状态显示不同操作 -->
              <template v-if="record.communityManager === '管理员' || record.communityManager === '注册人'">
                <!-- 已经是管理员或注册人，显示管理员标识 -->
                <a-tag color="gold" style="margin-left: 8px;">
                  <template #icon>
                    <CrownOutlined />
                  </template>
                  {{ record.communityManager }}
                </a-tag>
              </template>
              <template v-else-if="isRegistrant">
                <!-- 只有注册人才能看到设为管理员按钮 -->
                <a-button 
                  type="link" 
                  size="small" 
                  danger
                  @click="confirmSetAsManager(record)"
                  :loading="settingManagerId === record.volunteerId"
                >
                  <template #icon>
                    <CrownOutlined />
                  </template>
                  设为管理员
                </a-button>
              </template>
              
              <!-- 删除按钮：只有注册人和管理员可以看到，但注册人不能删除自己 -->
              <template v-if="(isRegistrant || isAdmin) && record.communityManager !== '注册人'">
                <a-button 
                  type="link" 
                  size="small" 
                  danger
                  @click="confirmRemoveVolunteer(record)"
                  :loading="removingVolunteerId === record.volunteerId"
                >
                  <template #icon>
                    <DeleteOutlined />
                  </template>
                  踢出
                </a-button>
              </template>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情模态框 -->
    <a-modal
      :open="detailModalVisible"
      @update:open="detailModalVisible = $event"
      title="志愿者详情"
      width="600px"
      :footer="null"
    >
      <div v-if="selectedVolunteer" class="detail-content">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="姓名">{{ selectedVolunteer.name }}</a-descriptions-item>
          <a-descriptions-item label="手机号">{{ selectedVolunteer.phone }}</a-descriptions-item>
          <a-descriptions-item label="性别">{{ getGenderText(selectedVolunteer.gender) }}</a-descriptions-item>
          <a-descriptions-item label="微信">{{ selectedVolunteer.wechatId || '未设置' }}</a-descriptions-item>
          <a-descriptions-item label="QQ">{{ selectedVolunteer.qqId || '未设置' }}</a-descriptions-item>
          <a-descriptions-item label="在线状态">{{ getOnlineStatusText(selectedVolunteer.onlineStatus) }}</a-descriptions-item>
          <a-descriptions-item label="加入状态">{{ getJoinStatusText(selectedVolunteer.isActivelyJoined) }}</a-descriptions-item>
          <a-descriptions-item label="社区身份">
            <a-tag v-if="selectedVolunteer.communityManager === '管理员' || selectedVolunteer.communityManager === '注册人'" color="gold">
              <template #icon>
                <CrownOutlined />
              </template>
              {{ selectedVolunteer.communityManager }}
            </a-tag>
            <span v-else style="color: #999;">员工</span>
          </a-descriptions-item>
          <a-descriptions-item label="帮助次数">{{ selectedVolunteer.helpCount || 0 }}</a-descriptions-item>
          <a-descriptions-item label="评分">{{ selectedVolunteer.rating || 0 }}</a-descriptions-item>
          <a-descriptions-item label="地址" :span="2">{{ selectedVolunteer.locationAddress || '未设置位置' }}</a-descriptions-item>
          <a-descriptions-item label="其他信息" :span="2">{{ selectedVolunteer.otherInfo || '无' }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { 
  SearchOutlined, 
  ReloadOutlined,
  EyeOutlined,
  EditOutlined,
  CrownOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { communityApi } from '@/api/community'
import { useAuthStore } from '@/stores/auth'

export default {
  name: 'VolunteerList',
  components: {
    SearchOutlined,
    ReloadOutlined,
    EyeOutlined,
    EditOutlined,
    CrownOutlined,
    DeleteOutlined
  },
  setup() {
    const authStore = useAuthStore()
    const loading = ref(false)
    const windowWidth = ref(window.innerWidth)
    const volunteerList = ref([])
    const detailModalVisible = ref(false)
    const selectedVolunteer = ref(null)
    const settingManagerId = ref(null)
    const removingVolunteerId = ref(null)

    const searchForm = reactive({
      keyword: '',
      gender: '',
      onlineStatus: '',
      joinStatus: ''
    })

    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showQuickJumper: true,
      showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`
    })

    const columns = [
      {
        title: '姓名',
        dataIndex: 'name',
        key: 'name',
        width: 120
      },
      {
        title: '手机号',
        dataIndex: 'phone',
        key: 'phone',
        width: 130
      },
      {
        title: '性别',
        dataIndex: 'gender',
        key: 'gender',
        width: 80
      },
      {
        title: '在线状态',
        dataIndex: 'onlineStatus',
        key: 'onlineStatus',
        width: 100
      },
      {
        title: '加入状态',
        key: 'joinStatus',
        width: 100
      },
      {
        title: '管理员状态',
        key: 'managerStatus',
        width: 120
      },
      {
        title: '评分',
        key: 'rating',
        width: 120
      },
      {
        title: '帮助次数',
        dataIndex: 'helpCount',
        key: 'helpCount',
        width: 100
      },
      {
        title: '地址',
        dataIndex: 'locationAddress',
        key: 'locationAddress',
        width: 200,
        ellipsis: true
      },
      {
        title: '操作',
        key: 'action',
        width: 380,
        fixed: 'right'
      }
    ]

    // 计算统计信息
    const totalCount = computed(() => pagination.total)
    const onlineCount = computed(() => 
      volunteerList.value.filter(item => item.onlineStatus === 1).length
    )
    const joinedCount = computed(() => 
      volunteerList.value.filter(item => item.isActivelyJoined === 1).length
    )
    const averageRating = computed(() => {
      const ratings = volunteerList.value.map(item => item.rating || 0)
      if (ratings.length === 0) return 0
      return (ratings.reduce((sum, rating) => sum + rating, 0) / ratings.length).toFixed(1)
    })

    // 检查当前用户是否是注册人（保留用于其他功能）
    const isRegistrant = computed(() => {
      return authStore.volunteer?.communityManager === '注册人'
    })

    // 检查当前用户是否是管理员
    const isAdmin = computed(() => {
      return authStore.volunteer?.communityManager === '管理员'
    })

    // 获取志愿者列表
    const fetchVolunteerList = async () => {
      try {
        loading.value = true
        
        // 获取用户注册的社区ID列表
        const userCommunities = JSON.parse(localStorage.getItem('userCommunities') || '[]')
        console.log('🔍 用户社区ID列表:', userCommunities)
        
        if (userCommunities.length === 0) {
          console.log('❌ 用户没有管理的社区')
          message.error('您没有管理的社区')
          volunteerList.value = []
          pagination.total = 0
          return
        }

        // 使用第一个社区ID查询志愿者
        const communityId = userCommunities[0]
        console.log('🔍 使用社区ID查询志愿者:', communityId)
        console.log('🔍 请求参数:', {
          communityId: String(communityId),
          current: pagination.current,
          pageSize: pagination.pageSize
        })
        
        const response = await communityApi.getCommunityVolunteers(communityId, pagination.current, pagination.pageSize)
        console.log('✅ 志愿者列表获取成功:', response)
        
        if (response && response.records) {
          // 处理大数字ID问题
          volunteerList.value = response.records.map(volunteer => {
            // 确保volunteerId是字符串格式
            if (volunteer.volunteerId && typeof volunteer.volunteerId === 'number') {
              volunteer.volunteerId = String(volunteer.volunteerId)
            }
            return volunteer
          })
          pagination.total = response.total
          pagination.current = response.current
          pagination.pageSize = response.size
        }
      } catch (error) {
        console.error('获取志愿者列表失败:', error)
        console.error('错误详情:', {
          message: error.message,
          response: error.response,
          stack: error.stack
        })
        message.error(error.message || '获取志愿者列表失败')
      } finally {
        loading.value = false
      }
    }

    // 搜索处理
    const handleSearch = () => {
      pagination.current = 1
      fetchVolunteerList()
    }

    // 表格变化处理
    const handleTableChange = (pag) => {
      pagination.current = pag.current
      pagination.pageSize = pag.pageSize
      fetchVolunteerList()
    }

    // 刷新数据
    const handleRefresh = () => {
      fetchVolunteerList()
    }

    // 查看详情
    const viewDetail = (record) => {
      selectedVolunteer.value = record
      detailModalVisible.value = true
    }

    // 编辑志愿者
    const editVolunteer = (record) => {
      message.info('编辑志愿者功能开发中...')
    }

    // 获取性别文本
    const getGenderText = (gender) => {
      switch (gender) {
        case 1: return '男'
        case 2: return '女'
        default: return '未知'
      }
    }

    // 获取在线状态文本
    const getOnlineStatusText = (status) => {
      switch (status) {
        case 0: return '离线'
        case 1: return '在线'
        case 2: return '忙碌'
        default: return '未知'
      }
    }

    // 获取在线状态颜色
    const getOnlineStatusColor = (status) => {
      switch (status) {
        case 0: return 'red'
        case 1: return 'green'
        case 2: return 'orange'
        default: return 'default'
      }
    }

    // 获取加入状态文本
    const getJoinStatusText = (status) => {
      return status === 1 ? '已加入' : '未加入'
    }

    // 确认设为管理员
    const confirmSetAsManager = (volunteer) => {
      Modal.confirm({
        title: '确认设为管理员',
        content: `确定要将志愿者"${volunteer.name}"设为社区管理员吗？`,
        okText: '确认',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => setAsManager(volunteer)
      })
    }

    // 设为管理员
    const setAsManager = async (volunteer) => {
      settingManagerId.value = volunteer.volunteerId
      try {
        console.log('🔍 开始设置管理员:', volunteer)
        
        // 获取用户注册的社区ID列表
        const userCommunities = JSON.parse(localStorage.getItem('userCommunities') || '[]')
        if (userCommunities.length === 0) {
          message.error('您没有管理的社区')
          return
        }

        const communityId = userCommunities[0]
        console.log('🔍 使用社区ID设置管理员:', communityId)
        
        const response = await communityApi.addCommunityManager(
          communityId, 
          volunteer.volunteerId, 
          '管理员' // 角色名称
        )
        
        console.log('✅ 设置管理员成功:', response)
        message.success('设置管理员成功')
        
        // 刷新志愿者列表
        await fetchVolunteerList()
        
      } catch (error) {
        console.error('设置管理员失败:', error)
        message.error('设置管理员失败')
      } finally {
        settingManagerId.value = null
      }
    }

    // 确认踢出志愿者
    const confirmRemoveVolunteer = (volunteer) => {
      Modal.confirm({
        title: '确认踢出志愿者',
        content: `确定要将志愿者"${volunteer.name}"从社区中移除吗？`,
        okText: '确认',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => removeVolunteer(volunteer)
      })
    }

    // 踢出志愿者
    const removeVolunteer = async (volunteer) => {
      removingVolunteerId.value = volunteer.volunteerId
      try {
        console.log('🔍 开始踢出志愿者:', volunteer)
        
        // 获取用户注册的社区ID列表
        const userCommunities = JSON.parse(localStorage.getItem('userCommunities') || '[]')
        if (userCommunities.length === 0) {
          message.error('您没有管理的社区')
          return
        }

        const communityId = userCommunities[0]
        console.log('🔍 使用社区ID踢出志愿者:', communityId)
        
        const response = await communityApi.removeVolunteerFromCommunity(
          communityId, 
          volunteer.volunteerId
        )
        
        console.log('✅ 踢出志愿者成功:', response)
        message.success('踢出志愿者成功')
        
        // 刷新志愿者列表
        await fetchVolunteerList()
        
      } catch (error) {
        console.error('踢出志愿者失败:', error)
        message.error('踢出志愿者失败')
      } finally {
        removingVolunteerId.value = null
      }
    }

    const handleResize = () => { windowWidth.value = window.innerWidth }
    onMounted(() => {
      window.addEventListener('resize', handleResize)
      fetchVolunteerList()
    })
    onUnmounted(() => {
      window.removeEventListener('resize', handleResize)
    })

    return {
      loading,
      windowWidth,
      volunteerList,
      searchForm,
      detailModalVisible,
      selectedVolunteer,
      settingManagerId,
      removingVolunteerId,
      pagination,
      columns,
      totalCount,
      onlineCount,
      joinedCount,
      averageRating,
      handleSearch,
      handleTableChange,
      handleRefresh,
      viewDetail,
      editVolunteer,
      confirmSetAsManager,
      setAsManager,
      confirmRemoveVolunteer,
      removeVolunteer,
      getGenderText,
      getOnlineStatusText,
      getOnlineStatusColor,
      getJoinStatusText,
      isRegistrant,
      isAdmin
    }
  }
}
</script>

<style scoped>
.volunteer-list {
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

.search-section {
  margin-bottom: 18px;
  padding: 16px;
  background: var(--bg);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
}

.stats-section {
  margin-bottom: 18px;
  padding: 16px 20px;
  background: var(--surface);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
}

.detail-content {
  padding: 8px 0;
}

@media (max-width: 768px) {
  .search-section {
    padding: 12px;
  }
  .stats-section {
    padding: 12px;
  }
  .stats-section .ant-col {
    margin-bottom: 8px;
  }
  :deep(.ant-table) {
    font-size: 12px;
  }
  :deep(.ant-modal) {
    max-width: calc(100vw - 32px) !important;
    margin: 16px;
  }
  :deep(.ant-modal-body) {
    padding: 16px;
  }
}

@media (max-width: 640px) {
  :deep(.ant-card-body) {
    padding: 12px;
  }
}
</style>
