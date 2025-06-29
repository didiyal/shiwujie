<template>
	<view class="container">
		<!-- 顶部图标 -->
		<view class="top-icon">
			<image src="/static/iconOrig/community.png" mode="aspectFit" class="icon"></image>
		</view>

		<view class="box">
			<!-- 蓝色卡片 -->
			<view class="stats-card">
				<view class="stats-item">
					<text class="number">526,711</text>
					<text class="label">盲人</text>
				</view>
				<view class="divider"></view>
				<view class="stats-item">
					<text class="number">6,880,957</text>
					<text class="label">志愿者</text>
				</view>
			</view>

			<!-- 注册信息 -->
			<view class="register-info">
				<text class="register-text">注册于2024年3月30号</text>
				<text class="language">中文</text>
			</view>

			<!-- 接听求助电话按钮 -->
			<button type="primary" class="help-call-btn" @click="toVideo()">接听求助电话</button>
			
			<!-- 提示文本 -->
			<text class="tip-text">当有人需要您的帮助时，您将收到通知。</text>
		</view>

	</view>
	<custom-tabbar tabbarType="volunteer" :currentPage="0"></custom-tabbar>
</template>

<script setup>
	import {
		onMounted
	} from "vue";
	import CustomTabbar from "@/components/customTabbar.vue";

	function toVideo() {
		uni.switchTab({
			url: '/pages/videoTest/videoTest'
		})
	}

	import {
		websocketInit
	} from "../../utils/socket";


	onMounted(() => {
		// 3. 调用全局方法创建 WebSocket 连接
		const app = getApp();
		let user = uni.getStorageSync("userInfo");
		app.connectWebSocket(user.id);
	})
</script>


<style lang="scss">
.container {
	min-height: 100vh;
	background: #f5f5f5;
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 40rpx 20rpx;
}

.box {
	background-color: white;
	border-radius: 50px;
	padding: 20px;
	width: 100%;
	box-sizing: border-box;
	margin: 0 auto;
}

.top-icon {
	margin: 0rpx 0;
	background: #1677FF;

	.icon {
		width: 1000rpx;
		height: 300rpx;
	}
}

.stats-card {
	width: 90%;
	background: #fff;
	border-radius: 20rpx;
	padding: 40rpx;
	display: flex;
	justify-content: space-around;
	align-items: center;
	color: #40BFFF;

	.stats-item {
		display: flex;
		flex-direction: column;
		align-items: center;

		.number {
			font-size: 40rpx;
			font-weight: bold;
			margin-bottom: 8rpx;
		}

		.label {
			font-size: 28rpx;
		}
	}

	.divider {
		width: 2rpx;
		height: 80rpx;
		background: rgba(255, 255, 255, 0.3);
	}
}

.register-info {
	margin: 40rpx 0;
	background: #fff;
	padding: 20rpx 40rpx;
	border-radius: 12rpx;
	width: 90%;
	display: flex;
	justify-content: space-between;
	align-items: center;

	.register-text {
		color: #666;
		font-size: 28rpx;
	}

	.language {
		color: #999;
		font-size: 24rpx;
	}
}

// 修改后的接听求助电话按钮样式
.help-call-btn {
    width: 90% !important; // 稍微收窄一点
    height: 230rpx !important; // 调整高度
    background: #1677FF !important;
    color: #FFFFFF !important;
    border-radius: 20rpx !important; // 增加圆角
    font-size: 36rpx !important; // 稍微增大字号
    font-weight: 500 !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    box-shadow: 0 8rpx 16rpx rgba(22, 119, 255, 0.2) !important; // 添加阴影
    transition: all 0.3s ease !important; // 添加过渡效果
    
    &:active {
        transform: scale(0.98);
        box-shadow: 0 4rpx 8rpx rgba(22, 119, 255, 0.1) !important;
    }
}

.tip-text {
	color: #999;
	font-size: 28rpx;
	margin: 40rpx 0;
	margin-top: 40rpx; // 增加顶部间距
	text-align: center;
}

.tab-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	height: 100rpx;
	background: #fff;
	display: flex;
	justify-content: space-around;
	align-items: center;
	border-top: 1rpx solid #eee;

	.tab-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		font-size: 24rpx; 
		color: #999;

		&.active {
			color: #40BFFF;
		}

		.tab-icon {
			width: 48rpx;
			height: 48rpx;
			margin-bottom: 4rpx;
		}
	}
}
</style>