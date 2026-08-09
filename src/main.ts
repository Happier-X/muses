import { createApp } from 'vue'
import App from './App.vue'
import router from './router';

/* Tailwind v4 管道：解析 Konsta UI 的 @theme / 组件层 */
import './theme/tailwind.css';

/* 跟随系统深浅色：同步 document.documentElement 的 .dark class（Konsta 暗色机制） */
import { useSystemDark } from './composables/useSystemDark';

useSystemDark();

const app = createApp(App)
  .use(router);

router.isReady().then(() => {
  app.mount('#app');
});
