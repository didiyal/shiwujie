import { BaseEntity } from './base'

/**
 * 社区信息模型
 */
export class CommunityModel extends BaseEntity {
  constructor(data = {}) {
    super(data)
    
    // 基本信息
    this.communityId = data.communityId || null
    this.communityName = data.communityName || ''
    this.communityDescription = data.communityDescription || ''
    this.communityTypeId = data.communityTypeId || null
    this.communityLevelId = data.communityLevelId || null
    
    // 地址信息
    this.province = data.province || ''
    this.city = data.city || ''
    this.district = data.district || ''
    this.address = data.address || ''
    
    // 管理信息
    this.registerVolunteerId = data.registerVolunteerId || null
    this.parentCommunityId = data.parentCommunityId || null
    this.isDefaultCommunity = data.isDefaultCommunity || 0
    this.communityStatus = data.communityStatus || 0
    
    // 其他信息
    this.registrationInfo = data.registrationInfo || ''
  }

  /**
   * 获取社区状态文本
   * @returns {string}
   */
  getStatusText() {
    switch (this.communityStatus) {
      case 0:
        return '未审核'
      case 1:
        return '已审核'
      case 2:
        return '已停用'
      default:
        return '未知状态'
    }
  }

  /**
   * 获取是否默认社区文本
   * @returns {string}
   */
  getDefaultCommunityText() {
    return this.isDefaultCommunity === 1 ? '是' : '否'
  }

  /**
   * 获取完整地址
   * @returns {string}
   */
  getFullAddress() {
    const parts = [this.province, this.city, this.district, this.address]
    return parts.filter(part => part).join(' ')
  }

  /**
   * 转换为普通对象
   * @returns {object}
   */
  toObject() {
    return {
      ...super.toObject(),
      communityId: this.communityId,
      communityName: this.communityName,
      communityDescription: this.communityDescription,
      communityTypeId: this.communityTypeId,
      communityLevelId: this.communityLevelId,
      province: this.province,
      city: this.city,
      district: this.district,
      address: this.address,
      registerVolunteerId: this.registerVolunteerId,
      parentCommunityId: this.parentCommunityId,
      isDefaultCommunity: this.isDefaultCommunity,
      communityStatus: this.communityStatus,
      registrationInfo: this.registrationInfo
    }
  }

  /**
   * 从对象创建实例
   * @param {object} data 数据对象
   * @returns {CommunityModel}
   */
  static fromObject(data) {
    return new CommunityModel(data)
  }
}

/**
 * 社区类型枚举
 */
export const CommunityTypeEnum = {
  SOCIAL_ORGANIZATIONS: {
    name: '社会团体',
    description: '企业,国企,个人成立的私人团体',
    typeId: 1
  },
  SELF_COVER_ORGANIZATIONS: {
    name: '基层群众自治组织',
    description: '居委会或村委会',
    typeId: 2
  },
  ESTABLISHED_UNIVERSITY: {
    name: '高校内部成立',
    description: '高校,初高中成立的组织',
    typeId: 3
  },
  OTHER_ORGANIZATIONS: {
    name: '其它公益组织',
    description: '其它组织',
    typeId: 4
  }
}

/**
 * 社区级别枚举
 */
export const CommunityLevelEnum = {
  PROVINCE: {
    name: '省级',
    description: '省级社区',
    levelId: 1
  },
  CITY: {
    name: '市级',
    description: '市级社区',
    levelId: 2
  },
  STREET: {
    name: '街道级',
    description: '街道级社区',
    levelId: 3
  }
}

/**
 * 社区状态枚举
 */
export const CommunityStatusEnum = {
  PENDING: 0,    // 未审核
  APPROVED: 1,   // 已审核
  DISABLED: 2    // 已停用
} 