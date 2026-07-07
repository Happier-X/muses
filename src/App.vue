<template>
  <ion-app>
    <ion-router-outlet />
    <MiniPlayer />
  </ion-app>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { IonApp, IonRouterOutlet } from '@ionic/vue'
import MiniPlayer from '@/components/MiniPlayer.vue'
import { initializePlayer } from '@/features/player/controller'

onMounted(() => {
  void initializePlayer()
})
</script>
