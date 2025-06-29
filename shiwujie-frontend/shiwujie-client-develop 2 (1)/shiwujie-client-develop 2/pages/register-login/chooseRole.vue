<template>
    <view class="role-select-container">
        <!-- 顶部标题 -->
        <view class="header">
            <text class="title">选择你的身份</text>
        </view>
        
        <!-- 身份选择区域 -->
        <view class="role-options">
            <!-- 志愿者选项 -->
            <view class="role-card" @click="selectRole(1)">
                <image 
                    src="/static/images/volunteer.png" 
                    mode="aspectFit" 
                    class="role-image"
                />
                <button 
                    :class="['role-btn', selectedRole === 1 ? 'active' : '']"
                >我是志愿者</button>
            </view>
            
            <!-- 盲人选项 -->
            <view class="role-card" @click="selectRole(0)">
                <image 
                    src="/static/images/user.png" 
                    mode="aspectFit" 
                    class="role-image"
                />
                <button 
                    :class="['role-btn', selectedRole === 0 ? 'active' : '']"
                >我是盲人</button>
            </view>
        </view>
    </view>
</template>

<script setup>
import { ref } from 'vue';
import {updateUserInfo} from '../../api/loginAndRegister';
import Storage from '../../utils/storage';
import {toPagesByUserRole} from '../../utils/loginUtils';

//控制动态显示按钮
const selectedRole = ref(0);

/**
 * 用户身份修改
 * @param {Object} status 用户身份
 */
async function handlerUpdateUserStatus(status){
	let user = Storage.getUserInfo();
	user.status = status;
	try {
		console.log("发送修改身份请求")
		const res = await updateUserInfo(user);
		if(res.code == 1){
			//更新用户信息
			Storage.setUserInfo(res.data);
			Storage.setUserRole(res.data.status);
			console.log("用户信息修改成功",user);
			// 延迟跳转到主页
			setTimeout(() => {
			    toPagesByUserRole(res.data.status)
			}, 1500);
		}	   
	} catch (error) {
	    uni.showToast({
	        title: '身份选择失败，请重试',
	     
	    });
	}
}


/**
 * 点击选择身份按钮调用
 */
const selectRole = async (roleCode) => {
    selectedRole.value = roleCode;
    console.log(0)
	// 显示选择成功提示
	uni.showToast({
	    title: '身份选择成功',
	   
	    duration: 1500
	});

	console.log(1)
	//后端更新身份，并跳转页面
	handlerUpdateUserStatus(roleCode);
    
};
</script>

<style lang="scss" scoped>
.role-select-container {
    min-height: 100vh;
    background-color: #ffffff;
    padding: 40rpx;
    
    .header {
        text-align: center;
        margin-bottom: 80rpx;
        
        .title {
            font-size: 36rpx;
            font-weight: 500;
            color: #333;
        }
    }
    
    .role-options {
        display: flex;
        flex-direction: column;
        gap: 60rpx;
        padding: 40rpx;
        
        .role-card {
            background: #fff;
            border-radius: 20rpx;
            padding: 40rpx;
            box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
            display: flex;
            flex-direction: column;
            align-items: center;
            transition: all 0.3s;
            
            &:active {
                transform: scale(0.98);
            }
            
            .role-image {
                width: 300rpx;
                height: 300rpx;
                margin-bottom: 40rpx;
            }
            
            .role-btn {
                width: 80%;
                height: 88rpx;
                line-height: 88rpx;
                text-align: center;
                border-radius: 44rpx;
                font-size: 32rpx;
                color: #666;
                background-color: #f5f5f5;
                border: none;
                transition: all 0.3s;
                
                &.active {
                    background-color: #ff6b6b;
                    color: #fff;
                }
            }
        }
    }
}
</style>