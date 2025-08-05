<template>
  <div class="blind-list">
    <a-card title="视障人士管理" class="management-card">
      <!-- 搜索和操作栏 -->
      <div class="search-section">
        <a-row :gutter="16" align="middle">
          <a-col :span="8">
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
          <a-col :span="4">
            <a-select
              v-model="searchForm.gender"
              placeholder="性别"
              allow-clear
              @change="handleSearch"
              size="large"
            >
              <a-select-option value="">全部性别</a-select-option>
              <a-select-option :value="0">男</a-select-option>
              <a-select-option :value="1">女</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="4">
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
          <a-col :span="4" style="text-align: right">
            <a-button type="primary" @click="showAddModal" size="large">
              <template #icon>
                <PlusOutlined />
              </template>
              添加视障人士
            </a-button>
          </a-col>
        </a-row>
      </div>

      <!-- 统计信息 -->
      <div class="stats-section">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-statistic title="总人数" :value="totalCount" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="已加入" :value="joinedCount" :value-style="{ color: '#52c41a' }" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="已认证身份证" :value="idCardCount" :value-style="{ color: '#1890ff' }" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="已认证残疾证" :value="disabilityCardCount" :value-style="{ color: '#faad14' }" />
          </a-col>
        </a-row>
      </div>

      <!-- 视障人士列表 -->
      <a-table
        :columns="columns"
        :data-source="blindList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="blindId"
        class="management-table"
      >
        <template #bodyCell="{ column, record }">
          <!-- 性别列 -->
          <template v-if="column.key === 'gender'">
            <a-tag :color="record.gender === 0 ? 'blue' : 'pink'">
              {{ record.getGenderText() }}
            </a-tag>
          </template>

          <!-- 认证状态列 -->
          <template v-if="column.key === 'certification'">
            <div class="certification-cell">
              <a-tag :color="record.isIdCard ? 'green' : 'red'" size="small">
                {{ record.getIdCardText() }}
              </a-tag>
              <a-tag :color="record.isDisabilityCard ? 'green' : 'red'" size="small">
                {{ record.getDisabilityCardText() }}
              </a-tag>
            </div>
          </template>

          <!-- 加入状态列 -->
          <template v-if="column.key === 'joinStatus'">
            <a-tag :color="record.isActivelyJoined === 1 ? 'green' : 'orange'">
              {{ record.getJoinStatusText() }}
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
              <a-button type="link" size="small" @click="editBlind(record)">
                <template #icon>
                  <EditOutlined />
                </template>
                编辑
              </a-button>
              <a-button type="link" size="small" @click="viewHelpHistory(record)">
                <template #icon>
                  <HistoryOutlined />
                </template>
                求助记录
              </a-button>
              <!-- 踢出按钮：只有注册人和管理员可以看到 -->
              <template v-if="isRegistrant || isAdmin">
                <a-button 
                  type="link" 
                  size="small" 
                  danger
                  @click="confirmRemoveBlind(record)"
                  :loading="removingBlindId === record.blindId"
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
      title="视障人士详情"
      width="600px"
      :footer="null"
    >
      <div v-if="selectedBlind" class="detail-content">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="姓名">{{ selectedBlind.name }}</a-descriptions-item>
          <a-descriptions-item label="手机号">{{ selectedBlind.phone }}</a-descriptions-item>
          <a-descriptions-item label="性别">{{ selectedBlind.getGenderText() }}</a-descriptions-item>
          <a-descriptions-item label="微信">{{ selectedBlind.wechatId || '未设置' }}</a-descriptions-item>
          <a-descriptions-item label="QQ">{{ selectedBlind.qqId || '未设置' }}</a-descriptions-item>
          <a-descriptions-item label="身份证认证">{{ selectedBlind.getIdCardText() }}</a-descriptions-item>
          <a-descriptions-item label="残疾证认证">{{ selectedBlind.getDisabilityCardText() }}</a-descriptions-item>
          <a-descriptions-item label="加入状态">{{ selectedBlind.getJoinStatusText() }}</a-descriptions-item>
          <a-descriptions-item label="求助次数">{{ selectedBlind.helpRequestCount }}</a-descriptions-item>
          <a-descriptions-item label="地址" :span="2">{{ selectedBlind.getFullAddress() }}</a-descriptions-item>
          <a-descriptions-item label="其他信息" :span="2">{{ selectedBlind.otherInfo || '无' }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { 
  SearchOutlined, 
  PlusOutlined,
  EyeOutlined,
  EditOutlined,
  HistoryOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { userApi } from '@/api/user'
import { BlindModel } from '@/models/blind'
import { useAuthStore } from '@/stores/auth'

export default {
  name: 'BlindList',
  components: {
    SearchOutlined,
    PlusOutlined,
    EyeOutlined,
    EditOutlined,
    HistoryOutlined,
    DeleteOutlined
  },
  setup() {
    const authStore = useAuthStore()
    const loading = ref(false)
    const blindList = ref([])
    const detailModalVisible = ref(false)
    const selectedBlind = ref(null)
    const removingBlindId = ref(null)

    const searchForm = reactive({
      keyword: '',
      gender: '',
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

    // 检查当前用户是否是注册人
    const isRegistrant = computed(() => {
      return authStore.volunteer?.communityManager === '注册人'
    })

    // 检查当前用户是否是管理员
    const isAdmin = computed(() => {
      return authStore.volunteer?.communityManager === '管理员'
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
        title: '认证状态',
        key: 'certification',
        width: 120
      },
      {
        title: '加入状态',
        key: 'joinStatus',
        width: 100
      },
      {
        title: '求助次数',
        dataIndex: 'helpRequestCount',
        key: 'helpRequestCount',
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
        width: 280,
        fixed: 'right'
      }
    ]

    // 计算统计信息
    const totalCount = computed(() => pagination.total)
    const joinedCount = computed(() => 
      blindList.value.filter(item => item.isActivelyJoined === 1).length
    )
    const idCardCount = computed(() => 
      blindList.value.filter(item => item.isIdCard).length
    )
    const disabilityCardCount = computed(() => 
      blindList.value.filter(item => item.isDisabilityCard).length
    )

    // 获取视障人士列表
    const fetchBlindList = async () => {
      try {
        loading.value = true
        
        // 构建请求参数
        const params = {
          current: pagination.current,
          pageSize: pagination.pageSize,
          communityId: authStore.volunteerInfo?.communityId
        }
        
        console.log('请求参数:', params)
        const response = await userApi.getBlindList(params)
        console.log('视障人士列表响应:', response)
        
        if (response && response.records) {
          // 处理大数字ID问题
          blindList.value = response.records.map(blind => {
            // 确保blindId是字符串格式
            if (blind.blindId && typeof blind.blindId === 'number') {
              blind.blindId = String(blind.blindId)
            }
            return new BlindModel(blind)
          })
          pagination.total = response.total
          pagination.current = response.current
          pagination.pageSize = response.size
        }
      } catch (error) {
        console.error('获取视障人士列表失败:', error)
        console.error('错误详情:', {
          message: error.message,
          response: error.response,
          stack: error.stack
        })
        message.error(error.message || '获取视障人士列表失败')
      } finally {
        loading.value = false
      }
    }

    // 搜索处理
    const handleSearch = () => {
      pagination.current = 1
      fetchBlindList()
    }

    // 表格变化处理
    const handleTableChange = (pag) => {
      pagination.current = pag.current
      pagination.pageSize = pag.pageSize
      fetchBlindList()
    }

    // 显示添加模态框
    const showAddModal = () => {
      message.info('添加视障人士功能开发中...')
    }

    // 查看详情
    const viewDetail = (record) => {
      selectedBlind.value = record
      detailModalVisible.value = true
    }

    // 编辑视障人士
    const editBlind = (record) => {
      message.info('编辑视障人士功能开发中...')
    }

    // 查看求助记录
    const viewHelpHistory = (record) => {
      message.info('查看求助记录功能开发中...')
    }

    // 删除视障人士
    const deleteBlind = (record) => {
      Modal.confirm({
        title: '删除视障人士',
        content: `确定要删除视障人士"${record.name}"吗？此操作不可恢复。`,
        okType: 'danger',
        onOk: async () => {
          message.info('删除视障人士功能开发中...')
        }
      })
    }

    // 踢出视障人士
    const confirmRemoveBlind = (blind) => {
      Modal.confirm({
        title: '确认踢出视障人士',
        content: `确定要将视障人士"${blind.name}"从社区中移除吗？`,
        okText: '确认',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => removeBlind(blind)
      })
    }

    // 踢出视障人士
    const removeBlind = async (blind) => {
      removingBlindId.value = blind.blindId
      try {
        console.log('🔍 开始踢出视障人士:', blind)
        
        // 获取用户注册的社区ID列表
        const userCommunities = JSON.parse(localStorage.getItem('userCommunities') || '[]')
        if (userCommunities.length === 0) {
          message.error('您没有管理的社区')
          return
        }

        const communityId = userCommunities[0]
        console.log('🔍 使用社区ID踢出视障人士:', communityId)
        
        const response = await userApi.removeBlindFromCommunity(
          blind.blindId,
          communityId
        )
        
        console.log('✅ 踢出视障人士成功:', response)
        message.success('踢出视障人士成功')
        
        // 刷新视障人士列表
        await fetchBlindList()
        
      } catch (error) {
        console.error('踢出视障人士失败:', error)
        message.error('踢出视障人士失败')
      } finally {
        removingBlindId.value = null
      }
    }

    // 组件挂载时获取数据
    onMounted(() => {
      fetchBlindList()
    })

    return {
      loading,
      blindList,
      searchForm,
      detailModalVisible,
      selectedBlind,
      removingBlindId,
      pagination,
      columns,
      totalCount,
      joinedCount,
      idCardCount,
      disabilityCardCount,
      handleSearch,
      handleTableChange,
      showAddModal,
      viewDetail,
      editBlind,
      viewHelpHistory,
      deleteBlind,
      confirmRemoveBlind,
      removeBlind,
      isRegistrant,
      isAdmin
    }
  }
}
</script>

<style scoped>
.blind-list {
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

.certification-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-content {
  padding: 16px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .blind-list {
    padding: 16px;
  }
  
  .search-section {
    padding: 16px;
  }
  
  .stats-section {
    padding: 16px;
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