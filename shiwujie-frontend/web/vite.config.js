import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 9090,
    strictPort: true, // 端口被占用时直接报错退出
    open: true,
    proxy: {
      // 代理所有 /api 请求到后端
      '/api': {
        target: 'http://43.139.38.62:8100', // 后端服务器地址
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path // 保持 /api 前缀
      }
    }
  }
}) 