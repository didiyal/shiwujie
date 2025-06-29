<script lang="ts">
import { defineComponent, ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ArrowLeftOutlined } from "@ant-design/icons-vue";

export default defineComponent({
  name: "ActivityContent",
  components: {
    ArrowLeftOutlined,
  },
  setup() {
    const router = useRouter();
    const route = useRoute();

    // 使用路由参数创建活动数据
    const activityData = ref({
      id: route.query.id,
      title: route.query.activityName as string,
      joinCount: route.query.joinCount,
      peopleCount: route.query.peopleCount,
      beginTime: route.query.beginTime,
      endTime: route.query.endTime,
      content: route.query.content,
      status: route.query.status,
      location: route.query.location,
      description: route.query.description,
      requirements: JSON.parse(route.query.requirements as string || '[]'),
      process: JSON.parse(route.query.process as string || '[]')
    });

    const goBack = () => {
      router.back();
    };

    // 实际项目中，这里可以根据路由参数获取具体活动数据
    onMounted(() => {
      console.log("活动ID:", route.query.id);
      // 这里可以调用API获取活动详情
    });

    return {
      activityData,
      goBack,
    };
  },
});
</script>

<template>
  <div class="activity-content">
    <div class="header">
      <a-button type="link" @click="goBack">
        <arrow-left-outlined />
        返回
      </a-button>
      <h2>{{ activityData.title }}</h2>
    </div>

    <a-descriptions bordered :column="3">
      <a-descriptions-item label="活动状态" :span="1">
        <a-tag :color="activityData.status === '进行中' ? 'green' : 'blue'">
          {{ activityData.status }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="活动地点" :span="1">
        {{ activityData.location }}
      </a-descriptions-item>
      <a-descriptions-item label="参与人数" :span="1">
        {{ activityData.joinCount }}/{{ activityData.peopleCount }}
      </a-descriptions-item>
      <a-descriptions-item label="活动时间" :span="3">
        {{ activityData.beginTime }} 至 {{ activityData.endTime }}
      </a-descriptions-item>
      <a-descriptions-item label="活动描述" :span="3">
        {{ activityData.description }}
      </a-descriptions-item>
    </a-descriptions>

    <div class="section">
      <h3>活动要求</h3>
      <ul>
        <li v-for="(req, index) in activityData.requirements" :key="index">
          {{ req }}
        </li>
      </ul>
    </div>

    <div class="section">
      <h3>活动流程</h3>
      <ul>
        <li v-for="(proc, index) in activityData.process" :key="index">
          {{ proc }}
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.activity-content {
  padding: 24px;
}

.header {
  margin-bottom: 24px;
}

.section {
  margin-top: 24px;
}

h3 {
  margin-bottom: 16px;
}

ul {
  padding-left: 20px;
}

li {
  margin-bottom: 8px;
}
</style>
