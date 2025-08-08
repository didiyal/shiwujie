<template>
  <div class="login-container">
    <div class="login-background">
      <div class="background-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
      </div>
    </div>
    
    <div class="login-form">
      <div class="login-header">
        <div class="logo-container">
          <div class="logo-icon">🌟</div>
          <h1>视无界社区管理</h1>
        </div>
        <p class="subtitle">{{ isRegister ? '社区入驻注册' : '社区管理员登录' }}</p>
      </div>

      <!-- 切换按钮 -->
      <div class="login-tabs">
        <a-button 
          :type="!isRegister ? 'primary' : 'default'"
          @click="switchToLogin"
          class="tab-button"
        >
          管理员登录
        </a-button>
        <a-button 
          :type="isRegister ? 'primary' : 'default'"
          @click="switchToRegister"
          class="tab-button"
        >
          社区入驻
        </a-button>
      </div>
      
      <!-- 管理员登录表单 -->
      <form v-if="!isRegister" class="login-form-native" @submit.prevent="handleLogin">
        <!-- 密码设置提示 -->
        <div class="password-tip">
          <a-alert
            message="账户安全提示"
            description="管理员账户安全必须设置密码，可在APP志愿者端设置修改密码。"
            type="warning"
            show-icon
            class="security-alert"
          />
        </div>
        
        <div class="form-group">
          <label>手机号 *</label>
          <input
            v-model="loginForm.phone"
            type="tel"
            placeholder="请输入手机号"
            required
            pattern="^1[3-9]\d{9}$"
            class="form-input"
          />
        </div>
        
        <div class="form-group">
          <label>密码 *</label>
          <input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            required
            minlength="6"
            class="form-input"
          />
        </div>
        
        <div class="form-actions">
          <button
            type="submit"
            :disabled="loading"
            class="submit-btn"
          >
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </div>
      </form>

      <!-- 社区入驻注册表单 -->
      <form v-else class="register-form" @submit.prevent="handleRegisterClick">
        <!-- 注册人信息 -->
        <div class="form-section">
          <h3 class="section-title">注册人信息</h3>
          <div class="form-row">
            <div class="form-group">
              <label>姓名 *</label>
              <input
                v-model="registerForm.volunteer.name"
                type="text"
                placeholder="请输入姓名"
                required
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label>手机号 *</label>
              <input
                v-model="registerForm.volunteer.phone"
                type="tel"
                placeholder="请输入手机号"
                required
                class="form-input"
              />
            </div>
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label>性别 *</label>
              <select v-model="registerForm.volunteer.gender" required class="form-input">
                <option value="">请选择性别</option>
                <option :value="0">男</option>
                <option :value="1">女</option>
              </select>
            </div>
            <div class="form-group">
              <label class="optional">微信账号</label>
              <input
                v-model="registerForm.volunteer.wechatId"
                type="text"
                placeholder="请输入微信账号"
                class="form-input"
              />
            </div>
          </div>
          
          <div class="form-group">
            <label>身份证号 *</label>
            <input
              v-model="registerForm.volunteer.idCard"
              type="text"
              placeholder="请输入身份证号"
              required
              class="form-input"
            />
          </div>
          
          <div class="form-group">
            <label class="optional">其它信息</label>
            <textarea
              v-model="registerForm.volunteer.otherInfo"
              placeholder="请输入其它信息"
              rows="2"
              class="form-input"
            ></textarea>
          </div>
        </div>

        <!-- 社区信息 -->
        <div class="form-section">
          <h3 class="section-title">社区信息</h3>
          <div class="form-group">
            <label>社区名称 *</label>
            <input
              v-model="registerForm.communityName"
              type="text"
              placeholder="请输入社区名称"
              required
              class="form-input"
            />
          </div>
          
          <div class="form-group">
            <label>社区类型 *</label>
            <select v-model="registerForm.communityType" required class="form-input">
              <option value="">请选择社区类型</option>
              <option value="社会团体">社会团体</option>
              <option value="基层群众自治组织">基层群众自治组织</option>
              <option value="高校内部成立">高校内部成立</option>
              <option value="其它公益组织">其它公益组织</option>
            </select>
          </div>
          
          <div class="form-group">
            <label class="optional">社区介绍</label>
            <textarea
              v-model="registerForm.communityDescription"
              placeholder="请输入社区介绍"
              rows="2"
              class="form-input"
            ></textarea>
          </div>
        </div>

        <!-- 地址信息 -->
        <div class="form-section">
          <h3 class="section-title">地址信息</h3>
          <div class="form-row">
            <div class="form-group">
              <label>省份 *</label>
              <select v-model="registerForm.province" @change="handleProvinceChange" required class="form-input">
                <option value="">请选择省份</option>
                <option value="北京市">北京市</option>
                <option value="天津市">天津市</option>
                <option value="河北省">河北省</option>
                <option value="山西省">山西省</option>
                <option value="内蒙古自治区">内蒙古自治区</option>
                <option value="辽宁省">辽宁省</option>
                <option value="吉林省">吉林省</option>
                <option value="黑龙江省">黑龙江省</option>
                <option value="上海市">上海市</option>
                <option value="江苏省">江苏省</option>
                <option value="浙江省">浙江省</option>
                <option value="安徽省">安徽省</option>
                <option value="福建省">福建省</option>
                <option value="江西省">江西省</option>
                <option value="山东省">山东省</option>
                <option value="河南省">河南省</option>
                <option value="湖北省">湖北省</option>
                <option value="湖南省">湖南省</option>
                <option value="广东省">广东省</option>
                <option value="广西壮族自治区">广西壮族自治区</option>
                <option value="海南省">海南省</option>
                <option value="重庆市">重庆市</option>
                <option value="四川省">四川省</option>
                <option value="贵州省">贵州省</option>
                <option value="云南省">云南省</option>
                <option value="西藏自治区">西藏自治区</option>
                <option value="陕西省">陕西省</option>
                <option value="甘肃省">甘肃省</option>
                <option value="青海省">青海省</option>
                <option value="宁夏回族自治区">宁夏回族自治区</option>
                <option value="新疆维吾尔自治区">新疆维吾尔自治区</option>
              </select>
            </div>
            <div class="form-group">
              <label>城市 *</label>
              <input
                v-model="registerForm.city"
                type="text"
                placeholder="请输入城市"
                required
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label>区县 *</label>
              <input
                v-model="registerForm.district"
                type="text"
                placeholder="请输入区县"
                required
                class="form-input"
              />
            </div>
          </div>
          
          <div class="form-group">
            <label>具体地址 *</label>
            <input
              v-model="registerForm.address"
              type="text"
              placeholder="请输入具体地址"
              required
              class="form-input"
            />
          </div>
          
          <div class="form-group">
            <label class="optional">注册信息</label>
            <textarea
              v-model="registerForm.registrationInfo"
              placeholder="请输入注册信息"
              rows="2"
              class="form-input"
            ></textarea>
          </div>
        </div>

        <div class="form-actions">
          <button
            type="submit"
            :disabled="loading"
            class="submit-btn"
          >
            {{ loading ? '注册中...' : '注册入驻' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { communityApi, CommunityLoginModel } from '@/api'
import { message } from 'ant-design-vue'
import { addSafeIdToList } from '@/utils/bigIntUtils'

export default {
  name: 'Login',
  setup() {
    const router = useRouter()
    const authStore = useAuthStore()
    const loading = ref(false)
    const isRegister = ref(false)
    
    const loginForm = reactive({
      phone: '',
      password: ''
    })
    
    const registerForm = reactive({
      communityName: '',
      communityType: '',
      communityDescription: '',
      province: '',
      city: '',
      district: '',
      address: '',
      registrationInfo: '',
      volunteer: {
        name: '',
        phone: '',
        gender: 0, // 设置默认值为0（男），避免null值
        wechatId: '',
        idCard: '',
        otherInfo: ''
      }
    })

    // 确保响应式对象正确初始化
    console.log('初始化 registerForm:', registerForm)

    const switchToLogin = () => {
      isRegister.value = false
      // 重置登录表单
      loginForm.phone = ''
      loginForm.password = ''
    }

    const switchToRegister = () => {
      isRegister.value = true
      // 重置注册表单
      Object.assign(registerForm, {
        communityName: '',
        communityType: '',
        communityDescription: '',
        province: '',
        city: '',
        district: '',
        address: '',
        registrationInfo: '',
        volunteer: {
          name: '',
          phone: '',
          gender: 0, // 保持默认值为0（男），避免null值
          wechatId: '',
          idCard: '',
          otherInfo: ''
        }
      })
    }

    const handleProvinceChange = (value) => {
      // 清空城市和区县
      registerForm.city = ''
      registerForm.district = ''
    }

    const handleLogin = async () => {
      console.log('🚀 handleLogin函数被调用了!')
      loading.value = true
      
      try {
        // 添加原始请求调试信息
        console.log('📤 发送登录请求:', {
          phone: loginForm.phone,
          password: loginForm.password
        })
        
        await authStore.login(loginForm.phone, loginForm.password)
        message.success('登录成功')
        router.push('/')
      } catch (error) {
        console.error('❌ 登录失败:', error)
        // 直接显示错误信息
        message.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }

    const handleRegisterClick = async () => {
      console.log('🚀 注册按钮被点击了!')
      // 检查必填字段
      console.log('姓名字段值:', registerForm.volunteer.name)
      console.log('姓名字段是否为空:', !registerForm.volunteer.name)
      console.log('完整表单数据:', JSON.stringify(registerForm, null, 2))
      
      if (!registerForm.volunteer.name) {
        message.error('请输入姓名')
        return
      }
      if (!registerForm.volunteer.phone) {
        message.error('请输入手机号')
        return
      }
      if (!registerForm.volunteer.idCard) {
        message.error('请输入身份证号')
        return
      }
      if (!registerForm.communityName) {
        message.error('请输入社区名称')
        return
      }
      if (!registerForm.communityType) {
        message.error('请选择社区类型')
        return
      }
      if (!registerForm.province) {
        message.error('请选择省份')
        return
      }
      if (!registerForm.city) {
        message.error('请输入城市')
        return
      }
      if (!registerForm.district) {
        message.error('请输入区县')
        return
      }
      if (!registerForm.address) {
        message.error('请输入具体地址')
        return
      }
      console.log('📝 注册表单数据:', registerForm)
      handleRegister()
    }

    const handleRegister = async () => {
      console.log('🚀 handleRegister函数被调用了!')
      console.log('📝 注册表单数据:', registerForm)
      
      // 直接发送请求，不做前端验证，让后端处理
      console.log('✅ 直接发送请求到后端')
      
      loading.value = true
      
      try {
        console.log('🔍 前端注册请求:', registerForm)
        console.log('🔗 API路径:', '/api/community/community/Register')
        console.log('🔗 完整请求URL:', 'http://43.139.38.62:8081/api/community/community/Register')
        console.log('📤 发送的数据:', JSON.stringify(registerForm, null, 2))
        
        const data = await communityApi.register(registerForm)
        console.log('✅ 后端注册成功返回:', data)
        
        // 使用新的模型处理响应数据
        const loginModel = new CommunityLoginModel(data)
        
        // 注册成功后自动登录
        authStore.token = loginModel.token
        authStore.volunteer = loginModel.volunteer
        authStore.isLoggedIn = true
        localStorage.setItem('token', loginModel.token)
        
        // 保存用户创建的社区ID到localStorage
        // 根据登录响应数据结构，社区ID在 data.data.volunteer.communityId
        console.log('🔍 注册响应数据结构:', data);
        console.log('🔍 data.data:', data.data);
        console.log('🔍 data.data.volunteer:', data.data?.volunteer);
        console.log('🔍 data.data.volunteer.communityId:', data.data?.volunteer?.communityId);
        
        // 保存社区ID到localStorage - 使用安全的大数字处理工具
        if (data.data && data.data.volunteer && data.data.volunteer.communityId) {
          addSafeIdToList('userCommunities', data.data.volunteer.communityId);
        } else {
          console.log('❌ 注册响应中没有找到communityId');
        }
        
        message.success('社区注册成功，已自动登录')
        router.push('/')
      } catch (error) {
        console.error('❌ 注册失败:', error)
        // 直接显示错误信息
        message.error(error.message || '注册失败')
      } finally {
        loading.value = false
      }
    }

    return {
      loginForm,
      registerForm,
      loading,
      isRegister,
      switchToLogin,
      switchToRegister,
      handleProvinceChange,
      handleLogin,
      handleRegister,
      handleRegisterClick
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
  padding: 20px;
  box-sizing: border-box;
}

.login-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
}

.background-shapes {
  position: relative;
  width: 100%;
  height: 100%;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  width: 80px;
  height: 80px;
  top: 20%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 120px;
  height: 120px;
  top: 60%;
  right: 10%;
  animation-delay: 2s;
}

.shape-3 {
  width: 60px;
  height: 60px;
  bottom: 20%;
  left: 20%;
  animation-delay: 4s;
}

.shape-4 {
  width: 100px;
  height: 100px;
  top: 10%;
  right: 20%;
  animation-delay: 1s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(180deg);
  }
}

.login-form {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 48px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  z-index: 1;
  animation: slideInUp 0.8s ease-out;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: rgba(102, 126, 234, 0.3) transparent;
  display: flex;
  flex-direction: column;
}

.login-form::-webkit-scrollbar {
  width: 6px;
}

.login-form::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 3px;
}

.login-form::-webkit-scrollbar-thumb {
  background: rgba(102, 126, 234, 0.3);
  border-radius: 3px;
}

.login-form::-webkit-scrollbar-thumb:hover {
  background: rgba(102, 126, 234, 0.5);
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(50px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
  flex-shrink: 0;
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.logo-icon {
  font-size: 32px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.login-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 16px;
  color: #666;
  margin: 0;
  font-weight: 400;
}

.login-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 32px;
  background: #f8f9fa;
  padding: 8px;
  border-radius: 12px;
  flex-shrink: 0;
}

.tab-button {
  flex: 1;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.tab-button:hover {
  transform: translateY(-2px);
}

.security-alert {
  margin-bottom: 24px;
  border-radius: 12px;
  border: 1px solid #fff3cd;
  background: #fffbf0;
}

.login-form-native,
.register-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.form-section {
  margin-bottom: 32px;
  padding: 24px;
  background: #f8f9fa;
  border-radius: 16px;
  border: 1px solid #e9ecef;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e9ecef;
}

.form-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.form-row .form-group {
  flex: 1;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e9ecef;
  border-radius: 12px;
  font-size: 14px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: white;
  box-sizing: border-box;
}

.form-input:hover {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
}

.form-input::placeholder {
  color: #bfbfbf;
}

.form-actions {
  margin-top: 32px;
  flex-shrink: 0;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.6);
}

.submit-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 必填字段标记 */
.form-group label:not(.optional)::after {
  content: " *";
  color: #ff4d4f;
  font-weight: normal;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-container {
    padding: 16px;
    align-items: flex-start;
    min-height: 100vh;
  }
  
  .login-form {
    margin: 20px 0;
    padding: 32px 24px;
    border-radius: 20px;
    max-height: calc(100vh - 40px);
  }
  
  .login-header h1 {
    font-size: 24px;
  }
  
  .form-row {
    flex-direction: column;
    gap: 0;
  }
  
  .form-section {
    padding: 20px;
  }
}

/* 深色主题适配 */
@media (prefers-color-scheme: dark) {
  .login-form {
    background: rgba(30, 30, 30, 0.95);
    color: white;
  }
  
  .form-section {
    background: rgba(40, 40, 40, 0.8);
    border-color: #444;
  }
  
  .section-title {
    color: white;
    border-bottom-color: #444;
  }
  
  .form-group label {
    color: #ccc;
  }
  
  .form-input {
    background: #2a2a2a;
    border-color: #444;
    color: white;
  }
}
</style> 