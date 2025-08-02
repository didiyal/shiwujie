import BaseApiService from './base'
import { processBigNumbers } from '@/utils/bigIntUtils'
import http from '@/utils/request'

/**
 * 社区管理API服务类
 * 继承自BaseApiService，提供社区相关的API调用
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
    // 确保communityId是字符串格式，避免精度丢失
    const safeUpdateData = {
      ...updateData,
      communityId: String(updateData.communityId)
    }
    console.log('🔍 updateCommunity - 原始数据:', updateData)
    console.log('🔍 updateCommunity - 安全数据:', safeUpdateData)
    return this.post('/update', safeUpdateData)
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

  /**
   * 分页查询社区下的子社区列表
   * @param {number|string} communityId 父社区ID
   * @param {number} current 当前页
   * @param {number} pageSize 每页大小
   * @returns {Promise<Page<CommunityVO>>}
   */
  getSubCommunityList(communityId, current = 1, pageSize = 10) {
    // 确保communityId是字符串格式，避免精度丢失
    const safeCommunityId = String(communityId)
    console.log('🔍 getSubCommunityList - 原始ID:', communityId, '类型:', typeof communityId)
    console.log('🔍 getSubCommunityList - 安全ID:', safeCommunityId, '类型:', typeof safeCommunityId)
    return this.get('/sub/list/vo', { 
      communityId: safeCommunityId,
      current,
      pageSize
    })
  }

  /**
   * 查询社区下的员工(志愿者)
   * @param {number|string} communityId 社区ID
   * @param {number} current 当前页
   * @param {number} pageSize 每页大小
   * @returns {Promise<Page<VolunteerVO>>}
   */
  getCommunityEmployees(communityId, current = 1, pageSize = 10) {
    // 确保communityId是字符串格式，避免精度丢失
    const safeCommunityId = String(communityId)
    console.log('🔍 getCommunityEmployees - 原始ID:', communityId, '类型:', typeof communityId)
    console.log('🔍 getCommunityEmployees - 安全ID:', safeCommunityId, '类型:', typeof safeCommunityId)
    // 直接使用完整的API路径，不依赖baseURL拼接
    return http.get('/community/communitymanager/employees', { 
      communityId: safeCommunityId,
      current,
      pageSize
    })
  }
}

// 创建单例实例
const communityApi = new CommunityApiService()

export { CommunityApiService, communityApi } 