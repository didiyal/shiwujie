import App from './App'

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
Vue.config.productionTip = false
App.mpType = 'app'
const app = new Vue({
  ...App
})


Vue.config.isCustomElement = tag => {
  return ['AR-CanvasView'].includes(tag);
};
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
import CustomTabbar from './components/customTabbar.vue'
/* import backButton from './components/BackButton.vue' */
export function createApp() {
  const app = createSSRApp(App)
  
  // 注册全局组件
  app.component('custom-tabbar', CustomTabbar)
  // 全局注册组件
 /* app.component('back-button', backButton) */
  
  return {
    app
  }
}
// #endif