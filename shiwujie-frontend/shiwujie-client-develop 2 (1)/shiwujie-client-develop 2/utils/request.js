import { API_BASE_URL, HTTP_STATUS } from '@/utils/config';
import Storage from '@/utils/storage';

class HttpRequest {
  constructor(baseUrl = API_BASE_URL) {
    this.baseUrl = baseUrl; // 统一基础 API 地址
    this.defaultOptions = {
      header: {
        'Content-Type': 'application/json'
      },
      withCredentials: true
    };
  }

  // 统一获取请求配置
  getRequestConfig(options) {
    const token = Storage.getToken(); // 统一获取 Token
    const fullUrl = options.url.startsWith('http') ? options.url : `${this.baseUrl}${options.url}`;

    return {
      ...this.defaultOptions,
      ...options,
      url: fullUrl, // 确保请求 URL 是完整的
      header: {
        ...this.defaultOptions.header,
        ...options.header,
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}) // 自动添加 Token（如果有）
      }
    };
  }

  // 处理响应
  handleResponse(res) {
    if (!res || !res.data) {
      return Promise.reject(new Error('服务器无响应'));
    }
    if (res.data.code === HTTP_STATUS.UNAUTHORIZED) {
      this.handleTokenExpired();
      return Promise.reject(new Error('登录已过期'));
    }
    if (res.data.code !== HTTP_STATUS.SUCCESS) {
      return Promise.reject(new Error(res.data.description || '请求失败'));
    }
    return res.data;
  }

  // 处理 Token 过期
  handleTokenExpired() {
    Storage.clearAll();
    uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/register-login/login' });
    }, 1500);
  }

  // 处理错误
  handleError(error) {
    
    return Promise.reject(error);
  }

  // 统一请求方法
  async request(options) {
    try {
      const config = this.getRequestConfig(options);
      const res = await uni.request(config); // 直接获取对象
      return this.handleResponse(res); // 传入正确的 res
    } catch (error) {
      return this.handleError(error);
    }
  }

  // GET 请求
  get(url, data = {}, options = {}) {
    return this.request({
      url,
      data,
      method: 'GET',
      ...options
    });
  }

  // POST 请求
  post(url, data = {}, options = {}) {
    return this.request({
      url,
      data,
      method: 'POST',
      ...options
    });
  }

  // PUT 请求
  put(url, data = {}, options = {}) {
    return this.request({
      url,
      data,
      method: 'PUT',
      ...options
    });
  }

  // DELETE 请求
  delete(url, data = {}, options = {}) {
    return this.request({
      url,
      data,
      method: 'DELETE',
      ...options
    });
  }
}

export const http = new HttpRequest();
export default http;
