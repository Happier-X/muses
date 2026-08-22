import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import TabsPage from '../views/TabsPage.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/tabs/songs'
  },
  // child2/child3：刮削中心（旧路径重定向兼容）
  {
    path: '/scrape',
    redirect: '/tabs/scrape'
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
        // 兼容旧书签/深层链接：不再作为导航入口
        path: 'categories',
        redirect: '/tabs/albums'
      },
      {
        path: 'sources',
        component: () => import('@/views/SourcesPage.vue')
      },
      {
        path: 'sources/webdav',
        component: () => import('@/views/SourceWebDavPage.vue')
      },
      {
        // 全屏目录浏览页（会话经内存服务传递，深链直达无会话时兜底回表单页）
        path: 'sources/webdav/browse',
        component: () => import('@/views/SourceWebDavBrowsePage.vue')
      },
      {
        path: 'sources/webdav/:id',
        component: () => import('@/views/SourceWebDavPage.vue')
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
        component: () => import('@/views/AlbumsPage.vue')
      },
      {
        path: 'artists',
        component: () => import('@/views/ArtistsPage.vue')
      },
      {
        path: 'playlists',
        component: () => import('@/views/PlaylistsPage.vue')
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
        path: 'scrape',
        component: () => import('@/views/ScrapePage.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
