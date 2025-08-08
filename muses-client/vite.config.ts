import { resolve } from 'path'
import { defineConfig } from 'electron-vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'

export default defineConfig({
    plugins: [vue(), UnoCSS()],
    resolve: {
      alias: {
        '@': resolve(__dirname,'src/')
      }
    },
})
