const BASE_URL = 'http://47.92.170.90:8080';
export { BASE_URL };
/**
 * 封装的HTTP请求方法
 * @param {Object} option - 请求选项
 * @param {string} option.url - 请求路径
 * @param {string} option.method - 请求方法，默认为GET
 * @param {Object} option.data - 请求数据
 * @param {Object} option.header - 请求头
 * @param {boolean} option.showLoading - 是否显示加载提示，默认为true
 * @returns {Promise} 返回Promise对象
 */
export function http(option) {
  if (option.showLoading !== false) {
    uni.showLoading({
      title: option.loadingText || "加载中",
      mask: true
    });
  }
  
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + option.url,
      method: option.method || "GET",
      data: option.data || {},
      header: option.header || {},
      timeout: option.timeout || 30000,
      success: res => {
        // 处理响应数据
        if (res.statusCode >= 200 && res.statusCode < 300) {
          // 处理特殊格式的响应 [null, {data: {...}}]
          if (Array.isArray(res.data) && res.data.length > 1 && res.data[0] === null && res.data[1]) {
            console.log('检测到特殊格式响应，进行转换');
            resolve(res.data[1]);
          } else {
            resolve(res);
          }
        } else {
          console.error('请求失败，状态码:', res.statusCode);
          reject({
            errMsg: `请求失败，状态码: ${res.statusCode}`,
            statusCode: res.statusCode,
            data: res.data
          });
        }
      },
      fail: err => {
        console.error('请求失败:', err);
        reject(err);
      },
      complete: () => {
        if (option.showLoading !== false) {
          uni.hideLoading();
        }
      }
    });
  });
}

/**
 * 封装的聊天API请求方法
 * @param {Object} messages - 消息数组
 * @param {string} model - 模型名称，默认为qwen2.5-vl-72b-instruct
 * @param {boolean} stream - 是否流式响应，默认为false
 * @returns {Promise} 返回Promise对象
 */
export function chatRequest(messages, model = "qwen2.5-vl-72b-instruct", stream = false) {
  return http({
    url: "/api/chat",
    method: "POST",
    data: {
      model: model,
      messages: messages,
      stream: stream
    }
  });
}

/**
 * 封装的TTS API请求方法
 * @param {string} content - 要转换为语音的文本内容
 * @returns {Promise} 返回Promise对象
 */
export function ttsRequest(content) {
  return http({
    url: "/api/tts",
    method: "POST",
    data: {
      content: content
    }
  });
}

/**
 * 封装的语音识别API请求方法
 * @param {string} filePath - 音频文件路径
 * @param {Object} options - 其他选项
 * @returns {Promise} 返回Promise对象
 */
export function speechToTextRequest(filePath, options = {}) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + "/api/speech-to-text",
      filePath: filePath,
      name: 'audio',
      method: 'POST',
      timeout: options.timeout || 60000,
      header: {
        'Content-Type': 'multipart/form-data'
      },
      formData: {
        format: options.format || 'pcm',
        sampleRate: options.sampleRate || '16000',
        numberOfChannels: options.numberOfChannels || '1',
        encodeBitRate: options.encodeBitRate || '16000'
      },
      success: (uploadRes) => {
        try {
          const result = JSON.parse(uploadRes.data);
          resolve(result);
        } catch (e) {
          reject({
            errMsg: '解析响应失败',
            error: e,
            data: uploadRes.data
          });
        }
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
}

export default {
  http,
  chatRequest,
  ttsRequest,
  speechToTextRequest,
  BASE_URL
}; 