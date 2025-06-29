// api/user.js
import { ref } from 'vue';

// 封装 uni.request 为 Promise
function request(options) {
  return new Promise((resolve, reject) => {
    uni.request({
      ...options,
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data);  // 请求成功，返回数据
        } else {
          reject(res.data);  // 请求失败，返回错误信息
        }
      },
      fail: (err) => {
        reject(err);  // 请求失败，返回错误信息
      }
    });
  });
}

/**
 * 创建家庭
 * @param {string} familyName - 家庭名称
 * @returns {Promise} 返回创建家庭请求的Promise对象
 */
export function createFamily(familyName) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('jwt_token');
    
    const baseUrl = 'http://43.139.38.62:8081';
    const url = `${baseUrl}/add?familyName=${encodeURIComponent(familyName)}`;

    console.log('请求URL:', url);
    console.log('token:', token);

    uni.request({
      url: url,
      method: 'GET',
      header: {
        'Authorization': `Bearer ${token}`
      },
      success: (res) => {
        console.log('请求成功:', res);
        resolve(res);
      },
      fail: (err) => {
        console.error('请求失败:', err);
        reject(err);
      }
    });
  });
}



/**
 * 加入家庭
 * @param {string} familyAccount - 家庭账号
 * @returns {Promise} 返回加入家庭请求的Promise对象
 */
export function joinFamily(familyAccount) {
  const token = uni.getStorageSync('jwt_token');
  
  return new Promise((resolve, reject) => {
    uni.request({
      url: 'http://43.139.38.62:8081/join',
      method: 'POST',
      header: {
        'Content-Type': 'application/x-www-form-urlencoded', // 修改Content-Type
        'Authorization': `Bearer ${token}`
      },
      // 使用 data 传递 query 参数
      data: {
        familyAccount: familyAccount
      },
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res);
        } else {
          reject(new Error(res.data?.description || '加入家庭失败'));
        }
      },
      fail: (err) => {
        reject(new Error('加入家庭请求失败：' + err.errMsg));
      }
    });
  });
}


/**
 * 解散家庭
*/
export function deleteFamilyInfo(id) {
    const token = uni.getStorageSync('jwt_token');  

    return uni.request({
        url: `http://43.139.38.62:8081/delete?id=${id}`,  // 适配 API 路由
        method: 'DELETE',
        header: {
            'Content-Type':'application/x-www-form-urlencoded',
            'Authorization': `Bearer ${token}`,
        },
    }).then(response => response.data).catch(error => {
        console.error('删除家庭请求失败:', error);
        return null;
    });
}



/** 获取家庭信息
*/
export function getFamilyByAccountInfo(familyAccount) {
  const token = uni.getStorageSync('jwt_token');

  return request({
    url: `http://43.139.38.62:8081/get/account?familyAccount=${familyAccount}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': `Bearer ${token}`,
    },
  });
}

// 通过ID获取家庭信息
export function getFamilyByIdInfo(id) {
  const token = uni.getStorageSync('jwt_token');

  return request({
    url: `http://43.139.38.62:8081/get/id?id=${id}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': `Bearer ${token}`,
    },
  });
}



/**
 * 从家庭中移除用户
 * @param {number} id - 要移除的用户ID
 * @returns {Promise} 返回移除用户请求的Promise对象
 */
export function removeUserFromFamilyInfo(id) {
  const token = uni.getStorageSync('jwt_token');
  
  return new Promise((resolve, reject) => {
    uni.request({
      url: 'http://43.139.38.62:8081/remove/user',
      method: 'DELETE',
      header: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': `Bearer ${token}`
      },
      data: {
        id: id
      },
      success: (res) => {
        // 统一使用后端返回的description
        if (res.statusCode === 200) {
          resolve(res.data?.description || '移除成功');
        } else {
          reject(res.data?.description || '移除失败');
        }
      },
      fail: (err) => {
        reject('请求失败，请检查网络');
      }
    });
  });
}


// 退出家庭
export function quitFamilyInfo() {
  const token = uni.getStorageSync('jwt_token');

  return request({
    url: `http://43.139.38.62:8081/leave`,
    method: 'DELETE',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': `Bearer ${token}`,
    },
  });
}



// 修改 API 方法
/**
 * 修改家庭名称
 * @param {Object} params - 请求参数
 * @param {string} params.familyName - 新的家庭名称
 * @param {number} params.id - 家庭ID
 * @returns {Promise} 返回修改家庭名称的Promise对象
 */
export function updateFamilyNameInfo(params) {
  const token = uni.getStorageSync('jwt_token');
  
  return request({
    url: 'http://43.139.38.62:8081/update/name',
    method: 'PUT',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': `Bearer ${token}`
    },
    data: {
      familyName: params.familyName,
      id: params.id
    }
  });
}
