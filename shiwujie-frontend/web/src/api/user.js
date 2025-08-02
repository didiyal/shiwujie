import BaseApiService from './base'

/**
 * 用户管理API服务类
 * 继承自BaseApiService，提供用户相关的API调用
 */
class UserApiService extends BaseApiService {
  constructor() {
    super('/user')
  }

  /**
   * 志愿者一键登录注册
   * @param {string} phone 手机号
   * @returns {Promise<VolunteerLoginSuccessVO>}
   */
  volunteerQuickLogin(phone) {
    return this.post('/volunteer/login/loginAndRegisterQuickly', { phone })
  }

  /**
   * 志愿者密码登录注册
   * @param {string} phone 手机号
   * @param {string} password 密码
   * @returns {Promise<VolunteerLoginSuccessVO>}
   */
  volunteerLoginAndRegister(phone, password) {
    return this.post('/volunteer/login/loginAndRegister', {
      phone,
      password
    })
  }

  /**
   * 盲人一键登录注册
   * @param {string} phone 手机号
   * @returns {Promise<BlindLoginSuccessVO>}
   */
  blindQuickLogin(phone) {
    return this.post('/blind/login/loginAndRegisterQuickly', { phone })
  }

  /**
   * 盲人密码登录注册
   * @param {string} phone 手机号
   * @param {string} password 密码
   * @returns {Promise<BlindLoginSuccessVO>}
   */
  blindLoginAndRegister(phone, password) {
    return this.post('/blind/login/loginAndRegister', {
      phone,
      password
    })
  }

  /**
   * 更新志愿者信息
   * @param {VolunteerVO} volunteer 志愿者信息
   * @returns {Promise<boolean>}
   */
  updateVolunteerInfo(volunteer) {
    return this.post('/volunteer/update', volunteer)
  }

  /**
   * 更新盲人信息
   * @param {BlindVO} blind 盲人信息
   * @returns {Promise<boolean>}
   */
  updateBlindInfo(blind) {
    return this.post('/blind/update', blind)
  }

  /**
   * 获取志愿者信息
   * @param {number} volunteerId 志愿者ID
   * @returns {Promise<VolunteerVO>}
   */
  getVolunteerById(volunteerId) {
    return this.get('/volunteer/get/id/vo', { volunteerId })
  }

  /**
   * 获取盲人信息
   * @param {number} blindId 盲人ID
   * @returns {Promise<BlindVO>}
   */
  getBlindById(blindId) {
    return this.get('/blind/get/id/vo', { blindId })
  }

  /**
   * 检查志愿者登录状态
   * @returns {Promise<VolunteerVO>}
   */
  checkVolunteerLogin() {
    return this.get('/volunteer/login/check')
  }

  /**
   * 检查盲人登录状态
   * @returns {Promise<BlindVO>}
   */
  checkBlindLogin() {
    return this.get('/blind/login/check')
  }

  /**
   * 修改志愿者密码
   * @param {string} oldPassword 旧密码
   * @param {string} newPassword 新密码
   * @returns {Promise<boolean>}
   */
  updateVolunteerPassword(oldPassword, newPassword) {
    return this.post('/volunteer/update/password', {
      oldPassword,
      newPassword
    })
  }

  /**
   * 修改盲人密码
   * @param {string} oldPassword 旧密码
   * @param {string} newPassword 新密码
   * @returns {Promise<boolean>}
   */
  updateBlindPassword(oldPassword, newPassword) {
    return this.post('/blind/update/password', {
      oldPassword,
      newPassword
    })
  }
}

// 创建单例实例
const userApi = new UserApiService()

export { UserApiService, userApi } 