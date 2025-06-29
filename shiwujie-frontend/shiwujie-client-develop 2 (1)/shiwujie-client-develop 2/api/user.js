// api/user.js
import http from '../utils/request.js' 
import Storage from '../utils/storage.js'


// 添加一个防重复弹窗的标记
let isShowingModal = false;

export async function checkUserCertification() {
  // 如果已经在显示弹窗，直接返回
  if (isShowingModal) {
    return false;
  }

  try {
    const res = await getUserInfo(); 
    const user = res.data?.data;

    const isCertified = Boolean(user?.userCertificate) && user.userCertificate !== 'false';

    if (!isCertified) {
      isShowingModal = true; // 设置标记
      
      return new Promise((resolve) => {
        uni.showModal({
          title: '提示',
          content: '您还未完善证件信息，无法使用该功能',
          confirmText: '去补全证件',
          showCancel: false,
          success: () => {
            // 同时触发状态更新和页面跳转
            uni.$emit('updateTabBarManual', 4); // 4是"我的"页面的id
            
            uni.switchTab({
              url: '/pages/userLayout/userLayout',
              success: () => {
                // 跳转成功后再次确保状态更新
                uni.$emit('updateTabBarManual', 4);
                // 触发一个特殊的事件表明这是从认证检查跳转来的
                uni.$emit('fromCertificationCheck', true);
              }
            });
            resolve(false);
          },
          complete: () => {
            isShowingModal = false; // 重置标记
          }
        });
      });
    }
    return true;
  } catch (err) {
    console.error('检查用户认证状态失败：', err);
    isShowingModal = false; // 确保错误时也重置标记
    return false;
  }
}

/**
 * 获取当前用户信息
 * @returns {Promise} 返回用户信息请求的Promise对象
 */
export function getUserInfo() {
  const token = uni.getStorageSync('jwt_token');
  
  return uni.request({
    url: 'http://43.139.38.62:8081/shiwujie/user/mine/check',
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': `Bearer ${token}`,
    },
    withCredentials: true
  });
}

/**
 * 添加用户证件
 * @param {string} certificate 身份证件号码
 * @returns {Promise} 返回添加结果
 */
export function addCertificate(certificate) {
  // 将certificate作为query参数，而不是请求体
  return http.post(
    `http://43.139.38.62:8081/shiwujie/user/certificate/add?certificate=${encodeURIComponent(certificate)}`,  // URL中包含查询参数
    {},  // 空的请求体，因为参数已在URL中
    {  // options (可选)
      withCredentials: true
    }
  );
}


/**
 * 注销用户账户
 * @returns {Promise} 返回注销结果
 */
export function deleteUserAccount() {
  return http.get(
    'http://43.139.38.62:8081/shiwujie/user/mine/delete',
    {
      withCredentials: true
    }
  );
}