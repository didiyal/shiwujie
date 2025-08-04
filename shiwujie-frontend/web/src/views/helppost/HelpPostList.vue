<template>
  <div class="helppost-list">
    <a-card title="求助帖管理">
      <div class="action-bar">
        <a-row :gutter="16" align="middle">
          <a-col :span="8">
            <a-input-search
              v-model="searchForm.keyword"
              placeholder="搜索求助内容"
              @search="handleSearch"
              allow-clear
            />
          </a-col>
          <a-col :span="4">
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
          <a-col :span="12" style="text-align: right">
            <a-button @click="handleRefresh">
              🔄
              刷新
            </a-button>
          </a-col>
        </a-row>
      </div>

      <a-table
        :columns="columns"
        :data-source="[]"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="viewDetail(record)">
                查看
              </a-button>
              <a-button type="link" size="small" danger @click="deleteHelpPost(record)">
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import { ref, reactive } from 'vue';

export default {
  name: 'HelpPostList',
  setup() {
    const loading = ref(false);
    const searchForm = reactive({
      keyword: '',
      status: ''
    });

    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0
    });

    const columns = [
      { title: '发布人', dataIndex: 'userName', key: 'userName' },
      { title: '求助内容', dataIndex: 'content', key: 'content' },
      { title: '发布时间', dataIndex: 'createTime', key: 'createTime' },
      { title: '状态', dataIndex: 'status', key: 'status' },
      { title: '操作', key: 'action', width: 150 }
    ];

    const handleSearch = () => {
      pagination.current = 1;
    };

    const handleRefresh = () => {
      // 刷新逻辑
    };

    const handleTableChange = (pag) => {
      pagination.current = pag.current;
      pagination.pageSize = pag.pageSize;
    };

    const viewDetail = (record) => {
      // 查看详情逻辑
    };

    const deleteHelpPost = (record) => {
      // 删除逻辑
    };

    return {
      loading,
      searchForm,
      pagination,
      columns,
      handleSearch,
      handleRefresh,
      handleTableChange,
      viewDetail,
      deleteHelpPost
    };
  }
};
</script>

<style scoped>
.helppost-list {
  padding: 0;
}

.action-bar {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}
</style> 