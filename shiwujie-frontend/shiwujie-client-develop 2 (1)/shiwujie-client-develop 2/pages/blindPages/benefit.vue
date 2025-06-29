
<template>
    <view class="classroom-container">
        <!-- 顶部导航 -->
        <view class="header">
            <text class="tab" :class="{ active: activeTab === 'online' }" @click="activeTab = 'online'">政策推送</text>
            <text class="tab" :class="{ active: activeTab === 'offline' }" @click="activeTab = 'offline'">在线课堂</text>
        </view>

        <!-- 政策推送内容 -->
        <view v-if="activeTab === 'online'">
            <!-- 学习交流圈 -->
            <view class="circle-section">
                <view class="circle-list">
                    <view class="circle-item" v-for="(item, index) in circles" :key="index">
                        <image :src="item.image" class="circle-image" @tap="handleCircleClick(index)"></image>
                    </view>
                </view>
            </view>

            <!-- 新闻列表 -->
            <view class="news-section">
                <view class="news-item" v-for="(news, index) in newsList" :key="index" @tap="handleNewsClick(index)">
                    <view class="news-content">
                        <view class="news-info">
                            <text class="news-title">{{ news.title }}</text>
                            <text class="news-desc">{{ news.desc }}</text>
                            <view class="news-meta">
                                <text class="date">{{ news.date }}</text>
                                <text class="views">{{ news.views }}</text>
                            </view>
                        </view>
                        <image :src="news.image" class="news-image"></image>
                    </view>
                </view>
            </view>
        </view>

        <!-- 在线课堂内容 -->
        <view v-if="activeTab === 'offline'" class="offline-container">
            <!-- 搜索框 -->
            <view class="search-box">
                <input type="text" placeholder="盲人按摩" class="search-input"/>
                <text class="scan-icon">⚲</text>
            </view>

            <!-- 分类导航 -->
            <view class="category-nav">
                <view class="category-item">
                    课程分类
                    <text class="arrow-down">▼</text>
                </view>
                <view class="category-item">
                    人气优先
                    <text class="arrow-down">▼</text>
                </view>
                <view class="category-item">
                    多章节
                    <text class="arrow-down">▼</text>
                </view>
            </view>

            <!-- 课程列表 -->
            <view class="course-list">
                <view 
                    class="course-item" 
                    v-for="(course, index) in courseList" 
                    :key="index"
                    @tap="handleCourseClick(course)"
                >
                    <view class="course-tag" v-if="course.tag">{{ course.tag }}</view>
                    <image :src="course.image" mode="aspectFill" class="course-image"></image>
                    <view class="course-info">
                        <view class="course-title">{{ course.title }}</view>
                        <view class="course-author">{{ course.author }}</view>
                        <view class="course-bottom">
                            <text class="course-price" v-if="course.price">¥{{ course.price }}</text>
                            <text class="course-free" v-else>免费</text>
                            <text class="course-students">{{ course.students }}人已学</text>
                        </view>
                    </view>
                </view>
            </view>
        </view>
		
		    <!-- 仅在当前页面显示 tabBar -->
		    <custom-tabbar tabbarType="blind" :currentPage="0"></custom-tabbar>
		
    </view>
</template>

<script setup>
import { ref } from 'vue';
import CustomTabbar from "@/components/customTabbar.vue";

const activeTab = ref('online'); // 默认显示政策推送

// 政策推送数据
const circles = ref([
    { 
        image: '/static/nav/mall-nav-1.png',
        path: '/pages/circle/detail1'
    },
    { 
        image: '/static/nav/mall-nav-2.png',
        path: '/pages/circle/detail2'
    },
    { 
        image: '/static/nav/mall-nav-3.png',
        path: '/pages/circle/detail3'
    },
    { 
        image: '/static/nav/mall-nav-4.png',
        path: '/pages/circle/detail4'
    }
]);

const newsList = ref([
    { 
        image: '/static/iconOrig/new1.jpeg', 
        title: '"千山千灯闹元宵"', 
        desc: '公益助残送温暖',
        date: '2020-12-20',
        views: '48194',
        path: '/pages/news/news1'
    },
    { 
        image: '/static/2.png', 
        title: '校企联合共商"智慧盲杖与电子盲道"开发', 
        desc: '为盲人出行保驾护航',
        date: '2020-12-20',
        views: '48194',
        path: '/pages/news/new2'
    },
    { 
        image: '/static/3.png', 
        title: '科技助残产品亮相服贸会', 
        desc: '盲人出行有了穿戴式导行设备',
        date: '2020-12-20',
        views: '48194',
        path: '/pages/news/new3'
    },
    { 
        image: '/static/iconOrig/new4.jpeg', 
        title: '一款手机软件连接起盲人和明眼人的世界', 
        desc: '"你是我的眼"',
        date: '2020-12-20',
        views: '48194',
        path: '/pages/news/new4'
    }
]);
// 在线课堂数据
const courseList = ref([
    {
        image: '/static/images/user/video1.jpg',
        title: '小星星摸读盲文《秘密花园》',
        author: '启明星小星星| 共20节课程',
        tag: '多章节',
        url: 'https://www.bilibili.com/video/BV17P4y1x7ac/?spm_id_from=333.1387.favlist.content.click'
    },
    {
        image: '/static/images/user/video2.jpg',
        title: '头部按摩技巧',
        author: '摄影师陈哥哥 | 共14节课程',
        url: 'https://www.bilibili.com/video/BV1RZ421E76m/?spm_id_from=333.337.search-card.all.click'
    },
    {
        image: '/static/images/user/video3.jpg',
        title: '古法艾灸',
        author: '贺贺老师 | 共2节课程',
        price: '19.9',
        students: '452',
        tag: '多章节',
        url: 'https://www.bilibili.com/video/BV1Xb421H7mh?vd_source=965f882ab0b3c783ecaa61b24eec0dbe'
    },
    {
        image: '/static/images/user/video4.jpg',
        title: '冥想',
        author: '麦子老师 | 共3节课程',
        price: '9.9',
        students: '290',
        tag: '多章节',
        url: 'https://www.bilibili.com/video/BV1Xb421H7mh?vd_source=965f882ab0b3c783ecaa61b24eec0dbe'
    }
]);
// 政策推送方法
const handleCircleClick = (index) => {
    const circle = circles.value[index];
    uni.navigateTo({
        url: circle.path,
        fail: () => {
            
        }
    });
};

const handleNewsClick = (index) => {
    const news = newsList.value[index];
    uni.navigateTo({
        url: news.path,
        fail: () => {
           
        }
    });
};

// 课程点击处理函数
const handleCourseClick = (course) => {
    // H5环境
    // #ifdef H5
    window.open(course.url, '_blank');
    // #endif
    
    // App环境
    // #ifdef APP-PLUS
    plus.runtime.openURL(course.url, (err) => {
        if (err) {
            handleFallback(course.url);
        }
    });
    // #endif
    
    // 小程序环境
    // #ifdef MP
    uni.navigateTo({
        url: `/pages/webview/webview?url=${encodeURIComponent(course.url)}`,
        fail: () => handleFallback(course.url)
    });
    // #endif
};

// 降级处理：复制链接到剪贴板
const handleFallback = (url) => {
    uni.setClipboardData({
        data: url,
        success: () => {
            uni.showToast({
                title: '链接已复制，请在浏览器中打开',
                icon: 'none',
                duration: 2000
            });
        }
    });
};

</script>

<style scoped>
.classroom-container {
    background-color: #f8f8f8;
    min-height: 100vh;
	padding-top: 80rpx; /* 增加顶部间距，使整体下移 */
}

.header {
    display: flex;
    padding: 20rpx 30rpx;
    background-color: #fff;
    border-bottom: 1rpx solid #eee;
}

.tab {
    margin-right: 40rpx;
    font-size: 32rpx;
    color: #666;
}

.tab.active {
    color: #40BFFF;
    font-weight: bold;
    position: relative;
}

.tab.active::after {
    content: '';
    position: absolute;
    bottom: -10rpx;
    left: 0;
    width: 100%;
    height: 4rpx;
    background-color: #40BFFF;
}
/* 政策推送样式 */
.circle-section {
    padding: 20rpx 0;
    background-color: #fff;
    margin-bottom: 20rpx;
}

.circle-list {
    display: flex;
    justify-content: space-around;
    padding: 0 30rpx;
}

.circle-item {
    text-align: center;
}

.circle-image {
    width: 100rpx;
    height: 100rpx;
    border-radius: 50%;
}

.news-section {
    background-color: #fff;
}

.news-item {
    padding: 30rpx;
    border-bottom: 1rpx solid #eee;
}

.news-content {
    display: flex;
    align-items: flex-start;
}

.news-info {
    flex: 1;
    margin-right: 20rpx;
}

.news-title {
    font-size: 28rpx;
    color: #333;
    margin-bottom: 16rpx;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    line-height: 1.4;
}

.news-desc {
    font-size: 24rpx;
    color: #999;
    margin-bottom: 20rpx;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 1;
    overflow: hidden;
}

.news-meta {
    font-size: 24rpx;
    color: #999;
    display: flex;
    align-items: center;
}

.news-image {
    width: 240rpx;
    height: 160rpx;
    border-radius: 8rpx;
    flex-shrink: 0;
    object-fit: cover;
}

.date {
    margin-right: 20rpx;
}

.views::before {
    content: '👁 ';
    margin-right: 4rpx;
}

/* 在线课堂样式 */
.offline-container {
    background: #f8f8f8;
    min-height: 100vh;
}

.search-box {
    margin: 20rpx;
    background: #f1f1f1;
    border-radius: 40rpx;
    padding: 10rpx 30rpx;
    display: flex;
    align-items: center;
}

.search-input {
    flex: 1;
    height: 60rpx;
    font-size: 28rpx;
}

.scan-icon {
    font-size: 40rpx;
    color: #999;
}

.category-nav {
    display: flex;
    padding: 20rpx;
    border-bottom: 1rpx solid #eee;
    background: #fff;
}

.category-item {
    flex: 1;
    text-align: center;
    font-size: 28rpx;
    color: #333;
    display: flex;
    align-items: center;
    justify-content: center;
}

.arrow-down {
    font-size: 24rpx;
    margin-left: 6rpx;
    color: #999;
}

.course-list {
    padding: 20rpx;
}

.course-item {
    background: #fff;
    border-radius: 12rpx;
    margin-bottom: 20rpx;
    overflow: hidden;
    position: relative;
}

.course-tag {
    position: absolute;
    left: 0;
    top: 20rpx;
    background: rgba(0,0,0,0.5);
    color: #fff;
    font-size: 24rpx;
    padding: 4rpx 16rpx;
    border-radius: 0 20rpx 20rpx 0;
}

.course-image {
    width: 100%;
    height: 360rpx;
    display: block;
}

.course-info {
    padding: 20rpx;
}

.course-title {
    font-size: 32rpx;
    color: #333;
    margin-bottom: 10rpx;
}

.course-author {
    font-size: 26rpx;
    color: #666;
    margin-bottom: 16rpx;
}

.course-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.course-price {
    color: #ff4d4f;
    font-size: 32rpx;
    font-weight: bold;
}

.course-free {
    color: #52c41a;
    font-size: 32rpx;
}

.course-students {
    font-size: 24rpx;
    color: #999;
}
</style>