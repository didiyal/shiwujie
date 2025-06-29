import { createApp } from "vue";
import { createPinia } from "pinia";
import piniaPluginPersistedstate from "pinia-plugin-persistedstate";
import App from "./App.vue";
import router from "./router";
import Antd from "ant-design-vue";
import "ant-design-vue/dist/reset.css";

const pinia = createPinia();
pinia.use(piniaPluginPersistedstate);

const app = createApp(App);
app.use(router);
app.use(Antd);
app.use(pinia);

// 忽略 ResizeObserver 循环警告
const originalError = console.error;
console.error = (...args) => {
  if (args[0].includes("ResizeObserver loop")) {
    return;
  }
  originalError.apply(console, args);
};

app.mount("#app");
