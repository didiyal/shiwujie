<template>
  <div id="globalHeader">
    <a-row :wrap="false">
      <a-col flex="300px">
        <div class="title-bar">
          <img
            class="logo"
            src="../assets/视无界2.jpg"
            alt="logo"
            @click="handleToHome"
          />
          <div class="title"></div>
        </div>
      </a-col>
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="items"
          @click="doMenuClick"
        />
      </a-col>
      <a-col flex="80px">
        <div class="user-login-status">
          <div class="right">
            <a-button type="primary" @click="handleLogout">退出登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import { h, ref } from "vue";
import {
  MailOutlined,
  SettingOutlined,
  LogoutOutlined,
} from "@ant-design/icons-vue";
import { MenuProps } from "ant-design-vue";
import { useRouter } from "vue-router";

const router = useRouter();

//点击菜单后的路由跳转事件
const doMenuClick = ({ key }: { key: string }) => {
  router.push({
    path: key,
  });
};
const current = ref<string[]>(["mail"]);
//监听路由状态,
router.afterEach((to, from, failure) => {
  current.value = [to.path];
});
const items = ref<MenuProps["items"]>([
  {
    key: "/volunteer/community",
    icon: () => h(MailOutlined),
    label: "社区管理",
    title: "社区管理",
  },
  {
    key: "/volunteer/activity",
    icon: () => h(SettingOutlined),
    label: "活动",
    title: "活动",
  },
  {
    key: "/volunteer/community",
    label: h(
      "a",
      { href: "http://www.shi-wu-jie.xyz", target: "_blank" },
      "视无界官网"
    ),
    title: "视无界官网",
  },
]);

const emit = defineEmits(["logout"]);

const handleLogout = () => {
  emit("logout");
  router.push("/login");
};
const handleToHome = () => {
  emit("logout");
  router.push("/home");
};
</script>

<style scoped>
#globalHeader {
  margin-top: 15px;
}

.title-bar {
  margin-left: 15px;
  display: flex;
  align-items: center;
}

.title {
  color: black;
  font-size: 18px;
  margin-left: 16px;
}

.logo {
  height: 48px;
}

.right {
  float: right;
  margin-right: 24px;
}
</style>
