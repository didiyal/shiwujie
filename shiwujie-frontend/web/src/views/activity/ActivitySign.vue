<template>
  <div class="activity-sign">
    <a-card title="活动报名管理">
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
              <a-button type="primary" size="small" @click="checkIn(record)">
                签到
              </a-button>
              <a-button type="default" size="small" @click="checkOut(record)">
                签退
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
  name: 'ActivitySign',
  setup() {
    const loading = ref(false);
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0
    });

    const columns = [
      { title: '活动名称', dataIndex: 'activityName', key: 'activityName' },
      { title: '报名人', dataIndex: 'userName', key: 'userName' },
      { title: '报名时间', dataIndex: 'signUpTime', key: 'signUpTime' },
      { title: '签到状态', dataIndex: 'checkInStatus', key: 'checkInStatus' },
      { title: '操作', key: 'action', width: 150 }
    ];

    const handleTableChange = (pag) => {
      pagination.current = pag.current;
      pagination.pageSize = pag.pageSize;
    };

    const checkIn = (record) => {
      // 签到逻辑
    };

    const checkOut = (record) => {
      // 签退逻辑
    };

    return {
      loading,
      pagination,
      columns,
      handleTableChange,
      checkIn,
      checkOut
    };
  }
};
</script> 