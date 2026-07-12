<template>
  <div class="login-page" :class="{ 'register-mode': isRegister }">
    <div class="login-card" :class="{ wide: isRegister }">
      <div class="login-brand">
        <div class="login-logo"><GlobalOutlined /></div>
        <div>
          <div class="login-wordmark">视<span>无</span>界</div>
          <div class="login-subbrand">社区管理后台</div>
        </div>
      </div>

      <!-- 切换 -->
      <div class="form-tabs">
        <button class="form-tab" :class="{ active: !isRegister }" @click="switchToLogin">管理员登录</button>
        <button class="form-tab" :class="{ active: isRegister }" @click="switchToRegister">社区入驻注册</button>
      </div>

      <!-- 管理员登录 -->
      <form v-if="!isRegister" class="login-form-native" @submit.prevent="handleLogin">
        <div class="login-title">欢迎回来</div>
        <div class="login-sub">使用手机号和密码登录管理后台</div>

        <div class="form-group">
          <label class="label">手机号 <span class="req">*</span></label>
          <input
            v-model="loginForm.phone"
            type="tel"
            class="form-input"
            placeholder="请输入手机号"
            required
            pattern="^1[3-9]\d{9}$"
          />
        </div>

        <div class="form-group">
          <label class="label">密码 <span class="req">*</span></label>
          <input
            v-model="loginForm.password"
            type="password"
            class="form-input"
            placeholder="请输入密码"
            required
            minlength="6"
          />
        </div>

        <div class="login-actions">
          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '登录中…' : '登录' }}
          </button>
        </div>
        <div class="login-foot">管理员账户需在 APP 志愿者端设置密码</div>
      </form>

      <!-- 社区入驻注册 -->
      <form v-else class="register-form" @submit.prevent="handleRegisterClick">
        <!-- 注册人信息 -->
        <div class="field-section">
          <div class="sec-label">注册人信息</div>
          <div class="form-row">
            <div class="form-group">
              <label class="label">姓名 <span class="req">*</span></label>
              <input v-model="registerForm.volunteer.name" type="text" class="form-input" placeholder="请输入姓名" required />
            </div>
            <div class="form-group">
              <label class="label">手机号 <span class="req">*</span></label>
              <input v-model="registerForm.volunteer.phone" type="tel" class="form-input" placeholder="请输入手机号" required />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="label">性别 <span class="req">*</span></label>
              <select v-model="registerForm.volunteer.gender" class="form-input" required>
                <option value="">请选择性别</option>
                <option :value="0">男</option>
                <option :value="1">女</option>
              </select>
            </div>
            <div class="form-group">
              <label class="label">微信账号</label>
              <input v-model="registerForm.volunteer.wechatId" type="text" class="form-input" placeholder="请输入微信账号" />
            </div>
          </div>

          <div class="form-group">
            <label class="label">身份证号 <span class="req">*</span></label>
            <input v-model="registerForm.volunteer.idCard" type="text" class="form-input" placeholder="请输入身份证号" required />
          </div>

          <div class="form-group">
            <label class="label">其它信息</label>
            <textarea v-model="registerForm.volunteer.otherInfo" class="form-textarea" placeholder="请输入其它信息" rows="2"></textarea>
          </div>
        </div>

        <!-- 社区信息 -->
        <div class="field-section">
          <div class="sec-label">社区信息</div>
          <div class="form-group">
            <label class="label">社区名称 <span class="req">*</span></label>
            <input v-model="registerForm.communityName" type="text" class="form-input" placeholder="请输入社区名称" required />
          </div>
          <div class="form-group">
            <label class="label">社区类型 <span class="req">*</span></label>
            <select v-model="registerForm.communityType" class="form-input" required>
              <option value="">请选择社区类型</option>
              <option value="社会团体">社会团体</option>
              <option value="基层群众自治组织">基层群众自治组织</option>
              <option value="高校内部成立">高校内部成立</option>
              <option value="其它公益组织">其它公益组织</option>
            </select>
          </div>
          <div class="form-group">
            <label class="label">社区介绍</label>
            <textarea v-model="registerForm.communityDescription" class="form-textarea" placeholder="请输入社区介绍" rows="2"></textarea>
          </div>
        </div>

        <!-- 地址信息 -->
        <div class="field-section">
          <div class="sec-label">地址信息</div>
          <div class="form-row">
            <div class="form-group">
              <label class="label">省份 <span class="req">*</span></label>
              <select v-model="registerForm.province" class="form-input" required @change="handleProvinceChange">
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
              <label class="label">城市 <span class="req">*</span></label>
              <input v-model="registerForm.city" type="text" class="form-input" placeholder="请输入城市" required />
            </div>
            <div class="form-group">
              <label class="label">区县 <span class="req">*</span></label>
              <input v-model="registerForm.district" type="text" class="form-input" placeholder="请输入区县" required />
            </div>
          </div>

          <div class="form-group">
            <label class="label">具体地址 <span class="req">*</span></label>
            <input v-model="registerForm.address" type="text" class="form-input" placeholder="请输入具体地址" required />
          </div>

          <div class="form-group">
            <label class="label">注册信息</label>
            <textarea v-model="registerForm.registrationInfo" class="form-textarea" placeholder="请输入注册信息" rows="2"></textarea>
          </div>
        </div>

        <div class="login-actions">
          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '注册中…' : '提交入驻申请' }}
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
import { GlobalOutlined } from '@ant-design/icons-vue'

export default {
  name: 'Login',
  components: { GlobalOutlined },
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
        gender: 0,
        wechatId: '',
        idCard: '',
        otherInfo: ''
      }
    })

    const switchToLogin = () => {
      isRegister.value = false
      loginForm.phone = ''
      loginForm.password = ''
    }

    const switchToRegister = () => {
      isRegister.value = true
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
          gender: 0,
          wechatId: '',
          idCard: '',
          otherInfo: ''
        }
      })
    }

    const handleProvinceChange = () => {
      registerForm.city = ''
      registerForm.district = ''
    }

    const handleLogin = async () => {
      loading.value = true
      try {
        await authStore.login(loginForm.phone, loginForm.password)
        message.success('登录成功')
        router.push('/')
      } catch (error) {
        message.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }

    const handleRegisterClick = async () => {
      if (!registerForm.volunteer.name) return message.error('请输入姓名')
      if (!registerForm.volunteer.phone) return message.error('请输入手机号')
      if (!registerForm.volunteer.idCard) return message.error('请输入身份证号')
      if (!registerForm.communityName) return message.error('请输入社区名称')
      if (!registerForm.communityType) return message.error('请选择社区类型')
      if (!registerForm.province) return message.error('请选择省份')
      if (!registerForm.city) return message.error('请输入城市')
      if (!registerForm.district) return message.error('请输入区县')
      if (!registerForm.address) return message.error('请输入具体地址')
      await handleRegister()
    }

    const handleRegister = async () => {
      loading.value = true
      try {
        const data = await communityApi.register(registerForm)
        const loginModel = new CommunityLoginModel(data)

        authStore.token = loginModel.token
        authStore.volunteer = loginModel.volunteer
        authStore.isLoggedIn = true
        localStorage.setItem('token', loginModel.token)

        if (data.data && data.data.volunteer && data.data.volunteer.communityId) {
          addSafeIdToList('userCommunities', data.data.volunteer.communityId)
        }

        message.success('社区注册成功，已自动登录')
        router.push('/')
      } catch (error) {
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
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: var(--bg);
}
.login-page.register-mode {
  align-items: flex-start;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: var(--surface);
  border: 1px solid var(--border-l);
  border-radius: var(--radius-lg);
  padding: 36px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.login-card.wide {
  max-width: 720px;
  padding: 32px;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.login-logo {
  width: 38px;
  height: 38px;
  border-radius: 9px;
  background: var(--sidebar);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.login-wordmark {
  font-size: 17px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: -0.01em;
  line-height: 1.2;
}
.login-wordmark span {
  color: var(--primary);
}
.login-subbrand {
  font-size: 11px;
  color: var(--text-2);
  margin-top: 1px;
}

.form-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 22px;
  background: var(--bg);
  padding: 3px;
  border-radius: 9px;
}
.form-tab {
  flex: 1;
  height: 32px;
  border: none;
  border-radius: 7px;
  font-family: var(--font);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-2);
  background: transparent;
  cursor: pointer;
  transition: var(--tr);
}
.form-tab.active {
  background: var(--surface);
  color: var(--text);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.login-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 2px;
}
.login-sub {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 22px;
}

.field-section {
  margin-bottom: 22px;
}
.sec-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-2);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 10px;
}

.form-row {
  display: flex;
  gap: 14px;
}
.form-row .form-group {
  flex: 1;
}
.form-group {
  margin-bottom: 16px;
}
.label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text);
}
.label .req,
.form-group label:not(.optional)::after {
  color: var(--danger);
}
.form-group label:not(.optional)::after {
  content: ' *';
  font-weight: 400;
}
.label .req {
  display: none; /* 用 ::after 统一标记 */
}

.form-input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  font-family: var(--font);
  font-size: 13px;
  color: var(--text);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  transition: var(--tr);
  appearance: none;
}
.form-input:hover {
  border-color: var(--text-3);
}
.form-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-t);
}
.form-input::placeholder {
  color: var(--text-3);
}
select.form-input {
  cursor: pointer;
  padding-right: 30px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236e6e73' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
}
.form-textarea {
  width: 100%;
  min-height: 64px;
  padding: 10px 12px;
  font-family: var(--font);
  font-size: 13px;
  color: var(--text);
  line-height: 1.6;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  resize: vertical;
  transition: var(--tr);
}
.form-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-t);
}
.form-textarea::placeholder {
  color: var(--text-3);
}

.login-actions {
  margin-top: 8px;
}
.submit-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 22px;
  font-family: var(--font);
  font-size: 15px;
  font-weight: 600;
  background: var(--primary);
  color: #fff;
  cursor: pointer;
  transition: var(--tr);
}
.submit-btn:hover:not(:disabled) {
  background: var(--primary-h);
}
.submit-btn:active:not(:disabled) {
  transform: scale(0.99);
}
.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.login-foot {
  text-align: center;
  font-size: 12px;
  color: var(--text-2);
  margin-top: 16px;
}

@media (max-width: 768px) {
  .login-page {
    padding: 24px 16px;
    align-items: flex-start;
  }
  .login-card {
    padding: 26px 22px;
  }
  .form-row {
    flex-direction: column;
    gap: 0;
  }
}
</style>
