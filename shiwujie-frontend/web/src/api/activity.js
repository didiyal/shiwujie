import BaseApiService from './base'

/**
 * 活动管理API服务类
 * 继承自BaseApiService，提供活动相关的API调用
 */
class ActivityApiService extends BaseApiService {
  constructor() {
    super('/activity')
  }

  /**
   * 创建活动
   * @param {ActivityCreateRequest} activityData 活动数据
   * @returns {Promise<ActivityVO>}
   */
  createActivity(activityData) {
    return this.post('/create', activityData)
  }

  /**
   * 更新活动信息
   * @param {ActivityUpdateRequest} activityData 活动数据
   * @returns {Promise<ActivityVO>}
   */
  updateActivity(activityData) {
    return this.post('/update', activityData)
  }

  /**
   * 删除活动
   * @param {number} activityId 活动ID
   * @returns {Promise<boolean>}
   */
  deleteActivity(activityId) {
    return this.delete('/delete', { activityId })
  }

  /**
   * 获取活动详情
   * @param {number} activityId 活动ID
   * @returns {Promise<ActivityVO>}
   */
  getActivityById(activityId) {
    return this.get('/get/id/vo', { activityId })
  }

  /**
   * 获取活动列表
   * @param {number} current 当前页
   * @param {number} size 每页大小
   * @param {object} filters 筛选条件
   * @returns {Promise<Page<ActivityVO>>}
   */
  getActivityList(current = 1, size = 10, filters = {}) {
    return this.get('/list', {
      current,
      size,
      ...filters
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
   * @param {number} activityId 活动ID
   * @param {number} volunteerId 志愿者ID
   * @returns {Promise<boolean>}
   */
  cancelActivitySign(activityId, volunteerId) {
    return this.delete('/sign/cancel', { activityId, volunteerId })
  }

  /**
   * 获取活动报名列表
   * @param {number} activityId 活动ID
   * @param {number} current 当前页
   * @param {number} size 每页大小
   * @returns {Promise<Page<ActivitySignVO>>}
   */
  getActivitySignList(activityId, current = 1, size = 10) {
    return this.get('/sign/list', {
      activityId,
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
   * @param {number} activityId 活动ID
   * @returns {Promise<ActivityStatsVO>}
   */
  getActivityStats(activityId) {
    return this.get('/stats', { activityId })
  }
}

// 创建单例实例
const activityApi = new ActivityApiService()

export { ActivityApiService, activityApi } 