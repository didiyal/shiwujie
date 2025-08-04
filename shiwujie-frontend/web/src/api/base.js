import http from '@/utils/request'
import { processBigNumbers } from '@/utils/bigIntUtils'

/**
 * 基础API服务类
 * 提供通用的API调用方法
 */
class BaseApiService {
  constructor(baseUrl = '') {
    this.baseUrl = baseUrl
  }

  /**
   * GET请求
   * @param {string} url 请求路径
   * @param {object} params 查询参数
   * @param {object} config 额外配置
   * @returns {Promise}
   */
  get(url, params = {}, config = {}) {
    // 处理请求参数中的大数字
    const safeParams = processBigNumbers(params)
    console.log('🔍 BaseApiService.get - 原始参数:', params)
    console.log('🔍 BaseApiService.get - 安全参数:', safeParams)
    return http.get(this.baseUrl + url, safeParams, config)
  }

  /**
   * POST请求
   * @param {string} url 请求路径
   * @param {object} data 请求数据
   * @param {object} config 额外配置
   * @returns {Promise}
   */
  post(url, data = {}, config = {}) {
    const fullUrl = this.baseUrl + url
    // 处理请求数据中的大数字
    const safeData = processBigNumbers(data)
    console.log('🌐 BaseApiService.post - 完整URL:', fullUrl)
    console.log('📤 BaseApiService.post - 原始数据:', data)
    console.log('📤 BaseApiService.post - 安全数据:', safeData)
    return http.post(fullUrl, safeData, config)
  }

  /**
   * PUT请求
   * @param {string} url 请求路径
   * @param {object} data 请求数据
   * @param {object} config 额外配置
   * @returns {Promise}
   */
  put(url, data = {}, config = {}) {
    return http.put(this.baseUrl + url, data, config)
  }

  /**
   * DELETE请求
   * @param {string} url 请求路径
   * @param {object} params 查询参数
   * @param {object} config 额外配置
   * @returns {Promise}
   */
  delete(url, params = {}, config = {}) {
    // 处理请求参数中的大数字
    const safeParams = processBigNumbers(params)
    console.log('🔍 BaseApiService.delete - 原始参数:', params)
    console.log('🔍 BaseApiService.delete - 安全参数:', safeParams)
    return http.delete(this.baseUrl + url, safeParams, config)
  }

  /**
   * 上传文件
   * @param {string} url 请求路径
   * @param {FormData} formData 表单数据
   * @param {object} config 额外配置
   * @returns {Promise}
   */
  upload(url, formData, config = {}) {
    return http.post(this.baseUrl + url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      ...config
    })
  }
}

export default BaseApiService 