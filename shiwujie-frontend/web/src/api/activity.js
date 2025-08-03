import BaseApiService from './base'

/**
 * 活动管理API服务类
 * 继承自BaseApiService，提供活动相关的API调用
 */
class ActivityApiService extends BaseApiService {
  constructor() {
    super('/community/activity')
  }

  /**
   * 创建活动
   * @param {ActivityAddRequest} activityData 活动数据
   * @returns {Promise<ActivityVO>}
   */
  createActivity(activityData) {
    return this.post('/add', activityData)
  }

  /**
   * 更新活动信息
   * @param {ActivityUpdateRequest} activityData 活动数据
   * @returns {Promise<ActivityVO>}
   */
  updateActivity(activityData) {
    // 确保activityId是字符串格式
    const safeData = { ...activityData }
    if (safeData.activityId) {
      safeData.activityId = String(safeData.activityId)
    }
    return this.post('/update', safeData)
  }

  /**
   * 删除活动
   * @param {number|string} activityId 活动ID
   * @returns {Promise<boolean>}
   */
  deleteActivity(activityId) {
    return this.post('/delete', { activityId: String(activityId) })
  }

  /**
   * 获取活动详情
   * @param {number|string} activityId 活动ID
   * @returns {Promise<ActivityVO>}
   */
  getActivityById(activityId) {
    return this.get('/get/id/vo', { activityId: String(activityId) })
  }

  /**
   * 分页查询社区下的活动列表
   * @param {Object} params 查询参数
   * @param {number|string} params.communityId 社区ID
   * @param {number} params.current 当前页
   * @param {number} params.pageSize 每页大小
   * @param {string} params.activityStatus 活动状态 (中文状态值)
   * @returns {Promise<Page<ActivityVO>>}
   */
  getActivityList(params) {
    const { communityId, current = 1, pageSize = 10, activityStatus } = params
    return this.get('/list/vo', {
      communityId: communityId ? String(communityId) : undefined,
      current,
      pageSize,
      activityStatus
    })
  }

  /**
   * 活动报名
   * @param {ActivitySignRequest} signData 报名数据
   * @returns {Promise<ActivitySignVO>}
   */
  signActivity(signData) {
    return this.post('/sign', signData)
  }

  /**
   * 取消活动报名
   * @param {number|string} activityId 活动ID
   * @param {number|string} volunteerId 志愿者ID
   * @returns {Promise<boolean>}
   */
  cancelActivitySign(activityId, volunteerId) {
    return this.delete('/sign/cancel', { 
      activityId: String(activityId), 
      volunteerId: String(volunteerId) 
    })
  }

  /**
   * 获取活动报名列表
   * @param {number|string} activityId 活动ID
   * @param {number} current 当前页
   * @param {number} size 每页大小
   * @returns {Promise<Page<ActivitySignVO>>}
   */
  getActivitySignList(activityId, current = 1, size = 10) {
    return this.get('/sign/list', {
      activityId: String(activityId),
      current,
      size
    })
  }

  /**
   * 审核活动报名
   * @param {ActivitySignReviewRequest} reviewData 审核数据
   * @returns {Promise<boolean>}
   */
  reviewActivitySign(reviewData) {
    return this.post('/sign/review', reviewData)
  }

  /**
   * 获取活动统计信息
   * @param {number|string} activityId 活动ID
   * @returns {Promise<ActivityStatsVO>}
   */
  getActivityStats(activityId) {
    return this.get('/stats', { activityId: String(activityId) })
  }
}

// 创建单例实例
const activityApi = new ActivityApiService()

export { ActivityApiService, activityApi } 