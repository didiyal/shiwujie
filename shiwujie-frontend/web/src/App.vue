<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
/* 全局样式重置和基础设置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  font-size: 16px;
  scroll-behavior: smooth;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: 100vh;
}

#app {
  height: 100vh;
}

/* 自定义滚动条 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 全局动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* 全局按钮样式优化 */
.ant-btn {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.ant-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.ant-btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.ant-btn-primary:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
}

/* 全局卡片样式优化 */
.ant-card {
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07);
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ant-card:hover {
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

/* 全局输入框样式优化 */
.ant-input, .ant-input-search {
  border-radius: 8px;
  border: 1px solid #d9d9d9;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ant-input:focus, .ant-input-search:focus,
.ant-input:hover, .ant-input-search:hover {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

/* 全局表格样式优化 */
.ant-table {
  border-radius: 12px;
  overflow: hidden;
}

.ant-table-thead > tr > th {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-bottom: 2px solid #dee2e6;
  font-weight: 600;
}

/* 全局菜单样式优化 */
.ant-menu {
  border-radius: 8px;
}

.ant-menu-item {
  border-radius: 6px;
  margin: 2px 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ant-menu-item:hover {
  background: rgba(102, 126, 234, 0.1);
}

.ant-menu-item-selected {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

/* 全局统计数字样式 */
.ant-statistic-title {
  color: #666;
  font-weight: 500;
}

.ant-statistic-content {
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 768px) {
  html {
    font-size: 14px;
  }
  
  .ant-card {
    margin-bottom: 16px;
  }
}
</style> 