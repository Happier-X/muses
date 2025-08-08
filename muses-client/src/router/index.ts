import { createWebHistory, createRouter } from 'vue-router'

const routes = [
    {
        path: '/',
        redirect: '/home'
    },
    {
        path: '/home',
        name: 'home',
        component: () => import('@/views/home/index.vue')
    },
    {
        path: '/song',
        name: 'song',
        component: () => import('@/views/song/index.vue')
    },
    {
        path: '/album',
        name: 'album',
        component: () => import('@/views/album/index.vue')
    },
    {
        path: '/artist',
        name: 'artist',
        component: () => import('@/views/artist/index.vue')
    },
    {
        path: '/playlist',
        name: 'playlist',
        component: () => import('@/views/playlist/index.vue')
    },
    {
        path: '/settings',
        name: 'settings',
        component: () => import('@/views/settings/index.vue')
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
