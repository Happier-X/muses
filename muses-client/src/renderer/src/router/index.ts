import { createWebHistory, createRouter } from 'vue-router'

const routes = [
    {
        path: '/',
        redirect: '/home'
    },
    {
        path: '/home',
        name: 'home',
        component: () => import('@renderer/views/home/index.vue')
    },
    {
        path: '/song',
        name: 'song',
        component: () => import('@renderer/views/song/index.vue')
    },
    {
        path: '/album',
        name: 'album',
        component: () => import('@renderer/views/album/index.vue')
    },
    {
        path: '/artist',
        name: 'artist',
        component: () => import('@renderer/views/artist/index.vue')
    },
    {
        path: '/playlist',
        name: 'playlist',
        component: () => import('@renderer/views/playlist/index.vue')
    },
    {
        path: '/settings',
        name: 'settings',
        component: () => import('@renderer/views/settings/index.vue')
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
