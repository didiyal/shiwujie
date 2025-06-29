<template>
	<view class="app-container">
		<slot></slot>
		<custom-tabbar></custom-tabbar> <!-- 添加自定义TabBar -->
	</view>
</template>

<script>
	import CustomTabbar from "@/components/customTabbar.vue"; // 引入自定义TabBar
	import Utils from "./utils/until";
	import {
		leaveBlindChannelAsyc
	} from "./api/video";
	const floatWin = uni.requireNativePlugin('Ba-FloatWinWeb');
	const rtcModule = uni.requireNativePlugin('AR-RtcModule');
	export default {
		globalData: {
			socketTask: null,
			isSokcetActive: false,
			blindUid: null,
		},
		onLaunch: function() {
			console.log('App Launch');
			let user = uni.getStorageSync("userInfo");
			//悬浮窗
			var globalEvent = uni.requireNativePlugin('globalEvent');
			console.log("开始注册悬浮窗事件");
			console.log(globalEvent);
			globalEvent.addEventListener('baFloatWinWeb', function(e) {
				console.log("收到点击");
				console.log('baFloatWinWeb：' + JSON.stringify(e));
				if (e.tag == "notify") {
					floatWin.hide({
							tag: "notify"
						},
						(res) => {
							console.log(res);
						});
				}
				switch (e.json) {
					case "openCall":
						console.log("悬浮窗---开始通话" + e);
						uni.navigateTo({
							url: '/pages/videoTest/videoTest',
						});
						break;
					case "reverse":
						console.log("悬浮窗---翻转摄像头" + e);
						rtcModule.switchCamera((res) => {
							//something
							console.log("切换前后置摄像头")
						})
						break;
					case "toIndex":
						console.log("悬浮窗---其它页面操作" + e);
						//关闭跳转其他页面的悬浮窗
						floatWin.hide({
								tag: "menu"
							},
							(res) => {
								console.log(res);
								
							});
						//跳转到主页
						uni.switchTab({
							url: '/pages/blindPages/blindHome',
						});
						//打开视频通话悬浮窗
						console.log("打开悬浮窗")
						//显示通话过程的悬浮窗
						let params = {
							isToast: true,
							tag: "menu"
						}
						params.webUrl = "file:///android_asset/testFloatWin2.html";
						params.width = 64;
						params.height = 128;
						params.xRatio = 0.8;
						params.yRatio = 0.7;
						floatWin.show(params,
							(res) => {
								console.log(res);
								
							});
						break;
					case "hangUp":
						console.log("悬浮窗---挂断通话" + e);
						//离开频道
						// 防止重复调用（如多次点击挂断）
						leaveBlindChannelAsyc();
						//离开频道
						rtcModule.leaveChannel((res) => {
							rtcModule.destroyRtc((res) => {
								//something
								console.log('AnyRTC 实例已销毁');
								uni.reLaunch({
									url: '/pages/blindPages/blindHome'
								});
							});
						});
						//关闭视频通话悬浮窗
						floatWin.hide({
								tag: "menu"
							},
							(res) => {
								console.log(res);
								
							});
						break;
					case "AI":
						console.log("悬浮窗---AI" + e);
						uni.switchTab({
							url: '/pages/AI-assistant/AI-help',
						})
						break;
					case "CALL_HELP":
						console.log("悬浮窗---盲人向家属求助" + e);
						uni.navigateTo({
							url: '/pages/videoTest/HelpUser',
						})
						break;
				}
			});
			// 锁定竖屏
			plus.screen.lockOrientation('portrait-primary');
			try {
				Utils.equipment();
			} catch (error) {
				//TODO handle the exception
				console.log(error);
			}

		},




		onShow: function() {
			console.log('App Show');
		},
		onHide: function() {
			console.log('App Hide');
		},
		components: {
			CustomTabbar // 注册组件
		},
		methods: {
			// 创建 WebSocket 连接（需在注册成功后调用）
			connectWebSocket(userId) {
				if (this.globalData.socketTask) {
					console.log('WebSocket 已存在，无需重复连接');
					return;
				}

				const socketTask = uni.connectSocket({
					url: `ws://43.139.38.62:8082/shiwujie/websocket/${userId}`,
					success: () => {
						this.globalData.socketTask = socketTask;
						this.setupSocketListeners();
					},
					fail: (err) => {
						console.error('连接失败:', err);
					}
				});
			},

			// 设置 WebSocket 监听器
			setupSocketListeners() {
				const socketTask = this.globalData.socketTask;

				socketTask.onOpen(() => {
					console.log('WebSocket 连接成功');
					this.globalData.isSocketActive = true;
				});

				socketTask.onMessage((res) => {
						const data = res.data;
						console.log("监听到信息:" + res)
						console.log(res)
						const msg = JSON.parse(res.data);
						console.log(msg);
						const blindUid = msg.blindUid;
						if (msg.title == 'CALL_HELP') {
							//打开悬浮窗
							console.log("打开视频求助悬浮窗")
							//显示
							let params = {
								isToast: true,
								tag: "notify"
							}
							params.webUrl =
								"file:///android_asset/testFloatWin4.html?title=视无界&content=您收到家属的求助&time=现在"
							params.height = 60;
							params.moveType = 1;
							floatWin.show(params,
								(res) => {
									console.log(res);
									
								});
							this.globalData.blindUid = blindUid;
						}});
						
				

				socketTask.onClose(() => {
					console.log('连接关闭');
					this.globalData.socketTask = null;
					this.globalData.isSocketActive = false;
				});

				socketTask.onError((err) => {
					console.error('连接错误:', err);
					socketTask.close();
					this.globalData.socketTask = null;
				});
			},

			// 关闭 WebSocket 连接（如退出登录时调用）
			disconnectWebSocket() {
				if (this.globalData.socketTask) {
					this.globalData.socketTask.close();
					this.globalData.socketTask = null;
				}
			}
		}
	}
</script>

<style>
	@import './static/iconfont.css';
	@import '@/commonStyle/style/commonStyle.scss';

	/* 每个页面公共css */
	.app-container {
		position: relative;
		min-height: 100vh;
		padding-bottom: 120rpx;
		/* 防止内容被TabBar遮挡 */
	}

	/* 其他全局样式 */
	page {
		background-color: #f5f5f5;
	}
</style>