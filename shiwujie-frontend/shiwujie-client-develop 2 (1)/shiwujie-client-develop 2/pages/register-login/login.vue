<template>
	<view class="register-container">
		<!-- 顶部欢迎语 -->
		<view class="welcome-section">
			<text class="hello">Hello!</text>
			<text class="sub-title">欢迎使用视无界</text>
			<text class="login-type-text">手机号密码登录</text>
		</view>

		<!-- 表单区域 -->
		<view class="form-section">
			<view class="input-group">
				<input class="account-input" maxlength="11" placeholder="请输入手机号" v-model="loginData.userPhone" />
			</view>

			<!-- 密码输入框 -->
			<view class="input-group">
				<input type="password" placeholder="请输入密码" class="password-input" v-model="loginData.userPassword" />
			</view>

			<!-- 登录按钮 -->
			<button class="login-btn" @click="handleLogin()">登录</button>
			<!-- 快捷登录按钮 -->
			<button class="quicklylogin-btn" @click="handleQuicklyLogin()">手机号一键登录</button>
	<!-- 自动注册提示 -->
	<view class="auto-login-tip">如未注册将自动帮您注册登录</view>
		</view>

		<!-- 底部协议 -->
		<view class="agreement">
			<text class="agreement-text">
				<text class="gray">登录即代表同意</text>
				<text class="link">《用户协议》</text>
				<text class="gray">和</text>
				<text class="link">《隐私政策》</text>
			</text>
		</view>
		
	</view>
</template>

<script setup>
	import {
		loginAndRegister
	} from '../../api/loginAndRegister';
	import Storage from '../../utils/storage';
	import {
		toPagesByUserRole
	} from '../../utils/loginUtils';
	import { ref, reactive } from 'vue';
	
		// 使用reactive创建响应式数据
		const loginData = reactive({
			userPhone: '',
			userPassword: ''
		});
		

		const handleQuicklyLogin = () => {
		  uni.navigateTo({
		    url: '/pages/register-login/loginAndRegisterQickliy',
		    fail: (err) => {
		      console.error('导航失败：', err);
		     
		    }
		  });
		};
		
	/**
	 * 手机号密码登录
	 */
	async function handlerLoginAndRegister() {
		//手机号密码登录
		// 添加输入验证
				if (!loginData.userPhone || !loginData.userPassword) {
					uni.showToast({
						title: '请输入手机号和密码',
						icon: 'none'
					});
					return;
				}
		
		try {
			const res = await loginAndRegister(loginData.userPhone, loginData.userPassword);
			if (res.code === 1) {
				console.log("手机号密码注册登录成功")
				console.log(res);
				//登录成功，更新储存数据
				Storage.setToken(res.data.token);
				Storage.setUserInfo(res.data.user);
				Storage.setUserRole(res.data.user.status);
				console.log(res.data.user.status);
				//跳转页面
				toPagesByUserRole(res.data.user.status);
			}else {
				uni.showToast({
					title: res.description || '登录失败',
					icon: 'none'
				});
				}
		} catch (error) {
			//TODO handle the exception
			console.log("登录失败" + error);
			
		}
	}

/**
 * 登录点击按钮
 */
	function handleLogin() {
		handlerLoginAndRegister();

	}
</script>

<style lang="scss" scoped>
	.register-container {
	    min-height: 100vh;
	    background-color: #ffffff;
	    padding: 60rpx 40rpx;
	    box-sizing: border-box;
	    position: relative;
	
	    .welcome-section {
	        margin-bottom: 80rpx;
	        padding-top: 40rpx;
	
	        .hello {
	            font-size: 64rpx;
	            font-weight: bold;
	            color: #333;
	            margin-bottom: 20rpx;
	            display: block;
	            line-height: 1.2;
	        }
	
	        .sub-title {
	            font-size: 28rpx;
	            color: #666;
	            margin-bottom: 20rpx;
	            display: block;
	            line-height: 1.5;
	        }
	
	        .login-type-text {
	            font-size: 32rpx;
	            color: #1677FF;
	            font-weight: bold;
	            line-height: 1.5;
	        }
	    }
	
	    .form-section {
	        margin-top: 60rpx;
	
	        .input-group {
	            position: relative;
	            margin-bottom: 40rpx;
	
	            input {
	                width: 100%;
	                height: 90rpx;
	                background-color: #F8F8F8;
	                border-radius: 45rpx;
	                padding: 0 40rpx;
	                font-size: 28rpx;
	                color: #333;
	                box-sizing: border-box;
	                border: none;
	                outline: none;
	
	                &::placeholder {
	                    color: #999;
	                }
	
	                &:focus {
	                    background-color: #F0F0F0;
	                }
	            }
	
	            &:last-child {
	                margin-bottom: 0;
	            }
	        }
	
	        .login-btn {
	            width: 100%;
	            height: 90rpx;
	            line-height: 90rpx;
	            background: linear-gradient(to right, #1677FF, #1677FF);
	            color: #fff;
	            font-size: 32rpx;
	            border-radius: 45rpx;
	            margin-top: 60rpx;
	            border: none;
	            outline: none;
	            transition: all 0.3s ease;
	
	            &:active {
	                opacity: 0.8;
	                transform: scale(0.98);
	            }
	        }
			
			
			.quicklylogin-btn{
	            width: 100%;
	            height: 90rpx;
	            line-height: 90rpx;
	            background: linear-gradient(to right, #1677FF, #1677FF);
	            color: #fff;
	            font-size: 32rpx;
	            border-radius: 45rpx;
	            margin-top: 60rpx;
	            border: none;
	            outline: none;
	            transition: all 0.3s ease;
	
	            &:active {
	                opacity: 0.8;
	                transform: scale(0.98);
	            }
	        }
	
	        .auto-login-tip {
	            text-align: center;
	            font-size: 30rpx;
	            color: #999;
	            margin-top: 20rpx;
	            line-height: 1.5;
	        }
	
	        .bottom-links {
	            text-align: center;
	            margin-top: 30rpx;
	
	            .link {
	                color: #1677FF;
	                font-size: 28rpx;
	                text-decoration: none;
	            }
	        }
	    }
	
	    .agreement {
	        position: fixed;
	        bottom: 60rpx;
	        left: 0;
	        width: 100%;
	        text-align: center;
	        padding: 0 40rpx;
	        box-sizing: border-box;
	
	        .agreement-text {
	            font-size: 24rpx;
	            margin-bottom: 10rpx;
	            display: block;
	            line-height: 1.5;
	
	            .gray {
	                color: #999;
	            }
	
	            .link {
	                color: #1677FF;
	                text-decoration: none;
	                display: inline-block;
	                
	                &:active {
	                    opacity: 0.8;
	                }
	            }
	        }
	    }
	}
</style>