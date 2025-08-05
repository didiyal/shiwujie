<template>
  <div class="community-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>我的社区管理</h2>
      <p>管理您创建的社区信息</p>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
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
            <a-button type="primary" @click="handleRefresh" size="large">
              🔄 刷新
            </a-button>
            <span class="community-count">共 {{ communityList.length }} 个社区</span>
          </div>
        </a-col>
      </a-row>
    </div>

    <!-- 社区列表 -->
    <div class="community-grid" v-if="communityList.length > 0">
      <div 
        v-for="community in communityList" 
        :key="community.communityId" 
        class="community-card"
      >
        <div class="card-header">
          <h3 class="community-name">{{ community.communityName }}</h3>
          <div class="community-status" :class="getStatusClass(community.communityStatus)">
            {{ getStatusText(community.communityStatus) }}
          </div>
        </div>
        
        <div class="card-content">
          <!-- 基本信息 -->
          <div class="info-section">
            <h4 class="section-title">基本信息</h4>
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
            <h4 class="section-title">地址信息</h4>
            <div class="address-info">
              <div class="address-item">
                <i class="address-icon">📍</i>
                <span class="address-text">{{ getFullAddress(community) }}</span>
              </div>
            </div>
          </div>
          
          <!-- 描述信息 -->
          <div class="info-section" v-if="community.communityDescription || community.registrationInfo">
            <h4 class="section-title">详细信息</h4>
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
              子社区列表
              <a-button 
                type="link" 
                size="small" 
                @click="loadSubCommunities(community.communityId)"
                :loading="loadingSubCommunities === community.communityId"
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
        
        <div class="card-actions">
          <a-button type="primary" size="small" @click="viewDetail(community)">
            查看详情
          </a-button>
          <a-button type="default" size="small" @click="editCommunity(community)">
            编辑
          </a-button>
          <a-button 
            type="primary" 
            size="small" 
            danger 
            @click="confirmDelete(community)"
            :loading="deletingCommunityId === community.communityId"
          >
            删除社区
          </a-button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state">
      <div class="empty-icon">🏘️</div>
      <h3>暂无社区</h3>
      <p>您还没有创建任何社区，请先注册社区入驻</p>
      <a-button type="primary" @click="$router.push('/login')">
        去注册社区
      </a-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <a-spin size="large" />
      <p>加载中...</p>
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
    const loadingSubCommunities = ref(null);
    const deletingCommunityId = ref(null);
    const communityList = ref([]);

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
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: 100vh;
  position: relative;
  /* 移动端软键盘适配 */
  min-height: 100dvh; /* 使用动态视口高度 */
  box-sizing: border-box;
}

.page-header {
  margin-bottom: 24px;
  text-align: center;
}

.page-header h2 {
  color: #1890ff;
  margin-bottom: 8px;
  font-size: 28px;
}

.page-header p {
  color: #666;
  margin: 0;
  font-size: 16px;
}

/* 搜索栏样式 */
.search-bar {
  margin-bottom: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #e9ecef;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.search-input {
  width: 100%;
}

.search-input .ant-input {
  border-radius: 8px;
  font-size: 14px;
}

.action-col {
  display: flex;
  align-items: center;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  justify-content: flex-end;
}

.community-count {
  color: #666;
  font-size: 14px;
  font-weight: 500;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.community-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

 .community-card {
   background: white;
   border: 1px solid #e9ecef;
   border-radius: 16px;
   padding: 24px;
   box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
   transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
   position: relative;
   overflow: hidden;
 }

 .community-card::before {
   content: '';
   position: absolute;
   top: 0;
   left: 0;
   right: 0;
   height: 4px;
   background: linear-gradient(90deg, #1890ff, #52c41a);
   opacity: 0;
   transition: opacity 0.3s ease;
 }

 .community-card:hover {
   box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
   transform: translateY(-4px);
   border-color: #1890ff;
 }

 .community-card:hover::before {
   opacity: 1;
 }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.community-name {
  margin: 0;
  color: #1890ff;
  font-size: 18px;
  font-weight: 600;
  flex: 1;
}

 .community-status {
   padding: 6px 12px;
   border-radius: 20px;
   font-size: 11px;
   font-weight: 600;
   white-space: nowrap;
   text-transform: uppercase;
   letter-spacing: 0.5px;
   box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
 }

 .status-pending {
   background: linear-gradient(135deg, #fff7e6, #ffe7ba);
   color: #d46b08;
   border: 1px solid #ffd591;
 }

 .status-approved {
   background: linear-gradient(135deg, #f6ffed, #d9f7be);
   color: #389e0d;
   border: 1px solid #b7eb8f;
 }

 .status-disabled {
   background: linear-gradient(135deg, #fff2f0, #ffccc7);
   color: #cf1322;
   border: 1px solid #ffa39e;
 }

 .status-unknown {
   background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
   color: #595959;
   border: 1px solid #d9d9d9;
 }

 .card-content {
   margin-bottom: 20px;
 }

 .info-section {
   margin-bottom: 20px;
  padding: 16px;
  background: #fafafa;
   border-radius: 8px;
   border-left: 4px solid #1890ff;
 }

 .info-section:last-child {
   margin-bottom: 0;
 }

 .section-title {
   margin: 0 0 12px 0;
   color: #1890ff;
   font-size: 14px;
   font-weight: 600;
   text-transform: uppercase;
   letter-spacing: 0.5px;
 }

 .info-grid {
   display: grid;
   grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
   gap: 12px;
 }

 .info-item {
   display: flex;
   align-items: center;
   font-size: 13px;
   line-height: 1.4;
 }

 .info-item .label {
   color: #666;
   min-width: 80px;
   font-weight: 500;
   margin-right: 8px;
 }

 .info-item .value {
   color: #333;
   flex: 1;
   font-weight: 500;
   word-break: break-all;
 }

 /* 志愿者ID特殊样式 */
 .volunteer-id {
   font-family: 'Courier New', monospace;
   font-size: 12px;
   background: #f0f0f0;
   padding: 2px 6px;
   border-radius: 4px;
   color: #1890ff;
   font-weight: 600;
 }

 .address-info {
   margin-top: 8px;
 }

 .address-item {
   display: flex;
   align-items: flex-start;
   gap: 8px;
 }

 .address-icon {
   font-style: normal;
   font-size: 16px;
   margin-top: 2px;
 }

 .address-text {
   color: #333;
   font-size: 13px;
   line-height: 1.4;
   flex: 1;
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
   display: block;
   color: #666;
   font-size: 12px;
   font-weight: 500;
   margin-bottom: 4px;
   text-transform: uppercase;
   letter-spacing: 0.5px;
 }

 .description-text {
   margin: 0;
   color: #333;
   font-size: 13px;
   line-height: 1.5;
   background: white;
   padding: 8px 12px;
   border-radius: 4px;
   border: 1px solid #e8e8e8;
 }

 .card-actions {
   display: flex;
   gap: 10px;
   justify-content: flex-end;
   padding-top: 20px;
   border-top: 1px solid #f0f0f0;
   margin-top: 20px;
 }

 .card-actions .ant-btn {
   border-radius: 8px;
   font-weight: 500;
   transition: all 0.2s ease;
 }

 .card-actions .ant-btn:hover {
   transform: translateY(-1px);
   box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
 }

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  border: 2px dashed #d9d9d9;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state h3 {
  color: #333;
  margin-bottom: 8px;
  font-size: 20px;
}

.empty-state p {
  color: #666;
  margin-bottom: 24px;
  font-size: 14px;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
}

.loading-state p {
  margin-top: 16px;
  color: #666;
}

/* 子社区列表样式 */
.sub-communities-content {
  margin-top: 12px;
}

.sub-communities-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sub-community-item {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px;
  transition: all 0.2s ease;
}

.sub-community-item:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.sub-community-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.sub-community-name {
  font-weight: 600;
  color: #1890ff;
  font-size: 14px;
}

.sub-community-status {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.sub-community-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sub-community-type {
  color: #666;
  font-size: 12px;
}

.sub-community-address {
  color: #999;
  font-size: 11px;
  line-height: 1.3;
}

.sub-communities-empty {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px dashed #d9d9d9;
}

.sub-communities-placeholder {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px dashed #d9d9d9;
}

.placeholder-text {
  color: #999;
}

.empty-text {
  color: #999;
}

/* 子社区分页样式 */
.sub-communities-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
}

.sub-communities-pagination .ant-pagination {
  margin: 0;
}

/* 子社区标题区域样式 */
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title .ant-btn-link {
  padding: 0;
  height: auto;
  font-size: 12px;
  color: #1890ff;
}

.section-title .ant-btn-link:hover {
  color: #40a9ff;
}

/* 底部安全区域 */
.bottom-safe-area {
  height: env(safe-area-inset-bottom, 20px);
  background: transparent;
  min-height: 20px;
}



/* 移动端软键盘适配 */
@media (max-width: 768px) {
  .community-list {
    padding: 16px;
    padding-bottom: env(safe-area-inset-bottom, 20px);
    /* 使用CSS环境变量处理底部安全区域 */
    min-height: calc(100dvh - env(safe-area-inset-bottom, 0px));
  }
  
  .search-bar {
    padding: 16px;
    /* 确保搜索栏在软键盘弹出时可见 */
    position: sticky;
    top: 0;
    z-index: 100;
    background: #f8f9fa;
    margin-bottom: 16px;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .community-grid {
    grid-template-columns: 1fr;
    /* 为软键盘留出空间 */
    margin-bottom: 100px;
  }
  
  .card-actions {
    flex-direction: column;
  }
  
  .card-actions .ant-btn {
    width: 100%;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .info-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .info-item .label {
    margin-bottom: 4px;
  }
  
  .volunteer-id {
    font-size: 11px;
    padding: 1px 4px;
  }
  
  /* 软键盘弹出时的特殊处理 */
  .community-list:has(.ant-input:focus) {
    padding-bottom: 200px; /* 为软键盘留出更多空间 */
  }
  
  /* 底部安全区域 */
  .bottom-safe-area {
    height: env(safe-area-inset-bottom, 20px);
    background: transparent;
  }
}

/* 平板端适配 */
@media (min-width: 769px) and (max-width: 1024px) {
  .community-grid {
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  }
  
  .search-bar {
    padding: 18px;
  }
}

/* iOS设备特殊处理 */
@supports (-webkit-touch-callout: none) {
  .community-list {
    /* iOS Safari 特殊处理 */
    min-height: -webkit-fill-available;
  }
  
  @media (max-width: 768px) {
    .community-list {
      min-height: -webkit-fill-available;
      padding-bottom: calc(env(safe-area-inset-bottom, 20px) + 100px);
    }
  }
}
</style> 