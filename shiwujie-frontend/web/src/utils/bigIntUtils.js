/**
 * 大数字ID处理工具
 * 用于处理JavaScript中19位数字ID的精度丢失问题
 */

/**
 * 将任意值转换为字符串格式的ID
 * 确保大数字不会丢失精度
 * @param {any} id - 要转换的ID
 * @returns {string} 字符串格式的ID
 */
export function toSafeId(id) {
  if (id === null || id === undefined || id === '') {
    return null;
  }
  
  // 如果已经是字符串，直接返回
  if (typeof id === 'string') {
    return id;
  }
  
  // 如果是数字，检查是否是大数字
  if (typeof id === 'number') {
    // 检查是否超出JavaScript安全整数范围
    if (id > Number.MAX_SAFE_INTEGER || id < Number.MIN_SAFE_INTEGER) {
      console.log('🔍 toSafeId: 检测到超出安全范围的大数字，转换为字符串:', id, '->', String(id));
      return String(id);
    }
    // 检查是否是大数字 (19位数字的阈值)
    if (id > 999999999999999999 || id < -999999999999999999) {
      console.log('🔍 toSafeId: 检测到大数字，转换为字符串:', id, '->', String(id));
      return String(id);
    }
  }
  
  return String(id);
}

/**
 * 检查两个ID是否相等（字符串比较）
 * @param {any} id1 - 第一个ID
 * @param {any} id2 - 第二个ID
 * @returns {boolean} 是否相等
 */
export function isIdEqual(id1, id2) {
  return toSafeId(id1) === toSafeId(id2);
}

/**
 * 深度处理对象中的大数字，将其转换为字符串
 * @param {any} obj - 要处理的对象
 * @returns {any} 处理后的对象
 */
export function processBigNumbers(obj) {
  if (obj === null || obj === undefined) {
    return obj;
  }
  
  if (typeof obj === 'number') {
    // 检查是否是大数字
    if (obj > 999999999999999999 || obj < -999999999999999999) {
      console.log('🔍 processBigNumbers: 检测到大数字，转换为字符串:', obj, '->', String(obj));
      return String(obj);
    }
    if (obj > Number.MAX_SAFE_INTEGER || obj < Number.MIN_SAFE_INTEGER) {
      console.log('🔍 processBigNumbers: 检测到超出安全范围的大数字，转换为字符串:', obj, '->', String(obj));
      return String(obj);
    }
    return obj;
  }
  
  if (typeof obj === 'string') {
    return obj;
  }
  
  if (Array.isArray(obj)) {
    return obj.map(item => processBigNumbers(item));
  }
  
  if (typeof obj === 'object') {
    const result = {};
    for (const [key, value] of Object.entries(obj)) {
      result[key] = processBigNumbers(value);
    }
    return result;
  }
  
  return obj;
}

/**
 * 从localStorage安全地获取ID列表
 * @param {string} key - localStorage键名
 * @returns {string[]} ID字符串数组
 */
export function getSafeIdList(key) {
  try {
    const data = localStorage.getItem(key);
    if (!data) return [];
    
    const idList = JSON.parse(data);
    if (!Array.isArray(idList)) return [];
    
    // 确保所有ID都是字符串格式
    return idList.map(id => toSafeId(id)).filter(id => id !== null);
  } catch (error) {
    console.error('解析localStorage ID列表失败:', error);
    return [];
  }
}

/**
 * 安全地保存ID列表到localStorage
 * @param {string} key - localStorage键名
 * @param {string[]} idList - ID字符串数组
 */
export function setSafeIdList(key, idList) {
  try {
    // 确保所有ID都是字符串格式
    const safeIdList = idList.map(id => toSafeId(id)).filter(id => id !== null);
    localStorage.setItem(key, JSON.stringify(safeIdList));
    console.log(`💾 安全保存ID列表到${key}:`, safeIdList);
  } catch (error) {
    console.error('保存ID列表到localStorage失败:', error);
  }
}

/**
 * 添加新ID到localStorage列表（去重）
 * @param {string} key - localStorage键名
 * @param {any} newId - 新ID
 */
export function addSafeIdToList(key, newId) {
  const safeNewId = toSafeId(newId);
  if (!safeNewId) {
    console.warn('尝试添加无效ID:', newId);
    return;
  }
  
  const currentList = getSafeIdList(key);
  if (!currentList.includes(safeNewId)) {
    currentList.push(safeNewId);
    setSafeIdList(key, currentList);
    console.log(`✅ 成功添加ID到${key}:`, safeNewId);
  } else {
    console.log(`⚠️ ID已存在于${key}:`, safeNewId);
  }
}

/**
 * 从localStorage列表中移除ID
 * @param {string} key - localStorage键名
 * @param {any} idToRemove - 要移除的ID
 */
export function removeSafeIdFromList(key, idToRemove) {
  const safeIdToRemove = toSafeId(idToRemove);
  if (!safeIdToRemove) {
    console.warn('尝试移除无效ID:', idToRemove);
    return;
  }
  
  const currentList = getSafeIdList(key);
  const newList = currentList.filter(id => id !== safeIdToRemove);
  
  if (newList.length !== currentList.length) {
    setSafeIdList(key, newList);
    console.log(`🗑️ 成功从${key}移除ID:`, safeIdToRemove);
  } else {
    console.log(`⚠️ ID不存在于${key}:`, safeIdToRemove);
  }
} 