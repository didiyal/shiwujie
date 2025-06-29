<template>
  <view class="profile-container">
     <!-- 背景图片 -->
     <image 
       class="background-image" 
       src="/static/images/background.png" 
       mode="aspectFill"
     />
     
     <!-- 返回按钮 -->
     <view class="header">
       
     </view>
 
     <!-- 用户信息卡片 -->
     <view class="profile-content" :aria-hidden="showFamilyModal">
       <!-- 白色背景卡片 -->
       <view class="info-card">
         <!-- 用户名和家庭信息 -->
         <text class="username">{{ familyInfo?.familyName ? `家庭名称：${familyInfo.familyName}` : '未加入家庭' }}</text>
		 
		 <text class="family-title" 
		       :accessible="true" 
		       :aria-label="'家庭账号：' + (familyInfo?.familyAccount || '暂无家庭账号')">
		   家庭账号：{{ familyInfo?.familyAccount || '-' }}
		 </text>
        <!-- <text class="family-title">
          {{ familyInfo?.familyAccount || '-' }}
         </text> -->
 
        <!-- 统计数据 -->
        <view class="stats-container">
        <!--  <view class="stat-item">
           <text class="stat-number">{{ familyInfo?.id || '-' }}</text>
            <text class="stat-label">家庭ID</text>
          </view> -->
		  
		  
          <view class="stat-item">
            <text class="stat-number">{{ userList.length || 0 }}</text>
            <text class="stat-label">家庭成员</text>
          </view>
          <view class="stat-item">
            <text class="stat-number">
              {{ currentUserRole }} <!-- 使用计算属性显示角色 -->
            </text>
            <text class="stat-label">角色</text>
          </view>
        </view>
        
        <!-- 家庭成员列表 -->
      <view class="member-list" v-if="userList.length > 0">
        <text class="section-title">家庭成员</text>
        <view class="member-container">
          <view class="member-item" v-for="member in userList" :key="member.id">
            <view class="member-info">
            <!--  <text class="member-id">ID: {{ member.id }}</text> -->
              <text class="member-name">用户名：{{ member.userName||'未设置' }}</text>
              <text class="member-account">账号: {{ member.userAccount }}</text>
              <text class="member-role" :class="{ 'admin-role': member.id === familyInfo?.userId }">
                {{ member.id === familyInfo?.userId ? '家主' : '成员' }}
              </text>
            </view>
            <!-- 添加移除按钮 - 只有家主可以看到，且不能移除自己 -->
            <view class="member-actions" v-if="isCurrentUserOwner && member.id !== familyInfo?.userId">
              <button class="remove-btn" @click="handleRemoveMember(member.id)">移除</button>
            </view>
          </view>
        </view>
      </view>

         <!-- 操作按钮 - 根据用户身份显示不同按钮 -->
         <view class="action-buttons" v-if="familyInfo">
           <!-- 家主显示修改和解散按钮 -->
           <template v-if="isCurrentUserOwner">
             <button class="message-btn" @click="handleUpdateFamily">
               修改家庭信息
             </button>
             <button class="follow-btn" @click="handleDismissFamily">
               解散家庭
             </button>
           </template>
           <!-- 普通成员只显示退出按钮 -->
           <template v-else>
             <button class="follow-btn" @click="handleQuitFamily">
               退出家庭
             </button>
           </template>
         </view>
       </view>
     </view>
    
    <!-- 添加家庭弹窗组件 -->
    <view 
      class="family-modal" 
      v-if="showFamilyModal" 
      role="dialog" 
      aria-modal="true" 
      aria-labelledby="modal-title"
      ref="modalRef"
    >
      <view class="modal-content">
        <view class="modal-header">
          <text class="modal-title" id="modal-title">创建或加入家庭</text>
        </view>
        
        <view class="modal-tabs">
          <text 
            :class="['tab-item', modalType === 'join' ? 'active-tab' : '']" 
            @click="switchModalType('join')"
            role="tab"
            :aria-selected="modalType === 'join'"
            tabindex="0"
          >加入家庭</text>
          <text 
            :class="['tab-item', modalType === 'create' ? 'active-tab' : '']" 
            @click="switchModalType('create')"
            role="tab"
            :aria-selected="modalType === 'create'"
            tabindex="0"
          >创建家庭</text>
        </view>
        
        <view class="modal-form">
          <!-- 加入家庭表单 -->
          <view v-if="modalType === 'join'" class="form-content" role="tabpanel">
            <input 
              type="text" 
              v-model="familyAccount" 
              placeholder="请输入家庭账号" 
              class="form-input"
              aria-label="家庭账号"
              ref="joinInput"
            />
            <button 
              class="form-button join-btn" 
              @click="handleJoinFamily"
              aria-label="加入家庭"
            >加入家庭</button>
          </view>
          
          <!-- 创建家庭表单 -->
          <view v-if="modalType === 'create'" class="form-content" role="tabpanel">
            <input 
              type="text" 
              v-model="familyName" 
              placeholder="请输入家庭名称" 
              class="form-input"
              aria-label="家庭名称"
              ref="createInput"
            />
            <button 
              class="form-button create-btn" 
              @click="handleCreateFamily"
              aria-label="创建家庭"
            >创建家庭</button>
          </view>
        </view>
      </view>
    </view>
  </view>
  <custom-tabbar
       :tabbarType="tabbarType" 
       :currentPage="2"
     ></custom-tabbar>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue';
import CustomTabbar from "@/components/customTabbar.vue";
import { onShow } from '@dcloudio/uni-app';

import Storage from "../../utils/storage";
import {
  createFamily, getFamilyByAccountInfo,
  getFamilyByIdInfo, joinFamily, quitFamilyInfo,
  deleteFamilyInfo, updateFamilyNameInfo, removeUserFromFamilyInfo,
} from '../../api/family';
import { getUserInfo } from '../../api/user'; 

// 响应式数据
const userInfo = ref(null);
const familyInfo = ref(null);
const userList = ref([]);
const showFamilyModal = ref(false);
const modalType = ref('join');
const familyAccount = ref('');
const familyName = ref('');
const isCurrentUserOwner = ref(false); // 当前用户是否为家主

// 引用元素
const modalRef = ref(null);
const joinInput = ref(null);
const createInput = ref(null);

// 计算属性：获取当前用户角色
const currentUserRole = computed(() => {
  return isCurrentUserOwner.value ? '家主' : '成员';
});

// 返回上一页
const goBack = () => {
  uni.navigateBack({
    delta: 1
  });
};

// 显示弹窗并设置焦点
const showModal = () => {
  showFamilyModal.value = true;
  // 等DOM更新后设置焦点
  nextTick(() => {
    focusModal();
  });
};

// 设置弹窗内第一个元素的焦点
const focusModal = () => {
  if (modalType.value === 'join') {
    if (joinInput.value) {
      joinInput.value.focus();
    }
  } else {
    if (createInput.value) {
      createInput.value.focus();
    }
  }
};

// 检查当前用户是否为家主
const checkIsOwner = async () => {
  try {
    const res = await getUserInfo();
    if (res.data?.code === 1 && res.data?.data) {
      const currentUserId = res.data.data.id;
      isCurrentUserOwner.value = currentUserId === familyInfo.value?.userId;
    }
  } catch (err) {
    console.error('获取用户信息失败：', err);
    isCurrentUserOwner.value = false;
  }
};

// 初始化用户信息
const initUserInfo = () => {
  const storageUserInfo = Storage.getUserInfo();
  console.log('从Storage获取的用户信息:', storageUserInfo);
  
  if (!storageUserInfo) {
    console.log('未找到用户信息，显示弹窗');
    showModal();
    return false;
  }
  
  userInfo.value = storageUserInfo;
  
  if (!storageUserInfo.familyAccount) {
    console.log('用户未加入家庭，显示弹窗');
    showModal();
    return false;
  }
  
  return true;
};

// 获取家庭信息
const getFamilyInfo = async () => {
  try {
    const storageUserInfo = Storage.getUserInfo();
    if (!storageUserInfo?.familyAccount) {
      console.log('用户未加入家庭');
      showModal();
      return;
    }
    
    const res = await getFamilyByAccountInfo(storageUserInfo.familyAccount);
    console.log('获取家庭信息返回数据:', res);
    
    if (res.code === 1 && res.data) {
      // 存储整个家庭信息
      familyInfo.value = res.data;
      Storage.setFamilyList(res.data);
      
      // 更新用户列表
      if (res.data.userList) {
        userList.value = res.data.userList;
      }
      
      // 获取家庭信息后检查是否为家主
      await checkIsOwner();
    } else {
      throw new Error(res.description || '获取家庭信息失败');
    }
  } catch (error) {
    console.error('获取家庭信息失败:', error);
    familyInfo.value = null;
    userList.value = [];
    Storage.removeFamilyList();
    
    
  }
};

// 页面初始化
const initPage = async () => {
  try {
    uni.showLoading({ title: '加载中...' });
    
    const hasFamily = initUserInfo();
    console.log('初始化用户信息结果:', hasFamily);
    
    if (hasFamily) {
      await getFamilyInfo();
      
      // 获取存储的家庭信息
      const storedFamilyInfo = Storage.getFamilyList();
      console.log('从Storage获取的家庭信息:', storedFamilyInfo);
      
      if (storedFamilyInfo) {
        familyInfo.value = storedFamilyInfo;
        userList.value = storedFamilyInfo.userList || [];
      }
    }
  } catch (error) {
    console.error('页面初始化失败:', error);
    
  } finally {
    uni.hideLoading();
  }
};

// 切换弹窗类型
const switchModalType = (type) => {
  modalType.value = type;
  familyAccount.value = '';
  familyName.value = '';
  
  // 切换后重设焦点
  nextTick(() => {
    focusModal();
  });
};

// 加入家庭
const handleJoinFamily = async () => {
  if (!familyAccount.value) {
    uni.showToast({
      title: '请输入家庭账号',
      icon: 'none'
    });
    return;
  }

  try {
    uni.showLoading({ title: '加入中...' });
    const res = await joinFamily(familyAccount.value);
    
    console.log('加入家庭返回数据:', res);
    
    if (res.statusCode === 200 && res.data.code === 1) {
      // 存储整个家庭信息
      familyInfo.value = res.data.data;
      Storage.setFamilyList(res.data.data);
      
      // 更新用户列表
      if (res.data.data.userList) {0
        userList.value = res.data.data.userList;
      }
	  
      const currentUser = Storage.getUserInfo();
      if (currentUser) {
        currentUser.familyAccount = familyAccount.value;
        Storage.setUserInfo(currentUser);
      }
      
      uni.showToast({
        title: '加入成功',
        
      });
      showFamilyModal.value = false;
      await getFamilyInfo();
    } else {
      throw new Error(res.data?.description || '加入家庭失败');
    }
  } catch (error) {
    console.error('加入家庭失败:', error);
    
  } finally {
    uni.hideLoading();
  }
};

// 创建家庭
const handleCreateFamily = async () => {
  if (!familyName.value) {
    uni.showToast({
      title: '请输入家庭名称',
      icon: 'none'
    });
    return;
  }

  try {
    uni.showLoading({ title: '创建中...' });
    
    const token = uni.getStorageSync('jwt_token');
    if (!token) {
      throw new Error('请先登录');
    }

    console.log('创建家庭请求参数:', {
      familyName: familyName.value,
      token
    });
    
    const res = await createFamily(familyName.value);
    console.log('创建家庭返回数据:', res);

    if (res.data.code === 1) {
      // 存储整个家庭信息
      familyInfo.value = res.data.data;
      Storage.setFamilyList(res.data.data);
      
      // 更新用户列表
      if (res.data.data.userList) {
        userList.value = res.data.data.userList;
      }
      
      const currentUser = Storage.getUserInfo();
      if (currentUser && res.data.data?.familyAccount) {
        currentUser.familyAccount = res.data.data.familyAccount;
        Storage.setUserInfo(currentUser);
      }
      
      uni.showToast({
        title: '创建成功',
        
      });
      
      showFamilyModal.value = false;
      await getFamilyInfo();
    } else {
      throw new Error(res.data?.description || '创建家庭失败');
    }
  } catch (error) {
    console.error('创建家庭失败:', error);
    uni.showToast({
      title: `创建失败: ${error.message}`,
      icon: 'none',
      duration: 3000
    });
  } finally {
    uni.hideLoading();
  }
};

// 退出家庭方法
const handleQuitFamily = async () => {
  try {
    const result = await quitFamilyInfo();
    if (result.code === 1) {
      Storage.removeFamilyList();
      familyInfo.value = null;
      userList.value = [];

      const currentUser = Storage.getUserInfo();
      if (currentUser) {
        currentUser.familyAccount = '';
        Storage.setUserInfo(currentUser);
      }
      
      uni.showToast({
        title: result.description || '退出成功',
        
      });
      
      showModal();
    } else {
      throw new Error(result.description || '退出失败');
    }
  } catch (error) {
    console.error('退出家庭失败:', error);
    
  }
};

// 解散家庭
const handleDismissFamily = async () => {
  try {
    // 添加二次确认
    const confirmResult = await new Promise((resolve) => {
      uni.showModal({
        title: '确认解散',
        content: '确定要解散该家庭吗？此操作不可撤销。',
        confirmText: '确认解散',
        cancelText: '取消',
        success: (res) => resolve(res.confirm)
      });
    });

    if (!confirmResult) {
      return;
    }

    // 添加加载提示
    uni.showLoading({ title: '解散中...' });

    // 传入家庭ID
    const result = await deleteFamilyInfo(familyInfo.value?.id);
    
    if (result.code === 1) {
      // 清除所有相关数据
      Storage.removeFamilyList();
      familyInfo.value = null;
      userList.value = []; // 清空用户列表
      
      // 清除用户的家庭账号
      const currentUser = Storage.getUserInfo();
      if (currentUser) {
        currentUser.familyAccount = '';
        Storage.setUserInfo(currentUser);
      }

      uni.showToast({
        title: result.description || '解散成功',
        
      });
      
      // 显示创建/加入家庭的弹窗
      showModal();

      // 重新获取家庭信息
      await getFamilyInfo();
    } else {
      throw new Error(result.description || '解散家庭失败');
    }
  } catch (error) {
    console.error('解散家庭失败:', error);
    
  } finally {
    // 确保加载提示被关闭
    uni.hideLoading();
  }
};

// 修改家庭信息
const handleUpdateFamily = async () => {
  // 使用 uni.showModal 创建输入弹窗
  uni.showModal({
    title: '修改家庭名称',
    editable: true,
    placeholderText: '请输入新的家庭名称',
    success: async (res) => {
      if (res.confirm && res.content?.trim()) {
        try {
          uni.showLoading({ title: '修改中...' });
          
          // 传递家庭名称和家庭ID
          const result = await updateFamilyNameInfo({
            familyName: res.content.trim(),
            id: familyInfo.value?.id // 从当前家庭信息中获取ID
          });
          
          if (result.code === 1) {
            uni.showToast({
              title: result.description || '修改成功',
            });
            
            // 修改成功后刷新家庭信息
            await getFamilyInfo();
          } else {
            throw new Error(result.description || '修改失败');
          }
        } catch (error) {
          console.error('修改家庭信息失败:', error);
          uni.showToast({
            title: error.message || '修改失败',
            icon: 'none'
          });
        } finally {
          uni.hideLoading();
        }
      } else if (res.confirm && !res.content?.trim()) {
        uni.showToast({
          title: '家庭名称不能为空',
          icon: 'none'
        });
      }
    }
  });
};

// 移除家庭成员方法
const handleRemoveMember = async (memberId) => {
  try {
    // 添加二次确认
    const confirmResult = await new Promise((resolve) => {
      uni.showModal({
        title: '确认移除',
        content: '确定要将该成员移出家庭吗？',
        confirmText: '确认移除',
        cancelText: '取消',
        success: (res) => resolve(res.confirm)
      });
    });

    if (!confirmResult) {
      return;
    }

    // 添加加载提示
    uni.showLoading({ title: '处理中...' });
    
    // 调用移除用户API
    const result = await removeUserFromFamilyInfo(memberId);
    
    if (result) {
      uni.showToast({
        title: result || '移除成功',
      });
      
      // 移除成功后刷新家庭信息
      await getFamilyInfo();
    } else {
      throw new Error('移除家庭成员失败');
    }
  } catch (error) {
    console.error('移除家庭成员失败:', error);
    
  } finally {
    uni.hideLoading();
  }
};

// 页面加载时初始化
onShow(() => {
  // 每次页面显示时刷新家庭信息
  initPage();
});
// 页面卸载时清理
onUnmounted(() => {
  // 已移除键盘事件监听器的清理
});

// 导出方法和数据
defineExpose({
  userInfo,
  familyInfo,
  userList,
  isCurrentUserOwner, 
  currentUserRole, 
  getFamilyInfo,
  getLatestUserList: () => {
    const familyData = Storage.getFamilyList();
    return familyData?.userList || [];
  },
  goBack,
  showFamilyModal,
  modalType,
  familyAccount,
  familyName,
  handleJoinFamily,
  handleCreateFamily,
  switchModalType,
  initUserInfo,
  initPage,
  handleQuitFamily,
  handleDismissFamily,
  handleUpdateFamily,
  handleRemoveMember
});
</script>


<style lang="scss">
.profile-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  padding-bottom: 120rpx;
  box-sizing: border-box;
  overflow-y: auto;
  
  .background-image {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
  }

  .header {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    padding: 44px 16px 16px;
    z-index: 2;

    .back-button {
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(255, 255, 255, 0.2);
      backdrop-filter: blur(10px);
      border-radius: 50%;
      
      .back-icon {
        color: #fff;
        font-size: 20px;
      }
    }
  }

  .profile-content {
    position: relative;
    z-index: 2;
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-top: 35%;
    padding: 0 20px 40rpx;

    .avatar {
      width: 100px;
      height: 100px;
      border-radius: 50%;
      border: 3px solid #fff;
      margin-bottom: -50px;
      z-index: 3;
    }

    .info-card {
      width: 100%;
      background: #fff;
      border-radius: 20px;
      padding: 40px 20px 30px;
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 20rpx;

      .username {
        font-size: 20px;
        font-weight: 600;
        color: #333;
        margin-bottom: 8px;
      }

      .family-title {
        font-size: 16px;
        color: #999;
        margin-bottom: 24px;
      }

      .stats-container {
        display: flex;
        justify-content: space-around;
        width: 100%;
        margin-bottom: 24px;
        padding: 0 10px;

        .stat-item {
          display: flex;
          flex-direction: column;
          align-items: center;
          padding: 0 15px;

          /* .stat-number {
            font-family: monospace;
            font-size: 18px;
            font-weight: 600;
            color: #333;
            margin-bottom: 4px;
          } */

          .stat-label {
            font-size: 12px;
            color: #999;
            text-transform: uppercase;
          }
        }
      }

      .member-list {
        width: 100%;
        margin-top: 20px;
        padding: 0 16px;
        
        .section-title {
          font-size: 18px;
          font-weight: 600;
          color: #333;
          margin-bottom: 16px;
          padding-bottom: 8px;
          border-bottom: 1px solid #eee;
        }
        
        .member-container {
          max-height: 400rpx;
          overflow-y: auto;
          -webkit-overflow-scrolling: touch;
          padding-right: 10rpx;
          
          .member-item {
            background: #f8f8f8;
            border-radius: 8px;
            padding: 12px;
            margin-bottom: 12px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            
            .member-info {
              display: flex;
              flex-direction: column;
              gap: 4px;
              
              .member-id {
                font-size: 12px;
                color: #999;
              }
              
              .member-name {
               font-size: 14px;
                font-weight: 500;
                color: #333;
				
              }
              
              .member-account {
                font-size: 14px;
                color: #666;
              }
              
              .member-role {
                font-size: 12px;
                color: #666;
                padding: 2px 8px;
                background: #eee;
                border-radius: 4px;
                align-self: flex-start;
                
                &.admin-role {
                  color: #fff;
                  background: #1677FF;
                }
              }
            }
          }
        }
      }

      .action-buttons {
        display: flex;
        gap: 16px;
        width: 100%;
        margin-top: 30rpx;
        margin-bottom: 20rpx;

        button {
          flex: 1;
          height: 44px;
          border-radius: 22px;
          font-size: 16px;
          font-weight: 500;
          border: none;
        }

        .follow-btn {
          background: #1677FF;
          color: #fff;
        }

        .message-btn {
          background: #1677FF;
          color: #fff;
          border: 1px solid #eee;
        }
      }
    }
  }
  
  .family-modal {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.6);
    z-index: 10;
    display: flex;
    justify-content: center;
    align-items: center;
    
    .modal-content {
      width: 80%;
      background: #fff;
      border-radius: 16px;
      padding: 24px;
      max-height: 80vh;
      overflow-y: auto;
      
      .modal-header {
        text-align: center;
        margin-bottom: 20px;
        
        .modal-title {
          font-size: 20px;
          font-weight: 600;
          color: #333;
        }
      }
      
      .modal-tabs {
        display: flex;
        justify-content: center;
        margin-bottom: 24px;
        border-bottom: 1px solid #eee;
        
        .tab-item {
          padding: 10px 20px;
          font-size: 16px;
          color: #999;
          position: relative;
          
          &.active-tab {
            color: #1677FF;
            font-weight: 500;
            
            &::after {
              content: '';
              position: absolute;
              bottom: -1px;
              left: 50%;
              transform: translateX(-50%);
              width: 30px;
              height: 2px;
              background: #1677FF;
            }
          }
        }
      }
      
      .modal-form {
        .form-content {
          display: flex;
          flex-direction: column;
          gap: 16px;
          
          .form-input {
            height: 44px;
            border-radius: 8px;
            border: 1px solid #eee;
            padding: 0 16px;
            font-size: 16px;
          }
          
          .form-button {
            height: 44px;
            border-radius: 8px;
            font-size: 16px;
            font-weight: 500;
            color: #fff;
            border: none;
            
            &.join-btn {
              background: #1677FF;
            }
            
            &.create-btn {
              background: #1677FF;
            }
          }
        }
      }
    }
  }
}

.member-container::-webkit-scrollbar {
  width: 4px;
}

.member-container::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 2px;
}
</style>