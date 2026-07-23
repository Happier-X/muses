/// <reference types="vitest" />

import legacy from '@vitejs/plugin-legacy'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { defineConfig } from 'vite'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // legacy 只负责旧浏览器 chunk；modern chunk 使用插件内置较新基线，避免 modernTargets 警告。
    legacy({
      targets: ['Chrome >= 67', 'Edge >= 79', 'Firefox >= 68', 'Safari >= 14'],
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    // Ionic 的 :host-context() 会被 lightningcss 误报；改用 esbuild 压缩 CSS。
    cssMinify: 'esbuild',
    // Ionic / AMLL-Pixi 业务上就是大 chunk，避免无意义的体积告警噪音。
    chunkSizeWarningLimit: 1600,
    rollupOptions: {
      // 关闭构建期插件耗时统计告警（Windows 下经常被 legacy 二次打包触发）。
      checks: {
        pluginTimings: false,
      },
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
    environment: 'jsdom',
  },
})
