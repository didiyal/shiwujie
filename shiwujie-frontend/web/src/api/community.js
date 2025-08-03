import BaseApiService from './base'
import { processBigNumbers } from '@/utils/bigIntUtils'
import http from '@/utils/request'

/**
 * 社区管理API服务类
 * 继承自BaseApiService，提供社区相关的API调用方法
 */
class CommunityApiService extends BaseApiService {
  constructor() {
    super('/community/community')
  }

  /**
   * 社区管理人员登录
   * @param {string} phone 手机号
   * @param {string} password 密码
   * @returns {Promise<CommunityLoginSuccessVO>}
   */
  login(phone, password) {
    return this.post('/Login', {
      phone,
      password
    })
  }

  /**
   * 检查社区管理员登录状态
   * @returns {Promise<VolunteerVO>}
   */
  checkLogin() {
    return this.get('/login/check')
  }

  /**
   * 社区入驻注册
   * @param {CommunityRegisterRequest} registerData 注册数据
   * @returns {Promise<CommunityLoginSuccessVO>}
   */
  register(registerData) {
    return this.post('/Register', registerData)
  }

  /**
   * 通过ID查询社区信息
   * @param {number|string} communityId 社区ID
   * @returns {Promise<CommunityVO>}
   */
  getCommunityById(communityId) {
    // 确保communityId是字符串格式，避免精度丢失
    const safeCommunityId = String(communityId)
    console.log('🔍 getCommunityById - 原始ID:', communityId, '类型:', typeof communityId)
    console.log('🔍 getCommunityById - 安全ID:', safeCommunityId, '类型:', typeof safeCommunityId)
    return this.get('/get/id/vo', { communityId: safeCommunityId })
  }

  /**
   * 修改社区信息
   * @param {CommunityUpdateRequest} updateData 更新数据
   * @returns {Promise<CommunityVO>}
   */
  updateCommunity(updateData) {
    return this.post('/update', updateData)
  }

  /**
   * 删除社区
   * @param {number|string} communityId 社区ID
   * @returns {Promise<boolean>}
   */
  deleteCommunity(communityId) {
    // 确保communityId是字符串格式，避免精度丢失
    const safeCommunityId = String(communityId)
    console.log('🔍 deleteCommunity - 原始ID:', communityId, '类型:', typeof communityId)
    console.log('🔍 deleteCommunity - 安全ID:', safeCommunityId, '类型:', typeof safeCommunityId)
    return this.delete('/delete', { communityId: safeCommunityId })
  }

  /**
   * 获取子社区列表
   * @param {number} communityId 父社区ID
   * @param {number} current 当前页
   * @param {number} size 每页大小
   * @returns {Promise<Page<CommunityVO>>}
   */
  getSubCommunities(communityId, current = 1, size = 10) {
    return this.get('/get/sub/communities', {
      communityId,
      current,
      size
    })
  }

  addCommunityManager(communityId, volunteerId, roleName) {
    const safeCommunityId = String(communityId)
    const safeVolunteerId = String(volunteerId)
    console.log('🔍 addCommunityManager - 原始参数:', { communityId, volunteerId, roleName })
    console.log('🔍 addCommunityManager - 安全参数:', { safeCommunityId, safeVolunteerId, roleName })
    
    return this.post('/community/communitymanager/manager/add', {
      communityId: safeCommunityId,
      volunteerId: safeVolunteerId,
      roleName
    })
  }

  /**
   * 获取社区加入审核列表
   * @returns {Promise<CommunityJoinReviewVO[]>}
   */
  getCommunityJoinReviewList() {
    return http.get('/community/communityjoinreview/get/list/vo')
  }

  /**
   * 更新社区加入审核状态
   * @param {Object} updateData 更新数据
   * @returns {Promise<Boolean>}
   */
  updateCommunityJoinReview(updateData) {
    return http.put('/community/communityjoinreview/update', updateData)
  }

  /**
   * 获取社区员工列表
   * @param {number|string} communityId 社区ID
   * @param {number} current 当前页
   * @param {number} pageSize 每页大小
   * @returns {Promise<Page<VolunteerVO>>}
   */
  getCommunityEmployees(communityId, current, pageSize) {
    return http.get('/community/communitymanager/employees', {
      communityId: String(communityId),
      current,
      pageSize
    })
  }
}

// 创建单例实例
const communityApi = new CommunityApiService()

export { CommunityApiService, communityApi } 