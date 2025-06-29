<template>
  <view>
    <!-- 搜索组件和导航组件都需要始终显示 -->
    <search-component @locationSelected="onDestinationSelected" />
    <navigate-component :destination="destinationLocation" />
  </view>
</template>

<script>
import SearchComponent from "@/components/search.vue";
import NavigateComponent from "@/components/navigate.vue";

export default {
  components: {
    SearchComponent,
    NavigateComponent,
  },
  data() {
    return {
      destinationLocation: null,
    };
  },
  methods: {
    onDestinationSelected(location) {
      console.log("目的地已选择:", location);
      // 确保数据格式正确
      this.destinationLocation = {
        latitude: parseFloat(location.latitude),
        longitude: parseFloat(location.longitude),
        name: location.name,
      };
      // 同时也存储到本地，以便 navigate 组件可以通过 updated 钩子获取
      uni.setStorageSync('selectedLocation', this.destinationLocation);
    },
  },
  // 添加 onShow 生命周期，以便在页面显示时检查本地存储
  onShow() {
    const selectedLocation = uni.getStorageSync('selectedLocation');
    if (selectedLocation) {
      this.destinationLocation = selectedLocation;
    }
  }
};
</script>
<style>
/* Add any necessary styles here */
</style>
