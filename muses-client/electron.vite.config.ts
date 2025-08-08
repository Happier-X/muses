import { resolve } from 'path'
import { defineConfig, externalizeDepsPlugin } from 'electron-vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'

export default defineConfig({
  main: {
    plugins: [externalizeDepsPlugin()],
    build:{
      rollupOptions:{
        input:{
          index:resolve(__dirname,'electron/main/index.ts')
        }
      }
    }
  },
  preload: {
    plugins: [externalizeDepsPlugin()],
    build:{
      rollupOptions:{
        input:{
          index:resolve(__dirname,'electron/preload/index.ts')
        }
      }
    }
  },
  renderer: {
    plugins: [vue(), UnoCSS()],
    root:'.',
    build:{
      rollupOptions:{
        input:{
          index:resolve(__dirname,'index.html')
        }
      }
    },
    resolve: {
      alias: {
        '@': resolve(__dirname,'src/')
      }
    },
  }
})
