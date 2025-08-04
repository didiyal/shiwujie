<template>
  <a-layout class="main-layout">
    <!-- 顶部导航栏 -->
    <a-layout-header class="header">
      <div class="header-content">
        <div class="logo">
          <span class="logo-text">视无界社区管理</span>
        </div>
        <div class="header-right">
          <div class="user-info">
            <a-avatar />
            <span class="username">{{ volunteerInfo?.name || '管理员' }}</span>
          </div>
        </div>
      </div>
    </a-layout-header>

    <a-layout>
      <!-- 侧边栏 -->
      <a-layout-sider
        :collapsed="collapsed"
        :trigger="null"
        collapsible
        class="sidebar"
        @collapse="collapsed = $event"
      >
        <a-menu
          :selectedKeys="selectedKeys"
          :openKeys="openKeys"
          mode="inline"
          theme="dark"
          @click="handleMenuClick"
          @update:selectedKeys="selectedKeys = $event"
          @update:openKeys="openKeys = $event"
        >
          <a-menu-item key="dashboard">
            📊
            <span>仪表板</span>
          </a-menu-item>
          
          <a-sub-menu key="community">
            <template #title>
              🏘️
              <span>社区管理</span>
            </template>
            <a-menu-item key="community-list">社区列表</a-menu-item>
            <a-menu-item key="community-review">加入审核</a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="activity">
            <template #title>
              📅
              <span>活动管理</span>
            </template>
            <a-menu-item key="activity-list">活动列表</a-menu-item>
            <a-menu-item key="activity-sign">报名管理</a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="helppost">
            <template #title>
              💬
              <span>求助管理</span>
            </template>
            <a-menu-item key="helppost-list">求助帖列表</a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="user">
            <template #title>
              👥
              <span>用户管理</span>
            </template>
            <a-menu-item key="volunteer-list">志愿者管理</a-menu-item>
            <a-menu-item key="employee-list">员工管理</a-menu-item>
            <a-menu-item key="blind-list">视障人士管理</a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="statistics">
            <template #title>
              📈
              <span>数据统计</span>
            </template>
            <a-menu-item key="community-stats">社区统计</a-menu-item>
            <a-menu-item key="activity-stats">活动统计</a-menu-item>
          </a-sub-menu>
          
          <!-- 退出登录 -->
          <a-menu-item key="logout" class="logout-item">
            🚪
            <span>退出登录</span>
          </a-menu-item>
        </a-menu>
      </a-layout-sider>

      <!-- 主内容区 -->
      <a-layout class="main-content">
        <a-layout-content class="content">
          <router-view />
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>
</template>

<script>
import { ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

export default {
  name: 'MainLayout',
  setup() {
    const router = useRouter();
    const route = useRoute();
    const authStore = useAuthStore();
    
    const collapsed = ref(false);
    const selectedKeys = ref([route.name]);
    const openKeys = ref(['community']);

    // 计算属性获取志愿者信息
    const volunteerInfo = computed(() => authStore.volunteerInfo);

    const handleMenuClick = ({ key }) => {
      if (key === 'logout') {
        // 退出登录
        authStore.clearLoginInfo();
        router.push('/login');
      } else {
        router.push({ name: key });
      }
    };

    return {
      collapsed,
      selectedKeys,
      openKeys,
      volunteerInfo,
      handleMenuClick
    };
  }
};
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
}

.header {
  background: #fff;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 24px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #333;
  padding: 8px 12px;
  border-radius: 6px;
}

.username {
  font-weight: 500;
}

.sidebar {
  background: #001529;
}

.main-content {
  background: #f0f2f5;
}

.content {
  margin: 24px;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100vh - 112px);
}

/* 退出登录样式 */
.logout-item {
  margin-top: auto !important;
  border-top: 1px solid #303030;
  color: #ff4d4f !important;
}

.logout-item:hover {
  background-color: #ff4d4f !important;
  color: #fff !important;
}

/* 确保退出登录在底部 */
:deep(.ant-menu) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

:deep(.ant-menu-item.logout-item) {
  margin-top: auto;
  border-top: 1px solid #303030;
}
</style> 