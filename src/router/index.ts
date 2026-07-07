import { createRouter, createWebHistory } from '@ionic/vue-router';
import { RouteRecordRaw } from 'vue-router';
import TabsPage from '../views/TabsPage.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/tabs/songs'
  },
  {
    path: '/tabs/',
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
