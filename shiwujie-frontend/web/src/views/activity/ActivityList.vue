<template>
  <div class="activity-list">
    <a-card title="活动管理" class="management-card">
      <!-- 搜索和操作栏 -->
      <div class="search-section">
        <a-row :gutter="16" align="middle">
          <a-col :span="6">
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
          <a-col :span="4">
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
            <a-select-option value="已取消">已取消</a-select-option>
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
          <a-col :span="6">
            <a-range-picker
              v-model="searchForm.dateRange"
              @change="handleSearch"
              placeholder="['开始日期', '结束日期']"
              size="large"
            />
          </a-col>
          <a-col :span="4" style="text-align: right">
            <a-button type="primary" @click="showCreateModal" size="large">
              <template #icon>
                <PlusOutlined />
              </template>
              创建活动
            </a-button>
          </a-col>
        </a-row>
      </div>

      <!-- 统计信息 -->
      <div class="stats-section">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-statistic title="总活动数" :value="totalCount" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="未开始" :value="pendingCount" :value-style="{ color: '#faad14' }" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="进行中" :value="ongoingCount" :value-style="{ color: '#1890ff' }" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="已结束" :value="completedCount" :value-style="{ color: '#52c41a' }" />
          </a-col>
        </a-row>
      </div>

      <!-- 活动列表 -->
      <a-table
        :columns="columns"
        :data-source="activityList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="activityId"
        class="management-table"
      >
        <template #bodyCell="{ column, record }">
          <!-- 活动时间列 -->
          <template v-if="column.key === 'activityTime'">
            <div class="time-cell">
              <div class="start-time">开始：{{ formatTime(record.startTime) }}</div>
              <div class="end-time">结束：{{ formatTime(record.endTime) }}</div>
            </div>
          </template>

          <!-- 状态列 -->
          <template v-if="column.key === 'activityStatus'">
            <a-tag :color="getStatusColor(record.activityStatus)">
              {{ getStatusText(record.activityStatus) }}
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
              <a-button type="link" size="small" @click="editActivity(record)">
                <template #icon>
                  <EditOutlined />
                </template>
                编辑
              </a-button>
              <a-button type="link" size="small" @click="viewSignList(record)">
                <template #icon>
                  <UserOutlined />
                </template>
                报名管理
              </a-button>
              <a-button type="link" size="small" danger @click="deleteActivity(record)">
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

    <!-- 创建活动弹窗 -->
    <a-modal
      v-model:open="createModalVisible"
      title="创建活动"
      width="800px"
      @ok="handleCreate"
      @cancel="handleCancel"
      :confirm-loading="createLoading"
    >
      <div class="create-form">
        <div class="form-row">
          <div class="form-group">
            <label for="activityName" class="form-label">活动名称 *</label>
            <input
              id="activityName"
              v-model="createForm.activityName"
              type="text"
              :class="['form-input', { error: formErrors.activityName }]"
              placeholder="请输入活动名称"
              @blur="validateField('activityName')"
            />
            <div v-if="formErrors.activityName" class="error-message">
              {{ formErrors.activityName }}
            </div>
          </div>
          <div class="form-group">
            <label for="communityId" class="form-label">所属社区 *</label>
            <select
              id="communityId"
              v-model="createForm.communityId"
              :class="['form-select', { error: formErrors.communityId }]"
              @change="validateField('communityId')"
            >
              <option value="">请选择社区</option>
              <option 
                v-for="community in communityList" 
                :key="community.communityId" 
                :value="community.communityId"
              >
                {{ community.communityName }}
              </option>
            </select>
            <div v-if="formErrors.communityId" class="error-message">
              {{ formErrors.communityId }}
            </div>
          </div>
        </div>

        <div class="form-group">
          <label for="activityContent" class="form-label">活动内容 *</label>
                      <textarea
              id="activityContent"
              v-model="createForm.activityContent"
              :class="['form-textarea', { error: formErrors.activityContent }]"
              placeholder="请输入活动内容描述"
              rows="4"
              @blur="validateField('activityContent')"
            ></textarea>
          <div v-if="formErrors.activityContent" class="error-message">
            {{ formErrors.activityContent }}
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label for="activityLocation" class="form-label">活动地点 *</label>
            <input
              id="activityLocation"
              v-model="createForm.activityLocation"
              type="text"
              :class="['form-input', { error: formErrors.activityLocation }]"
              placeholder="请输入活动地点"
              @blur="validateField('activityLocation')"
            />
            <div v-if="formErrors.activityLocation" class="error-message">
              {{ formErrors.activityLocation }}
            </div>
          </div>
          <div class="form-group">
            <label for="maxParticipants" class="form-label">人数限制</label>
            <input
              id="maxParticipants"
              v-model="createForm.maxParticipants"
              type="number"
              :class="['form-input', { error: formErrors.maxParticipants }]"
              placeholder="不填表示不限制"
              min="1"
              @blur="validateField('maxParticipants')"
            />
            <div v-if="formErrors.maxParticipants" class="error-message">
              {{ formErrors.maxParticipants }}
            </div>
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label for="startTime" class="form-label">开始时间 *</label>
            <input
              id="startTime"
              v-model="createForm.startTime"
              type="datetime-local"
              :class="['form-input', { error: formErrors.startTime }]"
              @change="validateField('startTime')"
            />
            <div v-if="formErrors.startTime" class="error-message">
              {{ formErrors.startTime }}
            </div>
          </div>
          <div class="form-group">
            <label for="endTime" class="form-label">结束时间 *</label>
            <input
              id="endTime"
              v-model="createForm.endTime"
              type="datetime-local"
              :class="['form-input', { error: formErrors.endTime }]"
              @change="validateField('endTime')"
            />
            <div v-if="formErrors.endTime" class="error-message">
              {{ formErrors.endTime }}
            </div>
          </div>
        </div>
             </div>
     </a-modal>

     <!-- 编辑活动弹窗 -->
     <a-modal
       v-model:open="editModalVisible"
       title="编辑活动"
       width="800px"
       @ok="handleUpdate"
       @cancel="handleEditCancel"
       :confirm-loading="createLoading"
     >
       <div class="create-form">
         <div class="form-row">
           <div class="form-group">
             <label for="editActivityName" class="form-label">活动名称 *</label>
             <input
               id="editActivityName"
               v-model="editForm.activityName"
               type="text"
               :class="['form-input', { error: editFormErrors.activityName }]"
               placeholder="请输入活动名称"
               @blur="validateEditField('activityName')"
             />
             <div v-if="editFormErrors.activityName" class="error-message">
               {{ editFormErrors.activityName }}
             </div>
           </div>
           <div class="form-group">
             <label for="editCommunityId" class="form-label">所属社区 *</label>
             <select
               id="editCommunityId"
               v-model="editForm.communityId"
               :class="['form-select', { error: editFormErrors.communityId }]"
               @change="validateEditField('communityId')"
             >
               <option value="">请选择社区</option>
               <option 
                 v-for="community in communityList" 
                 :key="community.communityId" 
                 :value="community.communityId"
               >
                 {{ community.communityName }}
               </option>
             </select>
             <div v-if="editFormErrors.communityId" class="error-message">
               {{ editFormErrors.communityId }}
             </div>
           </div>
         </div>

         <div class="form-group">
           <label for="editActivityContent" class="form-label">活动内容 *</label>
           <textarea
             id="editActivityContent"
             v-model="editForm.activityContent"
             :class="['form-textarea', { error: editFormErrors.activityContent }]"
             placeholder="请输入活动内容描述"
             rows="4"
             @blur="validateEditField('activityContent')"
           ></textarea>
           <div v-if="editFormErrors.activityContent" class="error-message">
             {{ editFormErrors.activityContent }}
           </div>
         </div>

         <div class="form-row">
           <div class="form-group">
             <label for="editActivityLocation" class="form-label">活动地点 *</label>
             <input
               id="editActivityLocation"
               v-model="editForm.activityLocation"
               type="text"
               :class="['form-input', { error: editFormErrors.activityLocation }]"
               placeholder="请输入活动地点"
               @blur="validateEditField('activityLocation')"
             />
             <div v-if="editFormErrors.activityLocation" class="error-message">
               {{ editFormErrors.activityLocation }}
             </div>
           </div>
           <div class="form-group">
             <label for="editMaxParticipants" class="form-label">人数限制</label>
             <input
               id="editMaxParticipants"
               v-model="editForm.maxParticipants"
               type="number"
               :class="['form-input', { error: editFormErrors.maxParticipants }]"
               placeholder="不填表示不限制"
               min="1"
               @blur="validateEditField('maxParticipants')"
             />
             <div v-if="editFormErrors.maxParticipants" class="error-message">
               {{ editFormErrors.maxParticipants }}
             </div>
           </div>
         </div>

         <div class="form-row">
           <div class="form-group">
             <label for="editStartTime" class="form-label">开始时间 *</label>
             <input
               id="editStartTime"
               v-model="editForm.startTime"
               type="datetime-local"
               :class="['form-input', { error: editFormErrors.startTime }]"
               @change="validateEditField('startTime')"
             />
             <div v-if="editFormErrors.startTime" class="error-message">
               {{ editFormErrors.startTime }}
             </div>
           </div>
           <div class="form-group">
             <label for="editEndTime" class="form-label">结束时间 *</label>
             <input
               id="editEndTime"
               v-model="editForm.endTime"
               type="datetime-local"
               :class="['form-input', { error: editFormErrors.endTime }]"
               @change="validateEditField('endTime')"
             />
             <div v-if="editFormErrors.endTime" class="error-message">
               {{ editFormErrors.endTime }}
             </div>
           </div>
         </div>
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
  UserOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { activityApi } from '@/api/activity'
import { communityApi } from '@/api/community'
import { useAuthStore } from '@/stores/auth'

export default {
  name: 'ActivityList',
  components: {
    SearchOutlined,
    PlusOutlined,
    EyeOutlined,
    EditOutlined,
    UserOutlined,
    DeleteOutlined
  },
  setup() {
    const authStore = useAuthStore()
    const loading = ref(false)
    const createLoading = ref(false)
    const activityList = ref([])
    const communityList = ref([])
    const createModalVisible = ref(false)
    const editModalVisible = ref(false)
    const createFormRef = ref()

    const searchForm = reactive({
      keyword: '',
      status: '',
      communityId: '',
      dateRange: []
    })

    const createForm = reactive({
      activityName: '',
      communityId: '',
      activityContent: '',
      activityLocation: '',
      maxParticipants: '',
      startTime: '',
      endTime: ''
    })

    const editForm = reactive({
      activityId: '',
      activityName: '',
      communityId: '',
      activityContent: '',
      activityLocation: '',
      maxParticipants: '',
      startTime: '',
      endTime: '',
      activityStatus: ''
    })

    const formErrors = reactive({
      activityName: '',
      communityId: '',
      activityContent: '',
      activityLocation: '',
      maxParticipants: '',
      startTime: '',
      endTime: ''
    })

    const editFormErrors = reactive({
      activityName: '',
      communityId: '',
      activityContent: '',
      activityLocation: '',
      maxParticipants: '',
      startTime: '',
      endTime: ''
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
        title: '活动名称',
        dataIndex: 'activityName',
        key: 'activityName',
        width: 200,
        ellipsis: true
      },
      {
        title: '所属社区',
        dataIndex: 'communityName',
        key: 'communityName',
        width: 120
      },
      {
        title: '活动时间',
        key: 'activityTime',
        width: 200
      },
      {
        title: '活动地点',
        dataIndex: 'activityLocation',
        key: 'activityLocation',
        width: 150,
        ellipsis: true
      },
      {
        title: '状态',
        dataIndex: 'activityStatus',
        key: 'activityStatus',
        width: 100
      },
      {
        title: '操作',
        key: 'action',
        width: 250,
        fixed: 'right'
      }
    ]

    // 计算统计信息
    const totalCount = computed(() => pagination.total)
    const pendingCount = computed(() => 
      activityList.value.filter(item => item.activityStatus === '未开始').length
    )
    const ongoingCount = computed(() => 
      activityList.value.filter(item => item.activityStatus === '进行中').length
    )
    const completedCount = computed(() => 
      activityList.value.filter(item => item.activityStatus === '已结束').length
    )

    // 获取活动列表
    const fetchActivityList = async () => {
      try {
        loading.value = true
        
        // 构建请求参数
        const params = {
          current: pagination.current,
          pageSize: pagination.pageSize,
          activityStatus: searchForm.status || undefined
        }
        
        // 确保状态参数是中文值
        if (params.activityStatus) {
          console.log('发送的活动状态参数:', params.activityStatus)
        }
        
        // 如果有选择特定社区，使用选择的社区ID
        // 否则使用当前登录用户的社区ID
        if (searchForm.communityId) {
          params.communityId = searchForm.communityId
        } else if (authStore.volunteerInfo?.communityId) {
          params.communityId = authStore.volunteerInfo.communityId
        }
        
        console.log('请求参数:', params)
        const response = await activityApi.getActivityList(params)
        console.log('活动列表响应:', response)
        
        if (response && response.records) {
          // 处理大数字ID问题
          activityList.value = response.records.map(activity => {
            // 确保activityId是字符串格式
            if (activity.activityId && typeof activity.activityId === 'number') {
              activity.activityId = String(activity.activityId)
            }
            return activity
          })
          pagination.total = response.total
          pagination.current = response.current
          pagination.pageSize = response.size
        }
      } catch (error) {
        console.error('获取活动列表失败:', error)
        console.error('错误详情:', {
          message: error.message,
          response: error.response,
          stack: error.stack
        })
        message.error(error.message || '获取活动列表失败')
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
        
        // 设置创建表单的默认社区ID
        if (currentUserCommunity && !createForm.communityId) {
          createForm.communityId = currentUserCommunity
        }
      } catch (error) {
        console.error('获取社区列表失败:', error)
      }
    }

    // 搜索处理
    const handleSearch = () => {
      pagination.current = 1
      fetchActivityList()
    }

    // 表格变化处理
    const handleTableChange = (pag) => {
      pagination.current = pag.current
      pagination.pageSize = pag.pageSize
      fetchActivityList()
    }

    // 显示创建模态框
    const showCreateModal = () => {
      createModalVisible.value = true
      // 设置默认社区ID
      if (authStore.volunteerInfo?.communityId && !createForm.communityId) {
        createForm.communityId = authStore.volunteerInfo.communityId
      }
    }

    // 表单验证函数
    const validateField = (fieldName) => {
      const value = createForm[fieldName]
      let error = ''

      switch (fieldName) {
        case 'activityName':
          if (!value || value.trim() === '') {
            error = '请输入活动名称'
          } else if (value.length < 2 || value.length > 50) {
            error = '活动名称长度在2-50个字符之间'
          }
          break
        case 'communityId':
          if (!value || value === '') {
            error = '请选择所属社区'
          }
          break
        case 'activityContent':
          if (!value || value.trim() === '') {
            error = '请输入活动内容'
          } else if (value.length < 10 || value.length > 500) {
            error = '活动内容长度在10-500个字符之间'
          }
          break
        case 'activityLocation':
          if (!value || value.trim() === '') {
            error = '请输入活动地点'
          }
          break
        case 'maxParticipants':
          if (value && (isNaN(value) || parseInt(value) < 1)) {
            error = '人数限制必须大于0'
          }
          break
        case 'startTime':
          if (!value || value === '') {
            error = '请选择开始时间'
          }
          break
        case 'endTime':
          if (!value || value === '') {
            error = '请选择结束时间'
          } else if (createForm.startTime && value <= createForm.startTime) {
            error = '结束时间必须晚于开始时间'
          }
          break
      }

      formErrors[fieldName] = error
      return error === ''
    }

    // 验证整个表单
    const validateForm = () => {
      const fields = ['activityName', 'communityId', 'activityContent', 'activityLocation', 'startTime', 'endTime']
      let isValid = true

      fields.forEach(field => {
        if (!validateField(field)) {
          isValid = false
        }
      })

      return isValid
    }

    // 编辑表单验证函数
    const validateEditField = (fieldName) => {
      const value = editForm[fieldName]
      let error = ''

      switch (fieldName) {
        case 'activityName':
          if (!value || value.trim() === '') {
            error = '请输入活动名称'
          } else if (value.length < 2 || value.length > 50) {
            error = '活动名称长度在2-50个字符之间'
          }
          break
        case 'communityId':
          if (!value || value === '') {
            error = '请选择所属社区'
          }
          break
        case 'activityContent':
          if (!value || value.trim() === '') {
            error = '请输入活动内容'
          } else if (value.length < 10 || value.length > 500) {
            error = '活动内容长度在10-500个字符之间'
          }
          break
        case 'activityLocation':
          if (!value || value.trim() === '') {
            error = '请输入活动地点'
          }
          break
        case 'maxParticipants':
          if (value && (isNaN(value) || parseInt(value) < 1)) {
            error = '人数限制必须大于0'
          }
          break
        case 'startTime':
          if (!value || value === '') {
            error = '请选择开始时间'
          }
          break
        case 'endTime':
          if (!value || value === '') {
            error = '请选择结束时间'
          } else if (editForm.startTime && value <= editForm.startTime) {
            error = '结束时间必须晚于开始时间'
          }
          break
      }

      editFormErrors[fieldName] = error
      return error === ''
    }

    // 验证整个编辑表单
    const validateEditForm = () => {
      const fields = ['activityName', 'communityId', 'activityContent', 'activityLocation', 'startTime', 'endTime']
      let isValid = true

      fields.forEach(field => {
        if (!validateEditField(field)) {
          isValid = false
        }
      })

      return isValid
    }

    // 清除表单错误
    const clearFormErrors = () => {
      Object.keys(formErrors).forEach(key => {
        formErrors[key] = ''
      })
    }

    // 清除编辑表单错误
    const clearEditFormErrors = () => {
      Object.keys(editFormErrors).forEach(key => {
        editFormErrors[key] = ''
      })
    }

    // 重置表单
    const resetForm = () => {
      Object.keys(createForm).forEach(key => {
        createForm[key] = ''
      })
      clearFormErrors()
    }

    // 重置编辑表单
    const resetEditForm = () => {
      Object.keys(editForm).forEach(key => {
        editForm[key] = ''
      })
      clearEditFormErrors()
    }

    // 创建活动
    const handleCreate = async () => {
      try {
        console.log('开始创建活动，当前表单数据:', createForm)
        
        // 先进行表单验证
        if (!validateForm()) {
          console.log('表单验证失败')
          return
        }
        
        createLoading.value = true
        
        // 构建请求数据
        const requestData = {
          activityName: createForm.activityName.trim(),
          communityId: createForm.communityId,
          activityContent: createForm.activityContent.trim(),
          activityLocation: createForm.activityLocation.trim(),
          maxParticipants: createForm.maxParticipants ? parseInt(createForm.maxParticipants) : 0,
          startTime: createForm.startTime,
          endTime: createForm.endTime
        }
        
        console.log('创建活动请求数据:', requestData)
        const response = await activityApi.createActivity(requestData)
        console.log('创建活动响应:', response)
        
        message.success('活动创建成功')
        createModalVisible.value = false
        resetForm()
        fetchActivityList()
      } catch (error) {
        console.error('创建活动失败:', error)
        
        // 这是API请求错误
        if (error.message) {
          message.error(error.message)
        } else {
          message.error('创建活动失败')
        }
      } finally {
        createLoading.value = false
      }
    }

    // 取消创建
    const handleCancel = () => {
      createModalVisible.value = false
      resetForm()
    }

    // 更新活动
    const handleUpdate = async () => {
      try {
        console.log('开始更新活动，当前表单数据:', editForm)
        
        // 先进行表单验证
        if (!validateEditForm()) {
          console.log('编辑表单验证失败')
          return
        }
        
        // 构建请求数据
        const requestData = {
          activityId: String(editForm.activityId), // 确保activityId是字符串格式
          activityName: editForm.activityName.trim(),
          communityId: editForm.communityId,
          activityContent: editForm.activityContent.trim(),
          activityLocation: editForm.activityLocation.trim(),
          maxParticipants: editForm.maxParticipants ? parseInt(editForm.maxParticipants) : 0,
          startTime: editForm.startTime,
          endTime: editForm.endTime,
          activityStatus: editForm.activityStatus
        }
        
        console.log('更新活动请求数据:', requestData)
        console.log('activityId类型:', typeof requestData.activityId, '值:', requestData.activityId)
        const response = await activityApi.updateActivity(requestData)
        console.log('更新活动响应:', response)
        
        message.success('活动更新成功')
        editModalVisible.value = false
        resetEditForm()
        fetchActivityList()
      } catch (error) {
        console.error('更新活动失败:', error)
        console.error('错误详情:', {
          message: error.message,
          response: error.response,
          activityId: requestData.activityId,
          requestData: requestData
        })
        
        // 这是API请求错误
        if (error.message) {
          message.error(error.message)
        } else {
          message.error('更新活动失败')
        }
      }
    }

    // 取消编辑
    const handleEditCancel = () => {
      editModalVisible.value = false
      resetEditForm()
    }

    // 查看详情
    const viewDetail = (record) => {
      console.log('查看活动详情:', record)
      // 这里可以跳转到详情页面或显示详情模态框
    }

    // 编辑活动
    const editActivity = (record) => {
      console.log('编辑活动:', record)
      
      // 填充编辑表单数据
      editForm.activityId = String(record.activityId) // 确保activityId是字符串格式
      editForm.activityName = record.activityName
      editForm.communityId = record.communityId
      editForm.activityContent = record.activityContent
      editForm.activityLocation = record.activityLocation
      editForm.maxParticipants = record.maxParticipants || ''
      editForm.startTime = record.startTime ? formatDateTimeForInput(record.startTime) : ''
      editForm.endTime = record.endTime ? formatDateTimeForInput(record.endTime) : ''
      editForm.activityStatus = record.activityStatus
      
      console.log('编辑表单activityId:', editForm.activityId, '类型:', typeof editForm.activityId)
      
      // 清除编辑表单错误
      clearEditFormErrors()
      
      // 显示编辑模态框
      editModalVisible.value = true
    }

    // 查看报名列表
    const viewSignList = (record) => {
      console.log('查看报名列表:', record)
      // 这里可以跳转到报名管理页面
    }

    // 删除活动
    const deleteActivity = (record) => {
      Modal.confirm({
        title: '删除活动',
        content: `确定要删除活动"${record.activityName}"吗？此操作不可恢复。`,
        okType: 'danger',
        onOk: async () => {
          try {
            console.log('删除活动ID:', record.activityId, '类型:', typeof record.activityId)
            const response = await activityApi.deleteActivity(record.activityId)
            console.log('删除活动响应:', response)
            message.success('删除成功')
            fetchActivityList()
          } catch (error) {
            console.error('删除活动失败:', error)
            console.error('删除活动错误详情:', {
              message: error.message,
              response: error.response,
              activityId: record.activityId
            })
            message.error(error.message || '删除失败')
          }
        }
      })
    }

    // 获取状态颜色
    const getStatusColor = (status) => {
      const colorMap = {
        '未开始': 'orange',
        '进行中': 'blue',
        '已结束': 'green',
        '已取消': 'red'
      }
      return colorMap[status] || 'default'
    }

    // 获取状态文本
    const getStatusText = (status) => {
      // 由于后端直接返回中文状态，这里直接返回原状态
      return status || '未知状态'
    }

    // 格式化时间
    const formatTime = (time) => {
      if (!time) return '-'
      return new Date(time).toLocaleString('zh-CN')
    }

    // 格式化时间为datetime-local输入框格式
    const formatDateTimeForInput = (time) => {
      if (!time) return ''
      const date = new Date(time)
      return date.toISOString().slice(0, 16)
    }

    // 组件挂载时获取数据
    onMounted(() => {
      fetchCommunityList()
      fetchActivityList()
    })

    return {
      loading,
      createLoading,
      activityList,
      communityList,
      searchForm,
      createModalVisible,
      editModalVisible,
      createForm,
      editForm,
      formErrors,
      editFormErrors,
      pagination,
      columns,
      totalCount,
      pendingCount,
      ongoingCount,
      completedCount,
      handleSearch,
      handleTableChange,
      showCreateModal,
      handleCreate,
      handleCancel,
      handleUpdate,
      handleEditCancel,
      validateEditField,
      viewDetail,
      editActivity,
      viewSignList,
      deleteActivity,
      getStatusColor,
      getStatusText,
      formatTime,
      formatDateTimeForInput
    }
  }
}
</script>

<style scoped>
.activity-list {
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
.search-section :deep(.ant-select-selector),
.search-section :deep(.ant-picker) {
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

.time-cell {
  font-size: 12px;
  line-height: 1.4;
}

.start-time {
  color: #1890ff;
  font-weight: 500;
}

.end-time {
  color: #8c8c8c;
}

.create-form {
  padding: 16px 0;
}

.form-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.form-group {
  flex: 1;
  position: relative;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #262626;
  font-size: 14px;
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
  background-color: #fff;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.form-input.error,
.form-select.error,
.form-textarea.error {
  border-color: #ff4d4f;
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.error-message {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
    gap: 12px;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .activity-list {
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