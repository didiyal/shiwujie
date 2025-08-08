<template>
  <div class="community-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">
            🏘️ 我的社区管理
          </h2>
          <p class="page-subtitle">管理您创建的社区信息</p>
        </div>
        <div class="header-right">
          <a-button type="primary" size="large" @click="handleRefresh">
            🔄 刷新
          </a-button>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-section">
      <div class="search-card">
        <a-row :gutter="16" align="middle">
          <a-col :xs="24" :sm="16" :md="12" :lg="8">
            <a-input-search
              v-model="searchForm.communityId"
              placeholder="输入社区ID搜索"
              @search="handleSearch"
              allow-clear
              size="large"
              class="search-input"
            />
          </a-col>
          <a-col :xs="24" :sm="8" :md="12" :lg="16" class="action-col">
            <div class="action-buttons">
              <span class="community-count">
                👥 共 {{ communityList.length }} 个社区
              </span>
            </div>
          </a-col>
        </a-row>
      </div>
    </div>

    <!-- 社区列表 -->
    <div class="community-grid" v-if="communityList.length > 0">
      <div 
        v-for="community in communityList" 
        :key="community.communityId" 
        class="community-card"
      >
        <div class="card-header">
          <div class="header-left">
            <h3 class="community-name">{{ community.communityName }}</h3>
            <div class="community-status" :class="getStatusClass(community.communityStatus)">
              <span class="status-dot"></span>
              {{ getStatusText(community.communityStatus) }}
            </div>
          </div>
          <div class="header-right">
            <a-dropdown>
              <a-button type="text" class="more-actions">
                ⋯
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="view" @click="viewDetail(community)">
                    👁️ 查看详情
                  </a-menu-item>
                  <a-menu-item key="edit" @click="editCommunity(community)">
                    ✏️ 编辑
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="delete" @click="confirmDelete(community)" danger>
                    🗑️ 删除社区
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>
        
        <div class="card-content">
          <!-- 基本信息 -->
          <div class="info-section">
            <h4 class="section-title">
              ℹ️ 基本信息
            </h4>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">社区类型：</span>
                <span class="value">{{ community.communityTypeName || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">社区等级：</span>
                <span class="value">{{ community.communityLevelName || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">注册人ID：</span>
                <span class="value volunteer-id">{{ community.registerVolunteerId || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">父级社区：</span>
                <span class="value">{{ community.parentCommunityId || '无' }}</span>
              </div>
            </div>
          </div>
          
          <!-- 地址信息 -->
          <div class="info-section">
            <h4 class="section-title">
              📍 地址信息
            </h4>
            <div class="address-info">
              <div class="address-item">
                <span class="address-icon">📍</span>
                <span class="address-text">{{ getFullAddress(community) }}</span>
              </div>
            </div>
          </div>
          
          <!-- 描述信息 -->
          <div class="info-section" v-if="community.communityDescription || community.registrationInfo">
            <h4 class="section-title">
              📄 详细信息
            </h4>
            <div class="description-content">
              <div v-if="community.communityDescription" class="description-item">
                <span class="description-label">社区介绍：</span>
                <p class="description-text">{{ community.communityDescription }}</p>
              </div>
              <div v-if="community.registrationInfo" class="description-item">
                <span class="description-label">注册信息：</span>
                <p class="description-text">{{ community.registrationInfo }}</p>
              </div>
            </div>
          </div>

          <!-- 子社区列表 -->
          <div class="info-section">
            <h4 class="section-title">
              🏘️ 子社区列表
              <a-button 
                type="link" 
                size="small" 
                @click="loadSubCommunities(community.communityId)"
                :loading="loadingSubCommunities === community.communityId"
                class="refresh-btn"
              >
                🔄 刷新子社区
              </a-button>
            </h4>
            <div class="sub-communities-content">
              <div v-if="community.subCommunities && community.subCommunities.length > 0" class="sub-communities-list">
                <div 
                  v-for="subCommunity in community.subCommunities" 
                  :key="subCommunity.communityId" 
                  class="sub-community-item"
                >
                  <div class="sub-community-header">
                    <span class="sub-community-name">{{ subCommunity.communityName }}</span>
                    <span class="sub-community-status" :class="getStatusClass(subCommunity.communityStatus)">
                      {{ getStatusText(subCommunity.communityStatus) }}
                    </span>
                  </div>
                  <div class="sub-community-info">
                    <span class="sub-community-type">{{ subCommunity.communityTypeName || '未设置类型' }}</span>
                    <span class="sub-community-address">{{ getFullAddress(subCommunity) }}</span>
                  </div>
                </div>
                
                <!-- 子社区分页控件 -->
                <div v-if="community.subCommunitiesTotal > 0" class="sub-communities-pagination">
                  <a-pagination
                    :current="community.subCommunitiesCurrent || 1"
                    :page-size="community.subCommunitiesPageSize || 10"
                    :total="community.subCommunitiesTotal || 0"
                    :show-size-changer="true"
                    :show-quick-jumper="true"
                    :show-total="(total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`"
                    @change="(page, pageSize) => handleSubCommunityPageChange(community.communityId, page, pageSize)"
                    @show-size-change="(current, size) => handleSubCommunityPageChange(community.communityId, 1, size)"
                    size="small"
                  />
                </div>
              </div>
              <div v-else-if="community.subCommunitiesLoaded" class="sub-communities-empty">
                <span class="empty-text">暂无子社区</span>
              </div>
              <div v-else class="sub-communities-placeholder">
                <span class="placeholder-text">点击"刷新子社区"查看子社区列表</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state">
      <div class="empty-illustration">
        <div class="empty-icon">🏘️</div>
      </div>
      <h3 class="empty-title">暂无社区</h3>
      <p class="empty-description">您还没有创建任何社区，请先注册社区入驻</p>
      <a-button type="primary" size="large" @click="$router.push('/login')">
        ➕ 去注册社区
      </a-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <a-spin size="large" />
      <p class="loading-text">加载中...</p>
    </div>

    <!-- 底部安全区域 -->
    <div class="bottom-safe-area"></div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { communityApi } from '@/api';
import { message, Modal } from 'ant-design-vue';
import { getSafeIdList, removeSafeIdFromList } from '@/utils/bigIntUtils';

export default {
  name: 'CommunityList',
  setup() {
    const router = useRouter();
    const authStore = useAuthStore();
    
    const loading = ref(false);
    const communityList = ref([]);
    const loadingSubCommunities = ref(null);
    const deletingCommunityId = ref(null);
    
    // 搜索表单
    const searchForm = reactive({
      communityId: ''
    });

    // 搜索处理
    const handleSearch = async () => {
      if (!searchForm.communityId) {
        message.warning('请输入社区ID');
        return;
      }
      
      loading.value = true;
      
      try {
        const response = await communityApi.getCommunityById(searchForm.communityId);
        
        // 清空原有列表，只显示搜索到的社区
        communityList.value = [response];
        
        // 添加子社区相关属性
        response.subCommunities = [];
        response.subCommunitiesLoaded = false;
        response.subCommunitiesTotal = 0;
        response.subCommunitiesCurrent = 1;
        response.subCommunitiesPageSize = 10;
        
        message.success('搜索成功');
      } catch (error) {
        console.error('❌ 搜索社区失败:', error);
        message.error('未找到该社区或搜索失败');
        communityList.value = [];
      } finally {
        loading.value = false;
      }
    };

    // 获取用户创建的社区列表
    const loadUserCommunities = async () => {
      if (!authStore.volunteer) {
        message.error('请先登录');
        router.push('/login');
        return;
      }

      loading.value = true;
      try {
        // 获取用户注册的社区ID
        const volunteerId = authStore.volunteer.volunteerId;
        
        if (!volunteerId) {
          message.error('用户信息不完整，请重新登录');
          router.push('/login');
          return;
        }

        // 从localStorage安全地获取用户创建的社区ID
        const userCommunities = getSafeIdList('userCommunities');
        
        if (userCommunities.length > 0) {
          // 获取所有用户创建的社区信息
          const communities = [];
          for (const communityId of userCommunities) {
            try {
              const response = await communityApi.getCommunityById(communityId);
              // 由于request.js的拦截器已经处理了业务逻辑，这里直接使用返回的数据
              // 添加子社区相关属性
              response.subCommunities = [];
              response.subCommunitiesLoaded = false;
              response.subCommunitiesTotal = 0;
              response.subCommunitiesCurrent = 1;
              response.subCommunitiesPageSize = 10;
              communities.push(response);
            } catch (error) {
              console.error(`获取社区 ${communityId} 信息失败:`, error);
              // 如果某个社区获取失败，从localStorage中移除该社区ID
              removeSafeIdFromList('userCommunities', communityId);
            }
          }
          communityList.value = communities;
        } else {
          // 如果没有社区ID，显示空状态
          communityList.value = [];
        }
      } catch (error) {
        console.error('获取社区列表失败:', error);
        message.error('获取社区列表失败');
        communityList.value = [];
      } finally {
        loading.value = false;
      }
    };

    // 加载子社区列表
    const loadSubCommunities = async (communityId, page = 1, pageSize = 10) => {
      console.log(`🔍 开始加载社区 ${communityId} 的子社区列表 - 第${page}页，每页${pageSize}条`);
      loadingSubCommunities.value = communityId;
      
      try {
        const response = await communityApi.getSubCommunityList(communityId, page, pageSize);
        
        // 找到对应的社区并更新子社区信息
        const communityIndex = communityList.value.findIndex(c => c.communityId === communityId);
        if (communityIndex !== -1) {
          communityList.value[communityIndex].subCommunities = response.records || [];
          communityList.value[communityIndex].subCommunitiesLoaded = true;
          communityList.value[communityIndex].subCommunitiesTotal = response.total || 0;
          communityList.value[communityIndex].subCommunitiesCurrent = page;
          communityList.value[communityIndex].subCommunitiesPageSize = pageSize;
        }
      } catch (error) {
        console.error(`获取社区 ${communityId} 子社区列表失败:`, error);
        message.error('获取子社区列表失败');
        
        // 找到对应的社区并标记加载失败
        const communityIndex = communityList.value.findIndex(c => c.communityId === communityId);
        if (communityIndex !== -1) {
          communityList.value[communityIndex].subCommunities = [];
          communityList.value[communityIndex].subCommunitiesLoaded = true;
          communityList.value[communityIndex].subCommunitiesTotal = 0;
          communityList.value[communityIndex].subCommunitiesCurrent = 1;
          communityList.value[communityIndex].subCommunitiesPageSize = 10;
        }
      } finally {
        loadingSubCommunities.value = null;
      }
    };

    // 子社区分页处理
    const handleSubCommunityPageChange = async (communityId, page, pageSize) => {
      await loadSubCommunities(communityId, page, pageSize);
    };

    // 删除社区
    const confirmDelete = (community) => {
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除社区"${community.communityName}"吗？此操作不可恢复。`,
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => deleteCommunity(community)
      });
    };

    const deleteCommunity = async (community) => {
      deletingCommunityId.value = community.communityId;
      try {
        const response = await communityApi.deleteCommunity(community.communityId);
        // 由于request.js的拦截器已经处理了业务逻辑，这里直接使用返回的数据
        message.success('社区删除成功');
        // 从列表中移除
        communityList.value = communityList.value.filter(
          item => item.communityId !== community.communityId
        );
        
        // 从localStorage中移除社区ID
        removeSafeIdFromList('userCommunities', community.communityId);
      } catch (error) {
        console.error('删除社区失败:', error);
        message.error('删除社区失败');
      } finally {
        deletingCommunityId.value = null;
      }
    };

    // 刷新数据
    const handleRefresh = () => {
      loadUserCommunities();
    };

    // 查看详情
    const viewDetail = (community) => {
      router.push(`/community/${community.communityId}`);
    };

    // 编辑社区
    const editCommunity = (community) => {
      router.push(`/community/${community.communityId}/edit`);
    };

    // 获取状态文本
    const getStatusText = (status) => {
      switch (status) {
        case 0: return '未审核';
        case 1: return '已审核';
        case 2: return '已停用';
        default: return '未知状态';
      }
    };

    // 获取状态样式类
    const getStatusClass = (status) => {
      switch (status) {
        case 0: return 'status-pending';
        case 1: return 'status-approved';
        case 2: return 'status-disabled';
        default: return 'status-unknown';
      }
    };

    // 获取完整地址
    const getFullAddress = (community) => {
      const parts = [
        community.province,
        community.city,
        community.district,
        community.address
      ];
      const address = parts.filter(part => part && part.trim() !== '').join(' ');
      return address || '地址信息未设置';
    };

    // 处理软键盘弹出
    const handleKeyboardShow = () => {
      const communityList = document.querySelector('.community-list');
      if (communityList) {
        communityList.style.paddingBottom = '200px';
        communityList.style.minHeight = 'calc(100dvh - 200px)';
      }
    };

    const handleKeyboardHide = () => {
      const communityList = document.querySelector('.community-list');
      if (communityList) {
        communityList.style.paddingBottom = '';
        communityList.style.minHeight = '';
      }
    };

    // 检测软键盘状态
    const detectKeyboard = () => {
      const initialViewportHeight = window.innerHeight;
      
      const checkKeyboard = () => {
        const currentViewportHeight = window.innerHeight;
        const keyboardHeight = initialViewportHeight - currentViewportHeight;
        
        if (keyboardHeight > 150) {
          // 软键盘弹出
          handleKeyboardShow();
        } else {
          // 软键盘收起
          handleKeyboardHide();
        }
      };
      
      window.addEventListener('resize', checkKeyboard);
    };

    // 组件挂载时加载数据和设置软键盘监听
    onMounted(() => {
      loadUserCommunities();
      
      // 设置软键盘检测
      detectKeyboard();
      
      // 监听输入框焦点事件作为备用方案
      const searchInput = document.querySelector('.search-input input');
      if (searchInput) {
        searchInput.addEventListener('focus', handleKeyboardShow);
        searchInput.addEventListener('blur', handleKeyboardHide);
      }
    });

    return {
      loading,
      loadingSubCommunities,
      deletingCommunityId,
      communityList,
      searchForm,
      handleSearch,
      handleRefresh,
      loadSubCommunities,
      confirmDelete,
      deleteCommunity,
      viewDetail,
      editCommunity,
      getStatusText,
      getStatusClass,
      getFullAddress
    };
  }
};
</script>

<style scoped>
.community-list {
  padding: 0;
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  padding: 32px;
  margin-bottom: 24px;
  color: white;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.3);
  position: relative;
  overflow: hidden;
}

.page-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);
  pointer-events: none;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
}

.header-left {
  flex: 1;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 12px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.page-subtitle {
  font-size: 16px;
  margin: 0;
  opacity: 0.9;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.search-section {
  margin-bottom: 24px;
}

.search-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.search-input {
  border-radius: 12px;
}

.action-col {
  display: flex;
  justify-content: flex-end;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 16px;
}

.community-count {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #666;
  padding: 8px 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.community-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

.community-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.community-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.card-header {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  padding: 20px 24px;
  border-bottom: 1px solid #e9ecef;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  flex: 1;
}

.community-name {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px 0;
}

.community-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 6px;
  width: fit-content;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-pending {
  color: #faad14;
  background: rgba(250, 173, 20, 0.1);
}

.status-approved {
  color: #52c41a;
  background: rgba(82, 196, 26, 0.1);
}

.status-disabled {
  color: #ff4d4f;
  background: rgba(255, 77, 79, 0.1);
}

.status-unknown {
  color: #999;
  background: rgba(153, 153, 153, 0.1);
}

.more-actions {
  color: #666;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.more-actions:hover {
  background: rgba(0, 0, 0, 0.05);
}

.card-content {
  padding: 24px;
}

.info-section {
  margin-bottom: 24px;
}

.info-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.refresh-btn {
  margin-left: auto;
  font-size: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.label {
  font-weight: 500;
  color: #666;
  font-size: 14px;
}

.value {
  color: #1a1a1a;
  font-weight: 500;
}

.volunteer-id {
  color: #667eea;
  font-family: 'Courier New', monospace;
}

.address-info {
  margin-top: 8px;
}

.address-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 0;
}

.address-icon {
  color: #667eea;
  margin-top: 2px;
  flex-shrink: 0;
}

.address-text {
  color: #1a1a1a;
  line-height: 1.5;
}

.description-content {
  margin-top: 8px;
}

.description-item {
  margin-bottom: 12px;
}

.description-item:last-child {
  margin-bottom: 0;
}

.description-label {
  font-weight: 500;
  color: #666;
  display: block;
  margin-bottom: 4px;
}

.description-text {
  color: #1a1a1a;
  line-height: 1.6;
  margin: 0;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid #667eea;
}

.sub-communities-content {
  margin-top: 12px;
}

.sub-communities-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sub-community-item {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e9ecef;
  transition: all 0.3s ease;
}

.sub-community-item:hover {
  background: #e9ecef;
  transform: translateX(4px);
}

.sub-community-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.sub-community-name {
  font-weight: 600;
  color: #1a1a1a;
}

.sub-community-status {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.sub-community-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #666;
}

.sub-communities-pagination {
  margin-top: 16px;
  text-align: center;
}

.sub-communities-empty,
.sub-communities-placeholder {
  text-align: center;
  padding: 24px;
  color: #999;
}

.empty-state {
  text-align: center;
  padding: 80px 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.empty-illustration {
  margin-bottom: 24px;
}

.empty-icon {
  font-size: 64px;
  color: #d9d9d9;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

.empty-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 12px 0;
}

.empty-description {
  font-size: 16px;
  color: #666;
  margin: 0 0 24px 0;
}

.loading-state {
  text-align: center;
  padding: 80px 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.loading-text {
  margin-top: 16px;
  color: #666;
  font-size: 16px;
}

.bottom-safe-area {
  height: 24px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    padding: 24px;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .header-content {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  
  .community-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}

/* 深色主题适配 */
@media (prefers-color-scheme: dark) {
  .search-card,
  .community-card,
  .empty-state,
  .loading-state {
    background: #1f1f1f;
    color: white;
    border-color: #333;
  }
  
  .card-header {
    background: linear-gradient(135deg, #2a2a2a 0%, #333 100%);
    border-bottom-color: #333;
  }
  
  .community-name,
  .section-title {
    color: white;
  }
  
  .label {
    color: #ccc;
  }
  
  .value {
    color: white;
  }
  
  .description-text {
    background: #2a2a2a;
    color: white;
  }
  
  .sub-community-item {
    background: #2a2a2a;
    border-color: #333;
  }
  
  .sub-community-item:hover {
    background: #333;
  }
}
</style> 