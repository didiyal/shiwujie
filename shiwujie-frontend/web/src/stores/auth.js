import { defineStore } from 'pinia'
import { communityApi, CommunityLoginModel, VolunteerModel } from '@/api'
import { addSafeIdToList } from '@/utils/bigIntUtils'

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
    // 初始化store，如果有token但没有用户信息，自动恢复
    async init() {
      if (this.token && !this.volunteer) {
        console.log('🔄 检测到token但无用户信息，自动恢复登录状态...');
        try {
          await this.checkLogin();
          console.log('✅ 自动恢复登录状态成功');
        } catch (error) {
          console.error('❌ 自动恢复登录状态失败:', error);
          this.clearLoginInfo();
        }
      }
    },

    // 社区管理人员登录
    async login(phone, password) {
      try {
        console.log('🔍 开始调用communityApi.login...')
        const response = await communityApi.login(phone, password)
        console.log('🔍 登录响应 (从API返回):', response)
        
        // 检查关键字段
        if (response && response.volunteer) {
          console.log('🔍 关键字段检查:')
          console.log('  - volunteerId:', response.volunteer.volunteerId, '(类型:', typeof response.volunteer.volunteerId, ')')
          console.log('  - communityId:', response.volunteer.communityId, '(类型:', typeof response.volunteer.communityId, ')')
          console.log('  - name:', response.volunteer.name)
          console.log('  - phone:', response.volunteer.phone)
        }
        
        // 创建登录模型
        const loginModel = new CommunityLoginModel(response)
        console.log('🔍 创建模型后的关键字段:')
        console.log('  - volunteerId:', loginModel.volunteer.volunteerId, '(类型:', typeof loginModel.volunteer.volunteerId, ')')
        console.log('  - communityId:', loginModel.volunteer.communityId, '(类型:', typeof loginModel.volunteer.communityId, ')')
        
        // 保存登录信息
        this.token = loginModel.token
        this.volunteer = loginModel.volunteer
        this.isLoggedIn = true
        
        localStorage.setItem('token', loginModel.token)
        
                 // 保存社区ID到localStorage - 使用安全的大数字处理工具
         if (loginModel.volunteer.communityId) {
           addSafeIdToList('userCommunities', loginModel.volunteer.communityId)
         }
        
        return loginModel
      } catch (error) {
        console.error('❌ 登录失败:', error)
        throw error
      }
    },

    // 检查登录状态
    async checkLogin() {
      try {
        const response = await communityApi.checkLogin()
        console.log('🔍 登录状态检查响应:', response)
        
        // 检查关键字段
        if (response) {
          console.log('🔍 checkLogin关键字段检查:')
          console.log('  - volunteerId:', response.volunteerId, '(类型:', typeof response.volunteerId, ')')
          console.log('  - communityId:', response.communityId, '(类型:', typeof response.communityId, ')')
          console.log('  - name:', response.name)
          console.log('  - phone:', response.phone)
        }
        
        // checkLogin接口直接返回VolunteerVO，不是包装在volunteer字段中
        this.volunteer = new VolunteerModel(response)
        this.isLoggedIn = true
        
                 // 保存社区ID到localStorage - 使用安全的大数字处理工具
         if (response && response.communityId) {
           addSafeIdToList('userCommunities', response.communityId)
         }
        
        return true
      } catch (error) {
        console.error('❌ 登录状态检查失败:', error)
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
      localStorage.removeItem('userCommunities')
    }
  }
}) 