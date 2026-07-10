<template>
  <ion-app>
    <ion-router-outlet />
    <MiniPlayer v-if="!isPlayerPage" />
  </ion-app>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { IonApp, IonRouterOutlet } from '@ionic/vue'
import { useRoute, useRouter } from 'vue-router'
import { App } from '@capacitor/app'
import MiniPlayer from '@/components/MiniPlayer.vue'
import { initializePlayer } from '@/features/player/controller'

const route = useRoute()
const router = useRouter()
const isPlayerPage = computed(() => route.path === '/player' || route.path === '/queue')

onMounted(() => {
  void initializePlayer()

  App.addListener('backButton', ({ canGoBack }) => {
    const path = route.path

    if (path === '/player' || path === '/queue') {
      if (canGoBack) {
        router.back()
      }
      return
    }

    App.exitApp()
  })
})
</script>
