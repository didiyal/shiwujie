<!-- search.vue -->
<template>
  <view class="container">
    <!-- 有用的搜索栏区域 -->
    <view class="search-bar">
      <view class="search-input-wrap" :class="{ 'input-focused': isFocused }">
        <!-- 搜索图标 -->
        <icon type="search" size="18" color="#666" />
        <!-- 搜索输入框 -->
        <input
          class="search-input"
          placeholder="请输入目的地"
          v-model="clientname"
          @input="inputes"
          @focus="onFocus"
          @blur="onBlur"
          focus
          auto-focus
        />
        <!-- 清除按钮 -->
        <view class="clear-btn" v-if="clientname" @tap.stop="clearInput">
          <text class="clear-icon">×</text>
        </view>
      </view>
      <!-- 取消按钮 -->
      <view class="cancel-btn" @tap="cancelSearch">
        <text>取消</text>
      </view>
    </view>

    <!-- 搜索结果列表 -->
    <scroll-view
      class="search-results"
      scroll-y
      v-if="list.length > 0"
      :class="{ 'slide-in': list.length > 0 }"
    >
      <!-- 搜索结果项 -->
      <view
        class="result-item"
        v-for="(item, index) in list"
        :key="index"
        @tap="selectLocation(item)"
        hover-class="item-hover"
        :style="{ animationDelay: index * 50 + 'ms' }"
      >
        <view class="item-content">
          <view class="location-info">
            <view class="name">{{ item.name }}</view>
            <view class="address">{{ item.district }}{{ item.address }}</view>
          </view>
          <view class="select-icon">
            <text class="arrow">›</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 无结果提示 -->
    <view class="no-result" v-if="showNoResult">
      <text class="no-result-text">未找到相关地点</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      list: [], // 搜索结果列表
      clientname: "", // 搜索关键词
      location: {}, // 选中的位置信息
      searchComplete: false, // 搜索完成标记
      hasSearched: false, // 是否进行过搜索
      isFocused: false, // 输入框焦点状态
    };
  },

  computed: {
    // 控制无结果提示的显示
    showNoResult() {
      return (
        this.hasSearched &&
        this.searchComplete &&
        this.clientname &&
        this.list.length === 0
      );
    },
  },

  onLoad() {
    uni.getLocation({
      type: "wgs84",
      altitude: true,
      geocode: true,
      success: (res) => {
        this.location = res;
      },
    });
  },

  methods: {
    // 清除搜索内容
    clearInput() {
      this.clientname = "";
      this.list = [];
      this.hasSearched = false;
      this.searchComplete = false;
    },

    // 取消搜索
    cancelSearch() {
      this.clearInput();
      uni.navigateBack();
    },

    // 处理输入事件
    inputes(e) {
      const keywords = e.detail.value;
      if (keywords) {
        this.hasSearched = true;
        this.searchComplete = false;
        this.getLocationList(keywords);
      } else {
        this.list = [];
        this.hasSearched = false;
        this.searchComplete = false;
      }
    },

    // 获取位置列表
    getLocationList(keywords) {
      // 首先检查是否有当前位置信息
      if (!this.location.latitude || !this.location.longitude) {
        // 如果没有位置信息，先获取位置
        uni.getLocation({
          type: "wgs84",
          success: (res) => {
            this.location = res;
            // 获取位置后再进行搜索
            this.performSearch(keywords);
          },
          fail: (err) => {
            console.error("获取位置失败:", err);
            // 位置获取失败时，使用默认城市搜索
            this.performSearch(keywords, "广州");
            
          }
        });
      } else {
        // 已有位置信息，直接搜索
        this.performSearch(keywords);
      }
    },

    // 执行搜索请求
    performSearch(keywords, defaultCity) {
      // 如果有经纬度，使用经纬度搜索附近，否则使用城市名
      const searchParams = {
        key: "1c54e3f887846601b2f383983ffc0827",
        keywords: keywords,
        offset: 20,
        page: 1,
        extensions: "all"
      };
      
      if (this.location.latitude && this.location.longitude && !defaultCity) {
        // 使用经纬度作为中心点搜索
        searchParams.location = `${this.location.longitude},${this.location.latitude}`;
      } else {
        // 使用城市名搜索
        searchParams.city = defaultCity || "广州";
      }
      
      uni.request({
        url: "https://restapi.amap.com/v3/place/text",
        method: "GET",
        data: searchParams,
        success: (res) => {
          this.searchComplete = true;
          if (res.data.status === "1" && res.data.pois) {
            // 处理返回的位置数据
            this.list = res.data.pois.map((item) => ({
              name: item.name,
              address: item.address,
              district: item.district,
              latitude: item.location.split(",")[1],
              longitude: item.location.split(",")[0],
            }));
          } else {
            this.list = [];
          }
        },
        fail: (err) => {
          console.error("搜索失败:", err);
          this.searchComplete = true;
          this.list = [];
          uni.showToast({
            title: "搜索失败，请重试",
            icon: "none",
          });
        },
      });
    },

    // 在 search.vue 中
    selectLocation(item) {
      const locationData = {
        latitude: parseFloat(item.latitude),
        longitude: parseFloat(item.longitude),
        name: item.name
      };
      
      // 触发事件通知父组件
      this.$emit('locationSelected', locationData);
      
      // 保存到本地存储
      uni.setStorageSync('selectedLocation', locationData);
      
      // 清空搜索结果但不隐藏搜索框
      this.clientname = item.name;
      this.list = [];
      this.hasSearched = false;
      this.searchComplete = false;
    },

    // 输入框获得焦点
    onFocus() {
      this.isFocused = true;
    },

    // 输入框失去焦点
    onBlur() {
      this.isFocused = false;
    },
  },
};
</script>

<style>
page {
  background-color: #f8f8f8;
  height: 100%;
}

.container {
  height: 100%;
  display: flex;
  flex-direction: column;
  animation: pageEnter 0.3s ease-out;
}

@keyframes pageEnter {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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
.clear-btn {
  padding: 10rpx 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}
.clear-icon {
  font-size: 40rpx;
  color: #999;
  font-weight: bold;
}
.cancel-btn {
  padding: 0 10rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}
.cancel-btn text {
  color: #007aff;
  font-size: 28rpx;
  transition: color 0.2s ease;
}
.cancel-btn:active text {
  color: #005ac1;
}
.search-results {
  flex: 1;
  background: #fff;
  position: relative;
  z-index: 99;
  transition: all 0.3s ease-out;
}
.result-item {
  padding: 0 30rpx;
  background: #fff;
  animation: fadeIn 0.3s ease-out forwards;
  opacity: 0;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(20rpx);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
.item-content {
  display: flex;
  align-items: center;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}
.location-info {
  flex: 1;
  overflow: hidden;
}
.name {
  font-size: 32rpx;
  color: #333;
  margin-bottom: 8rpx;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.address {
  font-size: 26rpx;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.select-icon {
  margin-left: 20rpx;
}
.arrow {
  font-size: 40rpx;
  color: #ccc;
  font-weight: 300;
  transition: transform 0.2s ease;
}
.item-hover {
  background-color: #f8f8f8;
  transition: background-color 0.2s ease;
}
.result-item:active .arrow {
  transform: translateX(4rpx);
}
.no-result {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  animation: fadeIn 0.3s ease-out;
}
.no-result-text {
  font-size: 28rpx;
  color: #999;
}
::-webkit-scrollbar {
  width: 0;
  height: 0;
  color: transparent;
}

/* 搜索框动画 */
.input-focused {
  transform: translateY(2rpx);
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.1);
}

/* 按钮点击效果 */
.button-hover {
  opacity: 0.7;
  transform: scale(0.98);
  transition: all 0.2s ease;
}

/* 搜索结果列表动画 */
.slide-in {
  animation: slideIn 0.3s ease-out;
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

/* 无结果提示动画 */
.fade-in {
  animation: fadeIn 0.3s ease-out;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
