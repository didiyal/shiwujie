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
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1000;
}

.header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);
  pointer-events: none;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 24px;
  position: relative;
  z-index: 1;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 0.5px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: white;
  padding: 8px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.username {
  font-weight: 600;
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.sidebar {
  background: linear-gradient(180deg, #001529 0%, #003a70 100%);
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.1);
  position: relative;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%);
  pointer-events: none;
}

:deep(.ant-menu) {
  background: transparent;
  border: none;
  padding: 16px 8px;
}

:deep(.ant-menu-item) {
  margin: 4px 8px;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.ant-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1);
}

:deep(.ant-menu-item-selected) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

:deep(.ant-menu-submenu-title:hover) {
  background: rgba(255, 255, 255, 0.1) !important;
}

.logout-item {
  color: #ff4d4f !important;
  border-radius: 8px;
  margin: 0;
  transition: all 0.3s ease;
}

.logout-item:hover {
  background: #ff4d4f !important;
  color: white !important;
  transform: translateX(4px);
}

.main-content {
  background: transparent;
}

.content {
  margin: 24px;
  padding: 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  min-height: calc(100vh - 112px);
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }
  
  .logo-text {
    font-size: 16px;
  }
  
  .user-info {
    padding: 6px 12px;
  }
  
  .content {
    margin: 16px;
    padding: 16px;
  }
}

/* 深色主题适配 */
@media (prefers-color-scheme: dark) {
  .content {
    background: #1f1f1f;
    color: white;
  }
}
</style> 