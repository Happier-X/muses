import { createWebHistory, createRouter } from 'vue-router'

const routes = [
    {
        path: '/',
        redirect: '/app'
    },
    {
        path: '/auth',
        name: 'auth',
        component: () => import('@/views/auth/index.vue')
    },
    {
        path: '/app',
        name: 'app',
        redirect:'/app/home',
        component: () => import('@/views/layout/normal/index.vue'),
        children: [
            {
                path: 'home',
                name: 'home',
                component: () => import('@/views/home/index.vue')
            },
            {
                path: 'songs',
                name: 'songs',
                component: () => import('@/views/songs/index.vue')
            },
            {
                path: 'albums',
                name: 'albums',
                component: () => import('@/views/albums/index.vue')
            },
            {
                path: 'artists',
                name: 'artists',
                component: () => import('@/views/artists/index.vue')
            },
            {
                path: 'playlists',
                name: 'playlists',
                component: () => import('@/views/playlists/index.vue')
            },
            {
                path: 'settings',
                name: 'settings',
                component: () => import('@/views/settings/index.vue')
            },

        ]
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
