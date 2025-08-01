import { createApp } from 'vue'
import App from './App.vue'
import Lew from "lew-ui";
import "lew-ui/style";
import 'virtual:uno.css';
import '@unocss/reset/sanitize/sanitize.css'
import '@unocss/reset/sanitize/assets.css'
import router from './router/index'

createApp(App).use(Lew).use(router).mount('#app')
