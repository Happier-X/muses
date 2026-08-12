import legacy from '@vitejs/plugin-legacy'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'

const rootDir = path.dirname(fileURLToPath(import.meta.url))

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
      '@': path.resolve(rootDir, './src'),
    },
  },
  build: {
    // 使用 esbuild 压缩 CSS，避免部分选择器被 lightningcss 误报。
    cssMinify: 'esbuild',
    // AMLL / Pixi 等业务 chunk 较大，提高阈值以减少无意义告警。
    chunkSizeWarningLimit: 1600,
    rollupOptions: {
      // 关闭构建期插件耗时统计告警（Windows 下经常被 legacy 二次打包触发）。
      checks: {
        pluginTimings: false,
      },
      output: {
        manualChunks(id) {
          if (
            id.includes('node_modules/vue/') ||
            id.includes('node_modules/@vue/') ||
            id.includes('node_modules/vue-router/')
          ) {
            return 'vue-vendor'
          }
        },
      },
    },
  },
})
