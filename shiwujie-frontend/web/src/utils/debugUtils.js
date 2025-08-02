/**
 * 调试工具 - 追踪大数字的完整数据流
 */

/**
 * 检查字符串中是否包含大数字
 * @param {string} text - 要检查的文本
 * @returns {Array} 找到的大数字数组
 */
export function findBigNumbers(text) {
  const patterns = [
    /"volunteerId":\s*(\d{19})/g,
    /"communityId":\s*(\d{19})/g,
    /"id":\s*(\d{19})/g,
    /"familyId":\s*(\d{19})/g
  ]
  
  const results = []
  patterns.forEach(pattern => {
    const matches = [...text.matchAll(pattern)]
    matches.forEach(match => {
      results.push({
        field: match[0].split(':')[0].replace(/"/g, ''),
        value: match[1],
        fullMatch: match[0]
      })
    })
  })
  
  return results
}

/**
 * 深度检查对象中的大数字
 * @param {any} obj - 要检查的对象
 * @param {string} path - 当前路径
 * @returns {Array} 找到的大数字数组
 */
export function inspectBigNumbers(obj, path = '') {
  const results = []
  
  if (obj === null || obj === undefined) {
    return results
  }
  
  if (typeof obj === 'number') {
    // 检查是否是大数字
    if (obj > 999999999999999999 || obj < -999999999999999999) {
      results.push({
        path: path,
        value: obj,
        type: 'number',
        stringValue: String(obj)
      })
    }
    return results
  }
  
  if (typeof obj === 'string') {
    // 检查字符串是否是大数字
    const num = Number(obj)
    if (!isNaN(num) && num > 999999999999999999) {
      results.push({
        path: path,
        value: obj,
        type: 'string',
        numberValue: num
      })
    }
    return results
  }
  
  if (Array.isArray(obj)) {
    obj.forEach((item, index) => {
      results.push(...inspectBigNumbers(item, `${path}[${index}]`))
    })
    return results
  }
  
  if (typeof obj === 'object') {
    for (const [key, value] of Object.entries(obj)) {
      const currentPath = path ? `${path}.${key}` : key
      results.push(...inspectBigNumbers(value, currentPath))
    }
    return results
  }
  
  return results
}

/**
 * 打印详细的数据流追踪信息
 * @param {string} stage - 阶段名称
 * @param {any} data - 数据
 * @param {string} description - 描述
 */
export function traceDataFlow(stage, data, description = '') {
  console.log(`🔍 [${stage}] ${description}`)
  console.log(`📊 数据类型:`, typeof data)
  
  if (typeof data === 'string') {
    console.log(`📝 字符串长度:`, data.length)
    console.log(`📝 字符串内容:`, data)
    
    // 检查字符串中的大数字
    const bigNumbers = findBigNumbers(data)
    if (bigNumbers.length > 0) {
      console.log(`🔢 在字符串中检测到大数字:`, bigNumbers)
    }
  } else if (typeof data === 'object' && data !== null) {
    console.log(`📦 对象内容:`, data)
    
    // 检查对象中的大数字
    const bigNumbers = inspectBigNumbers(data)
    if (bigNumbers.length > 0) {
      console.log(`🔢 在对象中检测到大数字:`, bigNumbers)
    }
  }
  
  console.log(`---`)
} 