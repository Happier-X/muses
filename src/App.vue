<template>
  <ion-app>
    <ion-router-outlet />
    <MiniPlayer v-if="!isPlayerPage" />
  </ion-app>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { IonApp, IonRouterOutlet } from '@ionic/vue'
import { useRoute } from 'vue-router'
import MiniPlayer from '@/components/MiniPlayer.vue'
import { initializePlayer } from '@/features/player/controller'

const route = useRoute()
const isPlayerPage = computed(() => route.path === '/player')

onMounted(() => {
  void initializePlayer()
})
</script>
