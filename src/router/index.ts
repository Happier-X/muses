import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import TabsPage from '../views/TabsPage.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/tabs/songs'
  },
  {
    path: '/tabs',
    component: TabsPage,
    children: [
      {
        path: '',
        redirect: '/tabs/songs'
      },
      {
        path: 'songs',
        component: () => import('@/views/SongsPage.vue')
      },
      {
        path: 'categories',
        component: () => import('@/views/CategoriesPage.vue')
      },
      {
        path: 'sources',
        component: () => import('@/views/SourcesPage.vue')
      },
      {
        path: 'settings',
        component: () => import('@/views/SettingsPage.vue')
      },
      // 旧 tab 路由收敛重定向
      {
        path: 'home',
        redirect: '/tabs/songs'
      },
      {
        path: 'music',
        redirect: '/tabs/songs'
      },
      {
        path: 'albums',
        redirect: '/tabs/categories'
      },
      {
        path: 'artists',
        redirect: '/tabs/categories'
      },
      {
        path: 'playlists',
        redirect: '/tabs/categories'
      },
      {
        path: 'playlists/:id',
        component: () => import('@/views/PlaylistDetailPage.vue')
      },
      {
        path: 'library/:kind/:name',
        component: () => import('@/views/LibraryDetailPage.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
