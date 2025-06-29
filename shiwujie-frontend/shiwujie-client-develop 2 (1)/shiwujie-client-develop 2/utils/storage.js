// 存储键名常量
export const STORAGE_KEYS = {
  TOKEN: 'jwt_token',
  USER_INFO: 'userInfo',
  USER_ROLE: 'userRole',
  FAMILY_LIST: 'familyList',
};

const Storage = {
  /** 存储 Token */
  setToken(token) {
    uni.setStorageSync(STORAGE_KEYS.TOKEN, token);
  },

  /** 获取 Token */
  getToken() {
    return uni.getStorageSync(STORAGE_KEYS.TOKEN) || null;
  },

  /** 删除 Token */
  removeToken() {
    uni.removeStorageSync(STORAGE_KEYS.TOKEN);
  },

  /** 存储用户信息 */
  setUserInfo(userInfo) {
    uni.setStorageSync(STORAGE_KEYS.USER_INFO, userInfo);
  },

  /** 获取用户信息 */
  getUserInfo() {
    return uni.getStorageSync(STORAGE_KEYS.USER_INFO) || null;
  },

  /** 删除用户信息 */
  removeUserInfo() {
    uni.removeStorageSync(STORAGE_KEYS.USER_INFO);
  },

  /** 存储用户角色 */
  setUserRole(role) {
    uni.setStorageSync(STORAGE_KEYS.USER_ROLE, role);
  },

  /** 获取用户角色 */
  getUserRole() {
    return uni.getStorageSync(STORAGE_KEYS.USER_ROLE) || null;
  },


/** 存储家庭列表 */
  setFamilyList(familyList) {
    uni.setStorageSync(STORAGE_KEYS.FAMILY_LIST, familyList);
  },

  /** 获取家庭列表 */
  getFamilyList() {
    return uni.getStorageSync(STORAGE_KEYS.FAMILY_LIST) || null;
  },

  /** 删除家庭列表 */
  removeFamilyList() {
    uni.removeStorageSync(STORAGE_KEYS.FAMILY_LIST);
  },


  /** 清空所有存储 */
  clearAll() {
    uni.clearStorageSync();
  }
};

export default Storage;
