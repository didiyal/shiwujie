<template>
  <div class="community-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>我的社区管理</h2>
      <p>管理您创建的社区信息</p>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="action-left">
        <a-button type="primary" @click="handleRefresh">
          🔄 刷新
          </a-button>
      </div>
      <div class="action-right">
        <span class="community-count">共 {{ communityList.length }} 个社区</span>
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
                <span class="value">{{ community.registerVolunteerId || '未设置' }}</span>
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

    // 获取用户创建的社区列表
    const loadUserCommunities = async () => {
      console.log('🔍 开始加载用户社区列表');
      console.log('🔍 authStore.volunteer:', authStore.volunteer);
      console.log('🔍 authStore.isLoggedIn:', authStore.isLoggedIn);
      
      if (!authStore.volunteer) {
        console.log('❌ authStore.volunteer 为空，重定向到登录页');
        message.error('请先登录');
        router.push('/login');
        return;
      }

      loading.value = true;
      try {
        // 获取用户注册的社区ID
        const volunteerId = authStore.volunteer.volunteerId;
        console.log('🔍 volunteerId:', volunteerId);
        
        if (!volunteerId) {
          console.log('❌ volunteerId 为空，用户信息不完整');
          message.error('用户信息不完整，请重新登录');
          router.push('/login');
          return;
        }

        // 从localStorage安全地获取用户创建的社区ID
        const userCommunities = getSafeIdList('userCommunities');
        console.log('🔍 从localStorage安全获取的社区ID列表:', userCommunities);
        console.log('🔍 localStorage.getItem("userCommunities"):', localStorage.getItem('userCommunities'));
        console.log('🔍 localStorage.getItem("token"):', localStorage.getItem('token'));
        
        if (userCommunities.length > 0) {
          // 获取所有用户创建的社区信息
          const communities = [];
          for (const communityId of userCommunities) {
            try {
              console.log(`🔍 正在获取社区ID ${communityId} 的信息... (类型: ${typeof communityId})`);
              const response = await communityApi.getCommunityById(communityId);
              console.log(`✅ 社区ID ${communityId} 信息获取成功:`, response);
              // 由于request.js的拦截器已经处理了业务逻辑，这里直接使用返回的数据
              // 添加子社区相关属性
              response.subCommunities = [];
              response.subCommunitiesLoaded = false;
              communities.push(response);
            } catch (error) {
              console.error(`❌ 获取社区 ${communityId} 信息失败:`, error);
              // 如果某个社区获取失败，从localStorage中移除该社区ID
              removeSafeIdFromList('userCommunities', communityId);
              console.log(`🗑️ 已从localStorage中移除无效的社区ID: ${communityId}`);
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
    const loadSubCommunities = async (communityId) => {
      console.log(`🔍 开始加载社区 ${communityId} 的子社区列表`);
      loadingSubCommunities.value = communityId;
      
      try {
        const response = await communityApi.getSubCommunityList(communityId, 1, 50);
        console.log(`✅ 社区 ${communityId} 子社区列表获取成功:`, response);
        
        // 找到对应的社区并更新子社区信息
        const communityIndex = communityList.value.findIndex(c => c.communityId === communityId);
        if (communityIndex !== -1) {
          communityList.value[communityIndex].subCommunities = response.records || [];
          communityList.value[communityIndex].subCommunitiesLoaded = true;
          console.log(`✅ 已更新社区 ${communityId} 的子社区信息`);
        }
      } catch (error) {
        console.error(`❌ 获取社区 ${communityId} 子社区列表失败:`, error);
        message.error('获取子社区列表失败');
        
        // 找到对应的社区并标记加载失败
        const communityIndex = communityList.value.findIndex(c => c.communityId === communityId);
        if (communityIndex !== -1) {
          communityList.value[communityIndex].subCommunities = [];
          communityList.value[communityIndex].subCommunitiesLoaded = true;
        }
      } finally {
        loadingSubCommunities.value = null;
      }
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
        console.log(`🗑️ 删除社区ID: ${community.communityId}, 类型: ${typeof community.communityId}`);
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

    // 组件挂载时加载数据
    onMounted(() => {
      loadUserCommunities();
    });

    return {
      loading,
      loadingSubCommunities,
      deletingCommunityId,
      communityList,
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

.community-count {
  color: #666;
  font-size: 14px;
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
</style> 