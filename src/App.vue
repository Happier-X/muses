<template>
  <ion-app :class="{ 'has-global-overlay': hasGlobalOverlay }">
    <ion-router-outlet class="app-router-outlet" />
    <MiniPlayer
      class="app-mini-player"
      :class="{ 'is-overlay-active': hasGlobalOverlay }"
      :aria-hidden="hasGlobalOverlay"
    />
    <!-- 有当前曲时保活 PlayerPage，避免关闭再打开重建 BackgroundRender 闪默认底（#22） -->
    <PlayerPage
      v-if="keepPlayerPageMounted"
      class="app-player-page"
      :class="{ 'is-player-visible': playerOverlayVisible }"
    />
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
import { initializePlayer, playerState } from '@/features/player/controller'
import { closePlayerOverlay, closeQueueOverlay, playerOverlayVisible, queueOverlayVisible } from '@/features/player/overlay'

const PlayerPage = defineAsyncComponent(() => import('@/views/PlayerPage.vue'))
const QueuePage = defineAsyncComponent(() => import('@/views/QueuePage.vue'))
const hasGlobalOverlay = computed(() => playerOverlayVisible.value || queueOverlayVisible.value)
/** 播放中/有当前曲时不卸载沉浸页，仅隐藏，保留 AMLL 动态背景 */
const keepPlayerPageMounted = computed(
  () => playerOverlayVisible.value || !!playerState.currentSong,
)
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

.queue-overlay-enter-active,
.queue-overlay-leave-active {
  transition: transform 220ms ease;
}

.queue-overlay-enter-from,
.queue-overlay-leave-to {
  transform: translateY(100%);
}

/* 关闭态：移出视口但保持挂载，背景不销毁 */
.app-player-page:not(.is-player-visible) {
  transform: translateY(100%);
  pointer-events: none;
  /* 仍参与合成层，避免再次打开时整页白闪 */
  visibility: visible;
}

.app-player-page {
  transition: transform 220ms ease;
}

.app-player-page.is-player-visible {
  transform: translateY(0);
  pointer-events: auto;
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
