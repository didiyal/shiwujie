<script lang="ts">
import { defineComponent, reactive, ref } from "vue";
import {
  UserOutlined,
  LockOutlined,
  LeftOutlined,
} from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import { useRouter } from "vue-router";

export default defineComponent({
  name: "RegisterAndLogin",
  components: {
    UserOutlined,
    LockOutlined,
    LeftOutlined,
  },
  setup() {
    const router = useRouter();
    const activeKey = ref("login");

    const loginForm = reactive({
      username: "",
      password: "",
    });

    const registerForm = reactive({
      username: "",
      password: "",
      confirmPassword: "",
    });

    const validateConfirmPassword = async (_rule: any, value: string) => {
      if (value !== registerForm.password) {
        return Promise.reject("两次输入的密码不一致");
      }
      return Promise.resolve();
    };

    const handleLogin = (values: any) => {
      console.log("登录表单:", values);
      // 获取已注册用户列表
      const users = JSON.parse(localStorage.getItem("users") || "[]");

      // 检查是否是管理员账号
      if (values.username === "admin" && values.password === "123456") {
        localStorage.setItem("isLogin", "true");
        message.success("登录成功");
        router.push("/volunteer/community");
        return;
      }

      // 检查是否是注册用户
      const user = users.find(
        (u: any) =>
          u.username === values.username && u.password === values.password
      );

      if (user) {
        localStorage.setItem("isLogin", "true");
        message.success("登录成功");
        router.push("/volunteer/community");
      } else {
        message.error("账号或密码错误");
      }
    };

    const handleRegister = (values: any) => {
      console.log("注册表单:", values);
      // 获取已注册用户列表
      const users = JSON.parse(localStorage.getItem("users") || "[]");

      // 检查用户名是否已存在
      if (
        values.username === "admin" ||
        users.some((u: any) => u.username === values.username)
      ) {
        message.error("该用户名已被使用");
        return;
      }

      // 添加新用户
      users.push({
        username: values.username,
        password: values.password,
      });

      // 保存更新后的用户列表
      localStorage.setItem("users", JSON.stringify(users));
      message.success("注册成功");
      activeKey.value = "login";
    };

    const goHome = () => {
      router.push("/home");
    };

    return {
      activeKey,
      loginForm,
      registerForm,
      handleLogin,
      handleRegister,
      validateConfirmPassword,
      goHome,
    };
  },
});
</script>

<template>
  <div class="login-container">
    <a-button class="back-home-btn" type="link" @click="goHome">
      <template #icon>
        <left-outlined />
      </template>
      返回首页
    </a-button>

    <a-card :bordered="false" class="login-card">
      <h1 class="title">视无界</h1>
      <a-tabs v-model:activeKey="activeKey">
        <a-tab-pane key="login" tab="登录">
          <a-form :model="loginForm" @finish="handleLogin">
            <a-form-item
              name="username"
              :rules="[{ required: true, message: '请输入手机号' }]"
            >
              <a-input v-model:value="loginForm.username" placeholder="手机号">
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-input>
            </a-form-item>
            <a-form-item
              name="password"
              :rules="[{ required: true, message: '请输入密码' }]"
            >
              <a-input-password
                v-model:value="loginForm.password"
                placeholder="密码"
              >
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" block>登录</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="register" tab="注册">
          <a-form :model="registerForm" @finish="handleRegister">
            <a-form-item
              name="username"
              :rules="[{ required: true, message: '请输入用户名' }]"
            >
              <a-input
                v-model:value="registerForm.username"
                placeholder="用户名"
              >
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-input>
            </a-form-item>
            <a-form-item
              name="password"
              :rules="[{ required: true, message: '请输入密码' }]"
            >
              <a-input-password
                v-model:value="registerForm.password"
                placeholder="密码"
              >
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>
            <a-form-item
              name="confirmPassword"
              :rules="[
                { required: true, message: '请确认密码' },
                { validator: validateConfirmPassword },
              ]"
            >
              <a-input-password
                v-model:value="registerForm.confirmPassword"
                placeholder="确认密码"
              >
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" block>注册</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: url(../../assets/背景图.jpg);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 20px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9); /* 半透明白色背景 */
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.login-card .title {
  font-size: 36px;
  text-align: center;
}

.back-home-btn {
  position: fixed;
  top: 20px;
  left: 20px;
  font-size: 20px;
  color: #180e0e !important;
  z-index: 1000;
}

.back-home-btn:hover {
  color: #40a9ff !important;
}
</style>
