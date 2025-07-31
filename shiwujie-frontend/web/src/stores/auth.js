import { defineStore } from 'pinia'
import { communityApi } from '@/api/community'
import { CommunityLoginModel, VolunteerModel } from '@/models/volunteer'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    volunteer: null,
    isLoggedIn: false
  }),

  getters: {
    hasToken: (state) => !!state.token,
    volunteerInfo: (state) => state.volunteer
  },

  actions: {
    // 社区管理人员登录
    async login(phone, password) {
      try {
        const data = await communityApi.login(phone, password)
        const loginModel = new CommunityLoginModel(data)
        
        // 保存登录信息，复用APP中的逻辑
        this.token = loginModel.token
        this.volunteer = loginModel.volunteer
        this.isLoggedIn = true
        
        localStorage.setItem('token', loginModel.token)
        return loginModel
      } catch (error) {
        throw error
      }
    },

    // 检查登录状态
    async checkLogin() {
      try {
        const data = await communityApi.checkLogin()
        this.volunteer = new VolunteerModel(data)
        this.isLoggedIn = true
        return true
      } catch (error) {
        this.clearLoginInfo()
        throw error
      }
    },

    // 清除登录信息
    clearLoginInfo() {
      this.token = ''
      this.volunteer = null
      this.isLoggedIn = false
      localStorage.removeItem('token')
    }
  }
}) 