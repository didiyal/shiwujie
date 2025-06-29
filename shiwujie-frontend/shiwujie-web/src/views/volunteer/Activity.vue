<script setup lang="ts">
import { computed, ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useActivityStore } from "@/stores/activity";
import { message } from "ant-design-vue";
import type { FormInstance, DatePickerProps } from "ant-design-vue";
import dayjs, { Dayjs } from 'dayjs';

interface ActivityItem {
  key: string;
  activityName: string;
  joinCount: number;
  peopleCount: number;
  beginTime: string;
  endTime: string;
  content: string;
  status: string;
  location: string;
  description: string;
  requirements: string[];
  process: string[];
}

interface FormState {
  activityName: string;
  location: string;
  peopleCount: number;
  beginTime: Dayjs | null;
  endTime: Dayjs | null;
  description: string;
  requirements: string[];
  process: string[];
  status: string;
}

const activityStore = useActivityStore();
const router = useRouter();

const columns = [
  {
    title: "活动名称",
    dataIndex: "activityName",
    width: "20%",
  },
  {
    title: "活动地点",
    dataIndex: "location",
    width: "15%",
  },
  {
    title: "参与人数",
    dataIndex: "joinCount",
    width: "15%",
  },
  {
    title: "活动时间",
    dataIndex: "beginTime",
    width: "20%",
  },
  {
    title: "状态",
    dataIndex: "status",
    width: "15%",
  },
  {
    title: "操作",
    key: "action",
    width: "15%",
  },
];

const dataSource = computed(() => activityStore.activities);
const showAddModal = ref(false);
const formRef = ref<FormInstance>();
const formState = ref<FormState>({
  activityName: "",
  location: "",
  peopleCount: 0,
  beginTime: null,
  endTime: null,
  description: "",
  requirements: [""],
  process: [""],
  status: "未开始"
});

const handleAdd = () => {
  showAddModal.value = true;
};

const handleAddOk = () => {
  formRef.value?.validate().then(() => {
    if (!formState.value.beginTime || !formState.value.endTime) {
      message.error('请选择时间');
      return;
    }

    const now = dayjs();
    const beginTime = formState.value.beginTime.format('YYYY年MM月DD日 HH时mm分ss秒');
    const endTime = formState.value.endTime.format('YYYY年MM月DD日 HH时mm分ss秒');

    // 根据开始时间判断活动状态
    let status = "未开始";
    if (formState.value.beginTime.isBefore(now)) {
      status = "进行中";
    }

    const newActivity: ActivityItem = {
      key: String(dataSource.value.length + 1),
      joinCount: 0,
      content: formState.value.description,
      beginTime,
      endTime,
      status,
      activityName: formState.value.activityName,
      location: formState.value.location,
      peopleCount: formState.value.peopleCount,
      description: formState.value.description,
      requirements: formState.value.requirements,
      process: formState.value.process
    };
    
    activityStore.addActivity(newActivity);
    message.success('添加成功');
    showAddModal.value = false;
    formState.value = {
      activityName: "",
      location: "",
      peopleCount: 0,
      beginTime: null,
      endTime: null,
      description: "",
      requirements: [""],
      process: [""],
      status: "未开始"
    };
  }).catch(() => {
    message.error('请填写完整信息');
  });
};

const onDelete = (key: string) => {
  activityStore.deleteActivity(key);
  message.success('删除成功');
};

const handleDetail = (record: ActivityItem) => {
  router.push({
    path: "/volunteer/activityContent",
    query: {
      id: record.key,
      activityName: record.activityName,
      joinCount: record.joinCount,
      peopleCount: record.peopleCount,
      beginTime: record.beginTime,
      endTime: record.endTime,
      content: record.content,
      status: record.status,
      location: record.location,
      description: record.description,
      requirements: JSON.stringify(record.requirements),
      process: JSON.stringify(record.process)
    },
  });
};

// 动态添加/删除要求
const addRequirement = () => {
  if (formState.value.requirements) {
    formState.value.requirements.push("");
  }
};

const removeRequirement = (index: number) => {
  if (formState.value.requirements) {
    formState.value.requirements.splice(index, 1);
  }
};

// 动态添加/删除流程
const addProcess = () => {
  if (formState.value.process) {
    formState.value.process.push("");
  }
};

const removeProcess = (index: number) => {
  if (formState.value.process) {
    formState.value.process.splice(index, 1);
  }
};

// 更新活动状态的函数
const updateActivityStatus = () => {
  const now = dayjs();
  dataSource.value.forEach(activity => {
    const beginTime = dayjs(activity.beginTime, 'YYYY年MM月DD日 HH时mm分ss秒');
    const endTime = dayjs(activity.endTime, 'YYYY年MM月DD日 HH时mm分ss秒');
    
    if (beginTime.isBefore(now) && endTime.isAfter(now)) {
      activityStore.updateActivity(activity.key, { status: "进行中" });
    } else if (endTime.isBefore(now)) {
      activityStore.updateActivity(activity.key, { status: "已结束" });
    } else {
      activityStore.updateActivity(activity.key, { status: "未开始" });
    }
  });
};

// 在组件挂载时和每分钟更新一次状态
onMounted(() => {
  updateActivityStatus();
  setInterval(updateActivityStatus, 60000);
});
</script>

<template>
  <div>
    <a-button
      type="primary"
      style="margin-bottom: 16px"
      @click="handleAdd"
    >
      添加活动
    </a-button>
    <a-table bordered :data-source="dataSource" :columns="columns">
      <template #bodyCell="{ column, text, record }">
        <!-- 参与人数列 -->
        <template v-if="column.dataIndex === 'joinCount'">
          {{ record.joinCount }}/{{ record.peopleCount }}
        </template>
        
        <!-- 活动时间列 -->
        <template v-if="column.dataIndex === 'beginTime'">
          {{ record.beginTime }} 至 {{ record.endTime }}
        </template>
        
        <!-- 状态列 -->
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === '进行中' ? 'green' : 'blue'">
            {{ record.status }}
          </a-tag>
        </template>
        
        <!-- 操作列 -->
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" @click="handleDetail(record)">详情</a-button>
            <a-popconfirm
              title="确定要删除该活动吗?"
              @confirm="onDelete(record.key)"
            >
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 添加活动的模态框 -->
    <a-modal
      v-model:open="showAddModal"
      title="添加活动"
      width="720px"
      @ok="handleAddOk"
    >
      <a-form
        ref="formRef"
        :model="formState"
        layout="vertical"
      >
        <a-form-item
          label="活动名称"
          name="activityName"
          :rules="[{ required: true, message: '请输入活动名称' }]"
        >
          <a-input v-model:value="formState.activityName" />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="活动地点"
              name="location"
              :rules="[{ required: true, message: '请输入活动地点' }]"
            >
              <a-input v-model:value="formState.location" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="计划人数"
              name="peopleCount"
              :rules="[{ required: true, message: '请输入计划人数' }]"
            >
              <a-input-number
                v-model:value="formState.peopleCount"
                :min="1"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="开始时间"
              name="beginTime"
              :rules="[{ required: true, message: '请选择开始时间' }]"
            >
              <a-date-picker
                v-model:value="formState.beginTime"
                show-time
                format="YYYY年MM月DD日 HH时mm分ss秒"
                style="width: 100%"
                :show-now="false"
                value-type="dayjs"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="结束时间"
              name="endTime"
              :rules="[{ required: true, message: '请选择结束时间' }]"
            >
              <a-date-picker
                v-model:value="formState.endTime"
                show-time
                format="YYYY年MM月DD日 HH时mm分ss秒"
                style="width: 100%"
                :show-now="false"
                value-type="dayjs"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item
          label="活动描述"
          name="description"
          :rules="[{ required: true, message: '请输入活动描述' }]"
        >
          <a-textarea
            v-model:value="formState.description"
            :rows="4"
          />
        </a-form-item>

        <a-form-item label="活动要求" required>
          <template v-for="(req, index) in formState.requirements" :key="index">
            <a-space align="baseline" style="display: flex; margin-bottom: 8px;">
              <a-input v-model:value="formState.requirements[index]" />
              <a-button
                type="link"
                danger
                @click="removeRequirement(index)"
                v-if="formState.requirements.length > 1"
              >
                删除
              </a-button>
            </a-space>
          </template>
          <a-button type="dashed" block @click="addRequirement">
            添加要求
          </a-button>
        </a-form-item>

        <a-form-item label="活动流程" required>
          <template v-for="(proc, index) in formState.process" :key="index">
            <a-space align="baseline" style="display: flex; margin-bottom: 8px;">
              <a-input v-model:value="formState.process[index]" />
              <a-button
                type="link"
                danger
                @click="removeProcess(index)"
                v-if="formState.process.length > 1"
              >
                删除
              </a-button>
            </a-space>
          </template>
          <a-button type="dashed" block @click="addProcess">
            添加流程
          </a-button>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style lang="less" scoped>
.editable-cell {
  position: relative;

  .editable-cell-input-wrapper,
  .editable-cell-text-wrapper {
    padding-right: 24px;
  }

  .editable-cell-text-wrapper {
    padding: 5px 24px 5px 5px;
  }

  .editable-cell-icon,
  .editable-cell-icon-check {
    position: absolute;
    right: 0;
    width: 20px;
    cursor: pointer;
  }

  .editable-cell-icon {
    margin-top: 4px;
    display: none;
  }

  .editable-cell-icon-check {
    line-height: 28px;
  }

  .editable-cell-icon:hover,
  .editable-cell-icon-check:hover {
    color: #108ee9;
  }

  .editable-add-btn {
    margin-bottom: 8px;
  }
}

.editable-cell:hover .editable-cell-icon {
  display: inline-block;
}
</style>
