export const API_BASE_URL = 'http://43.139.38.62:8081/';

export const API_ROUTES = {
  // 用户相关 API
  LOGIN: '/shiwujie/user/login', 
  LOGOUT: '/shiwujie/user/mine/logout', // 登出
  CHECK_AUTH: '/shiwujie/user/test/jwt', // 检查登录状态
  USER_INFO: '/shiwujie/user/mine/check', // 获取用户信息
  UPDATE_USER: '/shiwujie/user/mine/update', // 更新用户信息

  // 家庭相关 API
  FAMILY_ADD: '/shiwujie/family/add', // 添加家庭
  FAMILY_GET_BY_ACCOUNT: '/shiwujie/family/get/account', // 根据家庭账号获取家庭信息
  FAMILY_GET_BY_ID: '/shiwujie/family/get/id', // 根据 ID 获取家庭信息
  FAMILY_UPDATE_NAME: '/shiwujie/family/update/name', // 更新家庭名称
  FAMILY_REMOVE_USER: '/shiwujie/family/remove/user', // 移除家庭中的用户
  FAMILY_DELETE: '/shiwujie/family/delete', // 删除家庭
  FAMILY_JOIN:'/shiwujie/family/join',//加入家庭
  

  /* 
  FAMILY_INFO: '/shiwujie/family/info', // 查询家庭信息
  FAMILY_MEMBERS: '/shiwujie/family/members', // 查询家庭成员
  QUIT_FAMILY: '/shiwujie/family/quit', // 退出家庭 */
};

export const HTTP_STATUS = {  
    SUCCESS: 1,
    UNAUTHORIZED: 401,
    ERROR: 0
};

export const STORAGE_KEYS = {
    TOKEN: 'jwt_token',
    USER_INFO: 'user_info',
    USER_STATUS: 'user_status'
}; 