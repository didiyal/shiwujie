<template>
	<view class="page-container" role="main" :accessible="true" :aria-label="'主页，当前为主页面'"
		accessibilityLabel="主页，当前为主页面">
		<view class="ai-writing-container">
			<!-- 顶部状态栏 -->
			<view class="status-bar" aria-hidden="true">
			</view>

			<!-- 标题栏 -->
			<view class="header" role="banner" aria-hidden="true">
				<text class="header-title">主页</text>
			</view>

			<!-- 功能卡片区域 -->
			<view class="feature-row" role="main" :accessible="true" :aria-label="'功能区域，包含连线志愿者和紧急求助两个按钮'">
				<!-- 视频通话卡片 -->
				<button class="feature-card purple" @click="toBlindVideoPage" role="button" :accessible="true"
					:aria-label="'连线志愿者，点击开始视频通话'" tabindex="0">
					<view class="card-content">
						<text class="card-title">连线志愿者</text>
					</view>
				</button>

				<!-- 紧急求助按钮 -->
				<button class="feature-card emergency" @click="handleEmergency" role="button" :accessible="true"
					:aria-label="'紧急求助，点击获取紧急帮助'" tabindex="0">
					<view class="card-content">
						<text class="card-title">紧急求助</text>
					</view>
				</button>
			</view>
		</view>
	</view>

	<!-- TabBar -->
	<custom-tabbar :tabbarType="tabbarType" :currentPage="0" :accessible="true" role="navigation"
		:aria-label="'底部导航栏'"></custom-tabbar>
</template>

<script>
	const floatWin = uni.requireNativePlugin('Ba-FloatWinWeb')
	import CustomTabbar from "@/components/customTabbar.vue";
	import {
		checkUserCertification
	} from "../../api/user";
	import { websocketInit } from "../../utils/socket";
	export default {
		components: {
			CustomTabbar
		},

		data() {
			return {
				url1: "file:///android_asset/testFloatWin.html",
				url2: "file:///android_asset/testFloatWin3.html",
				url3: "file:///android_asset/testFloatWin4.html",
				hasShownFloatWin: false,
				isBlindUser: true ,// 添加盲人用户状态,
				user: uni.getStorageSync("userInfo")
			}
		},

		async onShow() {
			try {
				const isCertified = await checkUserCertification();
				if (!isCertified) {
					this.showAccessibilityToast('请先完成实名认证后再使用此功能');
					return;
				}

				if (!this.hasShownFloatWin) {
					this.showAIFw();
					// 3. 调用全局方法创建 WebSocket 连接
					const app = getApp();
					let user = uni.getStorageSync("userInfo");
					app.connectWebSocket(user.id);
					this.hasShownFloatWin = true;
				}
			} catch (error) {
				console.error('认证检查失败：', error);
				
			}
		},

		onReady() {
			setTimeout(() => {
				
			}, 500);

			this.$nextTick(() => {
				const firstButton = this.$el.querySelector('.feature-card');
				if (firstButton) {
					firstButton.focus();
				}
			});
		},

		methods: {
			/* showAccessibilityToast(message) { */
			showAIFw() {
				let params = {
					isToast: true,
					tag: "button",
					webUrl: this.url2,
					width: 40,
					height: 40,
					xRatio: 0.8,
					yRatio: 0.2
				};
				floatWin.show(params, (res) => {
					console.log(res);
					
				});
			},

			showVideoFw() {
				let params = {
					isToast: true,
					tag: "menu",
					webUrl: this.url1,
					width: 64,
					height: 64,
					xRatio: 0.8,
					yRatio: 0.7
				};
				floatWin.show(params, (res) => {
					console.log(res);
					
				});
			},

			toBlindVideoPage() {
				
				// this.showVideoFw();
				uni.switchTab({
					url: '/pages/videoTest/videoTest'
				});
			},

			handleEmergency() {
				if(this.user.familyAccount > 0){
					uni.showModal({
						title: '提示',
						content:'正在进入紧急求助页面,您将向家人紧急求助，点击确定开始求助',
						showCancel: false,
						success: () => {
							// this.showAccessibilityToast('已进入紧急求助页面');
							uni.navigateTo({
								url: '/pages/videoTest/BlindHelp',
						
							});
						},
						fail: (err) => {
						}
					});
				}else{
					uni.showModal({
						title: '提示',
						content:'您未加入家庭，无法向家人紧急求助，请创建或者加入家庭',
						showCancel: false,
						success: () => {
							uni.switchTab({
								url:'/pages/family/family',
							})
						},
						fail: (err) => {
						}
					});
				}

			},

			handleKeyboardNavigation(event) {
				if (event.key === 'Tab') {
					event.preventDefault();
					const buttons = this.$el.querySelectorAll('.feature-card');
					const currentFocusIndex = Array.from(buttons).findIndex(button =>
						document.activeElement === button
					);

					let nextIndex;
					if (event.shiftKey) {
						nextIndex = currentFocusIndex <= 0 ? buttons.length - 1 : currentFocusIndex - 1;
					} else {
						nextIndex = currentFocusIndex >= buttons.length - 1 ? 0 : currentFocusIndex + 1;
					}

					buttons[nextIndex]?.focus();
				}
			
		},
		},

		mounted() {
			document.addEventListener('keydown', this.handleKeyboardNavigation);
		},

		beforeDestroy() {
			document.removeEventListener('keydown', this.handleKeyboardNavigation);
		}
	}
</script>


<style lang="scss">
	page {
		background-color: #ffffff;
		font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
	}

	.ai-writing-container {
		position: relative;
		background: #ffffff;
		padding-top: 120rpx;
		min-height: 100vh;
		display: flex;
		flex-direction: column;
	}

	/* Header样式 */
	.header {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		height: 180rpx;
		background-color: #0D47A1;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: var(--status-bar-height) 30rpx 0;
		z-index: 100;

		.header-title {
			font-size: 40rpx;
			font-weight: 600;
			color: #ffffff;
			margin-top: 20rpx;
		}
	}

	/* 状态栏 */
	.status-bar {
		height: var(--status-bar-height);
		width: 100%;
		background-color: #0D47A1;
	}

	/* 功能卡片区域 */
	.feature-row {
		position: fixed;
		bottom: 250rpx;
		left: 50%;
		transform: translateX(-50%);
		width: 90%;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
		gap: 20rpx; // 设置按钮间距
	}

	/* 功能卡片样式 */
	.feature-card {
		width: 100%;
		border-radius: 50rpx;
		padding: 20rpx 0;
		height: 300rpx; // 设置基础高度为300rpx
		display: flex;
		justify-content: center;
		align-items: center;
		position: relative;
		overflow: hidden;
		box-shadow: 0 16rpx 20rpx rgba(69, 133, 245, 0.2);
		background: #163AAC;
		border: none;
		transition: all 0.3s ease;

		// 连线志愿者按钮样式 - 高度为基础高度的2倍
		&.purple {
			height: 600rpx; // 2倍高度
		}

		// 紧急求助按钮样式 - 保持基础高度
		&.emergency {
			height: 300rpx; // 1倍高度
			background: #D32F2F;
			box-shadow: 0 16rpx 20rpx rgba(211, 47, 47, 0.2);

			&:hover {
				box-shadow: 0 20rpx 25rpx rgba(211, 47, 47, 0.3);
			}

			&:active {
				background: #B71C1C;
			}
		}

		&:focus {
			outline: 8rpx solid #ffffff;
			outline-offset: 4rpx;
			box-shadow: 0 0 0 4rpx #1677FF;
		}

		&:active {
			opacity: 0.9;
			transform: scale(0.98);
		}

		.card-content {
			text-align: center;
			pointer-events: none;

			.card-title {
				font-size: 82rpx;
				font-weight: bold;
				color: white;
				margin: 0;
			}
		}
	}

	// 添加无障碍高对比度焦点样式
	@media (forced-colors: active) {
		.feature-card:focus {
			outline: 3px solid ButtonText;
		}
	}

	// 适配 iOS 安全区
	@supports (padding-bottom: constant(safe-area-inset-bottom)) {
		.ai-writing-container {
			padding-bottom: constant(safe-area-inset-bottom);
		}
	}

	@supports (padding-bottom: env(safe-area-inset-bottom)) {
		.ai-writing-container {
			padding-bottom: env(safe-area-inset-bottom);
		}
	}

	// 添加触摸反馈
	@media (hover: hover) {
		.feature-card:hover {
			transform: translateY(-2rpx);
			box-shadow: 0 20rpx 25rpx rgba(69, 133, 245, 0.3);
		}
	}

	// 无障碍焦点样式
	:focus-visible {
		outline: 4rpx solid #1677FF !important;
		outline-offset: 4rpx !important;
	}

	// 确保按钮在无障碍模式下可见
	button {
		&[aria-disabled="true"] {
			opacity: 0.5;
			cursor: not-allowed;
		}

		&:not([aria-disabled="true"]):active {
			opacity: 0.9;
		}
	}
</style>