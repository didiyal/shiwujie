<template>
  <div class="login-container">
    <div class="login-form">
      <div class="login-header">
        <h1>视无界社区管理</h1>
        <p>{{ isRegister ? '社区入驻注册' : '社区管理员登录' }}</p>
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
        <div class="form-group">
          <label>手机号 *</label>
          <input
            v-model="loginForm.phone"
            type="tel"
            placeholder="请输入手机号"
            required
            pattern="^1[3-9]\d{9}$"
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
          <h3>注册人信息</h3>
          <div class="form-row">
            <div class="form-group">
              <label>姓名 *</label>
              <input
                v-model="registerForm.volunteer.name"
                type="text"
                placeholder="请输入姓名"
                required
              />
            </div>
            <div class="form-group">
              <label>手机号 *</label>
              <input
                v-model="registerForm.volunteer.phone"
                type="tel"
                placeholder="请输入手机号"
                required
              />
            </div>
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label>性别 *</label>
              <select v-model="registerForm.volunteer.gender" required>
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
            />
          </div>
          
                     <div class="form-group">
             <label class="optional">其它信息</label>
            <textarea
              v-model="registerForm.volunteer.otherInfo"
              placeholder="请输入其它信息"
              rows="2"
            ></textarea>
          </div>
        </div>

        <!-- 社区信息 -->
        <div class="form-section">
          <h3>社区信息</h3>
          <div class="form-group">
            <label>社区名称 *</label>
            <input
              v-model="registerForm.communityName"
              type="text"
              placeholder="请输入社区名称"
              required
            />
          </div>
          
          <div class="form-group">
            <label>社区类型 *</label>
            <select v-model="registerForm.communityType" required>
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
            ></textarea>
          </div>
          
          <!-- 地址信息 -->
          <div class="form-row">
            <div class="form-group">
              <label>省份 *</label>
              <select v-model="registerForm.province" @change="handleProvinceChange" required>
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
              />
            </div>
            <div class="form-group">
              <label>区县 *</label>
              <input
                v-model="registerForm.district"
                type="text"
                placeholder="请输入区县"
                required
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
            />
          </div>
          
                     <div class="form-group">
             <label class="optional">注册信息</label>
            <textarea
              v-model="registerForm.registrationInfo"
              placeholder="请输入注册信息"
              rows="2"
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
       console.log('�� 注册按钮被点击了!')
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
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-form {
  width: 500px;
  max-height: 85vh;
  overflow-y: auto;
  padding: 30px;
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

.login-tabs {
  display: flex;
  margin-bottom: 30px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #d9d9d9;
}

.tab-button {
  flex: 1;
  border-radius: 0;
  border: none;
}

.tab-button:first-child {
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}

.tab-button:last-child {
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
}

.form-section {
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
}

.form-section h3 {
  color: #1890ff;
  margin-bottom: 20px;
  font-size: 16px;
  font-weight: 600;
}

/* 滚动条样式 */
.login-form::-webkit-scrollbar {
  width: 6px;
}

.login-form::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.login-form::-webkit-scrollbar-thumb {
  background: #1890ff;
  border-radius: 3px;
}

.login-form::-webkit-scrollbar-thumb:hover {
  background: #40a9ff;
}

/* 原生表单样式 */
.login-form-native {
  width: 100%;
}

.register-form {
  width: 100%;
}

.form-row {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}

.form-row .form-group {
  flex: 1;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: #bfbfbf;
}

.form-group select {
  background-color: white;
  cursor: pointer;
}

.form-group textarea {
  resize: vertical;
  min-height: 60px;
}

.form-actions {
  margin-top: 30px;
  text-align: center;
}

.submit-btn {
  width: 100%;
  padding: 12px 24px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-btn:hover:not(:disabled) {
  background: #40a9ff;
}

.submit-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

/* 必填字段标记 */
.form-group label:not(.optional)::after {
  content: " *";
  color: #ff4d4f;
  font-weight: normal;
}
</style> 