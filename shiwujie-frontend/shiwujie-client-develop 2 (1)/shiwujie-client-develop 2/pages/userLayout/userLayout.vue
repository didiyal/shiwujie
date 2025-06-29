<template>
  <view 
    class="container"
    role="main"
    :accessible="true"
    :aria-label="isBlindUser ? '个人中心页面，盲人用户模式' : '个人中心页面'"
  >
    <!-- Header -->
    <view class="header">
    </view>

    <!-- User Profile -->
    <view class="user-profile">
      <view class="avatar-container">
        <image class="avatar" :src="userInfo?.avatar || '/static/images/touxiang.jpg'" mode="aspectFill"></image>
      </view>
      <view class="user-info">
        <text class="username">用户名：{{ userInfo?.userName || '未设置' }}</text>
        <text class="user-id" v-if="!isBlindUser">手机号：{{ formatPhone(userInfo?.userPhone) }}</text>
      </view>
    </view>

    <!-- family-card -->
    <view class="family-card">
      <view class="card-header">
        <view class="user-info-container">
          <view class="user-id-section">
            <text class="user-account">账号: {{ userInfo?.userAccount || '未设置' }}</text>
            <view class="id-badge" v-if="!isBlindUser">ID</view>
          </view>
          
          <view class="user-details">
            <view class="detail-item">
              <text class="detail-label" v-if="!isBlindUser">家庭账号:</text>
              <view class="detail-value-container">
                <text class="detail-value" v-if="!isBlindUser">{{ userInfo?.familyAccount || '还未注册家庭' }}</text>
                <button 
                  v-if="!userInfo?.familyAccount" 
                  class="join-family-btn"
                  @click="handleFamily"
                >
                  创建/加入家庭
                </button>
              </view>
            </view>
            <view class="detail-item">
              <text class="detail-label" v-if="!isBlindUser">性别:</text>
              <text class="detail-value" v-if="!isBlindUser">{{ formatGender(userInfo?.gender) }}</text>
            </view>
            <view class="detail-item">
              <text class="detail-label" v-if="!isBlindUser">邮箱:</text>
              <text class="detail-value" v-if="!isBlindUser">{{ formatEmail(userInfo?.userEmail) }}</text>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 只有盲人用户显示状态卡片 -->
      <view class="card-stats" v-if="isBlindUser">
        <view class="stat-item">
          <text class="stat-value" :class="{ 'certified': isCertified }">
            {{ isCertified ? '身份已认证' : '身份未认证' }}
            <text class="stat-unit"></text>
          </text>
        </view>
      </view>

      <view class="card-actions">
        <view 
          class="action-button call-button" 
          @click="changeInfo"
          :accessible="true"
          :aria-label="'修改个人信息，点击可修改用户名等个人资料'"
          role="button"
          :aria-disabled="false"
        >
          <text v-if="!isBlindUser" class="icon" aria-hidden="true">??</text>
          <text 
            class="action-text"
            :accessible="false"
          >修改个人信息</text>
        </view>
        <!-- 只有盲人用户显示证件按钮 -->
        <view 
          v-if="isBlindUser && !isCertified"
          class="action-button chat-button" 
          @click="showCertificateModal"
          :accessible="true"
          :aria-label="'添加证件，点击打开证件添加窗口'"
          role="button"
        >
          <text class="icon" aria-hidden="true">??</text>
          <text class="action-text">我的证件</text>
        </view>
      </view>
    </view>

    <!-- 抽屉导航菜单 -->
    <view class="drawer-menu">
      <!-- 家庭菜单项 -->
     <view class="menu-item" 
           v-if="!isBlindUser"  
           @click="handleFamily"
           :accessible="true"
           :aria-label="'我的家庭，点击进入家庭页面'"
           role="button">
         <view class="left-content">
           <image class="menu-icon" src="/static/iconOrig/family.png" mode="aspectFit"></image>
           <text class="menu-text">我的家庭</text>
         </view>
     </view>

      <!-- 退出登录菜单项 -->
      <view class="menu-item" 
            @click="userLogout"
            :accessible="true"
            :aria-label="'退出登录，点击退出当前账号'"
            role="button">
        <view class="left-content">
          <image v-if="!isBlindUser" class="menu-icon" src="/static/nodata/right.png" mode="aspectFit"></image>
          <text class="menu-text">退出登录</text>
        </view>
      </view>

      <!-- 注销账户菜单项 -->
      <view class="menu-item" 
            @click="handleDeleteAccount"
            :accessible="true"
            :aria-label="'注销账户，点击永久删除账号'"
            role="button">
        <view class="left-content">
          <image v-if="!isBlindUser" class="menu-icon" src="/static/iconOrig/deleteAccoount.png" mode="aspectFit"></image>
          <text class="menu-text">注销账户</text>
        </view>
      </view>
    </view>

    <!-- 证件号输入弹窗 -->
    <view class="modal" v-if="showModal">
      <view class="modal-content" 
            role="dialog"
            :accessible="true"
            :aria-label="'添加证件弹窗'">
        <view class="modal-header">
          <text class="modal-title" 
                role="heading" 
                :accessible="true"
                :aria-label="'添加证件标题'">添加证件</text>
        </view>
        
        <view class="modal-body">
          <view class="input-container">
            <input 
              type="text"
              v-model="certificateNumber"
              @input="handleInput"
              placeholder="请输入证件号"
              class="certificate-input"
              maxlength="20"
              :accessible="true"
              :aria-label="'证件号输入框，请输入20位数字'"
              role="textbox"
            />
          </view>
          <text class="balance-text" 
                :accessible="true"
                :aria-label="'当前证件号：' + (certificateNumber || '未设置')">
            当前证件号: {{ certificateNumber || '未设置' }}
          </text>

          <button 
            class="submit-btn" 
            :class="{'submit-btn-disabled': !isValidCertificate}"
            :disabled="!isValidCertificate"
            @click="handleSubmit"
            :accessible="true"
            :aria-label="isValidCertificate ? '点击添加证件' : '请先输入20位证件号'"
            role="button"
          >
            添加证件
          </button>
        </view>
      </view>
      <view class="modal-backdrop" 
            @click="closeModal"
            :accessible="true"
            :aria-label="'点击关闭弹窗'"
            role="button"></view>
    </view>

    <!-- 仅在当前页面显示 tabBar -->
    <custom-tabbar 
      :tabbarType="tabbarType" 
      :currentPage="4"
      :accessible="true"
      role="navigation"
    ></custom-tabbar>
  </view>
</template>

<script setup>
import CustomTabbar from "@/components/customTabbar.vue";
import { ref, onMounted, computed, onUnmounted } from 'vue';
import Storage from "../../utils/storage";
import { deleteUserAccount } from "../../api/user";
import { onShow } from '@dcloudio/uni-app';
const floatWin = uni.requireNativePlugin('Ba-FloatWinWeb');

// 创建响应式的用户信息
const userInfo = ref(null);

// 证件弹窗相关的响应式数据
const showModal = ref(false);
const certificateNumber = ref('');

// 判断是否为盲人用户
const isBlindUser = computed(() => {
  return userInfo.value?.status === 0;
});

// 创建认证状态的计算属性
const isCertified = computed(() => {
  const user = userInfo.value;
  return Boolean(user?.userCertificate) && user?.userCertificate !== 'false';
});

// 验证证件号
const isValidCertificate = computed(() => {
  return certificateNumber.value && certificateNumber.value.length === 20;
});

// 使用不同的变量名
const currentUserInfo = Storage.getUserInfo();
const tabbarType = computed(() => {
  return currentUserInfo.status === 0 ? 'blind' : 'volunteer';
});

// 定义导航方法
const navigateTo = (url) => {
  uni.showLoading({
    title: '加载中...'
  });

  uni.navigateTo({
    url: url,
    success: () => {
      uni.hideLoading();
    },
    fail: (err) => {
      console.error('导航失败：', err);
      uni.switchTab({
        url: url,
        success: () => {
          uni.hideLoading();
        },
        fail: (switchErr) => {
          console.error('切换标签页失败：', switchErr);
          uni.hideLoading();
          
        }
      });
    }
  });
};

// 格式化方法
const formatPhone = (phone) => {
  if (!phone) return '未设置';
  return phone.toString().replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
};

const formatGender = (gender) => {
  if (gender === undefined || gender === null) return '未设置';
  return gender === 0 ? '男' : '女';
};

const formatEmail = (email) => {
  if (!email) return 'NULL';
  const [username, domain] = email.split('@');
  if (!domain) return email;
  const maskedUsername = username.length > 4 
    ? `${username.slice(0, 3)}***${username.slice(-1)}`
    : username;
  return `${maskedUsername}@${domain}`;
};

// 弹窗相关方法
const showCertificateModal = () => {
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
  certificateNumber.value = '';
};

// 修改input的处理，限制只能输入数字
const handleInput = (e) => {
  certificateNumber.value = e.detail.value.replace(/\D/g, '').slice(0, 20);
};

// 处理证件提交
const handleSubmit = async () => {
  if (!isValidCertificate.value) {
    uni.showToast({
      title: '请输入20位证件号',
    
    });
    return;
  }

  try {
    uni.showLoading({ title: '添加中...' });
    
    const token = uni.getStorageSync('jwt_token');
    const res = await uni.request({
      url: 'http://43.139.38.62:8081/shiwujie/user/certificate/add',
      method: 'POST',
      header: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': `Bearer ${token}`,
      },
      data: {
        certificate: certificateNumber.value
      }
    });

    if (res.data.code === 1) {
      await refreshUserInfo();
      
      closeModal();
    } else {
      throw new Error(res.data.description || '添加失败');
    }
  } catch (error) {

  } finally {
    uni.hideLoading();
  }
};

// 设置菜单点击处理
const changeInfo = () => {
  navigateTo('/pages/userLayout/infor');
};

const handleFamily = () => {
  uni.navigateTo({
    url: "/pages/family/volunteerFamily",
    success: () => {
      console.log('跳转成功');
    },
    fail: (err) => {
      console.error('跳转失败：', err);
      uni.showToast({
        title: '页面跳转失败',
        icon: 'none',
        duration: 2000
      });
    }
  });
};
// 退出登录方法
const userLogout = async () => {
  try {
    uni.showLoading({ title: '退出中...' });
    
    const token = uni.getStorageSync("jwt_token");
    const res = await uni.request({
      url: 'http://43.139.38.62:8081/shiwujie/user/mine/logout',
      method: 'GET',
      header: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      withCredentials: true,
    });

    if (res.data.code === 1) {
      uni.clearStorageSync();
      //关闭跳转其他页面的悬浮窗
      floatWin.hide({
        tag: "button"
      },
      (res) => {
        console.log(res);
       
      });
      //关闭socket连接
      const app = getApp();
      app.disconnectWebSocket();
      
      setTimeout(() => {
        uni.reLaunch({ url: '/pages/register-login/loginAndRegisterQickliy' });
      }, 1500);
    } else {
      throw new Error(res.data.description || '退出失败');
    }
  } catch (err) {
    
  } finally {
    uni.hideLoading();
  }
};

const handleDeleteAccount = () => {
  uni.showModal({
    title: '注销账户',
    content: '确定要注销账号吗？该操作无法撤回',
    confirmText: '确定注销',
    confirmColor: '#ff4d4f',
    cancelText: '取消',
    success: async (res) => {
      if (res.confirm) {
        try {
          uni.showLoading({ title: '处理中...' });
          
          const res = await deleteUserAccount();

          if (res.code === 1) {
            Storage.clearAll();
            //关闭跳转其他页面的悬浮窗
            floatWin.hide({
              tag: "button"
            },
            (res) => {
              console.log(res);
              
            });
            //关闭socket连接
            const app = getApp();
            app.disconnectWebSocket();
            uni.showToast({
              title: '账号已注销',
             
            });
            
            setTimeout(() => {
              uni.reLaunch({ url: '/pages/register-login/loginAndRegisterQickliy' });
            }, 1500);
          } else {
            throw new Error(res.message || '注销失败');
          }
        } catch (error) {
          console.error('注销失败:', error);
          
        } finally {
          uni.hideLoading();
        }
      }
    }
  });
};

// 用户认证检查
const checkUserAuth = async () => {
  const token = uni.getStorageSync("jwt_token");
  try {
    const res = await uni.request({
      url: 'http://43.139.38.62:8081/shiwujie/user/test/jwt',
      method: 'GET',
      header: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      withCredentials: true,
    });
    
    return {
      success: res.data.code === 1,
      message: res.data.description
    };
  } catch (err) {
    return {
      success: false,
      message: '网络请求失败'
    };
  }
};

// 刷新用户信息
const refreshUserInfo = async () => {
  const token = uni.getStorageSync('jwt_token');
  if (!token) {
    uni.redirectTo({
      url: '/pages/register-login/login'
    });
    return;
  }
  
  try {
    const userRes = await uni.request({
      url: 'http://43.139.38.62:8081/shiwujie/user/mine/check',
      method: 'GET',
      header: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      }
    });

    if (userRes.data.code === 1) {
      const userData = userRes.data.data;
      userInfo.value = userData;
      Storage.setUserInfo(userData);
    }

    const authRes = await checkUserAuth();
    if (!authRes.success) {
      throw new Error('用户验证失败');
    }
  } catch (err) {
    console.error('Error:', err);
    
  }
};

// 生命周期钩子
onMounted(() => {
  uni.setNavigationBarTitle({
    title: '个人中心'
  });
  
  // 设置页面无障碍描述
  // #ifdef APP-PLUS-ANDROID
  if(uni.getSystemInfoSync().platform === 'android') {
    try {
      setTimeout(() => {
        const currentWebview = plus.webview.currentWebview();
        if (currentWebview && typeof currentWebview.setContentDescription === 'function') {
          currentWebview.setContentDescription('个人中心页面');
        }
      }, 300);
    } catch (error) {
      console.warn('设置无障碍描述失败:', error);
    }
  }
  // #endif

  uni.$on('fromCertificationCheck', () => {
    uni.$emit('updateTabBarManual', 4);
  });

  refreshUserInfo();
});

onShow(() => {
  if(isBlindUser.value) {
    setTimeout(() => {
      uni.$emit('screenReader', {
        text: '已进入个人中心页面'
      });
    }, 500);
  }
  
  uni.$emit('onShow');
  uni.$emit('updateTabBarManual', 4);
  refreshUserInfo();
});

onUnmounted(() => {
  uni.$off('fromCertificationCheck');
});
</script>
<style lang="scss">
.container {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(180deg, #0D47A1 0%, #1677FF 50%);
  padding: 10px;
  margin: 0;
  padding-bottom: 40rpx;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  border-bottom-left-radius: 30px;
  border-bottom-right-radius: 30px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
  padding-top: 80rpx;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
}

.user-profile {
  display: flex;
  align-items: center;
  padding: 15px;
  margin-bottom: 15px;
}

.avatar-container {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 15px;
  border: 2px solid #fff;
}

.avatar {
  width: 100%;
  height: 100%;
  background-color: #ccc;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.username {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 5px;
}

.user-id {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.family-card {
  margin: 20rpx;
  background-color: #fff;
  border-radius: 24rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  padding: 30rpx;
  background-color: #ffffff;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
  margin-bottom: 30rpx;
  align-items: center;
  
  .user-info-container {
    flex: 1;
    
    .user-id-section {
      display: flex;
      align-items: center;
      margin-bottom: 20rpx;
      
      .user-account {
        font-size: 32rpx;
        font-weight: 600;
        color: #333333;
        margin-right: 14rpx;
      }
      
      .id-badge {
        background-color: #1fc7bc;
        color: #ffffff;
        font-size: 20rpx;
        padding: 3rpx 12rpx;
        border-radius: 16rpx;
        display: inline-block;
      }
    }
    
    .user-details {
      .detail-item {
        display: flex;
        align-items: center;
        margin-bottom: 10rpx;
        
        .detail-label {
          font-size: 26rpx;
          color: #666666;
          width: 120rpx;
          flex-shrink: 0;
        }
        
        .detail-value-container {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: space-between;
          
          .detail-value {
            font-size: 26rpx;
            color: #333333;
          }
          
          .join-family-btn {
            font-size: 24rpx;
            color: #fff;
            background: #1fc7bc;
            padding: 0 20rpx;
            height: 35rpx;
            line-height: 35rpx;
            margin: 0;
            border: none;
            min-height: 0;
            
            &::after {
              border: none;
            }
          }
        }
      }
    }
  }
}

.card-stats {
  display: flex;
  margin-bottom: 30rpx;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
}

.stat-unit {
  font-size: 24rpx;
  font-weight: normal;
  margin-left: 4rpx;
}

.stat-label {
  font-size: 24rpx;
  color: #999;
  margin-top: 4rpx;
}

.card-actions {
  display: flex;
  gap: 20rpx;
}

.action-button {
  flex: 1;
  height: 80rpx;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
}

.call-button {
  background-color: #f2f8ff;
  color: #4585F5;
   &:active {
        opacity: 0.8;
        transform: scale(0.98);
      }
      
      // 添加焦点样式
      &:focus {
        outline: 4rpx solid #4585F5;
        outline-offset: 4rpx;
      }
      
      // 添加无障碍高对比度模式
      @media (forced-colors: active) {
        border: 2px solid currentColor;
      }
}

.chat-button {
  background-color: #4585F5;
  color: #fff;
}

.icon {
  margin-right: 10rpx;
  font-size: 32rpx;
}

.action-text {
  font-weight: 500;
}

.drawer-menu {
  margin-top: 20rpx;
  background-color: #fff;
  box-shadow: 0 2rpx 10rpx rgba(212, 230, 241, 0.8);
  border-radius: 16rpx;
  overflow: hidden;
  margin: 20rpx;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 20rpx;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }

  .left-content {
    display: flex;
    align-items: center;

    .menu-icon {
      width: 40rpx;
      height: 40rpx;
      margin-right: 20rpx;
    }

    .menu-text {
      font-size: 28rpx;
      color: #333;
      font-family: "Microsoft YaHei", "微软雅黑";
    }
  }

  .arrow-icon {
    width: 32rpx;
    height: 32rpx;
    opacity: 0.5;
  }

  &:active {
    background-color: #f9f9f9;
  }
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  
  .modal-backdrop {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
  }
  
  .modal-content {
    position: absolute;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);
    width: 80%;
    background: #fff;
    border-radius: 20rpx;
    padding: 30rpx;
    z-index: 1000;
    
    .modal-header {
      text-align: center;
      margin-bottom: 30rpx;
      
      .modal-title {
        font-size: 32rpx;
        font-weight: bold;
        color: #333;
      }
    }
    
    .modal-body {
      .input-container {
        margin-bottom: 20rpx;
        
        .certificate-input {
          width: 100%;
          height: 80rpx;
          background: #f5f5f5;
          border-radius: 40rpx;
          padding: 0 30rpx;
          font-size: 28rpx;
          box-sizing: border-box;
        }
      }
      
      .balance-text {
        font-size: 24rpx;
        color: #999;
        text-align: center;
        margin-bottom: 30rpx;
      }
    }
    
    .agreement-row {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 30rpx;
      
      .agreement-text {
        font-size: 24rpx;
        color: #666;
        margin-left: 10rpx;
      }
    }
    
    .submit-btn {
      width: 100%;
      height: 80rpx;
      line-height: 80rpx;
      background: #1677FF;
      color: #fff;
      border-radius: 40rpx;
      font-size: 28rpx;
      
      &:disabled {
        background: #ccc;
      }
    }
  }
}
</style>