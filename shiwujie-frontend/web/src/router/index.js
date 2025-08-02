import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue')
      },
      {
        path: 'community-list',
        name: 'community-list',
        component: () => import('@/views/community/CommunityList.vue')
      },
      {
        path: 'community/:id/edit',
        name: 'community-edit',
        component: () => import('@/views/community/CommunityEdit.vue')
      },
      {
        path: 'community-review',
        name: 'community-review',
        component: () => import('@/views/community/CommunityReview.vue')
      },
      {
        path: 'activity-list',
        name: 'activity-list',
        component: () => import('@/views/activity/ActivityList.vue')
      },
      {
        path: 'activity-sign',
        name: 'activity-sign',
        component: () => import('@/views/activity/ActivitySign.vue')
      },
      {
        path: 'helppost-list',
        name: 'helppost-list',
        component: () => import('@/views/helppost/HelpPostList.vue')
      },
      {
        path: 'volunteer-list',
        name: 'volunteer-list',
        component: () => import('@/views/user/VolunteerList.vue')
      },
      {
        path: 'blind-list',
        name: 'blind-list',
        component: () => import('@/views/user/BlindList.vue')
      },
      {
        path: 'community-stats',
        name: 'community-stats',
        component: () => import('@/views/statistics/CommunityStats.vue')
      },
      {
        path: 'activity-stats',
        name: 'activity-stats',
        component: () => import('@/views/statistics/ActivityStats.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')
  const authStore = useAuthStore()
  
  if (to.meta.requiresAuth) {
    if (!token) {
      console.log('🚫 无token，重定向到登录页')
      next('/login')
      return
    }
    
    // 先初始化store，恢复用户信息
    await authStore.init()
    
    // 如果有token，检查本地状态
    if (!authStore.isLoggedIn) {
      try {
        console.log('🔍 本地状态未登录，检查登录状态...')
        await authStore.checkLogin()
        console.log('✅ 登录状态检查通过')
        next()
      } catch (error) {
        console.error('❌ 登录状态检查失败:', error)
        next('/login')
      }
    } else {
      // 本地状态已登录，直接通过
      console.log('✅ 本地状态已登录，直接通过')
      next()
    }
  } else {
    // 不需要认证的页面
    if (token && to.path === '/login') {
      // 已登录用户访问登录页面，重定向到首页
      console.log('🔄 已登录用户访问登录页，重定向到首页')
      next('/')
    } else {
      next()
    }
  }
})

export default router 