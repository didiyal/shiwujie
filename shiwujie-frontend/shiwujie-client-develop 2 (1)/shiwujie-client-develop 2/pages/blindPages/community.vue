<template>
  <view class="profile-container">
    <back-button custom-url="/pages/blindPages/blindHome"></back-button>
    <view class="status-bar"></view>

    <view class="header">
      <text 
        class="title"
        @click="showDevelopingTip"
        :accessible="true"
        :aria-label="'点击发送求助帖，功能开发中'"
      >点击发送求助帖</text>
    </view>

    <view class="user-info">
      <view class="avatar-container">
        <image class="avatar" src="/static/images/touxiang.jpg" mode="aspectFill"></image>
        <view class="status-dot"></view>
      </view>
    </view>

    <view class="tab-container">
      <view class="tab" :class="{ active: activeTab === 'chat' }" @click="switchTab('chat')">
        <text>聊天</text>
      </view>
      <view class="tab" :class="{ active: activeTab === 'group' }" @click="switchTab('group')">
        <text>群聊</text>
      </view>
    </view>

    <view class="chat-list" v-if="activeTab === 'chat'">
      <view class="chat-item">
        <view class="chat-content">
          <view class="chat-header">
            <text class="chat-name">陪同就医发帖回复</text>
            <text class="chat-time">15分钟前</text>
          </view>
          <text class="chat-message">你好，我这边有时间</text>
        </view>
      </view>

      <view class="chat-item">
        <view class="chat-content">
          <view class="chat-header">
            <text class="chat-name">3.26视频志愿者</text>
            <text class="chat-time">20分钟前</text>
          </view>
          <text class="chat-message">在接收帮助过程有没有遇到突发情况</text>
        </view>
      </view>

      <view class="chat-item">
        <view class="chat-content">
          <view class="chat-header">
            <text class="chat-name">3.24视频志愿者</text>
            <text class="chat-time">25分钟前</text>
          </view>
          <text class="chat-message">在接收帮助过程有没有遇到突发情况</text>
        </view>
      </view>
    </view>

    <view class="chat-list" v-if="activeTab === 'group'">
      <view class="chat-item">
        <view class="group-avatar">
          <text>👪</text>
        </view>
        <view class="chat-content">
          <view class="chat-header">
            <text class="chat-name">家庭群聊</text>
            <text class="chat-time">1分钟前</text>
          </view>
          <text class="chat-message">志愿者小王：各位家人，明早八点会上门协助进行检查，记得留在家中哦！</text>
        </view>
      </view>

      <view class="chat-item">
        <view class="group-avatar">
          <text>🏢</text>
        </view>
        <view class="chat-content">
          <view class="chat-header">
            <text class="chat-name">社区通知群</text>
            <text class="chat-time">5小时前</text>
          </view>
          <text class="chat-message">社区工作人员：本周五下午三点，社区将开展盲人定向行走培训活动，有意向的朋友可在群内报名。</text>
        </view>
      </view>

      <view class="chat-item">
        <view class="group-avatar">
          <text>🎮</text>
        </view>
        <view class="chat-content">
          <view class="chat-header">
            <text class="chat-name">邻里一家亲</text>
            <text class="chat-time">昨天</text>
          </view>
          <text class="chat-message">群友小李：有没有明天一起小区散步的</text>
        </view>
      </view>
    </view>
  </view>
  <custom-tabbar tabbarType="blind" :currentPage="0"></custom-tabbar>
</template>

<script>
import CustomTabbar from "@/components/customTabbar.vue";
import { checkUserCertification } from "../../api/user";

export default {
  components: {
    CustomTabbar
  },

  data() {
    return {
      activeTab: 'chat',
      hasCheckedCertification: false
    }
  },

  methods: {
    showDevelopingTip() {
      uni.showModal({
        title: '温馨提示',
        content: '功能尚在开发，敬请期待',
        showCancel: false
      });
    },

    switchTab(tab) {
      this.activeTab = tab;
    },

    async checkCertification() {
      try {
        const isCertified = await checkUserCertification();
        this.hasCheckedCertification = true;
        if (!isCertified) {
          return false;
        }
        return true;
      } catch (error) {
        console.error('认证检查失败：', error);
        return false;
      }
    },

    async checkBeforeAction(action) {
      const isCertified = await this.checkCertification();
      if (isCertified) {
        action();
      }
    }
  },

  async onLoad() {
    await this.checkCertification();
  },

  async onShow() {
    if (!this.hasCheckedCertification) {
      await this.checkCertification();
    }
  },

  onHide() {
    this.hasCheckedCertification = false;
  },

  onUnload() {
    this.hasCheckedCertification = false;
  }
}
</script>

<style lang="scss">
// 样式保持不变...
</style>
<style lang="scss">
	.hide-tabbar {
	  position: relative;
	  z-index: 999;
	}
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    padding-top: 80rpx; /* 增加顶部间距，使整体下移 */
	
}

.profile-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #ffffff;
  
}

/* 状态栏 */
.status-bar {
  display: flex;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  
  
  .time {
    font-weight: 600;
    font-size: 28rpx;
  }
  
  .status-icons {
    display: flex;
    gap: 10rpx;
    
    .icon {
      font-size: 28rpx;
    }
  }
}

/* 页面标题 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  padding: 20rpx 30rpx 40rpx;
  
  .title {
    font-size: 40rpx;
    font-weight: 600;
  }
  
  .edit-button {
    width: 40rpx;
    height: 40rpx;
    font-size: 32rpx;
  }
}

/* 用户信息区域 */
.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30rpx;
  
  .avatar-container {
    position: relative;
    margin-bottom: 30rpx;
    
    .avatar {
      width: 160rpx;
      height: 160rpx;
      border-radius: 80rpx;
      background-color: #e0e0e0;
    }
    
    .status-dot {
      position: absolute;
      bottom: 10rpx;
      right: 10rpx;
      width: 20rpx;
      height: 20rpx;
      background-color: #4CAF50;
      border-radius: 10rpx;
      border: 3rpx solid #ffffff;
    }
  }
  
  .username {
    font-size: 36rpx;
    font-weight: 600;
    margin-bottom: 10rpx;
  }
  
  .bio {
    font-size: 28rpx;
   color: #757575;
 
    margin-bottom: 10rpx;
  }
  
  .phone-number {
    font-size: 28rpx;
    color: #3F51B5;
    font-weight: 500;
  }
}

/* 选项卡 */
.tab-container {
  display: flex;
  margin: 30rpx;
  margin-top: 40rpx;
  border-radius: 20rpx;
  overflow: hidden;
  background-color: #f0f0f0;
  
  .tab {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 20rpx 0;
    font-size: 28rpx;
    /* color: #757575; */
	
    transition: all 0.3s ease;
    
    &.active {
      background-color: #0D47A1;
      color: #f0f0f0;
      font-weight: 500;
      box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
    }
  }
}

/* 聊天列表 */
.chat-list {
  flex: 1;
  padding: 0 30rpx;
  
  .chat-item {
    display: flex;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f0f0f0;
    
    .chat-avatar, .group-avatar {
      width: 80rpx;
      height: 80rpx;
      border-radius: 40rpx;
      margin-right: 20rpx;
      background-color: #e0e0e0;
    }
    
    .group-avatar {
      display: flex;
      justify-content: center;
      align-items: center;
      background-color: #e3f2fd;
      font-size: 36rpx;
    }
    
    .chat-content {
      flex: 1;
      
      .chat-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 10rpx;
        
        .chat-name {
          font-size: 28rpx;
          font-weight: 600;
        }
        
        .chat-time {
          font-size: 24rpx;
          color: #9e9e9e;
        }
      }
      
      .chat-message {
        font-size: 26rpx;
        color: #757575;
        line-height: 1.4;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
    }
  }
}



</style>