<template>
  <div class="flex flex-col h-full overflow-hidden">
    <div
      class="flex-1 relative overflow-hidden"
      :class="{ 'pointer-events-none': hasGlobalOverlay }"
    >
      <RouterView />
    </div>
    <MiniPlayer
      class="app-mini-player"
      :class="{ 'pointer-events-none': hasGlobalOverlay, 'is-overlay-active': hasGlobalOverlay }"
      :aria-hidden="hasGlobalOverlay"
    />
    <!-- 有当前曲时保活 PlayerPage，避免关闭再打开重建 BackgroundRender 闪默认底（#22） -->
    <PlayerPage
      v-if="keepPlayerPageMounted"
      class="app-player-page transition-transform duration-[220ms] ease-[ease]"
      :class="[
        playerOverlayVisible
          ? 'translate-y-0 pointer-events-auto visible [contain:none]'
          : 'translate-y-full pointer-events-none invisible [contain:paint]',
        { 'is-player-visible': playerOverlayVisible }
      ]"
    />
    <Transition
      enter-active-class="transition-transform duration-[220ms] ease-[ease]"
      enter-from-class="translate-y-full"
      leave-active-class="transition-transform duration-[220ms] ease-[ease]"
      leave-to-class="translate-y-full"
    >
      <QueuePage v-if="queueOverlayVisible" />
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, onUnmounted, watch } from 'vue'
import { RouterView } from 'vue-router'
import { App } from '@capacitor/app'
import type { PluginListenerHandle } from '@capacitor/core'
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
let backButtonListener: PluginListenerHandle | null = null
let appUnmounted = false

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
  appUnmounted = false
  void initializePlayer()

  void App.addListener('backButton', () => {
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
  }).then((handle) => {
    if (appUnmounted) {
      void handle.remove()
      return
    }
    backButtonListener = handle
  }).catch(() => undefined)
})

onUnmounted(() => {
  appUnmounted = true
  void backButtonListener?.remove()
  backButtonListener = null
  syncBodyOverlayLock(false)
  syncPlayerStatusBar(false)
})
</script>
