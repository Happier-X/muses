<template>
  <ion-app>
    <ion-router-outlet />
    <MiniPlayer
      class="app-mini-player"
      :class="{ 'is-overlay-active': playerOverlayVisible || queueOverlayVisible }"
      :aria-hidden="playerOverlayVisible || queueOverlayVisible"
    />
    <Transition name="player-overlay">
      <PlayerPage v-if="playerOverlayVisible" />
    </Transition>
    <Transition name="queue-overlay">
      <QueuePage v-if="queueOverlayVisible" />
    </Transition>
  </ion-app>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { IonApp, IonRouterOutlet } from '@ionic/vue'
import { App } from '@capacitor/app'
import MiniPlayer from '@/components/MiniPlayer.vue'
import PlayerPage from '@/views/PlayerPage.vue'
import QueuePage from '@/views/QueuePage.vue'
import { initializePlayer } from '@/features/player/controller'
import { closePlayerOverlay, closeQueueOverlay, playerOverlayVisible, queueOverlayVisible } from '@/features/player/overlay'

onMounted(() => {
  void initializePlayer()

  App.addListener('backButton', () => {
    if (queueOverlayVisible.value) {
      closeQueueOverlay()
      return
    }

    if (playerOverlayVisible.value) {
      closePlayerOverlay()
      return
    }

    App.exitApp()
  })
})
</script>

<style scoped>
.app-mini-player.is-overlay-active {
  pointer-events: none;
}

.player-overlay-enter-active,
.player-overlay-leave-active,
.queue-overlay-enter-active,
.queue-overlay-leave-active {
  transition: transform 220ms ease;
}

.player-overlay-enter-from,
.player-overlay-leave-to,
.queue-overlay-enter-from,
.queue-overlay-leave-to {
  transform: translateY(100%);
}
</style>
