<template>
  <view class="tabbar-container">
    <view class="tabbar-background"></view>
    <view 
      v-for="(item, index) in currentTabbarList" 
      :key="index"
      :class="['tabbar-item', item.centerItem ? 'center-item' : '', currentItem === item.id ? 'active' : '']"
      @click="changeItem(item)"
      :aria-label="item.text"
      :aria-role="button"
      :accessible="true"
      :aria-current="currentItem === item.id ? 'page' : undefined"
    >
      <view :class="['item-icon-container', item.centerItem ? 'center-icon-container' : '']">
        <image 
          :src="currentItem === item.id ? item.selectIcon : item.icon"
          mode="aspectFit"
          :class="['item-icon', item.centerItem ? 'center-icon' : '']"
          :aria-hidden="true"
        ></image>
      </view>
      <text :class="['item-text', currentItem === item.id ? 'active-text' : '', item.centerItem ? 'center-text' : '']">
        {{ item.text }}
      </text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { checkUserCertification } from "@/api/user.js";

// 定义 props
const props = defineProps({
  currentPage: {
    type: Number,
    default: 0
  },
  tabbarType: {
    type: String,
    default: 'blind',
    required: true
  }
});

// 定义响应式变量
const currentItem = ref(0);

// tabbar 配置保持不变
const blindTabbarList = [
  {
    id: 0,
    path: '/pages/blindPages/blindHome',
    icon: '/static/iconOrig/tabbarHome.png',
    selectIcon: '/static/iconOrig/home.png',
    text: '首页',
    centerItem: false
  },
  {
    id: 1,
    path: '/pages/blindPages/community',
    icon: '/static/iconOrig/community1.png',
    selectIcon: '/static/iconOrig/community2.png',
    text: '社区',
    centerItem: false
  },
  {
    id: 2,
    path: '/pages/AI-assistant/AI-help',
    icon: '/static/iconOrig/AI.png',
    selectIcon: '/static/iconOrig/AI.png',
    text: '智能AI',
    centerItem: true
  },
  {
    id: 3,
    path: '/pages/family/family',
    icon: '/static/iconOrig/family.png',
    selectIcon: '/static/iconOrig/family.png',
    text: '家庭',
    centerItem: false
  },
  {
    id: 4,
    path: '/pages/userLayout/userLayout',
    icon: '/static/iconOrig/PersonalCenter.png',
    selectIcon: '/static/iconOrig/touchPersonalCenter.png',
    text: '我的',
    centerItem: false
  }
];

const volunteerTabbarList = [
  {
    id: 0,
    path: '/pages/volunteerPages/volunteerHome',
    icon: '/static/iconOrig/tabbarHome.png',
    selectIcon: '/static/iconOrig/home.png',
    text: '首页',
    centerItem: false
  },
  {
    id: 1,
    path: '/pages/volunteerPages/community',
    icon: '/static/iconOrig/community.png',
    selectIcon: '/static/iconOrig/community.png',
    text: '社区',
    centerItem: true
  },
  {
    id: 2,
    path: '/pages/userLayout/userLayout',
    icon: '/static/iconOrig/PersonalCenter.png',
    selectIcon: '/static/iconOrig/touchPersonalCenter.png',
    text: '我的',
    centerItem: false
  }
];

const currentTabbarList = computed(() => {
  return props.tabbarType === 'blind' ? blindTabbarList : volunteerTabbarList;
});

const tabChangeEventName = computed(() => {
  return props.tabbarType === 'blind' ? 'tabChange' : 'volunteerTabChange';
});

// 新增：处理手动更新的事件
const handleManualUpdate = (tabId) => {
  currentItem.value = tabId;
  uni.$emit(tabChangeEventName.value, tabId);
};

// 修改：更新当前选中的 tab
const updateCurrentTab = (force = false) => {
  const pages = getCurrentPages();
  if (pages.length > 0) {
    const currentPage = pages[pages.length - 1];
    const currentPath = `/${currentPage.route}`;
    
    const matchedItem = currentTabbarList.value.find(item => 
      currentPath.includes(item.path.split('?')[0])
    );
    
    if (matchedItem) {
      if (force || currentItem.value !== matchedItem.id) {
        currentItem.value = matchedItem.id;
        uni.$emit(tabChangeEventName.value, matchedItem.id);
      }
    }
  }
};
let isProcessingChange = false;

// 修改：处理 tab 切换
const changeItem = async (item) => {
  // 如果正在处理切换，直接返回
  if (isProcessingChange || currentItem.value === item.id) return;
  
  try {
    isProcessingChange = true; // 设置处理标记
    
    // 如果是盲人用户且要跳转到AI页面，先进行证件验证
    if (props.tabbarType === 'blind' && item.path === '/pages/AI-assistant/AI-help') {
      const isCertified = await checkUserCertification();
      if (!isCertified) {
        // checkUserCertification 内部会处理弹窗和跳转逻辑
        return;
      }
    }

    // 更新当前选中项
    currentItem.value = item.id;
    uni.$emit(tabChangeEventName.value, item.id);

    uni.switchTab({
      url: item.path,
      success: () => {
        setTimeout(() => {
          updateCurrentTab(true);
        }, 50);
      },
      fail: (err) => {
        console.error('页面跳转失败：', err);
       
      }
    });
  } finally {
    // 确保处理完成后重置标记
    setTimeout(() => {
      isProcessingChange = false;
    }, 500); // 添加一个小延迟，防止快速重复点击
  }
};

onMounted(() => {
  // 隐藏默认 tabbar
  uni.hideTabBar();
  
  // 监听常规 tab 切换事件
  uni.$on(tabChangeEventName.value, (tabId) => {
    currentItem.value = tabId;
  });
  
  // 监听手动更新事件
  uni.$on('updateTabBarManual', handleManualUpdate);
  
  // 监听页面显示事件
  uni.$on('onShow', () => {
    updateCurrentTab();
  });
  
  // 初始化时更新当前选中的 tab
  updateCurrentTab();
});

onBeforeUnmount(() => {
  // 移除所有相关事件监听
  uni.$off(tabChangeEventName.value);
  uni.$off('updateTabBarManual');
  uni.$off('onShow');
});
</script>
<style lang="scss">
.tabbar-container {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 160rpx; // 增加整体高度
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  z-index: 999;
  padding-bottom: env(safe-area-inset-bottom);
}

.tabbar-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: #ffffff;
  box-shadow: 0 -1px 10px rgba(0, 0, 0, 0.05);
  border-top-left-radius: 24rpx;
  border-top-right-radius: 24rpx;
  z-index: -1;
}

.tabbar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  padding-bottom: 20rpx; // 增加底部间距
  transition: all 0.3s ease;
  // 调整缩放比例
  transform: v-bind("props.tabbarType === 'blind' ? 'scale(0.95)' : 'scale(1)'");
  
  &.active {
    transform: v-bind("props.tabbarType === 'blind' ? 'translateY(-4rpx) scale(0.95)' : 'translateY(-4rpx)'");
  }
  
  &.center-item {
    padding-bottom: 20rpx;
    transform: scale(1);
  }
}

.item-icon-container {
  width: v-bind("props.tabbarType === 'blind' ? '56rpx' : '64rpx'"); // 增大图标容器
  height: v-bind("props.tabbarType === 'blind' ? '56rpx' : '64rpx'");
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 10rpx;
  
  &.center-icon-container {
    width: v-bind("props.tabbarType === 'blind' ? '120rpx' : '130rpx'"); // 增大中间图标
    height: v-bind("props.tabbarType === 'blind' ? '120rpx' : '130rpx'");
    background: v-bind("props.tabbarType === 'blind' ? 'linear-gradient(135deg, #1677FF 0%, #0D47A1 100%)' : 'linear-gradient(135deg, #40BFFF 0%, #1677FF 100%)'");
    border-radius: 50%;
    box-shadow: 0 4rpx 25rpx #999;
    border: 2rpx solid #ffffff;
    margin-bottom: 10rpx;
    position: relative;
    z-index: 1000;
    transform: translateY(-20rpx); // 调整凸起高度
  }
}

.item-icon {
  width: v-bind("props.tabbarType === 'blind' ? '44rpx' : '52rpx'"); // 增大图标
  height: v-bind("props.tabbarType === 'blind' ? '44rpx' : '52rpx'");
  transition: all 0.3s ease;
  
  &.center-icon {
    width: v-bind("props.tabbarType === 'blind' ? '60rpx' : '64rpx'");
    height: v-bind("props.tabbarType === 'blind' ? '60rpx' : '64rpx'");
    filter: brightness(0) invert(1);
  }
}

.item-text {
  font-size: v-bind("props.tabbarType === 'blind' ? '24rpx' : '28rpx'"); // 增大文字
  color: #999;
  transition: all 0.3s ease;
  font-weight: normal;
  line-height: 1;
  margin-top: 8rpx;
  
  &.active-text {
    color: v-bind("props.tabbarType === 'blind' ? '#1677FF' : '#40BFFF'");
    font-weight: 500;
    transform: scale(1.05);
  }
  
  &.center-text {
    color: v-bind("props.tabbarType === 'blind' ? '#1677FF' : '#40BFFF'");
    font-weight: 500;
  }
}

.active .item-icon-container:not(.center-icon-container) {
  position: relative;
  
  &::after {
    content: '';
    position: absolute;
    bottom: -12rpx;
    left: 50%;
    transform: translateX(-50%);
    width: v-bind("props.tabbarType === 'blind' ? '10rpx' : '12rpx'");
    height: v-bind("props.tabbarType === 'blind' ? '10rpx' : '12rpx'");
    background-color: v-bind("props.tabbarType === 'blind' ? '#1677FF' : '#40BFFF'");
    border-radius: 50%;
    opacity: 0.8;
  }
}

@supports (padding-bottom: constant(safe-area-inset-bottom)) {
  .tabbar-container {
    padding-bottom: constant(safe-area-inset-bottom);
  }
}

@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .tabbar-container {
    padding-bottom: env(safe-area-inset-bottom);
  }
}
</style>