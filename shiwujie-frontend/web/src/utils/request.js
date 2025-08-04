import axios from 'axios'
import { message } from 'ant-design-vue'
import router from '@/router'
import { processBigNumbers } from './bigIntUtils'
import { traceDataFlow, findBigNumbers } from './debugUtils'

// 创建axios实例
const request = axios.create({
  baseURL: '/api', // 使用相对路径，通过Vite代理转发
  timeout: 30000, // 增加超时时间到30秒
  headers: {
    'Content-Type': 'application/json'
  },
  // 禁用自动JSON解析，手动处理
  transformResponse: [function (data) {
    // 直接返回原始字符串，不进行JSON解析
    return data;
  }]
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 添加token到请求头
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    console.log('🚀 API请求:', {
      method: config.method?.toUpperCase(),
      url: config.url,
      data: config.data,
      params: config.params,
      headers: config.headers
    })
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    console.log('📥 API响应:', {
      url: response.config.url,
      status: response.status,
      headers: response.headers
    })
    
    // 由于禁用了自动JSON解析，response.data就是原始字符串
    const originalText = response.data
    traceDataFlow('原始响应', originalText, 'Axios返回的原始字符串')
    
    // 在JSON.parse之前预处理大数字
    let processedText = originalText
    const bigNumberPatterns = [
      { pattern: /"volunteerId":\s*(\d{19})/g, replacement: '"volunteerId":"$1"' },
      { pattern: /"communityId":\s*(\d{19})/g, replacement: '"communityId":"$1"' },
      { pattern: /"activityId":\s*(\d{19})/g, replacement: '"activityId":"$1"' },
      { pattern: /"blindId":\s*(\d{19})/g, replacement: '"blindId":"$1"' },
      { pattern: /"id":\s*(\d{19})/g, replacement: '"id":"$1"' },
      { pattern: /"reviewId":\s*(\d{19})/g, replacement: '"reviewId":"$1"' },
      { pattern: /"familyId":\s*(\d{19})/g, replacement: '"familyId":"$1"' }
    ]
    
    bigNumberPatterns.forEach(({ pattern, replacement }) => {
      const beforeCount = (processedText.match(pattern) || []).length
      processedText = processedText.replace(pattern, replacement)
      const afterCount = (processedText.match(pattern) || []).length
      console.log(`🔍 替换模式: ${pattern.source}`)
      console.log(`🔍 替换前匹配数: ${beforeCount}, 替换后匹配数: ${afterCount}`)
    })
    
    console.log('🔍 预处理后的JSON文本:', processedText)
    
    // 检查预处理是否成功
    const originalBigNumbers = findBigNumbers(originalText)
    const processedBigNumbers = findBigNumbers(processedText)
    console.log('🔍 预处理前大数字:', originalBigNumbers)
    console.log('🔍 预处理后大数字:', processedBigNumbers)
    
    // 使用预处理后的文本进行JSON解析
    let data
    try {
      data = JSON.parse(processedText, (key, value) => {
        console.log('🔍 JSON.parse reviver 被调用:', key, value, '类型:', typeof value)
        
        // 检查是否是大数字 (19位数字的阈值)
        if (typeof value === 'number') {
          // 检查是否是19位数字 (大于 10^18)
          if (value > 999999999999999999 || value < -999999999999999999) {
            console.log('🔍 检测到大数字，转换为字符串:', key, value, '->', String(value))
            return String(value)
          }
          // 检查是否超出JavaScript安全整数范围
          if (value > Number.MAX_SAFE_INTEGER || value < Number.MIN_SAFE_INTEGER) {
            console.log('🔍 检测到超出安全范围的大数字，转换为字符串:', key, value, '->', String(value))
            return String(value)
          }
        }
        return value
      })
      
      // 进一步深度处理大数字
      traceDataFlow('JSON解析后', data, '自定义JSON.parse处理后的数据')
      data = processBigNumbers(data)
      traceDataFlow('深度处理后', data, 'processBigNumbers处理后的数据')
      
      // 替换response.data，确保后续处理使用正确数据
      response.data = data
      
    } catch (error) {
      console.error('JSON解析错误:', error)
      // 即使解析失败，也尝试处理大数字
      response.data = processBigNumbers(response.data)
    }
    
    console.log('🔍 处理后的响应数据:', response.data)
    
    // 处理业务响应码
    console.log('🔍 业务响应码处理:', {
      code: response.data.code,
      message: response.data.message,
      data: response.data.data
    })
    
    // 如果data.data存在，也打印出来
    if (response.data.data) {
      console.log('🔍 data.data 内容:', response.data.data)
      console.log('🔍 data.data 类型:', typeof response.data.data)
      console.log('🔍 data.data 结构 (JSON):', JSON.stringify(response.data.data, null, 2))
    }
    
    switch (data.code) {
      case 1: // 成功 (参考app项目的逻辑)
        console.log('✅ 业务处理成功')
        console.log('🔍 返回给业务层的数据:', data.data)
        return data.data
        
      case 40010: // token失效，需要重新登录
        console.warn('Token失效,需要重新登录')
        handleTokenExpired()
        const tokenError = new Error(data.message || '登录已过期，请重新登录')
        tokenError.response = { data }
        return Promise.reject(tokenError)
        
      case 40000: // 需要重新选择身份
        console.warn('需要重新选择身份')
        handleIdentityError()
        const identityError = new Error(data.message || '身份验证失败')
        identityError.response = { data }
        return Promise.reject(identityError)
        
      default:
        console.error('❌ 业务处理失败:', data.code, data.message)
        // 创建一个包含完整错误信息的错误对象
        const error = new Error(data.message || '请求失败')
        error.response = { data } // 将完整的响应数据附加到错误对象上
        return Promise.reject(error)
    }
  },
  error => {
    console.error('💥 网络请求失败:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status,
      config: error.config
    })
    
    if (error.response) {
      // 服务器返回错误状态码
      const { status, data } = error.response
      
      switch (status) {
        case 401:
          handleTokenExpired()
          return Promise.reject(new Error('未授权，请重新登录'))
        case 403:
          return Promise.reject(new Error('权限不足'))
        case 404:
          return Promise.reject(new Error('请求的资源不存在'))
        case 500:
          return Promise.reject(new Error('服务器内部错误'))
        default:
          return Promise.reject(new Error(data?.message || `请求失败 (${status})`))
      }
    } else if (error.request) {
      // 网络错误
      return Promise.reject(new Error('网络连接失败，请检查网络设置'))
    } else {
      // 其他错误
      return Promise.reject(new Error(error.message || '请求失败'))
    }
  }
)

// 处理token过期
function handleTokenExpired() {
  // 清除本地存储
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  
  // 显示提示
  message.error('登录已过期，请重新登录')
  
  // 跳转到登录页
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

// 处理身份验证错误
function handleIdentityError() {
  // 清除本地存储
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  
  // 显示提示
  message.error('身份验证失败，请重新登录')
  
  // 跳转到登录页
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

// 封装请求方法
const http = {
  get(url, params, config = {}) {
    return request.get(url, { params, ...config })
  },
  
  post(url, data, config = {}) {
    console.log('🚀 http.post - URL:', url)
    console.log('📤 http.post - 数据:', data)
    console.log('⚙️ http.post - 配置:', config)
    return request.post(url, data, config)
  },
  
  put(url, data, config = {}) {
    return request.put(url, data, config)
  },
  
  delete(url, params, config = {}) {
    return request.delete(url, { params, ...config })
  }
}

export default http 