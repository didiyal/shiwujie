import http from '@/utils/request'

/**
 * 活动管理API服务类
 * 使用完整的URL路径，不进行拼接
 */
class ActivityApiService {
  /**
   * 创建活动
   * @param {ActivityCreateRequest} activityData 活动数据
   * @returns {Promise<ActivityVO>}
   */
  createActivity(activityData) {
    return http.post('/community/activity/add', activityData)
  }

  /**
   * 更新活动信息
   * @param {ActivityUpdateRequest} activityData 活动数据
   * @returns {Promise<ActivityVO>}
   */
  updateActivity(activityData) {
    return http.post('/community/activity/update', activityData)
  }

  /**
   * 删除活动
   * @param {number|string} activityId 活动ID
   * @returns {Promise<boolean>}
   */
  deleteActivity(activityId) {
    return http.post('/community/activity/delete', null, { params: { activityId } })
  }

  /**
   * 获取活动详情
   * @param {number|string} activityId 活动ID
   * @returns {Promise<ActivityVO>}
   */
  getActivityById(activityId) {
    return http.get('/community/activity/get/vo', { activityId })
  }

  /**
   * 获取活动列表
   * @param {number} current 当前页
   * @param {number} size 每页大小
   * @param {object} filters 筛选条件
   * @returns {Promise<Page<ActivityVO>>}
   */
  getActivityList(current = 1, size = 10, filters = {}) {
    return http.get('/community/activity/list/vo', {
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
    return http.post('/community/activity/sign', signData)
  }

  /**
   * 取消活动报名
   * @param {number|string} activityId 活动ID
   * @param {number|string} volunteerId 志愿者ID
   * @returns {Promise<boolean>}
   */
  cancelActivitySign(activityId, volunteerId) {
    return http.delete('/community/activity/sign/cancel', { 
      activityId, 
      volunteerId
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
    return http.get('/community/activity/sign/list', {
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
    return http.post('/community/activity/sign/review', reviewData)
  }

  /**
   * 获取活动统计信息
   * @param {number|string} activityId 活动ID
   * @returns {Promise<ActivityStatsVO>}
   */
  getActivityStats(activityId) {
    return http.get('/community/activity/stats', { activityId })
  }

  /**
   * 分页查询活动下的报名签到VO
   * @param {object} params 查询参数
   * @param {number|string} params.communityId 社区ID
   * @param {number|string} params.activityId 活动ID (可选)
   * @param {number|string} params.volunteerId 志愿者ID (可选)
   * @param {number|string} params.blindId 盲人ID (可选)
   * @param {number} params.current 当前页
   * @param {number} params.pageSize 每页大小
   * @returns {Promise<Page<ActivitySignVO>>}
   */
  getActivitySignListPageVO(params) {
    return http.get('/community/activitysign/list/page/vo', params)
  }
}

// 创建单例实例
const activityApi = new ActivityApiService()

export { ActivityApiService, activityApi } 