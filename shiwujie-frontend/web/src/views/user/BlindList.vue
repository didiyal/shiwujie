<template>
  <div class="blind-list">
    <a-card title="视障人士管理">
      <div class="action-bar">
        <a-row :gutter="16" align="middle">
          <a-col :span="8">
            <a-input-search
              v-model="searchForm.keyword"
              placeholder="搜索视障人士姓名或手机号"
              @search="handleSearch"
              allow-clear
            />
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
              <a-button type="link" size="small" @click="editBlind(record)">
                编辑
              </a-button>
              <a-button type="link" size="small" danger @click="deleteBlind(record)">
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
  name: 'BlindList',
  setup() {
    const loading = ref(false);
    const searchForm = reactive({
      keyword: '',
      communityId: ''
    });

    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0
    });

    const columns = [
      { title: '姓名', dataIndex: 'name', key: 'name' },
      { title: '手机号', dataIndex: 'phone', key: 'phone' },
      { title: '所属社区', dataIndex: 'communityName', key: 'communityName' },
      { title: '求助次数', dataIndex: 'helpRequestCount', key: 'helpRequestCount' },
      { title: '注册时间', dataIndex: 'createTime', key: 'createTime' },
      { title: '操作', key: 'action', width: 200 }
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

    const editBlind = (record) => {
      // 编辑逻辑
    };

    const deleteBlind = (record) => {
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
      editBlind,
      deleteBlind
    };
  }
};
</script>

<style scoped>
.blind-list {
  padding: 0;
}

.action-bar {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}
</style> 