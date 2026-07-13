<template>
  <div class="main-layout">
    <!-- 侧栏遮罩（移动端） -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>

    <!-- 侧栏 -->
    <aside class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-brand">
        <div class="logo-mark"><GlobalOutlined /></div>
        <span class="sb-text">视<span>无</span>界</span>
      </div>

      <nav class="sidebar-nav">
        <div class="nav-group">
          <div class="nav-group-label">概览</div>
          <a class="nav-item" :class="{ active: route.name === 'dashboard' }" @click="go('dashboard')">
            <DashboardOutlined class="nav-icon" /><span>仪表板</span>
          </a>
        </div>

        <div class="nav-group">
          <div class="nav-group-label">管理</div>
          <a class="nav-item" :class="{ active: isCommunity }" @click="go('community-list')">
            <HomeOutlined class="nav-icon" /><span>社区管理</span>
          </a>
          <a class="nav-item" :class="{ active: route.name === 'activity-list' || route.name === 'activity-sign' }" @click="go('activity-list')">
            <CalendarOutlined class="nav-icon" /><span>活动管理</span>
          </a>
          <a class="nav-item" :class="{ active: route.name === 'helppost-list' }" @click="go('helppost-list')">
            <MessageOutlined class="nav-icon" /><span>求助管理</span>
          </a>
          <a class="nav-item" :class="{ active: isUser }" @click="go('volunteer-list')">
            <TeamOutlined class="nav-icon" /><span>用户管理</span>
          </a>
          <a class="nav-item" :class="{ active: route.name === 'community-review' }" @click="go('community-review')">
            <CheckCircleOutlined class="nav-icon" /><span>加入审核</span>
          </a>
        </div>

        <div class="nav-group">
          <div class="nav-group-label">数据</div>
          <a class="nav-item" :class="{ active: route.name === 'community-stats' || route.name === 'activity-stats' }" @click="go('community-stats')">
            <BarChartOutlined class="nav-icon" /><span>数据统计</span>
          </a>
        </div>

        <a class="nav-item logout" @click="handleLogout">
          <LogoutOutlined class="nav-icon" /><span>退出登录</span>
        </a>

        <a class="nav-item back-home" @click="goHome">
          <HomeOutlined class="nav-icon" /><span>返回首页</span>
        </a>
      </nav>

      <div class="sidebar-foot">
        <div class="avatar">{{ initial }}</div>
        <span class="sb-user">{{ displayName }}</span>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main-area">
      <header class="header">
        <div class="header-left">
          <button class="collapse-btn" @click="sidebarOpen = !sidebarOpen" aria-label="菜单">
            <MenuOutlined />
          </button>
          <span class="header-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <button class="icon-btn" aria-label="通知">
            <BellOutlined />
          </button>
          <div class="avatar" @click="handleLogout" title="退出登录">{{ initial }}</div>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  GlobalOutlined,
  DashboardOutlined,
  HomeOutlined,
  CalendarOutlined,
  MessageOutlined,
  TeamOutlined,
  CheckCircleOutlined,
  BarChartOutlined,
  LogoutOutlined,
  BellOutlined,
  MenuOutlined
} from '@ant-design/icons-vue'

const TITLE_MAP = {
  dashboard: '仪表板',
  'community-list': '社区管理',
  'community-edit': '编辑社区',
  'community-review': '加入审核',
  'activity-list': '活动管理',
  'activity-sign': '报名管理',
  'activity-sign-detail': '报名详情',
  'helppost-list': '求助管理',
  'volunteer-list': '志愿者管理',
  'employee-list': '员工管理',
  'blind-list': '视障人士管理',
  'community-stats': '社区统计',
  'activity-stats': '活动统计'
}

export default {
  name: 'MainLayout',
  components: {
    GlobalOutlined, DashboardOutlined, HomeOutlined, CalendarOutlined,
    MessageOutlined, TeamOutlined, CheckCircleOutlined, BarChartOutlined,
    LogoutOutlined, BellOutlined, MenuOutlined
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const authStore = useAuthStore()
    const sidebarOpen = ref(false)

    const volunteerInfo = computed(() => authStore.volunteerInfo || authStore.volunteer || {})
    const displayName = computed(() => volunteerInfo.value?.name || '管理员')
    const initial = computed(() => {
      const n = displayName.value
      return n ? n.charAt(0) : '管'
    })

    const pageTitle = computed(() => TITLE_MAP[route.name] || '管理后台')

    const isCommunity = computed(() =>
      ['community-list', 'community-edit', 'community-review'].includes(route.name)
    )
    const isUser = computed(() =>
      ['volunteer-list', 'employee-list', 'blind-list'].includes(route.name)
    )

    const go = (name) => {
      sidebarOpen.value = false
      router.push({ name })
    }

    const handleLogout = () => {
      authStore.clearLoginInfo()
      router.push('/login')
    }

    const goHome = () => {
      window.open('/', '_blank')
    }

    return {
      sidebarOpen,
      route,
      volunteerInfo,
      displayName,
      initial,
      pageTitle,
      isCommunity,
      isUser,
      go,
      handleLogout,
      goHome
    }
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: var(--bg);
}

/* ===================== 侧栏 ===================== */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 232px;
  background: var(--sidebar);
  color: #fff;
  display: flex;
  flex-direction: column;
  z-index: 100;
  overflow-y: auto;
}
.sidebar-brand {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}
.logo-mark {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
}
.sb-text {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.01em;
}
.sb-text span {
  color: var(--primary);
}

.sidebar-nav {
  flex: 1;
  padding: 10px 8px;
  display: flex;
  flex-direction: column;
}
.nav-group {
  margin-bottom: 6px;
}
.nav-group-label {
  font-size: 10px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.32);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 10px 12px 4px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 36px;
  padding: 0 12px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.65);
  cursor: pointer;
  transition: var(--tr);
  user-select: none;
  position: relative;
}
.nav-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.06);
}
.nav-item.active {
  color: #fff;
  background: rgba(0, 113, 227, 0.22);
}
.nav-item.active::after {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background: var(--primary);
  border-radius: 0 2px 2px 0;
}
.nav-icon {
  font-size: 15px;
  opacity: 0.6;
}
.nav-item:hover .nav-icon,
.nav-item.active .nav-icon {
  opacity: 1;
}
.nav-item.logout {
  margin-top: auto;
  color: rgba(255, 255, 255, 0.4);
}
.nav-item.logout:hover {
  color: var(--danger);
  background: rgba(255, 59, 48, 0.12);
}
.nav-item.back-home {
  color: rgba(255, 255, 255, 0.4);
}
.nav-item.back-home:hover {
  color: var(--primary, #0071e3);
  background: rgba(0, 113, 227, 0.12);
}

.sidebar-foot {
  padding: 10px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.avatar {
  width: 28px;
  height: 28px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.sb-user {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  flex: 1;
}

.sidebar-overlay {
  display: none;
}

/* ===================== 主区域 ===================== */
.main-area {
  margin-left: 232px;
  min-height: 100vh;
}
.header {
  position: sticky;
  top: 0;
  z-index: 50;
  height: 56px;
  background: rgba(245, 245, 247, 0.78);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}
.collapse-btn {
  display: none;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text);
  cursor: pointer;
  font-size: 17px;
}
.collapse-btn:hover {
  background: var(--border-l);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--tr);
}
.icon-btn:hover {
  background: var(--border-l);
  color: var(--text);
}
.header-right .avatar {
  background: var(--primary-t);
  color: var(--primary);
}

.content {
  padding: 24px 28px;
  min-height: calc(100vh - 56px);
}

/* ===================== 响应式 ===================== */
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    width: 260px;
  }
  .sidebar.open {
    transform: translateX(0);
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.2);
  }
  .sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.3);
    z-index: 99;
  }
  .collapse-btn {
    display: flex;
  }
  .main-area {
    margin-left: 0;
  }
  .header {
    padding: 0 16px;
  }
  .content {
    padding: 16px 14px;
  }
}
</style>
