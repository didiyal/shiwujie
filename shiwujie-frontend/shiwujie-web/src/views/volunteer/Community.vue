<script setup lang="ts">
import { computed, ref } from "vue";
import { message } from "ant-design-vue";
import { useCommunityStore } from "@/stores/community";

interface DataType {
  key: string;
  name: string;
  account: string;
  gender: string;
  volunteerCount: number;
  isOnline: boolean;
}

const communityStore = useCommunityStore();

const columns = [
  {
    title: "姓名",
    dataIndex: "name",
    width: "20%",
  },
  {
    title: "账号",
    dataIndex: "account",
    width: "20%",
  },
  {
    title: "性别",
    dataIndex: "gender",
    width: "15%",
  },
  {
    title: "志愿总次数",
    dataIndex: "volunteerCount",
    width: "15%",
  },
  {
    title: "在线状态",
    dataIndex: "isOnline",
    width: "15%",
  },
  {
    title: "操作",
    key: "action",
    width: "15%",
  },
];

// 使用 store 中的数据
const data = computed(() => communityStore.communities);
const count = computed(() => data.value.length + 1);

// 添加成员的模态框
const showAddModal = ref(false);
const newAccount = ref('');

const handleAdd = () => {
  showAddModal.value = true;
};

const handleAddOk = () => {
  if (!newAccount.value) {
    message.error('请输入账号');
    return;
  }

  // 检查账号是否已存在
  if (data.value.some(item => item.account === newAccount.value)) {
    message.error('该账号已存在');
    return;
  }

  const newData: DataType = {
    key: `${count.value}`,
    name: "新成员",  // 默认名称
    account: newAccount.value,
    gender: "未知",  // 默认性别
    volunteerCount: 0,  // 初始志愿次数
    isOnline: false,  // 初始在线状态
  };
  
  communityStore.addCommunity(newData);
  showAddModal.value = false;
  newAccount.value = '';
  message.success('添加成功');
};

const onDelete = (key: string) => {
  communityStore.deleteCommunity(key);
  message.success('删除成功');
};
</script>

<template>
  <div>
    <a-button
      type="primary"
      style="margin-bottom: 16px"
      @click="handleAdd"
    >
      添加成员
    </a-button>
    <a-table bordered :data-source="data" :columns="columns">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'isOnline'">
          <a-tag :color="record.isOnline ? 'green' : 'red'">
            {{ record.isOnline ? '在线' : '离线' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-popconfirm
            title="确定要删除该成员吗?"
            @confirm="onDelete(record.key)"
          >
            <a-button type="link" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <!-- 添加成员的模态框 -->
    <a-modal
      v-model:visible="showAddModal"
      title="添加成员"
      @ok="handleAddOk"
    >
      <a-form layout="vertical">
        <a-form-item label="账号" required>
          <a-input
            v-model:value="newAccount"
            placeholder="请输入账号"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.editable-cell {
  position: relative;
}

.editable-cell-input-wrapper,
.editable-cell-text-wrapper {
  padding-right: 24px;
}

.editable-cell-text-wrapper {
  padding: 5px 24px 5px 5px;
}
</style>
