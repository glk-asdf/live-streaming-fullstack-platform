import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 业务接口与 WebSocket 同源转发，避免后端散写跨域配置
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
      // 仅开发环境：直接读取 MediaMTX 管理 API 做状态探活
      '/media-api': {
        target: 'http://127.0.0.1:9997',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/media-api/, ''),
      },
    },
  },
})
