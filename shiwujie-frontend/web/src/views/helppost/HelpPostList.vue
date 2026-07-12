<template>
  <div class="helppost-list">
    <a-card :bordered="false">
      <div class="action-bar">
        <a-row :gutter="[16, 12]" align="middle">
          <a-col :xs="24" :sm="12" :md="8">
            <a-input-search
              v-model="searchForm.keyword"
              placeholder="搜索求助内容"
              @search="handleSearch"
              allow-clear
            />
          </a-col>
          <a-col :xs="24" :sm="12" :md="4">
            <a-select
              v-model="searchForm.status"
              placeholder="状态"
              allow-clear
              @change="handleSearch"
            >
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="0">待处理</a-select-option>
              <a-select-option value="1">处理中</a-select-option>
              <a-select-option value="2">已完成</a-select-option>
            </a-select>
          </a-col>
          <a-col :xs="24" :sm="24" :md="12" style="text-align: right">
            <a-button @click="handleRefresh">刷新</a-button>
          </a-col>
        </a-row>
      </div>

      <a-table
        :columns="columns"
        :data-source="helppostList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="helppostId"
        :scroll="{ x: 800 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.postStatus)">
              {{ getStatusText(record.postStatus) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="viewDetail(record)">
                查看
              </a-button>
              <!-- 删除功能仅限盲人用户使用，管理端隐藏 -->
              <!-- <a-button type="link" size="small" danger @click="deleteHelpPost(record)">
                删除
              </a-button> -->
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
    
    <!-- 求助帖详情弹窗 -->
    <a-modal
      :open="detailModalVisible"
      @update:open="detailModalVisible = $event"
      title="求助帖详情"
      :width="windowWidth < 768 ? '100%' : 600"
      :footer="null"
      :confirm-loading="detailLoading"
    >
      <div v-if="currentDetail" class="detail-content">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="求助帖ID" span="1">
            {{ currentDetail.helppostId }}
          </a-descriptions-item>
          <a-descriptions-item label="状态" span="1">
            <a-tag :color="getStatusColor(currentDetail.postStatus)">
              {{ getStatusText(currentDetail.postStatus) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="盲人ID" span="1">
            {{ currentDetail.blindId }}
          </a-descriptions-item>
          <a-descriptions-item label="志愿者ID" span="1">
            {{ currentDetail.volunteerId || '暂无响应' }}
          </a-descriptions-item>
          <a-descriptions-item label="社区ID" span="1">
            {{ currentDetail.communityId }}
          </a-descriptions-item>
          <a-descriptions-item label="求助地点" span="1">
            {{ currentDetail.helpLocation || '未指定' }}
          </a-descriptions-item>
          <a-descriptions-item label="求助内容" span="2">
            <div class="help-content">
              {{ currentDetail.helpContent }}
            </div>
          </a-descriptions-item>
        </a-descriptions>
      </div>
      <div v-else-if="detailLoading" class="loading-content">
        <a-spin size="large" />
        <p style="margin-top: 16px; text-align: center;">加载中...</p>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { communityApi } from '@/api/community'

export default {
  name: 'HelpPostList',
  setup() {
    const loading = ref(false)
    const windowWidth = ref(window.innerWidth)
    const helppostList = ref([])
    const detailModalVisible = ref(false)
    const currentDetail = ref(null)
    const detailLoading = ref(false)
    
    const searchForm = reactive({
      keyword: '',
      status: ''
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
      { title: '发布人', dataIndex: 'userName', key: 'userName', width: 120 },
      { title: '求助内容', dataIndex: 'helpContent', key: 'helpContent', width: 300, ellipsis: true },
      { title: '求助地点', dataIndex: 'helpLocation', key: 'helpLocation', width: 150 },
      { title: '发布时间', dataIndex: 'createTime', key: 'createTime', width: 150 },
      { title: '状态', key: 'status', width: 100 },
      { title: '操作', key: 'action', width: 150, fixed: 'right' }
    ]

    // 获取求助帖列表
    const fetchHelppostList = async () => {
      try {
        loading.value = true
        
        // 获取用户注册的社区ID列表
        const userCommunities = JSON.parse(localStorage.getItem('userCommunities') || '[]')
        if (userCommunities.length === 0) {
          message.error('您没有管理的社区')
          helppostList.value = []
          pagination.total = 0
          return
        }

        const communityId = userCommunities[0]
        console.log('🔍 使用社区ID查询求助帖:', communityId)
        
        const params = {
          current: pagination.current,
          pageSize: pagination.pageSize,
          communityId: communityId,
          keyword: searchForm.keyword || undefined,
          postStatus: searchForm.status || undefined
        }
        
        const response = await communityApi.getHelppostList(params.current, params.pageSize, {
          communityId: params.communityId,
          keyword: params.keyword,
          postStatus: params.postStatus
        })
        
        if (response && response.records) {
          // 处理大数字ID问题
          helppostList.value = response.records.map(helppost => {
            // 确保helppostId是字符串格式
            if (helppost.helppostId && typeof helppost.helppostId === 'number') {
              helppost.helppostId = String(helppost.helppostId)
            }
            return helppost
          })
          pagination.total = response.total
          pagination.current = response.current
          pagination.pageSize = response.size
        }
      } catch (error) {
        console.error('获取求助帖列表失败:', error)
        message.error(error.message || '获取求助帖列表失败')
      } finally {
        loading.value = false
      }
    }

    // 获取状态文本
    const getStatusText = (status) => {
      switch (status) {
        case 0: return '待处理'
        case 1: return '处理中'
        case 2: return '已完成'
        default: return '未知'
      }
    }

    // 获取状态颜色
    const getStatusColor = (status) => {
      switch (status) {
        case 0: return 'orange'
        case 1: return 'blue'
        case 2: return 'green'
        default: return 'default'
      }
    }

    const handleSearch = () => {
      pagination.current = 1
      fetchHelppostList()
    }

    const handleRefresh = () => {
      fetchHelppostList()
    }

    const handleTableChange = (pag) => {
      pagination.current = pag.current
      pagination.pageSize = pag.pageSize
      fetchHelppostList()
    }

    const viewDetail = async (record) => {
      try {
        detailLoading.value = true
        detailModalVisible.value = true
        
        console.log('🔍 查看求助帖详情 - 原始helppostId:', record.helppostId, '类型:', typeof record.helppostId)
        const detail = await communityApi.getHelppostById(record.helppostId)
        
        console.log('🔍 获取到的详情数据:', detail)
        console.log('🔍 详情中的helppostId:', detail?.helppostId, '类型:', typeof detail?.helppostId)
        
        // 处理大数字ID问题
        if (detail) {
          if (detail.helppostId && typeof detail.helppostId === 'number') {
            detail.helppostId = String(detail.helppostId)
            console.log('🔍 转换helppostId为字符串:', detail.helppostId)
          }
          if (detail.blindId && typeof detail.blindId === 'number') {
            detail.blindId = String(detail.blindId)
          }
          if (detail.volunteerId && typeof detail.volunteerId === 'number') {
            detail.volunteerId = String(detail.volunteerId)
          }
          if (detail.communityId && typeof detail.communityId === 'number') {
            detail.communityId = String(detail.communityId)
          }
        }
        
        currentDetail.value = detail
        console.log('✅ 获取求助帖详情成功:', detail)
      } catch (error) {
        console.error('❌ 获取求助帖详情失败:', error)
        message.error('获取详情失败: ' + (error.message || '未知错误'))
        detailModalVisible.value = false
      } finally {
        detailLoading.value = false
      }
    }

    // 删除功能仅限盲人用户使用，管理端不提供此功能
    // const checkTokenValidity = async () => { ... }
    // const deleteHelpPost = (record) => { ... }

    // 组件挂载时获取数据
    const handleResize = () => { windowWidth.value = window.innerWidth }
    onMounted(() => {
      window.addEventListener('resize', handleResize)
      fetchHelppostList()
    })
    onUnmounted(() => {
      window.removeEventListener('resize', handleResize)
    })

    return {
      loading,
      windowWidth,
      helppostList,
      searchForm,
      pagination,
      columns,
      detailModalVisible,
      currentDetail,
      detailLoading,
      handleSearch,
      handleRefresh,
      handleTableChange,
      viewDetail,
      getStatusText,
      getStatusColor
    }
  }
}
</script>

<style scoped>
.helppost-list {
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.action-bar {
  margin-bottom: 16px;
  padding: 14px 16px;
  background: var(--bg);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
}

.detail-content {
  padding: 8px 0;
}

.help-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  max-height: 200px;
  overflow-y: auto;
  padding: 10px 12px;
  background: var(--bg);
  border-radius: var(--radius-sm);
  font-size: 13px;
}

.loading-content {
  text-align: center;
  padding: 40px 0;
}

@media (max-width: 768px) {
  .action-bar {
    padding: 12px;
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
  :deep(.ant-descriptions) {
    font-size: 12px;
  }
  .help-content {
    font-size: 13px;
    max-height: 150px;
  }
}

@media (max-width: 640px) {
  :deep(.ant-card-body) {
    padding: 12px;
  }
  :deep(.ant-descriptions-view) {
    display: block;
  }
}
</style>
