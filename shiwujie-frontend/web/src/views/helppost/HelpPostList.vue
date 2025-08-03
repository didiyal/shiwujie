<template>
  <div class="helppost-list">
    <a-card title="求助帖管理" class="management-card">
      <!-- 搜索和筛选区域 -->
      <div class="search-section">
        <a-row :gutter="16" align="middle">
          <a-col :span="6">
            <a-input-search
              v-model="searchForm.keyword"
              placeholder="搜索求助内容"
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
              v-model="searchForm.postStatus"
              placeholder="求助状态"
              allow-clear
              @change="handleSearch"
              size="large"
            >
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="PENDING">待处理</a-select-option>
              <a-select-option value="PROCESSING">处理中</a-select-option>
              <a-select-option value="COMPLETED">已完成</a-select-option>
              <a-select-option value="CANCELLED">已取消</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="4">
            <a-select
              v-model="searchForm.communityId"
              placeholder="选择社区"
              allow-clear
              @change="handleSearch"
              size="large"
            >
              <a-select-option value="">全部社区</a-select-option>
              <a-select-option 
                v-for="community in communityList" 
                :key="community.communityId" 
                :value="community.communityId"
              >
                {{ community.communityName }}
              </a-select-option>
            </a-select>
          </a-col>
          <a-col :span="10" style="text-align: right">
            <a-space>
              <a-button type="primary" @click="handleSearch" size="large">
                <template #icon>
                  <SearchOutlined />
                </template>
                搜索
              </a-button>
              <a-button @click="handleRefresh" size="large">
                <template #icon>
                  <SyncOutlined />
                </template>
                刷新
              </a-button>
            </a-space>
          </a-col>
        </a-row>
      </div>

      <!-- 统计信息 -->
      <div class="stats-section">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-statistic title="总求助帖数" :value="totalCount" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="待处理" :value="pendingCount" :value-style="{ color: '#faad14' }" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="处理中" :value="processingCount" :value-style="{ color: '#1890ff' }" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="已完成" :value="completedCount" :value-style="{ color: '#52c41a' }" />
          </a-col>
        </a-row>
      </div>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="helpPostList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="helppostId"
        class="management-table"
      >
        <!-- 求助内容列 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'helpContent'">
            <div class="content-cell">
              <div class="content-text">{{ record.helpContent }}</div>
              <div class="content-location" v-if="record.helpLocation">
                <EnvironmentOutlined />
                {{ record.helpLocation }}
              </div>
            </div>
          </template>

          <!-- 状态列 -->
          <template v-if="column.key === 'postStatus'">
            <a-tag :color="getStatusColor(record.postStatus)">
              {{ getStatusText(record.postStatus) }}
            </a-tag>
          </template>

          <!-- 发布时间列 -->
          <template v-if="column.key === 'createTime'">
            <span>{{ formatTime(record.createTime) }}</span>
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
              <a-button 
                v-if="record.postStatus === 'PENDING'" 
                type="link" 
                size="small" 
                @click="assignVolunteer(record)"
              >
                <template #icon>
                  <UserAddOutlined />
                </template>
                分配志愿者
              </a-button>
              <a-button 
                v-if="record.postStatus === 'PROCESSING'" 
                type="link" 
                size="small" 
                @click="completeHelp(record)"
              >
                <template #icon>
                  <CheckOutlined />
                </template>
                标记完成
              </a-button>
              <a-button type="link" size="small" danger @click="deleteHelpPost(record)">
                <template #icon>
                  <DeleteOutlined />
                </template>
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 查看详情模态框 -->
    <a-modal
      v-model:open="detailModalVisible"
      title="求助帖详情"
      width="600px"
      :footer="null"
    >
      <div v-if="currentRecord" class="detail-content">
        <a-descriptions :column="1" bordered>
          <a-descriptions-item label="求助内容">
            {{ currentRecord.helpContent }}
          </a-descriptions-item>
          <a-descriptions-item label="求助位置" v-if="currentRecord.helpLocation">
            {{ currentRecord.helpLocation }}
          </a-descriptions-item>
          <a-descriptions-item label="发布状态">
            <a-tag :color="getStatusColor(currentRecord.postStatus)">
              {{ getStatusText(currentRecord.postStatus) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="发布时间">
            {{ formatTime(currentRecord.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="求助人ID">
            {{ currentRecord.blindId }}
          </a-descriptions-item>
          <a-descriptions-item label="志愿者ID" v-if="currentRecord.volunteerId">
            {{ currentRecord.volunteerId }}
          </a-descriptions-item>
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
  ReloadOutlined, 
  SyncOutlined, 
  EyeOutlined, 
  UserAddOutlined, 
  CheckOutlined, 
  DeleteOutlined,
  EnvironmentOutlined
} from '@ant-design/icons-vue'
import { communityApi } from '@/api/community'
import { useAuthStore } from '@/stores/auth'

export default {
  name: 'HelpPostList',
  components: {
    SearchOutlined,
    ReloadOutlined,
    SyncOutlined,
    EyeOutlined,
    UserAddOutlined,
    CheckOutlined,
    DeleteOutlined,
    EnvironmentOutlined
  },
  setup() {
    const authStore = useAuthStore()
    const loading = ref(false)
    const helpPostList = ref([])
    const communityList = ref([])
    const detailModalVisible = ref(false)
    const currentRecord = ref(null)

    const searchForm = reactive({
      keyword: '',
      postStatus: '',
      communityId: ''
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
        title: '求助内容', 
        dataIndex: 'helpContent', 
        key: 'helpContent',
        width: 300,
        ellipsis: true
      },
      { 
        title: '状态', 
        dataIndex: 'postStatus', 
        key: 'postStatus',
        width: 100
      },
      { 
        title: '社区ID', 
        dataIndex: 'communityId', 
        key: 'communityId',
        width: 120
      },
      { 
        title: '求助人ID', 
        dataIndex: 'blindId', 
        key: 'blindId',
        width: 120
      },
      { 
        title: '志愿者ID', 
        dataIndex: 'volunteerId', 
        key: 'volunteerId',
        width: 120
      },
      { 
        title: '发布时间', 
        dataIndex: 'createTime', 
        key: 'createTime',
        width: 180
      },
      { 
        title: '操作', 
        key: 'action', 
        width: 200,
        fixed: 'right'
      }
    ]

    // 计算统计信息
    const totalCount = computed(() => pagination.total)
    const pendingCount = computed(() => 
      helpPostList.value.filter(item => item.postStatus === 'PENDING').length
    )
    const processingCount = computed(() => 
      helpPostList.value.filter(item => item.postStatus === 'PROCESSING').length
    )
    const completedCount = computed(() => 
      helpPostList.value.filter(item => item.postStatus === 'COMPLETED').length
    )

    // 获取求助帖列表
    const fetchHelpPostList = async () => {
      try {
        loading.value = true
        
        // 构建请求参数
        const params = {
          current: pagination.current,
          pageSize: pagination.pageSize,
          postStatus: searchForm.postStatus || undefined
        }
        
        // 如果有选择特定社区，使用选择的社区ID
        // 否则使用当前登录用户的社区ID
        if (searchForm.communityId) {
          params.communityId = searchForm.communityId
        } else if (authStore.volunteerInfo?.communityId) {
          params.communityId = authStore.volunteerInfo.communityId
        }
        
        console.log('请求参数:', params)
        const response = await communityApi.getHelpPostList(params)
        console.log('求助帖列表响应:', response)
        
        if (response && response.records) {
          helpPostList.value = response.records
          pagination.total = response.total
          pagination.current = response.current
          pagination.pageSize = response.size
        }
      } catch (error) {
        console.error('获取求助帖列表失败:', error)
        message.error('获取求助帖列表失败')
      } finally {
        loading.value = false
      }
    }

    // 获取社区列表
    const fetchCommunityList = async () => {
      try {
        // 这里可以调用获取社区列表的API
        // 暂时使用模拟数据，包含当前用户的社区
        const currentUserCommunity = authStore.volunteerInfo?.communityId
        communityList.value = [
          { communityId: currentUserCommunity, communityName: '当前社区' },
          { communityId: '1', communityName: '示例社区1' },
          { communityId: '2', communityName: '示例社区2' }
        ]
        
        // 设置当前用户的社区为默认值
        if (currentUserCommunity && !searchForm.communityId) {
          searchForm.communityId = currentUserCommunity
        }
      } catch (error) {
        console.error('获取社区列表失败:', error)
      }
    }

    // 搜索处理
    const handleSearch = () => {
      pagination.current = 1
      fetchHelpPostList()
    }



    // 刷新
    const handleRefresh = () => {
      fetchHelpPostList()
    }

    // 表格变化处理
    const handleTableChange = (pag) => {
      pagination.current = pag.current
      pagination.pageSize = pag.pageSize
      fetchHelpPostList()
    }

    // 查看详情
    const viewDetail = (record) => {
      currentRecord.value = record
      detailModalVisible.value = true
    }

    // 分配志愿者
    const assignVolunteer = (record) => {
      Modal.confirm({
        title: '分配志愿者',
        content: '确定要为这个求助帖分配志愿者吗？',
        onOk: async () => {
          try {
            // 这里调用分配志愿者的API
            message.success('志愿者分配成功')
            fetchHelpPostList()
          } catch (error) {
            message.error('分配失败')
          }
        }
      })
    }

    // 标记完成
    const completeHelp = (record) => {
      Modal.confirm({
        title: '标记完成',
        content: '确定要将这个求助帖标记为已完成吗？',
        onOk: async () => {
          try {
            // 这里调用标记完成的API
            message.success('标记完成成功')
            fetchHelpPostList()
          } catch (error) {
            message.error('操作失败')
          }
        }
      })
    }

    // 删除求助帖
    const deleteHelpPost = (record) => {
      Modal.confirm({
        title: '删除求助帖',
        content: '确定要删除这个求助帖吗？此操作不可恢复。',
        okType: 'danger',
        onOk: async () => {
          try {
            // 这里调用删除API
            message.success('删除成功')
            fetchHelpPostList()
          } catch (error) {
            message.error('删除失败')
          }
        }
      })
    }

    // 获取状态颜色
    const getStatusColor = (status) => {
      const colorMap = {
        'PENDING': 'orange',
        'PROCESSING': 'blue',
        'COMPLETED': 'green',
        'CANCELLED': 'red'
      }
      return colorMap[status] || 'default'
    }

    // 获取状态文本
    const getStatusText = (status) => {
      const textMap = {
        'PENDING': '待处理',
        'PROCESSING': '处理中',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消'
      }
      return textMap[status] || status
    }

    // 格式化时间
    const formatTime = (time) => {
      if (!time) return '-'
      return new Date(time).toLocaleString('zh-CN')
    }

    // 组件挂载时获取数据
    onMounted(() => {
      fetchCommunityList()
      fetchHelpPostList()
    })

    return {
      loading,
      helpPostList,
      communityList,
      searchForm,
      pagination,
      columns,
      detailModalVisible,
      currentRecord,
      totalCount,
      pendingCount,
      processingCount,
      completedCount,
      handleSearch,
      handleRefresh,
      handleTableChange,
      viewDetail,
      assignVolunteer,
      completeHelp,
      deleteHelpPost,
      getStatusColor,
      getStatusText,
      formatTime
    }
  }
}
</script>

<style scoped>
.helppost-list {
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

.content-cell {
  max-width: 300px;
}

.content-text {
  font-size: 14px;
  color: #262626;
  line-height: 1.5;
  margin-bottom: 4px;
  word-break: break-word;
}

.content-location {
  font-size: 12px;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-content {
  max-height: 400px;
  overflow-y: auto;
}

.detail-content :deep(.ant-descriptions-item-label) {
  font-weight: 600;
  color: #262626;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .helppost-list {
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