import request from '@/utils/request'

export const communityApi = {
  // 社区管理人员登录 - 使用正确的接口路径
  login(phone, password) {
    return request.post('/api/community/community/Login', {
      phone,
      password
    })
  },

  // 检查社区管理员登录状态 - 修正接口路径
  checkLogin() {
    return request.get('/api/community/community/login/check')
  },

  // 社区注册
  register(registerData) {
    return request.post('/api/community/Register', registerData)
  }
} 