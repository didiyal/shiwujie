import { BaseEntity } from './base'

/**
 * 视障人士信息模型
 * 完全按照后端的BlindVO结构
 */
export class BlindModel extends BaseEntity {
  constructor(data = {}) {
    super(data)
    
    // 完全按照后端BlindVO的字段定义
    // 处理大数字ID，确保精度不丢失
    this.blindId = data.blindId ? String(data.blindId) : null
    this.communityId = data.communityId ? String(data.communityId) : null
    this.familyId = data.familyId ? String(data.familyId) : null
    this.name = data.name || ''
    this.phone = data.phone || ''
    this.password = data.password || ''
    this.gender = data.gender || 0 // 0-男 1-女
    this.wechatId = data.wechatId || ''
    this.qqId = data.qqId || ''
    this.isIdCard = data.isIdCard || false
    this.isDisabilityCard = data.isDisabilityCard || false
    this.otherInfo = data.otherInfo || ''
    this.helpRequestCount = data.helpRequestCount || 0
    this.isActivelyJoined = data.isActivelyJoined || 0
    this.latitude = data.latitude || null
    this.longitude = data.longitude || null
    this.locationAddress = data.locationAddress || ''
    this.locationUpdateTime = data.locationUpdateTime || null
    
    // 为了兼容BaseEntity，设置id字段
    this.id = this.blindId
  }

  /**
   * 获取性别文本
   * @returns {string}
   */
  getGenderText() {
    return this.gender === 0 ? '男' : '女'
  }

  /**
   * 获取身份证状态文本
   * @returns {string}
   */
  getIdCardText() {
    return this.isIdCard ? '已认证' : '未认证'
  }

  /**
   * 获取残疾证状态文本
   * @returns {string}
   */
  getDisabilityCardText() {
    return this.isDisabilityCard ? '已认证' : '未认证'
  }

  /**
   * 获取加入状态文本
   * @returns {string}
   */
  getJoinStatusText() {
    return this.isActivelyJoined === 1 ? '已加入' : '未加入'
  }

  /**
   * 获取完整地址
   * @returns {string}
   */
  getFullAddress() {
    return this.locationAddress || '未设置位置'
  }

  /**
   * 转换为普通对象
   * @returns {object}
   */
  toObject() {
    return {
      ...super.toObject(),
      blindId: this.blindId,
      communityId: this.communityId,
      familyId: this.familyId,
      name: this.name,
      phone: this.phone,
      password: this.password,
      gender: this.gender,
      wechatId: this.wechatId,
      qqId: this.qqId,
      isIdCard: this.isIdCard,
      isDisabilityCard: this.isDisabilityCard,
      otherInfo: this.otherInfo,
      helpRequestCount: this.helpRequestCount,
      isActivelyJoined: this.isActivelyJoined,
      latitude: this.latitude,
      longitude: this.longitude,
      locationAddress: this.locationAddress,
      locationUpdateTime: this.locationUpdateTime
    }
  }

  /**
   * 从对象创建实例
   * @param {object} data 数据对象
   * @returns {BlindModel}
   */
  static fromObject(data) {
    return new BlindModel(data)
  }
} 