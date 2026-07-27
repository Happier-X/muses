import { createApp } from 'vue'
import App from './App.vue'
import router from './router';

/* Tailwind v4 管道：解析 happier-ui 的 @theme / 组件层 */
import './theme/tailwind.css';

/* Design tokens */
import './theme/tokens.css';

const app = createApp(App)
  .use(router);

router.isReady().then(() => {
  app.mount('#app');
});
