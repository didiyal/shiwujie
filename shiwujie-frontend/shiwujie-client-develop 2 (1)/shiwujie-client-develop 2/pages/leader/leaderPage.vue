、<template>
	<view class="content">
		<image class="logo" src="/static/logo.jpg"></image>
		<view class="text-area">
			<text class="title">你好！欢迎使用视无界</text>
		</view>
	</view>
</template>

<script setup>
	import {testJwt} from '../../api/loginAndRegister';
	import {toPagesByUserRole} from '../../utils/loginUtils';
	import { onMounted } from 'vue';
	import Storage from '../../utils/storage';
	/**
	 * 校验令牌
	 * 1. 存在： 根据状态跳转页面
	 * 2. 不存在： 跳转到一键登录注册页面
	 */
	async function getJwtTokenFromBackend(){
		try {
			console.log("开始校验令牌");
			const res = await testJwt();
			if(res.code == 1){
				//令牌校验成功
				console.log("令牌校验成功,跳转到主页")
				console.log(res);
				
				
				//更新数据
				Storage.setUserInfo(res.data);
				Storage.setUserRole(res.data.status);
				uni.setStorageSync("userRole",res.data.status);
				uni.setStorageSync("user",res.data);
				
				//跳转到主页
				toPagesByUserRole(res.data.status);
				
				//若要测试令牌失效,请注释上面跳转内容
				// uni.navigateTo({
				// 	url:'/pages/register-login/loginAndRegisterQickliy'
				// })
			}else{
				//令牌校验失败，跳转到一键登录页面
				console.log("令牌校验失败，跳转到一键登录页面")
				uni.showToast({
					title: '未登录',
					icon: 'none',
				})
				//跳转到一键登录页面
				uni.navigateTo({
					url:'/pages/register-login/loginAndRegisterQickliy'
				})
			}
		} catch (error) {
			//令牌校验失败，跳转到一键登录页面
			console.log("令牌校验失败，跳转到一键登录页面")
			
			//跳转到一键登录页面
			uni.navigateTo({
				url:'/pages/register-login/loginAndRegisterQickliy'
			})
		}
	}

	//进入页面调用请求
	onMounted(() => {
		// // 模拟从后端获取 JWT 令牌
		setTimeout(() => {
			getJwtTokenFromBackend();
		}, 200); // 0.2秒后执行
		
	});
</script>

<style>
	.content {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}

	.logo {
		height: 300rpx;
		width: 300rpx;
		margin-top: 200rpx;
		margin-left: auto;
		margin-right: auto;
		margin-bottom: 50rpx;
		mix-blend-mode: multiply;
		border-radius: 5%;
	}

	.text-area {
		display: flex;
		justify-content: center;
	}

	.title {
		font-size: 36rpx;
		color: #8f8f94;
	}
</style>