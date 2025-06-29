<template>
  <div class="homepage-container">
    <div class="header">
      <div class="logo">
        <img :src="logoImg" alt="SHIWUJIE Logo" />
      </div>
      <div class="nav desktop-nav">
        <a-button type="link" class="control-center-btn" @click="goToLogin"
        >控制中心
        </a-button>
      </div>
      <!-- 移动端菜单按钮 -->
      <div class="mobile-menu-btn" @click="toggleMobileMenu">
        <div class="bar"></div>
        <div class="bar"></div>
        <div class="bar"></div>
      </div>
    </div>

    <!-- 移动端菜单 -->
    <div class="mobile-menu" :class="{ show: mobileMenuOpen }">
      <a-button type="link" class="control-center-btn" @click="goToLogin"
      >控制中心
      </a-button>
    </div>

    <div class="content">
      <div class="title">视无界</div>
      <div class="subtitle">帮助视障人士生活出行，打开视力的新视界！</div>
      <button class="download-button">
        <a href="http://43.139.38.62:8081/app/download" download="filename.txt" class="download-button">下载客户端</a>
      </button>
      <br /><br /><br />
    </div>
    <div class="chatbot">
      <div class="chatbot-message">Hi, 视无界</div>
      <div class="chatbot-icon">💬</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
// 导入图片
import logo from "@/assets/视无界2.jpg";
import bgImage from "@/assets/背景图.jpg";
import axios from "axios";

const logoImg = ref("");
const mobileMenuOpen = ref(false);
const router = useRouter();

onMounted(() => {
  // 直接使用导入的图片
  logoImg.value = logo;

  // 确保移动设备上正确设置viewport
  const viewport = document.querySelector("meta[name=\"viewport\"]");
  if (!viewport) {
    const meta = document.createElement("meta");
    meta.name = "viewport";
    meta.content =
      "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no";
    document.getElementsByTagName("head")[0].appendChild(meta);
  } else {
    viewport.content =
      "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no";
  }
});


const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value;
};

const goToLogin = () => {
  router.push("/login");
};
</script>

<style>
/* 全局样式确保正确的盒模型 */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}



/* 确保html和body占满整个高度 */
html,
body {
  height: 100%;
  width: 100%;
  overflow-x: hidden;
}
</style>

<style scoped>

.download-button {
  color: #f0f0f0;
  text-decoration: none;
}
.homepage-container {
  font-family: Arial, sans-serif;
  min-height: 100vh;
  width: 100%;
  /* 使用导入的背景图片 */
  background-image: url(@/assets/背景图.jpg);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
  overflow-x: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background-color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1001;
}

.logo img {
  width: 5%;
  height: auto;
}

.desktop-nav {
  display: flex;
  gap: 20px;
}

.desktop-nav a {
  text-decoration: none;
  color: #007bff;
  font-size: 18px;
  font-weight: bold;
}

.desktop-nav a:hover {
  color: #0056b3;
}

.mobile-menu-btn {
  display: none;
  flex-direction: column;
  cursor: pointer;
  z-index: 1002;
}

.bar {
  width: 25px;
  height: 3px;
  background-color: #007bff;
  margin: 3px 0;
  border-radius: 2px;
  transition: 0.3s;
}

.mobile-menu {
  display: none;
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  background-color: white;
  flex-direction: column;
  padding: 0;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  transform: translateY(-20px);
  opacity: 0;
  transition: opacity 0.3s, transform 0.3s;
  pointer-events: none;
}

.mobile-menu.show {
  transform: translateY(0);
  opacity: 1;
  pointer-events: auto;
}

.mobile-menu a {
  padding: 15px 20px;
  text-decoration: none;
  color: #007bff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid #f0f0f0;
}

.mobile-menu a:hover {
  background-color: #f8f9fa;
}

.content {
  text-align: center;
  padding: 100px 20px;
}

.title {
  font-size: 52px;
  font-weight: bold;
  color: #1e3c72;
  margin-bottom: 20px;
}

.subtitle {
  font-size: 28px;
  color: #2a5298;
  margin-bottom: 30px;
}

.download-button {
  background-color: #333;
  color: white;
  padding: 12px 24px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s;
}

.download-button:hover {
  background-color: #555;
}

.chatbot {
  margin: 30px auto 20px;
  background-color: white;
  padding: 10px 15px;
  border-radius: 5px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  width: 90%;
  max-width: 600px;
  height: 50px;
}

.chatbot-message {
  margin-right: 10px;
  font-size: 16px;
  color: #333;
  flex: 1;
}

.chatbot-icon {
  font-size: 28px;
  color: #333;
}

/* 平板电脑 */
@media only screen and (max-width: 992px) {
  .title {
    font-size: 44px;
  }

  .subtitle {
    font-size: 24px;
  }
}

/* 手机设备 */
@media only screen and (max-width: 768px) {
  .desktop-nav {
    display: none !important; /* 强制隐藏桌面导航 */
  }

  .mobile-menu-btn {
    display: flex !important; /* 强制显示移动端菜单按钮 */
  }

  .mobile-menu {
    display: flex !important; /* 显示移动端菜单结构 */
  }

  .title {
    font-size: 36px;
  }

  .subtitle {
    font-size: 20px;
  }

  .content {
    padding: 60px 15px;
  }

  .download-button {
    padding: 10px 20px;
    font-size: 16px;
    width: 80%;
    max-width: 300px;
  }
}

/* 小型手机屏幕 */
@media only screen and (max-width: 480px) {
  .logo img {
    width: 90px;
  }

  .title {
    font-size: 32px;
  }

  .subtitle {
    font-size: 18px;
    padding: 0 10px;
  }

  .chatbot {
    padding: 8px 12px;
    height: auto;
    min-height: 45px;
  }

  .chatbot-message {
    font-size: 14px;
  }

  .chatbot-icon {
    font-size: 24px;
  }
}

/* 超小屏幕适配 */
@media only screen and (max-width: 320px) {
  .header {
    padding: 8px 15px;
  }

  .title {
    font-size: 28px;
  }

  .subtitle {
    font-size: 16px;
  }
}

.control-center-btn {
  font-size: 20px !important;
  color: #1890ff !important;
  font-weight: bold;
}

.control-center-btn:hover {
  color: #40a9ff !important;
}
</style>
