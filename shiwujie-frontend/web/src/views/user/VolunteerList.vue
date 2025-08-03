<template>
  <div class="volunteer-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>志愿者管理</h2>
      <p>管理社区内的所有志愿者成员</p>
      <div class="info-alert">
        <a-alert
          message="信息提示"
          description="显示所有加入社区的志愿者，包括普通成员和管理员。"
          type="info"
          show-icon
        />
      </div>
    </div>

    <!-- 操作栏 -->
      <div class="action-bar">
      <div class="action-left">
        <a-button type="primary" @click="handleRefresh">
          🔄 刷新
            </a-button>
      </div>
      <div class="action-right">
        <span class="volunteer-count">共 {{ total }} 个志愿者</span>
      </div>
      </div>

    <!-- 志愿者列表 -->
    <div class="volunteer-grid" v-if="volunteerList.length > 0">
      <div 
        v-for="volunteer in volunteerList" 
        :key="volunteer.volunteerId" 
        class="volunteer-card"
      >
        <div class="card-header">
          <div class="volunteer-info">
            <h3 class="volunteer-name">{{ volunteer.name }}</h3>
            <div class="volunteer-status" :class="getOnlineStatusClass(volunteer.onlineStatus)">
              {{ getOnlineStatusText(volunteer.onlineStatus) }}
            </div>
          </div>
          <div class="volunteer-rating">
            <span class="rating-label">评分:</span>
            <span class="rating-value">{{ volunteer.rating || 0 }}</span>
          </div>
        </div>
        
        <div class="card-content">
          <!-- 基本信息 -->
          <div class="info-section">
            <h4 class="section-title">基本信息</h4>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">志愿者ID：</span>
                <span class="value">{{ volunteer.volunteerId }}</span>
              </div>
              <div class="info-item">
                <span class="label">手机号：</span>
                <span class="value">{{ volunteer.phone || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">性别：</span>
                <span class="value">{{ getGenderText(volunteer.gender) }}</span>
              </div>
              <div class="info-item">
                <span class="label">社区ID：</span>
                <span class="value">{{ volunteer.communityId || '未设置' }}</span>
              </div>
            </div>
          </div>
          
          <!-- 位置信息 -->
          <div class="info-section" v-if="volunteer.locationAddress">
            <h4 class="section-title">位置信息</h4>
            <div class="location-info">
              <div class="location-item">
                <i class="location-icon">📍</i>
                <span class="location-text">{{ volunteer.locationAddress }}</span>
              </div>
              <div class="location-time" v-if="volunteer.locationUpdateTime">
                <span class="time-label">更新时间：</span>
                <span class="time-value">{{ formatTime(volunteer.locationUpdateTime) }}</span>
              </div>
            </div>
          </div>
          
          <!-- 帮助统计 -->
          <div class="info-section">
            <h4 class="section-title">帮助统计</h4>
            <div class="stats-grid">
              <div class="stats-item">
                <span class="stats-label">帮助次数：</span>
                <span class="stats-value">{{ volunteer.helpCount || 0 }}</span>
              </div>
              <div class="stats-item">
                <span class="stats-label">活跃状态：</span>
                <span class="stats-value" :class="getActiveStatusClass(volunteer.isActivelyJoined)">
                  {{ getActiveStatusText(volunteer.isActivelyJoined) }}
                </span>
              </div>
            </div>
          </div>

          <!-- 其他信息 -->
          <div class="info-section" v-if="volunteer.otherInfo">
            <h4 class="section-title">其他信息</h4>
            <div class="other-info">
              <p class="other-text">{{ volunteer.otherInfo }}</p>
            </div>
          </div>
        </div>
        
        <div class="card-actions">
          <a-button type="primary" size="small" @click="viewDetail(volunteer)">
            查看详情
              </a-button>
          <a-button type="default" size="small" @click="editVolunteer(volunteer)">
                编辑
              </a-button>
          <!-- 只有注册人才能看到设为管理员按钮 -->
          <a-button 
            v-if="isRegistrant && volunteer.communityManager !== '注册人'"
            type="primary" 
            size="small" 
            danger
            @click="confirmSetAsManager(volunteer)"
            :loading="settingManagerId === volunteer.volunteerId"
          >
            设为管理员
              </a-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-container" v-if="total > 0">
      <a-pagination
        :current="currentPage"
        :pageSize="pageSize"
        :total="total"
        :show-size-changer="true"
        :show-quick-jumper="true"
        :show-total="(total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`"
        @change="handlePageChange"
        @showSizeChange="handlePageSizeChange"
      />
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && volunteerList.length === 0" class="empty-state">
      <div class="empty-icon">👥</div>
      <h3>暂无志愿者</h3>
      <p>当前社区下还没有志愿者信息</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <a-spin size="large" />
      <p>加载中...</p>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { communityApi } from '@/api';
import { message, Modal } from 'ant-design-vue';

export default {
  name: 'VolunteerList',
  setup() {
    const router = useRouter();
    const authStore = useAuthStore();
    const loading = ref(false);
    const volunteerList = ref([]);
    const currentPage = ref(1);
    const pageSize = ref(10);
    const total = ref(0);
    const settingManagerId = ref(null);

    // 检查当前用户是否是注册人
    const isRegistrant = computed(() => {
      return authStore.volunteer?.communityManager === '注册人';
    });

    // 获取志愿者列表
    const loadVolunteerList = async () => {
      console.log('🔍 开始加载志愿者列表');
      
      if (!authStore.volunteer) {
        console.log('❌ authStore.volunteer 为空，重定向到登录页');
        message.error('请先登录');
        router.push('/login');
        return;
      }

      // 调试当前用户身份信息
      console.log('🔍 当前用户身份信息:', {
        volunteerId: authStore.volunteer.volunteerId,
        communityManager: authStore.volunteer.communityManager,
        isRegistrant: authStore.volunteer?.communityManager === '注册人'
      });

      loading.value = true;
      try {
        // 获取用户注册的社区ID列表
        const userCommunities = JSON.parse(localStorage.getItem('userCommunities') || '[]');
        console.log('🔍 用户社区ID列表:', userCommunities);
        
        if (userCommunities.length === 0) {
          console.log('❌ 用户没有管理的社区');
          message.error('您没有管理的社区');
          volunteerList.value = [];
          total.value = 0;
          return;
        }

        // 使用第一个社区ID查询志愿者
        const communityId = userCommunities[0];
        console.log('🔍 使用社区ID查询志愿者:', communityId);
        console.log('🔍 请求参数:', {
          communityId: String(communityId),
          current: currentPage.value,
          pageSize: pageSize.value
        });
        
        try {
          const response = await communityApi.getCommunityVolunteers(communityId, currentPage.value, pageSize.value);
          console.log('✅ 志愿者列表获取成功:', response);
          console.log('🔍 响应数据详情:', {
            records: response.records,
            total: response.total,
            recordsLength: response.records?.length || 0
          });
          
          volunteerList.value = response.records || [];
          total.value = response.total || 0;
        } catch (error) {
          console.error('❌ 志愿者列表获取失败:', error);
          console.error('❌ 错误详情:', {
            message: error.message,
            response: error.response?.data,
            status: error.response?.status
          });
          message.error('获取志愿者列表失败，请稍后重试');
          volunteerList.value = [];
          total.value = 0;
        }
        
        // 调试每个志愿者的身份信息
        if (volunteerList.value.length > 0) {
          console.log('🔍 志愿者身份信息:');
          volunteerList.value.forEach((volunteer, index) => {
            console.log(`  志愿者${index + 1}:`, {
              volunteerId: volunteer.volunteerId,
              name: volunteer.name,
              communityManager: volunteer.communityManager,
              isCurrentUser: volunteer.volunteerId === authStore.volunteer?.volunteerId
            });
          });
        }
        
      } catch (error) {
        console.error('获取志愿者列表失败:', error);
        message.error('获取志愿者列表失败');
        volunteerList.value = [];
        total.value = 0;
      } finally {
        loading.value = false;
      }
    };

    // 刷新数据
    const handleRefresh = () => {
      loadVolunteerList();
    };

    // 分页变化
    const handlePageChange = (page, pageSize) => {
      currentPage.value = page;
      loadVolunteerList();
    };

    // 页面大小变化
    const handlePageSizeChange = (current, size) => {
      currentPage.value = 1;
      pageSize.value = size;
      loadVolunteerList();
    };

    // 查看详情
    const viewDetail = (volunteer) => {
      console.log('查看志愿者详情:', volunteer);
      // TODO: 实现查看详情功能
      message.info('查看详情功能待实现');
    };

    // 编辑志愿者
    const editVolunteer = (volunteer) => {
      console.log('编辑志愿者:', volunteer);
      // TODO: 实现编辑功能
      message.info('编辑功能待实现');
    };

    // 获取在线状态文本
    const getOnlineStatusText = (status) => {
      switch (status) {
        case 0: return '离线';
        case 1: return '在线';
        case 2: return '忙碌';
        default: return '未知';
      }
    };

    // 获取在线状态样式类
    const getOnlineStatusClass = (status) => {
      switch (status) {
        case 0: return 'status-offline';
        case 1: return 'status-online';
        case 2: return 'status-busy';
        default: return 'status-unknown';
      }
    };

    // 获取性别文本
    const getGenderText = (gender) => {
      switch (gender) {
        case 0: return '未知';
        case 1: return '男';
        case 2: return '女';
        default: return '未知';
      }
    };

    // 获取活跃状态文本
    const getActiveStatusText = (status) => {
      switch (status) {
        case 0: return '非活跃';
        case 1: return '活跃';
        default: return '未知';
      }
    };

    // 获取活跃状态样式类
    const getActiveStatusClass = (status) => {
      switch (status) {
        case 0: return 'status-inactive';
        case 1: return 'status-active';
        default: return 'status-unknown';
      }
    };

    // 格式化时间
    const formatTime = (timeStr) => {
      if (!timeStr) return '';
      try {
        const date = new Date(timeStr);
        return date.toLocaleString('zh-CN');
      } catch (error) {
        return timeStr;
      }
    };

    // 确认设为管理员
    const confirmSetAsManager = (volunteer) => {
      Modal.confirm({
        title: '确认设为管理员',
        content: `确定要将志愿者"${volunteer.name}"设为社区管理员吗？`,
        okText: '确认',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => setAsManager(volunteer)
      });
    };

    // 设为管理员
    const setAsManager = async (volunteer) => {
      settingManagerId.value = volunteer.volunteerId;
      try {
        console.log('🔍 开始设置管理员:', volunteer);
        
        // 获取用户注册的社区ID列表
        const userCommunities = JSON.parse(localStorage.getItem('userCommunities') || '[]');
        if (userCommunities.length === 0) {
          message.error('您没有管理的社区');
          return;
        }

        const communityId = userCommunities[0];
        console.log('🔍 使用社区ID设置管理员:', communityId);
        
        const response = await communityApi.addCommunityManager(
          communityId, 
          volunteer.volunteerId, 
          '管理员' // 角色名称
        );
        
        console.log('✅ 设置管理员成功:', response);
        message.success('设置管理员成功');
        
        // 刷新志愿者列表
        await loadVolunteerList();
        
      } catch (error) {
        console.error('设置管理员失败:', error);
        message.error('设置管理员失败');
      } finally {
        settingManagerId.value = null;
      }
    };

    // 组件挂载时加载数据
    onMounted(() => {
      loadVolunteerList();
    });

    return {
      loading,
      volunteerList,
      currentPage,
      pageSize,
      total,
      settingManagerId,
      isRegistrant,
      handleRefresh,
      handlePageChange,
      handlePageSizeChange,
      viewDetail,
      editVolunteer,
      confirmSetAsManager,
      setAsManager,
      getOnlineStatusText,
      getOnlineStatusClass,
      getGenderText,
      getActiveStatusText,
      getActiveStatusClass,
      formatTime
    };
  }
};
</script>

<style scoped>
.volunteer-list {
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

.info-alert {
  margin-top: 16px;
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

.volunteer-count {
  color: #666;
  font-size: 14px;
}

.volunteer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.volunteer-card {
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.volunteer-card::before {
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

.volunteer-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-4px);
  border-color: #1890ff;
}

.volunteer-card:hover::before {
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

.volunteer-info {
  flex: 1;
}

.volunteer-name {
  margin: 0 0 8px 0;
  color: #1890ff;
  font-size: 18px;
  font-weight: 600;
}

.volunteer-status {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-online {
  background: linear-gradient(135deg, #f6ffed, #d9f7be);
  color: #389e0d;
  border: 1px solid #b7eb8f;
}

.status-offline {
  background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
  color: #595959;
  border: 1px solid #d9d9d9;
}

.status-busy {
  background: linear-gradient(135deg, #fff7e6, #ffe7ba);
  color: #d46b08;
  border: 1px solid #ffd591;
}

.status-unknown {
  background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
  color: #595959;
  border: 1px solid #d9d9d9;
}

.volunteer-rating {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rating-label {
  font-size: 12px;
  color: #666;
}

.rating-value {
  font-weight: 600;
  color: #1890ff;
  font-size: 14px;
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

.location-info {
  margin-top: 8px;
}

.location-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}

.location-icon {
  font-style: normal;
  font-size: 16px;
  margin-top: 2px;
}

.location-text {
  color: #333;
  font-size: 13px;
  line-height: 1.4;
  flex: 1;
}

.location-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #999;
}

.time-label {
  color: #999;
}

.time-value {
  color: #666;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.stats-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  line-height: 1.4;
}

.stats-label {
  color: #666;
  min-width: 80px;
  font-weight: 500;
  margin-right: 8px;
}

.stats-value {
  color: #333;
  flex: 1;
  font-weight: 500;
}

.status-active {
  color: #389e0d;
  font-weight: 600;
}

.status-inactive {
  color: #999;
}

.other-info {
  margin-top: 8px;
}

.other-text {
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

/* 设为管理员按钮特殊样式 */
.card-actions .ant-btn-danger {
  background: linear-gradient(135deg, #ff4d4f, #cf1322);
  border: none;
  color: white;
  font-weight: 600;
}

.card-actions .ant-btn-danger:hover {
  background: linear-gradient(135deg, #ff7875, #ff4d4f);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e9ecef;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .volunteer-list {
    padding: 16px;
  }
  
  .volunteer-grid {
    grid-template-columns: 1fr;
  }
  
  .card-actions {
    flex-direction: column;
  }
  
  .card-actions .ant-btn {
    width: 100%;
  }
}
</style> 