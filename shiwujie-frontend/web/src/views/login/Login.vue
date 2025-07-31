<template>
  <div class="login-container">
    <div class="login-form">
      <div class="login-header">
        <h1>视物界社区管理</h1>
        <p>社区管理员登录</p>
      </div>
      
      <a-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        @finish="handleLogin"
      >
        <a-form-item name="phone">
          <a-input
            v-model="loginForm.phone"
            size="large"
            placeholder="请输入手机号"
          >
            <template #prefix>📱</template>
          </a-input>
        </a-form-item>
        
        <a-form-item name="password">
          <a-input-password
            v-model="loginForm.password"
            size="large"
            placeholder="请输入密码"
          >
            <template #prefix>🔒</template>
          </a-input-password>
        </a-form-item>
        
        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="loading"
          >
            登录
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { message } from 'ant-design-vue'

export default {
  name: 'Login',
  setup() {
    const router = useRouter()
    const authStore = useAuthStore()
    const loginFormRef = ref()
    const loading = ref(false)

    const loginForm = reactive({
      phone: '',
      password: ''
    })

    const loginRules = {
      phone: [
        { required: true, message: '请输入手机号' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' }
      ],
      password: [
        { required: true, message: '请输入密码' },
        { min: 6, message: '密码长度不能少于6位' },
        { 
          pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{6,}$/, 
          message: '密码必须包含字母和数字' 
        }
      ]
    }

    const handleLogin = async () => {
      loading.value = true
      
      try {
        await authStore.login(loginForm.phone, loginForm.password)
        message.success('登录成功')
        router.push('/')
      } catch (error) {
        console.error('登录失败:', error)
        // 错误信息已经在request.js中处理了
      } finally {
        loading.value = false
      }
    }

    return {
      loginFormRef,
      loginForm,
      loginRules,
      loading,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-form {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  color: #1890ff;
  margin-bottom: 8px;
}

.login-header p {
  color: #666;
  margin: 0;
}
</style> 