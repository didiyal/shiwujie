<template>
  <view class="container">
    <!-- 顶部切换栏 -->
    <view class="tab-header">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'activity' }"
        @tap="switchTab('activity')"
      >
        社区活动
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'help' }"
        @tap="switchTab('help')"
      >
        社区求助
      </view>
    </view>

    <!-- 校内活动页面 -->
    <view v-if="currentTab === 'activity'">
      <!-- 搜索框 -->
      <view class="search-box">
        <input type="text" placeholder="Search" class="search-input" />
        <image src="/static/images/scan.png" mode="aspectFit" class="scan-icon" />
      </view>

      <!-- 标题区域 -->
      <view class="title-section">
        <text class="main-title">社区活动</text>
        <text class="sub-title">为你推荐社区热门活动</text>
      </view>

      <!-- 活动列表 -->
      <view class="activity-grid">
        <!-- 左侧活动卡片 -->
        <view class="activity-card">
          <image src="/static/iconOrig/new1.jpeg" mode="aspectFill" class="activity-image" />
          <view class="status-tag">报名中</view>
          <view class="activity-date">2025.03.30 至 2025.03.30</view>
          <view class="activity-title">公益助残送温暖</view>
          <view class="activity-tag">社区公益</view>
        </view>

        <!-- 右侧活动卡片 -->
        <view class="activity-card">
          <image src="/static/iconOrig/new4.jpeg" mode="aspectFill" class="activity-image" />
          <view class="status-tag">报名中</view>
          <view class="activity-date">2025.04.01 至 2025.04.02</view>
          <view class="activity-title">维护盲道：为盲人出行保驾护航</view>
          <view class="activity-tag">公益活动</view>
        </view>

        <!-- 左侧活动卡片 -->
        <view class="activity-card">
          <image src="/static/images/user/3.jpg" mode="aspectFill" class="activity-image" />
          <view class="status-tag">报名中</view>
          <view class="activity-date">2025.03.31 至 2025.03.31</view>
          <view class="activity-title">科技助残产品亮相服贸会招募志愿者</view>
          <view class="activity-tag">社区活动</view>
        </view>

        <!-- 右侧活动卡片 -->
        <view class="activity-card">
          <image src="/static/images/user/4.jpg" mode="aspectFill" class="activity-image" />
          <view class="status-tag">进行中</view>
          <view class="activity-date">2024.11.07 至 2025.06.30</view>
          <view class="activity-title">一款手机软件连接起盲人和明眼人的世界</view>
          <view class="activity-tag">科技讲座</view>
        </view>
      </view>

      <!-- 发布活动按钮 -->
      <view class="publish-btn">
        发布活动
      </view>
    </view>

    <!-- 社区求助页面 -->
    <view v-else class="help-list-container">
      <!-- 顶部搜索栏 -->
      <view class="search-header">
        <view class="search-box help-search">
          <text class="search-icon">🔍</text>
          <input type="text" placeholder="求助信息" class="search-input"/>
        </view>
        <text class="search-button">搜索</text>
      </view>

      <!-- 分类标签 -->
      <view class="tag-section">
        <text class="tag-title">推荐</text>
        <text class="tag-item">紧急</text>
        <text class="more-settings">✏️ 偏好设置</text>
      </view>

      <!-- 求助列表 -->
      <view class="help-list">
        <!-- 求助项目1 -->
        <view class="help-item">
          <view class="help-content">
            <view class="help-title">需要陪同就医</view>
            <view class="help-tags">
              <text class="tag">紧急</text>
              <text class="tag">医疗陪同</text>
              <text class="tag">可报销交通费</text>
            </view>
            <view class="help-location">上海市徐汇区 · 2公里</view>
          </view>
          <view class="help-right">
            <image class="avatar" src="/static/images/avatar.png"></image>
          </view>
        </view>

        <!-- 求助项目2 -->
        <view class="help-item">
          <view class="help-content">
            <view class="help-title">需要帮忙读信</view>
            <view class="help-tags">
              <text class="tag">文字阅读</text>
              <text class="tag">可线上</text>
            </view>
            <view class="help-location">上海市浦东新区 · 5公里</view>
          </view>
          <view class="help-right">
         
            <image class="avatar" src="/static/images/avatar.png"></image>
          </view>
        </view>

        <!-- 广告卡片 -->
        <view class="ad-card">
          <view class="ad-content">
            <text>成为志愿者，帮助更多需要帮助的人</text>
            <text class="ad-button">点击查看</text>
          </view>
          <image class="ad-image" src="/static/images/volunteer.png"></image>
        </view>

        <!-- 求助项目3 -->
        <view class="help-item">
          <view class="help-content">
            <view class="help-title">需要帮忙购物</view>
            <view class="help-tags">
              <text class="tag">日常购物</text>
              <text class="tag">上门服务</text>
            </view>
            <view class="help-location">上海市黄浦区 · 1公里</view>
          </view>
          <view class="help-right">
            <image class="avatar" src="/static/images/avatar.png"></image>
          </view>
        </view>
      </view>
    </view>
  </view>
  
    <custom-tabbar tabbarType="volunteer" :currentPage="0"></custom-tabbar>
  
</template>

<script setup>
import { ref } from 'vue';
import CustomTabbar from "@/components/customTabbar.vue";
// 当前选中的标签页
const currentTab = ref('activity');

// 切换标签页
const switchTab = (tab) => {
  currentTab.value = tab;
};
</script>


<style lang="scss">
.container {
  min-height: 100vh;
  background: #f5f5f5;
  
}

// 顶部切换栏样式
.tab-header {
  display: flex;
  justify-content: center;
  background: #fff;
  padding: 20rpx 0;
  position: sticky;
  top: 0;
  z-index: 100;

  .tab-item {
    padding: 16rpx 40rpx;
    font-size: 32rpx;
    color: #666;
    position: relative;
    margin: 0 20rpx;

    &.active {
      color: #333;
      font-weight: 500;

      &::after {
        content: '';
        position: absolute;
        bottom: -4rpx;
        left: 50%;
        transform: translateX(-50%);
        width: 40rpx;
        height: 4rpx;
        background: #1677FF;
        border-radius: 2rpx;
      }
    }
  }
}

// 活动页面样式
.search-box {
  background: #fff;
  
  border-radius: 16rpx;
  padding: 20rpx;
  display: flex;
  align-items: center;
  margin: 20rpx;

  .search-input {
    flex: 1;
    height: 72rpx;
    font-size: 28rpx;
    color: #999;
  }

  .scan-icon {
    width: 48rpx;
    height: 48rpx;
  }
}

.title-section {
  padding: 20rpx;
  

  .main-title {
    font-size: 40rpx;
    font-weight: bold;
    color: #333;
    display: block;
    margin-bottom: 10rpx;
  }

  .sub-title {
    font-size: 28rpx;
	
    color: #999;
  }
}

.activity-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  padding: 10rpx 20rpx;
  

  .activity-card {
    width: 48%;
    background: #fff;
    border-radius: 16rpx;
    overflow: hidden;
    margin-bottom: 20rpx;
    position: relative;

    .activity-image {
      width: 100%;
      height: 320rpx;
      border-radius: 16rpx 16rpx 0 0;
    }

    .status-tag {
      position: absolute;
      top: 20rpx;
      left: 20rpx;
      background: rgba(255, 255, 255, 0.9);
      padding: 4rpx 16rpx;
      border-radius: 8rpx;
      font-size: 24rpx;
    }

    .activity-date {
      padding: 16rpx;
      font-size: 24rpx;
      color: #999;
    }

    .activity-title {
      padding: 0 16rpx;
      font-size: 28rpx;
      color: #333;
      font-weight: 500;
      line-height: 1.4;
      margin-bottom: 16rpx;
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 2;
      overflow: hidden;
    }

    .activity-tag {
      padding: 0 16rpx 16rpx;
      font-size: 24rpx;
      color: #666;
      background: #f5f5f5;
      display: inline-block;
      margin-left: 16rpx;
      border-radius: 4rpx;
    }
  }
}

.publish-btn {
  position: fixed;
  bottom: 40rpx;
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  padding: 20rpx 60rpx;
  border-radius: 100rpx;
  font-size: 28rpx;
  color: #333;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);
}

// 求助页面样式
.help-list-container {
  padding-bottom: 100rpx;
}

.search-header {
  padding: 20rpx 30rpx;
  display: flex;
  align-items: center;
  background: #fff;
  
  .help-search {
    flex: 1;
    height: 72rpx;
    background: #f5f5f5;
    border-radius: 36rpx;
    display: flex;
    align-items: center;
    padding: 0 20rpx;
    margin-right: 20rpx;
    
    .search-icon {
      margin-right: 10rpx;
      color: #999;
    }
  }
  
  .search-button {
    color: #007AFF;
    font-size: 32rpx;
  }
}

.tag-section {
  padding: 20rpx 30rpx;
  display: flex;
  align-items: center;
  background: #fff;
  margin-top: 20rpx;
  
  .tag-title {
    font-size: 28rpx;
    margin-right: 20rpx;
  }
  
  .tag-item {
    font-size: 24rpx;
    color: #666;
    background: #f5f5f5;
    padding: 4rpx 16rpx;
    border-radius: 20rpx;
  }
  
  .more-settings {
    margin-left: auto;
    font-size: 24rpx;
    color: #666;
  }
}

.help-list {
  padding: 20rpx;
  
  .help-item {
    background: #fff;
    border-radius: 16rpx;
    padding: 30rpx;
    margin-bottom: 20rpx;
    display: flex;
    justify-content: space-between;
    
    .help-content {
      flex: 1;
      
      .help-title {
        font-size: 32rpx;
        font-weight: 500;
        margin-bottom: 16rpx;
      }
      
      .help-tags {
        margin-bottom: 16rpx;
        
        .tag {
          font-size: 24rpx;
          color: #666;
          background: #f5f5f5;
          padding: 4rpx 16rpx;
          border-radius: 20rpx;
          margin-right: 12rpx;
        }
      }
      
      .help-location {
        font-size: 24rpx;
        color: #999;
      }
    }
    
    .help-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      
     
      
      .avatar {
        width: 60rpx;
        height: 60rpx;
        border-radius: 30rpx;
      }
    }
  }
}

.ad-card {
  background: #fff7e6;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .ad-content {
    flex: 1;
    font-size: 28rpx;
    color: #f90;
    
    .ad-button {
      display: inline-block;
      margin-top: 16rpx;
      font-size: 24rpx;
      color: #fff;
      background: #f90;
      padding: 8rpx 20rpx;
      border-radius: 24rpx;
    }
  }
  
  .ad-image {
    width: 120rpx;
    height: 120rpx;
    margin-left: 20rpx;
  }
}
</style>

