<template>
  <ion-app :class="{ 'has-global-overlay': hasGlobalOverlay }">
    <ion-router-outlet class="app-router-outlet" />
    <MiniPlayer
      class="app-mini-player"
      :class="{ 'is-overlay-active': hasGlobalOverlay }"
      :aria-hidden="hasGlobalOverlay"
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
import { computed, defineAsyncComponent, onMounted, onUnmounted, watch } from 'vue'
import { IonApp, IonRouterOutlet } from '@ionic/vue'
import { App } from '@capacitor/app'
import { StatusBar, Style } from '@capacitor/status-bar'
import MiniPlayer from '@/components/MiniPlayer.vue'
import { initializePlayer } from '@/features/player/controller'
import { closePlayerOverlay, closeQueueOverlay, playerOverlayVisible, queueOverlayVisible } from '@/features/player/overlay'

const PlayerPage = defineAsyncComponent(() => import('@/views/PlayerPage.vue'))
const QueuePage = defineAsyncComponent(() => import('@/views/QueuePage.vue'))
const hasGlobalOverlay = computed(() => playerOverlayVisible.value || queueOverlayVisible.value)
let statusBarRequestToken = 0
let statusBarSyncQueue = Promise.resolve()

const syncPlayerStatusBar = (visible: boolean) => {
  const requestToken = ++statusBarRequestToken
  statusBarSyncQueue = statusBarSyncQueue
    .catch(() => undefined)
    .then(async () => {
      if (requestToken !== statusBarRequestToken) {
        return
      }

      await StatusBar.setStyle({ style: visible ? Style.Dark : Style.Default })
    })
    .catch(() => undefined)
}

const syncBodyOverlayLock = (locked: boolean) => {
  document.documentElement.classList.toggle('muses-overlay-open', locked)
  document.body.classList.toggle('muses-overlay-open', locked)
}

watch(hasGlobalOverlay, (locked) => {
  syncBodyOverlayLock(locked)
}, { immediate: true })

watch(playerOverlayVisible, (visible) => {
  syncPlayerStatusBar(visible)
})

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

    // 仅退到后台，不 destroy Activity，避免 media-session 前台服务随 unbind 被销毁。
    void App.minimizeApp().catch(() => {
      // 非 Android / 不可用时静默忽略，避免打断 UI。
    })
  })
})

onUnmounted(() => {
  syncBodyOverlayLock(false)
  syncPlayerStatusBar(false)
})
</script>

<style scoped>
.app-mini-player.is-overlay-active {
  pointer-events: none;
}

/* 打开全局 overlay 时彻底切断底层路由页交互与滚动，避免触摸穿透。 */
ion-app.has-global-overlay .app-router-outlet {
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

<style>
html.muses-overlay-open,
body.muses-overlay-open {
  overflow: hidden !important;
  overscroll-behavior: none;
}

/* 只锁路由页内容，不锁队列 overlay 自己的 ion-content。 */
body.muses-overlay-open ion-router-outlet ion-content {
  --overflow: hidden;
  pointer-events: none;
  overscroll-behavior: none;
}
</style>
