import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  server: {
    host: '172.17.112.1',
    proxy: {
      '/api': {
        target: 'http://172.17.112.1:8080',
        changeOrigin: true,
      },
      '/py-api': {
        target: 'http://172.17.112.1:5000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/py-api/, ''),
      },
      '/ai-api': {
        target: 'http://172.17.112.1:5001',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/ai-api/, ''),
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
