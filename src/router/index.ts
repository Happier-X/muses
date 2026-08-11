import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import TabsPage from '../views/TabsPage.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/tabs/home'
  },
  {
    path: '/tabs',
    component: TabsPage,
    children: [
      {
        path: '',
        redirect: '/tabs/home'
      },
      {
        path: 'home',
        component: () => import('@/views/HomePage.vue')
      },
      {
        path: 'music',
        component: () => import('@/views/MusicPage.vue')
      },
      // 旧独立 tab 路由收敛到 /tabs/music（分段切换），保留重定向兼容旧链接/深层链接
      {
        path: 'songs',
        redirect: '/tabs/music'
      },
      {
        path: 'albums',
        redirect: '/tabs/music'
      },
      {
        path: 'artists',
        redirect: '/tabs/music'
      },
      {
        path: 'playlists',
        redirect: '/tabs/music'
      },
      {
        path: 'playlists/:id',
        component: () => import('@/views/PlaylistDetailPage.vue')
      },
      {
        path: 'library/:kind/:name',
        component: () => import('@/views/LibraryDetailPage.vue')
      },
      {
        path: 'sources',
        component: () => import('@/views/SourcesPage.vue')
      },
      {
        path: 'settings',
        component: () => import('@/views/SettingsPage.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
