<template>
  <div class="community-list">
    <!-- 搜索和操作栏 -->
    <div class="action-bar">
      <a-row :gutter="16" align="middle">
        <a-col :span="8">
          <a-input-search
            v-model="searchForm.keyword"
            placeholder="搜索社区名称或地址"
            @search="handleSearch"
            allow-clear
          />
        </a-col>
        <a-col :span="4">
          <a-select
            v-model="searchForm.status"
            placeholder="社区状态"
            allow-clear
            @change="handleSearch"
          >
            <a-select-option value="">全部状态</a-select-option>
            <a-select-option value="0">未审核</a-select-option>
            <a-select-option value="1">已审核</a-select-option>
            <a-select-option value="2">已停用</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="4">
          <a-select
            v-model="searchForm.province"
            placeholder="选择省份"
            allow-clear
            @change="handleSearch"
          >
            <a-select-option value="">全部省份</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="8" style="text-align: right">
          <a-button type="primary" @click="showCreateModal">
            ➕
            创建社区
          </a-button>
          <a-button style="margin-left: 8px" @click="handleRefresh">
            🔄
            刷新
          </a-button>
        </a-col>
      </a-row>
    </div>

    <!-- 社区列表 -->
    <a-table
      :columns="columns"
      :data-source="[]"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="communityId"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="viewDetail(record)">
              查看
            </a-button>
            <a-button type="link" size="small" @click="editCommunity(record)">
              编辑
            </a-button>
            <a-button type="link" size="small" danger @click="deleteCommunity(record)">
              删除
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 创建社区弹窗 -->
    <a-modal
      :open="createModalVisible"
      title="创建社区"
      width="600px"
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
            <a-form-item label="社区名称" name="communityName">
              <a-input v-model="createForm.communityName" placeholder="请输入社区名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="社区类型" name="communityTypeId">
              <a-select v-model="createForm.communityTypeId" placeholder="请选择社区类型">
                <a-select-option value="1">城市社区</a-select-option>
                <a-select-option value="2">农村社区</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-form-item label="社区介绍" name="communityDescription">
          <a-textarea
            v-model="createForm.communityDescription"
            placeholder="请输入社区介绍"
            :rows="3"
          />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="省份" name="province">
              <a-select v-model="createForm.province" placeholder="请选择省份">
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="城市" name="city">
              <a-select v-model="createForm.city" placeholder="请选择城市">
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="区县" name="district">
              <a-select v-model="createForm.district" placeholder="请选择区县">
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="详细地址" name="address">
          <a-input v-model="createForm.address" placeholder="请输入详细地址" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';

export default {
  name: 'CommunityList',
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const createModalVisible = ref(false);
    const createFormRef = ref();

    const searchForm = reactive({
      keyword: '',
      status: '',
      province: ''
    });

    const createForm = reactive({
      communityName: '',
      communityTypeId: '',
      communityDescription: '',
      province: '',
      city: '',
      district: '',
      address: ''
    });

    const createRules = {
      communityName: [{ required: true, message: '请输入社区名称' }],
      communityTypeId: [{ required: true, message: '请选择社区类型' }],
      province: [{ required: true, message: '请选择省份' }],
      city: [{ required: true, message: '请选择城市' }],
      district: [{ required: true, message: '请选择区县' }],
      address: [{ required: true, message: '请输入详细地址' }]
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
        title: '社区名称',
        dataIndex: 'communityName',
        key: 'communityName'
      },
      {
        title: '地址',
        dataIndex: 'address',
        key: 'address'
      },
      {
        title: '状态',
        dataIndex: 'communityStatus',
        key: 'status'
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime'
      },
      {
        title: '操作',
        key: 'action',
        width: 200
      }
    ];

    const handleSearch = () => {
      pagination.current = 1;
      loadData();
    };

    const handleRefresh = () => {
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
      router.push(`/community/${record.communityId}`);
    };

    const editCommunity = (record) => {
      router.push(`/community/${record.communityId}/edit`);
    };

    const deleteCommunity = (record) => {
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
      handleRefresh,
      handleTableChange,
      showCreateModal,
      handleCreate,
      handleCancel,
      viewDetail,
      editCommunity,
      deleteCommunity,
      loadData
    };
  }
};
</script>

<style scoped>
.community-list {
  padding: 0;
}

.action-bar {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}
</style> 