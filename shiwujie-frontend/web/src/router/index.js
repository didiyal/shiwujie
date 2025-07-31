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
  
  if (to.meta.requiresAuth) {
    if (!token) {
      next('/login')
      return
    }
    
    // 如果有token但不在登录页面，检查登录状态
    if (to.path !== '/login') {
      try {
        const authStore = useAuthStore()
        await authStore.checkLogin()
        next()
      } catch (error) {
        next('/login')
      }
    } else {
      next()
    }
  } else {
    // 不需要认证的页面
    if (token && to.path === '/login') {
      // 已登录用户访问登录页面，重定向到首页
      next('/')
    } else {
      next()
    }
  }
})

export default router 