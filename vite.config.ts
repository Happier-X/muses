/// <reference types="vitest" />

import legacy from '@vitejs/plugin-legacy'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { defineConfig } from 'vite'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    legacy({
      targets: ['Chrome >= 67', 'Edge >= 79', 'Firefox >= 68', 'Safari >= 14'],
      modernTargets: ['Chrome >= 67', 'Edge >= 79', 'Firefox >= 68', 'Safari >= 14'],
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (
            id.includes('@applemusic-like-lyrics') ||
            id.includes('@pixi')
          ) {
            return 'amll-pixi'
          }
          if (id.includes('@ionic/vue') || id.includes('ionicons')) {
            return 'ionic'
          }
          if (
            id.includes('node_modules/vue/') ||
            id.includes('node_modules/@vue/') ||
            id.includes('node_modules/vue-router/') ||
            id.includes('node_modules/@ionic/vue-router/')
          ) {
            return 'vue-vendor'
          }
        },
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom'
  }
})
