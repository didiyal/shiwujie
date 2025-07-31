import axios from 'axios'
import { message } from 'ant-design-vue'

// 复用APP中的baseURL
const BASE_URL = 'http://43.139.38.62:8100'

const request = axios.create({
  baseURL: BASE_URL,
  timeout: 10000
})

// 请求拦截器 - 添加token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - 复用APP中的响应处理逻辑
request.interceptors.response.use(
  response => {
    const res = response.data
    
    // 复用APP中的响应码处理逻辑
    switch (res.code) {
      case 1:
        return res.data
      case 40010:
        // Token失效，需要重新登录
        localStorage.removeItem('token')
        window.location.href = '/login'
        message.error(res.message || '登录已过期，请重新登录')
        break
      case 40000:
        // 需要重新选择身份
        localStorage.removeItem('token')
        window.location.href = '/login'
        message.error(res.message || '身份验证失败，请重新登录')
        break
      default:
        message.error(res.message || '请求失败')
    }
    
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  error => {
    message.error('网络错误，请稍后重试')
    return Promise.reject(error)
  }
)

export default request 