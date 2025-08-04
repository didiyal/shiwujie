import { BaseEntity } from './base'

/**
 * 志愿者信息模型
 * 完全按照后端的VolunteerVO结构
 */
export class VolunteerModel extends BaseEntity {
  constructor(data = {}) {
    super(data)
    
    // 完全按照后端VolunteerVO的字段定义
    // 处理大数字ID，确保精度不丢失
    this.volunteerId = data.volunteerId ? String(data.volunteerId) : null
    this.communityId = data.communityId ? String(data.communityId) : null
    this.isActivelyJoined = data.isActivelyJoined || 0
    this.familyId = data.familyId || null
    this.name = data.name || ''
    this.phone = data.phone || ''
    this.password = data.password || ''
    this.gender = data.gender || 0 // 0-男 1-女
    this.wechatId = data.wechatId || ''
    this.qqId = data.qqId || ''
    this.isIdCard = data.isIdCard || false
    this.otherInfo = data.otherInfo || ''
    this.onlineStatus = data.onlineStatus || 0
    this.helpCount = data.helpCount || '0'
    this.rating = data.rating || null
    this.latitude = data.latitude || null
    this.longitude = data.longitude || null
    this.locationAddress = data.locationAddress || ''
    this.locationUpdateTime = data.locationUpdateTime || null
    this.communityManager = data.communityManager || ''
    
    // 为了兼容BaseEntity，设置id字段
    this.id = this.volunteerId
  }

  /**
   * 获取性别文本
   * @returns {string}
   */
  getGenderText() {
    return this.gender === 0 ? '男' : '女'
  }

  /**
   * 获取在线状态文本
   * @returns {string}
   */
  getOnlineStatusText() {
    return this.onlineStatus === 1 ? '在线' : '离线'
  }

  /**
   * 获取是否实名认证文本
   * @returns {string}
   */
  getIdCardText() {
    return this.isIdCard ? '已实名' : '未实名'
  }

  /**
   * 获取是否活跃加入文本
   * @returns {string}
   */
  getActivelyJoinedText() {
    return this.isActivelyJoined === 1 ? '活跃' : '非活跃'
  }

  /**
   * 转换为普通对象
   * @returns {object}
   */
  toObject() {
    return {
      ...super.toObject(),
      volunteerId: this.volunteerId,
      communityId: this.communityId,
      isActivelyJoined: this.isActivelyJoined,
      familyId: this.familyId,
      name: this.name,
      phone: this.phone,
      password: this.password,
      gender: this.gender,
      wechatId: this.wechatId,
      qqId: this.qqId,
      isIdCard: this.isIdCard,
      otherInfo: this.otherInfo,
      onlineStatus: this.onlineStatus,
      helpCount: this.helpCount,
      rating: this.rating,
      latitude: this.latitude,
      longitude: this.longitude,
      locationAddress: this.locationAddress,
      locationUpdateTime: this.locationUpdateTime,
      communityManager: this.communityManager
    }
  }

  /**
   * 从对象创建实例
   * @param {object} data 数据对象
   * @returns {VolunteerModel}
   */
  static fromObject(data) {
    return new VolunteerModel(data)
  }
}

/**
 * 社区登录成功响应模型
 */
export class CommunityLoginModel {
  constructor(data = {}) {
    this.volunteer = new VolunteerModel(data.volunteer || {})
    this.token = data.token || ''
  }

  /**
   * 转换为普通对象
   * @returns {object}
   */
  toObject() {
    return {
      volunteer: this.volunteer.toObject(),
      token: this.token
    }
  }

  /**
   * 从对象创建实例
   * @param {object} data 数据对象
   * @returns {CommunityLoginModel}
   */
  static fromObject(data) {
    return new CommunityLoginModel(data)
  }
}

/**
 * 志愿者登录成功响应模型
 */
export class VolunteerLoginModel {
  constructor(data = {}) {
    this.volunteer = new VolunteerModel(data.volunteer)
    this.token = data.token || ''
  }

  /**
   * 转换为普通对象
   * @returns {object}
   */
  toObject() {
    return {
      volunteer: this.volunteer.toObject(),
      token: this.token
    }
  }

  /**
   * 从对象创建实例
   * @param {object} data 数据对象
   * @returns {VolunteerLoginModel}
   */
  static fromObject(data) {
    return new VolunteerLoginModel(data)
  }
} 