<template>
  <div class="community-review">
    <a-card title="社区加入审核">
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
              <a-button type="primary" size="small" @click="approve(record)">
                通过
              </a-button>
              <a-button type="default" size="small" @click="reject(record)">
                拒绝
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
  name: 'CommunityReview',
  setup() {
    const loading = ref(false);
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0
    });

    const columns = [
      { title: '申请人', dataIndex: 'userName', key: 'userName' },
      { title: '申请社区', dataIndex: 'communityName', key: 'communityName' },
      { title: '申请时间', dataIndex: 'applyTime', key: 'applyTime' },
      { title: '操作', key: 'action', width: 150 }
    ];

    const handleTableChange = (pag) => {
      pagination.current = pag.current;
      pagination.pageSize = pag.pageSize;
    };

    const approve = (record) => {
      // 审核通过逻辑
    };

    const reject = (record) => {
      // 审核拒绝逻辑
    };

    return {
      loading,
      pagination,
      columns,
      handleTableChange,
      approve,
      reject
    };
  }
};
</script> 