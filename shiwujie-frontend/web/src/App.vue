<template>
  <a-config-provider :theme="themeConfig">
    <div id="app">
      <router-view />
    </div>
  </a-config-provider>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      themeConfig: {
        token: {
          colorPrimary: '#0071e3',
          colorInfo: '#0071e3',
          colorLink: '#0071e3',
          colorSuccess: '#34c759',
          colorWarning: '#ff9500',
          colorError: '#ff3b30',
          colorTextBase: '#1d1d1f',
          colorBgLayout: '#f5f5f7',
          colorBorder: '#d2d2d7',
          colorBorderSecondary: '#e8e8ed',
          borderRadius: 8,
          fontFamily:
            "-apple-system, 'SF Pro Display', 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif",
          fontSize: 14
        },
        components: {
          Button: {
            borderRadius: 16,
            controlHeight: 34,
            primaryShadow: 'none',
            defaultShadow: 'none',
            fontWeight: 500
          },
          Input: {
            borderRadius: 8,
            controlHeight: 36
          },
          Select: {
            borderRadius: 8,
            controlHeight: 36
          },
          Card: {
            borderRadiusLG: 12,
            boxShadowTertiary: 'none'
          },
          Modal: {
            borderRadiusLG: 16
          },
          Menu: {
            itemHeight: 36,
            itemBorderRadius: 8,
            itemMarginInline: 0
          }
        }
      }
    }
  }
}
</script>

<style>
/* 基础重置与全局已在 src/styles/tokens.css 定义。
   此处仅保留 Ant Design Vue 消息提示等浮动层级微调。 */
.ant-message-notice-content {
  border-radius: 12px !important;
}
</style>
