<template>
  <div class="community-review">
    <div class="page-header">
      <h2>社区加入审核</h2>
      <p>管理志愿者和盲人的社区加入申请</p>
      <div v-if="!hasReviewPermission" class="permission-warning">
        <a-alert
          message="权限提示"
          description="您当前没有审核权限。只有社区管理员和注册人可以审核加入申请。"
          type="warning"
          show-icon
        />
      </div>
    </div>

    <a-card>
      <div class="table-header">
        <div class="header-left">
          <a-button type="primary" @click="loadReviewList" :loading="loading">
            刷新列表
          </a-button>
        </div>
        <div class="header-right">
          <span class="review-count">
            共 {{ reviewList.length }} 条申请
            <span v-if="getPendingCount() > 0" class="pending-count">
              ({{ getPendingCount() }} 条待审核)
            </span>
          </span>
        </div>
      </div>

      <a-table
        :columns="columns"
        :data-source="reviewList"
        :loading="loading"
        :pagination="false"
        row-key="reviewId"
        :scroll="{ x: 800 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'userType'">
            <a-tag :color="record.volunteerId ? 'blue' : 'green'">
              {{ record.volunteerId ? '志愿者' : '盲人' }}
            </a-tag>
          </template>
          
          <template v-if="column.key === 'userName'">
            <div class="user-info">
              <span class="user-name">{{ record.userName || '未知用户' }}</span>
              <span class="user-id">ID: {{ record.volunteerId || record.blindId }}</span>
            </div>
          </template>

          <template v-if="column.key === 'communityName'">
            <div class="community-info">
              <span class="community-name">{{ record.communityName || '未知社区' }}</span>
              <span class="community-id">ID: {{ record.communityId }}</span>
            </div>
          </template>

          <template v-if="column.key === 'applyTime'">
            <span>{{ formatTime(record.applyTime) }}</span>
          </template>

          <template v-if="column.key === 'reviewStatus'">
            <a-tag 
              :color="getStatusColor(record.reviewStatus)"
              :class="getStatusClass(record.reviewStatus)"
            >
              {{ getStatusText(record.reviewStatus) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'action'">
            <!-- 根据审核状态显示不同的内容 -->
            <template v-if="record.reviewStatus === '待审核'">
              <a-space v-if="canReviewRecord(record)">
                <a-button
                  type="primary"
                  size="small"
                  @click="approve(record)"
                  :loading="processingId === record.reviewId"
                >
                  通过
                </a-button>
                <a-button
                  type="default"
                  size="small"
                  danger
                  @click="reject(record)"
                  :loading="processingId === record.reviewId"
                >
                  拒绝
                </a-button>
              </a-space>
              <span v-else class="no-permission">
                <a-tag color="orange">无审核权限</a-tag>
              </span>
            </template>
            
            <!-- 已通过的申请 -->
            <template v-else-if="record.reviewStatus === '已通过'">
              <a-tag color="green">已通过</a-tag>
            </template>

            <!-- 已拒绝的申请 -->
            <template v-else-if="record.reviewStatus === '已拒绝'">
              <a-tag color="red">已拒绝</a-tag>
            </template>
            
            <!-- 其他状态 -->
            <template v-else>
              <a-tag color="default">{{ record.reviewStatus || '未知状态' }}</a-tag>
            </template>
          </template>
        </template>
      </a-table>

      <!-- 空状态 -->
      <div v-if="!loading && reviewList.length === 0" class="empty-state">
        <h3>暂无待审核申请</h3>
        <p>当前没有志愿者或盲人的社区加入申请需要审核</p>
      </div>
    </a-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue';
import { communityApi } from '@/api';
import { message, Modal } from 'ant-design-vue';
import { useAuthStore } from '@/stores/auth';

export default {
  name: 'CommunityReview',
  setup() {
    const authStore = useAuthStore();
    const loading = ref(false);
    const processingId = ref(null);
    const reviewList = ref([]);

    const columns = [
      { 
        title: '申请类型', 
        dataIndex: 'userType', 
        key: 'userType',
        width: 100,
        align: 'center'
      },
      { 
        title: '申请人', 
        dataIndex: 'userName', 
        key: 'userName',
        width: 200
      },
      { 
        title: '申请社区', 
        dataIndex: 'communityName', 
        key: 'communityName',
        width: 200
      },
      { 
        title: '申请时间', 
        dataIndex: 'applyTime', 
        key: 'applyTime',
        width: 180
      },
      { 
        title: '审核状态', 
        dataIndex: 'reviewStatus', 
        key: 'reviewStatus',
        width: 120,
        align: 'center'
      },
      { 
        title: '操作', 
        key: 'action', 
        width: 150,
        align: 'center',
        fixed: 'right'
      }
    ];

    const loadReviewList = async () => {
      loading.value = true;
      try {
        console.log('🔍 开始获取社区审核列表');
        
        // 调试：显示当前用户权限信息
        const currentUser = authStore.volunteer;
        console.log('🔍 当前用户信息:', currentUser);
        console.log('🔍 用户权限状态:', {
          hasUser: !!currentUser,
          communityManager: currentUser?.communityManager,
          hasPermission: hasReviewPermission.value
        });
        
        const response = await communityApi.getCommunityJoinReviewList();
        console.log('✅ 获取社区审核列表成功:', response);
        
        // 检查response的结构
        console.log('🔍 response结构:', response);
        console.log('🔍 response.data:', response.data);
        
        // 根据实际返回的数据结构处理
        let data = [];
        if (response && response.data) {
          data = response.data;
        } else if (Array.isArray(response)) {
          data = response;
        } else {
          console.warn('⚠️ 意外的响应结构:', response);
          data = [];
        }
        
        reviewList.value = data;
        
        // 为每个审核记录添加用户和社区信息
        for (const review of reviewList.value) {
          // 根据volunteerId或blindId判断用户类型
          if (review.volunteerId) {
            review.userName = `志愿者${review.volunteerId}`;
          } else if (review.blindId) {
            review.userName = `盲人${review.blindId}`;
          }
          
          // 这里可以调用API获取社区名称，暂时使用ID
          review.communityName = `社区${review.communityId}`;
          
          // 添加详细的调试信息
          console.log('🔍 审核记录详情:', {
            reviewId: review.reviewId,
            reviewIdType: typeof review.reviewId,
            communityId: review.communityId,
            communityIdType: typeof review.communityId,
            volunteerId: review.volunteerId,
            blindId: review.blindId,
            reviewStatus: review.reviewStatus,
            applyTime: review.applyTime
          });
        }
        
        console.log('🔍 处理后的reviewList:', reviewList.value);
        message.success(`成功加载 ${reviewList.value.length} 条审核申请`);
      } catch (error) {
        console.error('❌ 获取社区审核列表失败:', error);
        message.error('获取审核列表失败');
      } finally {
        loading.value = false;
      }
    };

    const approve = (record) => {
      Modal.confirm({
        title: '确认通过申请',
        content: `确定要通过"${record.userName}"加入"${record.communityName}"的申请吗？`,
        okText: '确认通过',
        okType: 'primary',
        cancelText: '取消',
        onOk: () => handleReview(record, true)
      });
    };

    const reject = (record) => {
      Modal.confirm({
        title: '确认拒绝申请',
        content: `确定要拒绝"${record.userName}"加入"${record.communityName}"的申请吗？`,
        okText: '确认拒绝',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => handleReview(record, false)
      });
    };

    const handleReview = async (record, isApproved) => {
      processingId.value = record.reviewId;
      try {
        console.log('🔍 开始审核申请:', { record, isApproved });
        console.log('🔍 审核记录详情:', {
          reviewId: record.reviewId,
          reviewIdType: typeof record.reviewId,
          communityId: record.communityId,
          volunteerId: record.volunteerId,
          blindId: record.blindId,
          applyTime: record.applyTime
        });
        
        // 检查当前用户是否有审核权限
        const currentUser = authStore.volunteer;
        console.log('🔍 审核操作 - 当前用户信息:', currentUser);
        console.log('🔍 用户详细信息:', {
          volunteerId: currentUser?.volunteerId,
          volunteerIdType: typeof currentUser?.volunteerId,
          communityId: currentUser?.communityId,
          communityManager: currentUser?.communityManager,
          phone: currentUser?.phone
        });
        
        if (!currentUser) {
          message.error('用户信息不存在，请重新登录');
          return;
        }
        
        // 检查用户是否有社区管理权限
        if (!currentUser.communityManager || currentUser.communityManager === '') {
          console.log('❌ 审核权限检查失败 - communityManager:', currentUser.communityManager);
          message.error('您没有审核权限，只有社区管理员和注册人可以审核申请');
          return;
        }
        
        // 检查是否为管理员或注册人
        const hasPermission = currentUser.communityManager === '管理员' || 
                            currentUser.communityManager === '注册人';
        if (!hasPermission) {
          console.log('❌ 审核权限检查失败 - 角色不是管理员或注册人:', currentUser.communityManager);
          message.error('您没有审核权限，只有管理员和注册人可以审核申请');
          return;
        }
        
        console.log('✅ 权限检查通过 - 用户角色:', currentUser.communityManager);
        
        const updateData = {
          reviewId: record.reviewId,
          reviewResult: isApproved,
          reviewerId: currentUser.volunteerId
        };
        
        console.log('🔍 发送审核请求:', updateData);
        console.log('🔍 请求数据类型:', {
          reviewIdType: typeof updateData.reviewId,
          reviewerIdType: typeof updateData.reviewerId,
          reviewResultType: typeof updateData.reviewResult
        });
        
        const response = await communityApi.updateCommunityJoinReview(updateData);
        console.log('✅ 审核操作成功:', response);
        
        message.success(isApproved ? '申请已通过' : '申请已拒绝');
        
        // 刷新列表
        await loadReviewList();
      } catch (error) {
        console.error('❌ 审核操作失败:', error);
        console.error('❌ 错误详情:', {
          message: error.message,
          response: error.response?.data,
          status: error.response?.status
        });
        
        // 根据错误类型显示不同的提示
        if (error.message && error.message.includes('审核记录不存在')) {
          console.log('❌ 审核记录不存在错误');
          message.error('审核记录不存在，可能您没有审核权限或记录已被处理');
        } else if (error.message && error.message.includes('需要重新选择身份')) {
          console.log('❌ 身份验证错误');
          message.error('登录状态异常，请重新登录');
          // 可以在这里添加跳转到登录页的逻辑
        } else {
          console.log('❌ 其他错误:', error.message);
          message.error('审核操作失败: ' + (error.message || '未知错误'));
        }
      } finally {
        processingId.value = null;
      }
    };

    const formatTime = (timeStr) => {
      if (!timeStr) return '未知时间';
      try {
        const date = new Date(timeStr);
        return date.toLocaleString('zh-CN');
      } catch (error) {
        return timeStr;
      }
    };

    // 获取状态颜色
    const getStatusColor = (status) => {
      switch (status) {
        case '待审核':
          return 'orange';
        case '已通过':
          return 'green';
        case '已拒绝':
          return 'red';
        default:
          return 'default';
      }
    };

    // 获取状态样式类
    const getStatusClass = (status) => {
      switch (status) {
        case '待审核':
          return 'status-pending';
        case '已通过':
          return 'status-approved';
        case '已拒绝':
          return 'status-rejected';
        default:
          return 'status-unknown';
      }
    };

    // 获取状态文本
    const getStatusText = (status) => {
      switch (status) {
        case '待审核':
          return '待审核';
        case '已通过':
          return '已通过';
        case '已拒绝':
          return '已拒绝';
        default:
          return status || '未知状态';
      }
    };

    // 获取待审核数量
    const getPendingCount = () => {
      return reviewList.value.filter(item => item.reviewStatus === '待审核').length;
    };

    // 检查当前用户是否有审核权限
    const hasReviewPermission = computed(() => {
      const currentUser = authStore.volunteer;
      console.log('🔍 权限检查 - 当前用户:', currentUser);
      
      if (!currentUser) {
        console.log('❌ 权限检查失败 - 用户不存在');
        return false;
      }
      
      // 检查是否有社区管理权限
      if (!currentUser.communityManager || currentUser.communityManager === '') {
        console.log('❌ 权限检查失败 - 无社区管理权限:', currentUser.communityManager);
        return false;
      }
      
      // 检查是否为管理员或注册人
      const hasPermission = currentUser.communityManager === '管理员' || 
                           currentUser.communityManager === '注册人';
      
      console.log('🔍 权限检查结果:', {
        communityManager: currentUser.communityManager,
        hasPermission: hasPermission,
        volunteerId: currentUser.volunteerId,
        communityId: currentUser.communityId
      });
      
      return hasPermission;
    });

    // 添加一个方法来检查用户是否真的有权限操作特定记录
    const canReviewRecord = (record) => {
      const currentUser = authStore.volunteer;
      if (!currentUser) return false;
      
      // 检查用户是否有基本权限
      if (!hasReviewPermission.value) return false;
      
      // 检查记录是否属于用户所在的社区
      if (record.communityId !== currentUser.communityId) {
        console.log('❌ 记录不属于用户社区:', {
          recordCommunityId: record.communityId,
          userCommunityId: currentUser.communityId
        });
        return false;
      }
      
      return true;
    };

    onMounted(() => {
      loadReviewList();
    });

    return {
      loading,
      processingId,
      reviewList,
      columns,
      hasReviewPermission,
      canReviewRecord,
      loadReviewList,
      approve,
      reject,
      formatTime,
      getStatusColor,
      getStatusClass,
      getStatusText,
      getPendingCount
    };
  }
};
</script>

<style scoped>
.community-review {
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  margin-bottom: 18px;
}
.page-header h2 {
  margin: 0 0 2px 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text);
}
.page-header p {
  margin: 0;
  color: var(--text-2);
  font-size: 13px;
}
.permission-warning {
  margin-top: 14px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.review-count {
  font-size: 13px;
  color: var(--text-2);
  font-weight: 500;
}
.pending-count {
  color: var(--warning);
  font-weight: 600;
}

.empty-state {
  text-align: center;
  padding: 48px 20px;
  color: var(--text-3);
}
.empty-state h3 {
  margin: 0 0 6px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-2);
}
.empty-state p {
  margin: 0;
  font-size: 13px;
}

.user-info,
.community-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.user-name,
.community-name {
  font-weight: 600;
  color: var(--text);
  font-size: 13px;
}
.user-id,
.community-id {
  font-size: 12px;
  color: var(--text-2);
  font-family: var(--font-mono);
}

.no-permission {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
