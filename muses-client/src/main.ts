import { createApp } from 'vue'
import App from './App.vue'
import naive from 'naive-ui'
import 'virtual:uno.css';
import '@unocss/reset/sanitize/sanitize.css'
import '@unocss/reset/sanitize/assets.css'
import router from './router/index'

createApp(App).use(naive).use(router).mount('#app')
