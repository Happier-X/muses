import path from 'path'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      'happier-ui/tokens.css': path.resolve(
        __dirname,
        '../../packages/happier-ui/src/tokens.css',
      ),
      'happier-ui': path.resolve(
        __dirname,
        '../../packages/happier-ui/src/index.ts',
      ),
    },
  },
  server: {
    port: 5174,
  },
})
