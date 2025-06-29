<template>
  <view class="navigation-container">
    <!-- #ifdef MP-WEIXIN -->
    <map id="map" hidden="true"></map>
    <!-- #endif -->
    

    <!-- 位置信息展示 -->
    <view class="location-panel">
      <view class="location-item">
        <text class="label">当前位置:</text>
        <text class="value">{{ currentLocation.name || "定位中..." }}</text>
      </view>
      <view class="location-item">
        <text class="label">目的地:</text>
        <text class="value">{{
          (destination && destination.name) || "未选择"
        }}</text>
      </view>
    </view>

    <!-- 出行方式选择 -->
    <view class="travel-modes">
      <view
        class="mode-button"
        :class="{
          active: travelMode === 'drive',
          disabled: !destination,
          animate: animatingMode === 'drive',
        }"
        @tap="setTravelMode('drive')"
        hover-class="button-hover"
      >
        <text class="mode-icon">🚗</text>
        <text class="mode-text">驾车</text>
      </view>
      <view
        class="mode-button"
        :class="{
          active: travelMode === 'walk',
          disabled: !destination,
          animate: animatingMode === 'walk',
        }"
        @tap="setTravelMode('walk')"
        hover-class="button-hover"
      >
        <text class="mode-icon">🚶</text>
        <text class="mode-text">步行</text>
      </view>
      <view
        class="mode-button"
        :class="{
          active: travelMode === 'bike',
          disabled: !destination,
          animate: animatingMode === 'bike',
        }"
        @tap="setTravelMode('bike')"
        hover-class="button-hover"
      >
        <text class="mode-icon">🚲</text>
        <text class="mode-text">骑行</text>
      </view>
      <view
        class="mode-button"
        :class="{
          active: travelMode === 'bus',
          disabled: !destination,
          animate: animatingMode === 'bus',
        }"
        @tap="setTravelMode('bus')"
        hover-class="button-hover"
      >
        <text class="mode-icon">🚌</text>
        <text class="mode-text">公交</text>
      </view>
    </view>

    <!-- 导航按钮 -->
    <view
      class="start-nav-button"
      :class="{ disabled: !isTravelModeSelected }"
      @tap="openMap"
      hover-class="nav-button-hover"
    >
      开始导航
    </view>
  </view>
</template>

<script>
import Map from "@/js_sdk/fx-openMap/openMap.js";

export default {
  data() {
    return {
      travelMode: null,
      currentLocation: {
        latitude: null,
        longitude: null,
        name: "",
      },
      destination: {
        latitude: null,
        longitude: null,
        name: "",
      },
      animatingMode: null,
      searchKeyword: '',
      isFocused: false,
    };
  },
  mounted() {
    this.getCurrentLocation();
  },
  computed: {
    isTravelModeSelected() {
      return this.travelMode !== null;
    },
  },
  methods: {
    getCurrentLocation() {
      uni.getLocation({
        type: "wgs84",
        success: (res) => {
          uni.request({
            url: "https://restapi.amap.com/v3/geocode/regeo",
            method: "GET",
            data: {
             key: "1c54e3f887846601b2f383983ffc0827",
			
			
              location: `${res.longitude},${res.latitude}`,
            },
            success: (response) => {
              if (response.data.status === "1" && response.data.regeocode) {
                this.currentLocation = {
                  latitude: res.latitude,
                  longitude: res.longitude,
                  name: response.data.regeocode.formatted_address,
                };
              }
            },
          });
        },
        fail: (err) => {
          console.error("获取位置失败：", err);
          uni.showToast({
            title: "获取当前位置失败",
            icon: "none",
          });
        },
      });
    },
    setTravelMode(mode) {
      if (!this.destination) {
       
        return;
      }
      this.animatingMode = mode;
      this.travelMode = mode;
      setTimeout(() => {
        this.animatingMode = null;
      }, 300);
    },
    openMap() {
      if (!this.isTravelModeSelected) {
        return;
      }
      var options = {
        origin: {
          latitude: this.currentLocation.latitude,
          longitude: this.currentLocation.longitude,
          name: this.currentLocation.name,
        },
        destination: {
          latitude: this.destination.latitude,
          longitude: this.destination.longitude,
          name: this.destination.name,
        },
        // #ifdef MP-WEIXIN
        mapId: "map",
        // #endif
        mode: this.travelMode,
      };
      Map.routePlan(options, "wgs84");
    },
    navigateToSearch() {
      uni.navigateTo({
        url: '../search/search',
        success: () => {
          console.log('成功跳转到搜索页面');
        },
        fail: (err) => {
          console.error('跳转失败：', err);
         
        }
      });
    },
  },
  
  
  updated() {
      console.log('导航组件更新 updated');
      // 获取存储的位置信息
      const selectedLocation = uni.getStorageSync('selectedLocation');
      if (selectedLocation) {
          console.log('检测到存储的位置信息:', selectedLocation);
          this.destination = selectedLocation;
          
          // 获取存储的出行方式
          const selectedTravelMode = uni.getStorageSync('selectedTravelMode');
          if (selectedTravelMode) {
              console.log('检测到存储的出行方式:', selectedTravelMode);
              this.setTravelMode(selectedTravelMode);
              
              // 是否自动启动导航
              const autoStartNavigation = uni.getStorageSync('autoStartNavigation');
              if (autoStartNavigation) {
                  console.log('自动启动导航');
                  setTimeout(() => {
                      this.openMap();
                      // 导航启动后清除自动启动标记
                      uni.removeStorageSync('autoStartNavigation');
                  }, 500);
              }
          }
          
          // 清除存储，避免重复加载
          // 注意：这里不清除selectedLocation，因为可能需要在导航页面内继续使用
          // uni.removeStorageSync('selectedLocation');
      }
  }
  
};
</script>

<style lang="less">
.navigation-container {
  padding: 30rpx;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.search-bar {
  padding: 20rpx 30rpx;
  background: #ffffff;
  position: relative;
  z-index: 100;
  display: flex;
  align-items: center;
}
.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background-color: #f5f5f5;
  border-radius: 36rpx;
  padding: 15rpx 30rpx;
  position: relative;
  margin-right: 20rpx;
  transition: all 0.3s ease;
}
.search-input {
  flex: 1;
  margin-left: 20rpx;
  font-size: 28rpx;
  height: 60rpx;
  line-height: 60rpx;
}

.location-panel {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.1);
  animation: slideIn 0.3s ease-out;
}

.location-item {
  padding: 20rpx 0;

  &:first-child {
    border-bottom: 1rpx solid #eee;
  }

  .label {
    font-size: 32rpx;
    color: #666;
    margin-right: 20rpx;
  }

  .value {
    font-size: 32rpx;
    color: #333;
    font-weight: bold;
  }
}

.travel-modes {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  margin-bottom: 40rpx;
  animation: slideIn 0.3s ease-out 0.1s backwards;
}

.mode-button {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  text-align: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  &.active {
    background: #007aff;
    color: #fff;
    transform: scale(1.02);

    .mode-icon {
      animation: iconBounce 0.5s ease;
    }
  }

  &.animate {
    animation: selectPulse 0.3s ease-out;
  }

  &.disabled {
    opacity: 0.5;
    pointer-events: none;
  }

  &::after {
    content: "";
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: width 0.3s ease-out, height 0.3s ease-out;
  }

  &:active::after {
    width: 300rpx;
    height: 300rpx;
  }
}

.mode-icon {
  font-size: 48rpx;
  margin-bottom: 10rpx;
  transition: transform 0.3s ease;
}

.mode-text {
  font-size: 28rpx;
  position: relative;
  z-index: 1;
}

.button-hover {
  transform: scale(0.98);
  opacity: 0.9;
}

.start-nav-button {
  background: #007aff;
  color: #fff;
  font-size: 36rpx;
  padding: 30rpx;
  text-align: center;
  border-radius: 16rpx;
  margin-top: 40rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 122, 255, 0.3);
  transition: all 0.3s ease;
  animation: slideIn 0.3s ease-out 0.2s backwards;

  &.disabled {
    background: #ccc;
    pointer-events: none;
  }
}

.nav-button-hover {
  transform: scale(0.98);
  opacity: 0.9;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes selectPulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes iconBounce {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

/* 涟漪动画 */
@keyframes ripple {
  to {
    transform: translate(-50%, -50%) scale(4);
    opacity: 0;
  }
}
</style>
