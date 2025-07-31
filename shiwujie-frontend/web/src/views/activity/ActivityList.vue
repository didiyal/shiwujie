<template>
  <div class="activity-list">
    <!-- 搜索和操作栏 -->
    <div class="action-bar">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-input-search
            v-model="searchForm.keyword"
            placeholder="搜索活动名称"
            @search="handleSearch"
            allow-clear
          />
        </a-col>
        <a-col :span="4">
          <a-select
            v-model="searchForm.status"
            placeholder="活动状态"
            allow-clear
            @change="handleSearch"
          >
            <a-select-option value="">全部状态</a-select-option>
            <a-select-option value="0">未开始</a-select-option>
            <a-select-option value="1">进行中</a-select-option>
            <a-select-option value="2">已结束</a-select-option>
            <a-select-option value="3">已取消</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="4">
          <a-select
            v-model="searchForm.communityId"
            placeholder="选择社区"
            allow-clear
            @change="handleSearch"
          >
            <a-select-option value="">全部社区</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="6">
          <a-range-picker
            v-model="searchForm.dateRange"
            @change="handleSearch"
            placeholder="['开始日期', '结束日期']"
          />
        </a-col>
        <a-col :span="4" style="text-align: right">
          <a-button type="primary" @click="showCreateModal">
            ➕
            创建活动
          </a-button>
        </a-col>
      </a-row>
    </div>

    <!-- 活动列表 -->
    <a-table
      :columns="columns"
      :data-source="[]"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="activityId"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="viewDetail(record)">
              查看
            </a-button>
            <a-button type="link" size="small" @click="editActivity(record)">
              编辑
            </a-button>
            <a-button type="link" size="small" @click="viewSignList(record)">
              报名管理
            </a-button>
            <a-button type="link" size="small" danger @click="deleteActivity(record)">
              删除
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 创建活动弹窗 -->
    <a-modal
      :open="createModalVisible"
      title="创建活动"
      width="800px"
      @ok="handleCreate"
      @cancel="handleCancel"
      @update:open="createModalVisible = $event"
    >
      <a-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="活动名称" name="activityName">
              <a-input v-model="createForm.activityName" placeholder="请输入活动名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属社区" name="communityId">
              <a-select v-model="createForm.communityId" placeholder="请选择社区">
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="活动内容" name="activityContent">
          <a-textarea
            v-model="createForm.activityContent"
            placeholder="请输入活动内容"
            :rows="4"
          />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="活动地点" name="activityLocation">
              <a-input v-model="createForm.activityLocation" placeholder="请输入活动地点" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="人数限制" name="maxParticipants">
              <a-input-number
                v-model="createForm.maxParticipants"
                placeholder="不填表示不限制"
                :min="1"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="开始时间" name="startTime">
              <a-date-picker
                v-model="createForm.startTime"
                show-time
                style="width: 100%"
                placeholder="请选择开始时间"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束时间" name="endTime">
              <a-date-picker
                v-model="createForm.endTime"
                show-time
                style="width: 100%"
                placeholder="请选择结束时间"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';

export default {
  name: 'ActivityList',
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const createModalVisible = ref(false);
    const createFormRef = ref();

    const searchForm = reactive({
      keyword: '',
      status: '',
      communityId: '',
      dateRange: []
    });

    const createForm = reactive({
      activityName: '',
      communityId: '',
      activityContent: '',
      activityLocation: '',
      maxParticipants: null,
      startTime: null,
      endTime: null
    });

    const createRules = {
      activityName: [{ required: true, message: '请输入活动名称' }],
      communityId: [{ required: true, message: '请选择所属社区' }],
      activityContent: [{ required: true, message: '请输入活动内容' }],
      activityLocation: [{ required: true, message: '请输入活动地点' }],
      startTime: [{ required: true, message: '请选择开始时间' }],
      endTime: [{ required: true, message: '请选择结束时间' }]
    };

    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showQuickJumper: true
    });

    const columns = [
      {
        title: '活动名称',
        dataIndex: 'activityName',
        key: 'activityName'
      },
      {
        title: '所属社区',
        dataIndex: 'communityName',
        key: 'communityName'
      },
      {
        title: '活动时间',
        dataIndex: 'startTime',
        key: 'startTime'
      },
      {
        title: '活动地点',
        dataIndex: 'activityLocation',
        key: 'activityLocation'
      },
      {
        title: '状态',
        dataIndex: 'activityStatus',
        key: 'status'
      },
      {
        title: '操作',
        key: 'action',
        width: 250
      }
    ];

    const handleSearch = () => {
      pagination.current = 1;
      loadData();
    };

    const handleTableChange = (pag) => {
      pagination.current = pag.current;
      pagination.pageSize = pag.pageSize;
      loadData();
    };

    const showCreateModal = () => {
      createModalVisible.value = true;
    };

    const handleCreate = () => {
      createFormRef.value.validate().then(() => {
        createModalVisible.value = false;
        loadData();
      });
    };

    const handleCancel = () => {
      createModalVisible.value = false;
      createFormRef.value.resetFields();
    };

    const viewDetail = (record) => {
      router.push(`/activity/${record.activityId}`);
    };

    const editActivity = (record) => {
      router.push(`/activity/${record.activityId}/edit`);
    };

    const viewSignList = (record) => {
      router.push(`/activity/${record.activityId}/sign`);
    };

    const deleteActivity = (record) => {
      // 删除逻辑
    };

    const loadData = () => {
      loading.value = true;
      setTimeout(() => {
        loading.value = false;
      }, 1000);
    };

    return {
      loading,
      searchForm,
      createModalVisible,
      createForm,
      createFormRef,
      createRules,
      pagination,
      columns,
      handleSearch,
      handleTableChange,
      showCreateModal,
      handleCreate,
      handleCancel,
      viewDetail,
      editActivity,
      viewSignList,
      deleteActivity,
      loadData
    };
  }
};
</script>

<style scoped>
.activity-list {
  padding: 0;
}

.action-bar {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}
</style> 