<template>
  <div class="employee-list">
    <div class="page-header">
      <h2>员工管理</h2>
      <p>管理社区内的员工成员</p>
      <div class="info-alert">
        <a-alert
          message="信息提示"
          description="显示社区管理人员（注册人、管理员、员工）。"
          type="info"
          show-icon
        />
      </div>
    </div>

    <a-card>
      <div class="action-bar">
        <div class="action-left">
          <a-button type="primary" @click="handleRefresh" :loading="loading">
            刷新
          </a-button>
        </div>
        <div class="action-right">
          <span class="total-count">共 {{ total }} 名员工</span>
        </div>
      </div>

      <!-- 员工列表 -->
      <div class="employee-grid" v-if="!loading && employeeList.length > 0">
        <div 
          v-for="employee in employeeList" 
          :key="employee.volunteerId" 
          class="employee-card"
        >
          <div class="employee-header">
            <div class="employee-avatar">
              <span class="avatar-text">{{ employee.name?.charAt(0) || 'U' }}</span>
            </div>
            <div class="employee-info">
              <h3 class="employee-name">{{ employee.name || '未知用户' }}</h3>
              <p class="employee-phone">{{ employee.phone || '暂无手机号' }}</p>
            </div>
            <div class="employee-status">
              <span :class="getOnlineStatusClass(employee.onlineStatus)">
                {{ getOnlineStatusText(employee.onlineStatus) }}
              </span>
            </div>
          </div>
          
          <div class="employee-details">
            <div class="detail-item">
              <span class="label">ID:</span>
              <span class="value">{{ employee.volunteerId }}</span>
            </div>
            <div class="detail-item">
              <span class="label">角色:</span>
              <span class="value">{{ employee.communityManager || '员工' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">性别:</span>
              <span class="value">{{ getGenderText(employee.gender) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">帮助次数:</span>
              <span class="value">{{ employee.helpCount || 0 }}</span>
            </div>
          </div>
          
                     <div class="employee-actions">
             <a-space>
               <a-button size="small" @click="viewDetail(employee)">
                 查看详情
               </a-button>
               <a-button
                 v-if="isRegistrant"
                 size="small"
                 type="primary"
                 @click="editEmployee(employee)"
               >
                 编辑
               </a-button>
             </a-space>
           </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loading && employeeList.length === 0" class="empty-state">
        <h3>暂无员工</h3>
        <p>当前社区还没有员工信息</p>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <a-spin size="large">
          <div class="loading-content">
            <p>正在加载员工信息...</p>
          </div>
        </a-spin>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <a-pagination
          :current="currentPage"
          :page-size="pageSize"
          :total="total"
          :show-size-changer="true"
          :show-quick-jumper="true"
          :show-total="(total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`"
          @change="handlePageChange"
          @show-size-change="handlePageSizeChange"
        />
      </div>
    </a-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed, h } from 'vue';
import { communityApi } from '@/api';
import { message, Modal } from 'ant-design-vue';
import { useAuthStore } from '@/stores/auth';

export default {
  name: 'EmployeeList',
  setup() {
    const authStore = useAuthStore();
    const loading = ref(false);
    const employeeList = ref([]);
    const total = ref(0);
    const currentPage = ref(1);
    const pageSize = ref(10);

    // 检查是否为注册人
    const isRegistrant = computed(() => {
      const currentUser = authStore.volunteer;
      return currentUser?.communityManager === '注册人';
    });

    // 加载员工列表
    const loadEmployeeList = async () => {
      loading.value = true;
      try {
        // 获取用户注册的社区ID列表
        const userCommunities = JSON.parse(localStorage.getItem('userCommunities') || '[]');
        console.log('🔍 用户社区ID列表:', userCommunities);
        
        if (userCommunities.length === 0) {
          console.log('❌ 用户没有管理的社区');
          message.error('您没有管理的社区');
          employeeList.value = [];
          total.value = 0;
          return;
        }

        // 使用第一个社区ID查询员工
        const communityId = userCommunities[0];
        console.log('🔍 使用社区ID查询员工:', communityId);
        
        const response = await communityApi.getCommunityEmployees(communityId, currentPage.value, pageSize.value);
        console.log('✅ 员工列表获取成功:', response);
        console.log('🔍 响应数据详情:', {
          records: response.records,
          total: response.total,
          recordsLength: response.records?.length || 0
        });
        
        employeeList.value = response.records || [];
        total.value = response.total || 0;
        
        // 调试每个员工的身份信息
        if (employeeList.value.length > 0) {
          console.log('🔍 员工身份信息:');
          employeeList.value.forEach((employee, index) => {
            console.log(`  员工${index + 1}:`, {
              volunteerId: employee.volunteerId,
              name: employee.name,
              communityManager: employee.communityManager,
              isCurrentUser: employee.volunteerId === authStore.volunteer?.volunteerId
            });
          });
        }
        
      } catch (error) {
        console.error('获取员工列表失败:', error);
        message.error('获取员工列表失败');
        employeeList.value = [];
        total.value = 0;
      } finally {
        loading.value = false;
      }
    };

    // 刷新数据
    const handleRefresh = () => {
      loadEmployeeList();
    };

    // 分页变化
    const handlePageChange = (page, pageSize) => {
      currentPage.value = page;
      loadEmployeeList();
    };

    // 页面大小变化
    const handlePageSizeChange = (current, size) => {
      currentPage.value = 1;
      pageSize.value = size;
      loadEmployeeList();
    };

    // 查看详情
    const viewDetail = (employee) => {
      console.log('查看员工详情:', employee);
      // TODO: 实现查看详情功能
      message.info('查看详情功能待实现');
    };

    // 编辑员工
    const editEmployee = (employee) => {
      console.log('编辑员工:', employee);
      
      // 创建编辑表单
      const editForm = {
        volunteerId: employee.volunteerId,
        name: employee.name,
        phone: employee.phone,
        gender: employee.gender,
        communityManager: employee.communityManager,
        roleName: employee.communityManager === '注册人' ? '注册人' : 
                  employee.communityManager === '管理员' ? '管理员' : '员工'
      };
      
      // 显示编辑对话框
      Modal.confirm({
        title: '编辑员工信息',
        width: 600,
        content: h('div', [
          h('div', { style: 'margin-bottom: 16px;' }, [
            h('label', { style: 'display: block; margin-bottom: 8px; font-weight: bold;' }, '姓名:'),
            h('input', {
              value: editForm.name,
              onInput: (e) => editForm.name = e.target.value,
              style: 'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;'
            })
          ]),
          h('div', { style: 'margin-bottom: 16px;' }, [
            h('label', { style: 'display: block; margin-bottom: 8px; font-weight: bold;' }, '手机号:'),
            h('input', {
              value: editForm.phone,
              onInput: (e) => editForm.phone = e.target.value,
              style: 'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;'
            })
          ]),
          h('div', { style: 'margin-bottom: 16px;' }, [
            h('label', { style: 'display: block; margin-bottom: 8px; font-weight: bold;' }, '性别:'),
            h('select', {
              value: editForm.gender,
              onChange: (e) => editForm.gender = parseInt(e.target.value),
              style: 'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;'
            }, [
              h('option', { value: 0 }, '未知'),
              h('option', { value: 1 }, '男'),
              h('option', { value: 2 }, '女')
            ])
          ]),
          h('div', { style: 'margin-bottom: 16px;' }, [
            h('label', { style: 'display: block; margin-bottom: 8px; font-weight: bold;' }, '角色:'),
            h('select', {
              value: editForm.roleName,
              onChange: (e) => editForm.roleName = e.target.value,
              style: 'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;'
            }, [
              h('option', { value: '员工' }, '员工'),
              h('option', { value: '管理员' }, '管理员'),
              h('option', { value: '注册人' }, '注册人')
            ])
          ])
        ]),
        okText: '保存',
        cancelText: '取消',
        onOk: () => updateEmployee(editForm)
      });
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



    // 更新员工信息
    const updateEmployee = async (editForm) => {
      try {
        console.log('🔍 开始更新员工信息:', editForm);
        
        // 获取用户注册的社区ID列表
        const userCommunities = JSON.parse(localStorage.getItem('userCommunities') || '[]');
        if (userCommunities.length === 0) {
          message.error('您没有管理的社区');
          return;
        }

        const communityId = userCommunities[0];
        console.log('🔍 使用社区ID更新员工:', communityId);
        
        const response = await communityApi.updateCommunityManager(
          communityId,
          editForm.volunteerId,
          editForm.roleName
        );
        
        console.log('✅ 更新员工信息成功:', response);
        message.success('更新员工信息成功');
        
        // 刷新员工列表
        await loadEmployeeList();
        
      } catch (error) {
        console.error('更新员工信息失败:', error);
        message.error('更新员工信息失败: ' + (error.message || '未知错误'));
      }
    };

    onMounted(() => {
      loadEmployeeList();
    });

    return {
      loading,
      employeeList,
      total,
      currentPage,
      pageSize,
      isRegistrant,
      loadEmployeeList,
      handleRefresh,
      handlePageChange,
      handlePageSizeChange,
      viewDetail,
      editEmployee,
      updateEmployee,
      getOnlineStatusText,
      getOnlineStatusClass,
      getGenderText
    };
  }
};
</script>

<style scoped>
.employee-list {
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
.info-alert {
  margin-top: 14px;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  padding: 12px 16px;
  background: var(--bg);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
}
.total-count {
  color: var(--text-2);
  font-size: 13px;
  font-weight: 500;
}

.employee-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}
.employee-card {
  background: var(--surface);
  border: 1px solid var(--border-l);
  border-radius: var(--radius);
  padding: 18px;
  transition: var(--tr);
}
.employee-card:hover {
  border-color: var(--border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.employee-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.employee-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.avatar-text {
  color: #fff;
  font-size: 17px;
  font-weight: 600;
}
.employee-info {
  flex: 1;
  min-width: 0;
}
.employee-name {
  margin: 0 0 2px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.employee-phone {
  margin: 0;
  font-size: 13px;
  color: var(--text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.employee-details {
  margin-bottom: 14px;
}
.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;
}
.detail-item .label {
  color: var(--text-2);
}
.detail-item .value {
  color: var(--text);
  font-weight: 500;
}

.employee-actions {
  display: flex;
  justify-content: flex-end;
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

.loading-state {
  text-align: center;
  padding: 48px 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

.status-online { color: var(--success); font-weight: 500; font-size: 12px; }
.status-offline { color: var(--text-2); font-weight: 500; font-size: 12px; }
.status-busy { color: var(--warning); font-weight: 500; font-size: 12px; }
.status-unknown { color: var(--text-3); font-weight: 500; font-size: 12px; }
</style>
