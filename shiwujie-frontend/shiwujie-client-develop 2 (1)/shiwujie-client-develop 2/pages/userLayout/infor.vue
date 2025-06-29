<template>
  <view class="container">
    <view class="profile-header">
      <image src="/static/images/touxiang.jpg" class="profile-image"></image>
      <text class="profile-userName">{{ userInfo.userName }}</text>
      <text class="profile-userAccount">账号:{{ userInfo.userAccount }}</text>
    </view>

    <view class="form">
      <view class="form-item">
        <text>用户名</text>
        <input v-model="editForm.userName" placeholder="请输入用户名" />
      </view>

      <view class="form-item">
        <text>手机号</text>
        <input v-model="editForm.userPhone" placeholder="请输入手机号" />
      </view>

      <view class="form-item">
        <text>邮箱</text>
        <input v-model="editForm.userEmail" placeholder="请输入邮箱" />
      </view>

      <view class="form-item">
        <text>性别</text>
        <picker mode="selector" :range="genders" @change="handleGenderChange" :value="editForm.gender">
          <view>{{ genders[editForm.gender] || '请选择性别' }}</view>
        </picker>
      </view>

      <view class="form-item">
        <text>家庭ID</text>
        <input :value="userInfo.familyAccount" disabled />
      </view>

      <button class="updatePassword" @click="showPasswordModal">修改密码</button>
    </view>

    <button class="submit-btn" @click="handleSubmit">确认修改</button>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getUserInfo} from '../../api/user'; 
import { updateUserInfo } from '../../api/loginAndRegister';
import Storage from '../../utils/storage';

// 性别选项
const genders = ['男', '女'];

// 用户原始信息
const userInfo = ref({
  userName: '',
  userAccount: '',
  userPhone: '',
  userEmail: '',
  gender: 0,
  familyAccount: '',
  userUrl: '/static/touxiang.jpg'
});

// 编辑表单数据
const editForm = ref({
  userName: '',
  userPhone: '',
  userEmail: '',
  gender: 0
});

// 加载状态
const loading = ref(false);

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    loading.value = true;
	const token = Storage.getToken() || '';
	console.log('获取到的token:', token);
    const res = await getUserInfo();
    console.log('获取用户信息响应:', res);
	

    if (res.data.code === 1 && res.data.data) {
      const data = res.data.data;
      // 更新用户信息显示
      userInfo.value = {
        ...data,
        userUrl: '/static/touxiang.jpg' // 使用本地头像
      };
      
      // 初始化编辑表单
      editForm.value = {
        userName: data.userName || '',
        userPhone: data.userPhone || '',
        userEmail: data.userEmail || '',
        gender: data.gender || 0
      };
    } else {
      uni.showToast({
        title: res.data.message || '获取用户信息失败',
     
      });
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
   
  } finally {
    loading.value = false;
  }
};

// 处理性别选择
const handleGenderChange = (e) => {
  editForm.value.gender = parseInt(e.detail.value);
};

// 处理表单提交
const handleSubmit = async () => {
  if (loading.value) return;
  
  // 表单验证
  if (!editForm.value.userName.trim()) {
    uni.showToast({
      title: '用户名不能为空',
      icon: 'none'
    });
    return;
  }

  try {
    loading.value = true;
    const res = await updateUserInfo(editForm.value);
    console.log('更新用户信息响应:', res);

    if (res.code === 1) {
      uni.showToast({
        title: '修改成功',
       
      });

      // 重新获取用户信息
      await fetchUserInfo();

      // 确保 Storage 中的数据也是最新的
      Storage.setUserInfo(userInfo.value);
      
    } else {
      uni.showToast({
        title: res.message || '修改失败',
    
      });
    }
  } catch (error) {
    console.error('更新用户信息失败:', error);
    
  } finally {
    loading.value = false;
  }
};


// 显示密码修改弹窗
const showPasswordModal = () => {
  uni.showModal({
    title: '修改密码',
    placeholderText: '请输入新密码',
    editable: true,
    success: async (res) => {
      if (res.confirm && res.content) {
        try {
          loading.value = true;
          // 添加新密码的调试信息
          console.log('新密码:', res.content);
          
          const updateRes = await updateUserInfo({
            ...editForm.value,
            userPassword: res.content
          });

          // 添加更新结果的调试信息
          console.log('密码更新返回数据:', updateRes);

          if (updateRes.code === 1) {
            uni.showToast({
              title: '密码修改成功',
             
            });
          } else {
            uni.showToast({
              title: updateRes.message || '密码修改失败',
             
            });
          }
        } catch (error) {
          console.error('密码修改失败:', error);
         
        } finally {
          loading.value = false;
        }
      }
    }
  });
};

// 页面加载时获取用户信息
onMounted(() => {
  fetchUserInfo();
});
</script>

<style>
.container {
  padding: 20px;
  min-height: 100vh;
}

.profile-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 20px;
  border-radius: 12px;
  margin-bottom: 15px;
}

.profile-image {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  margin-bottom: 10px;
  border: 2px solid #eee;
}

.profile-userName {
  font-size: 18px;
  color: #333;
  margin-bottom: 5px;
}

.profile-userAccount {
  font-size: 14px;
  color: #666;
}

.form {
  background: #fff;
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 20px;
}

.form-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #eee;
}

.form-item:last-child {
  border-bottom: none;
}

.form-item text {
  color: #333;
  font-size: 16px;
  width: 80px;
}

.form-item input,
.form-item picker {
  flex: 1;
  text-align: right;
  color: #666;
}

.submit-btn {
  width: 100%;
  background-color: #5E8BFF;
  color: white;
  border-radius: 10px;
  padding: 12px;
  margin-top: 20px;
}

button[size="mini"] {
  margin-top: 15px;
  background-color: #f8f8f8;
  color: #333;
}
</style>