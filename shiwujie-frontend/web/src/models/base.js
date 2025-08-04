/**
 * API响应的基础包装类
 * 参考APP中的BaseResponse结构
 */
export class BaseResponse {
  constructor(data = {}) {
    this.code = data.code || 0
    this.message = data.message || ''
    this.description = data.description || ''
    this.data = data.data || null
  }

  /**
   * 判断请求是否成功
   * @returns {boolean}
   */
  isSuccess() {
    return this.code === 0
  }

  /**
   * 判断是否需要重新登录
   * @returns {boolean}
   */
  needReLogin() {
    return this.code === 40010
  }

  /**
   * 判断是否需要重新选择身份
   * @returns {boolean}
   */
  needReChooseIdentity() {
    return this.code === 40000
  }

  /**
   * 获取错误信息
   * @returns {string}
   */
  getErrorMessage() {
    return this.message || this.description || '请求失败'
  }
}

/**
 * 分页响应数据类
 */
export class PageResponse {
  constructor(data = {}) {
    this.records = data.records || []
    this.total = data.total || 0
    this.size = data.size || 10
    this.current = data.current || 1
    this.pages = data.pages || 0
  }

  /**
   * 获取当前页数据
   * @returns {Array}
   */
  getRecords() {
    return this.records
  }

  /**
   * 获取总记录数
   * @returns {number}
   */
  getTotal() {
    return this.total
  }

  /**
   * 获取总页数
   * @returns {number}
   */
  getPages() {
    return this.pages
  }

  /**
   * 判断是否有下一页
   * @returns {boolean}
   */
  hasNext() {
    return this.current < this.pages
  }

  /**
   * 判断是否有上一页
   * @returns {boolean}
   */
  hasPrevious() {
    return this.current > 1
  }
}

/**
 * 基础实体类
 */
export class BaseEntity {
  constructor(data = {}) {
    this.id = data.id || null
    this.createTime = data.createTime || null
    this.updateTime = data.updateTime || null
  }

  /**
   * 转换为普通对象
   * @returns {object}
   */
  toObject() {
    return {
      id: this.id,
      createTime: this.createTime,
      updateTime: this.updateTime
    }
  }

  /**
   * 从对象创建实例
   * @param {object} data 数据对象
   * @returns {BaseEntity}
   */
  static fromObject(data) {
    return new BaseEntity(data)
  }
} 