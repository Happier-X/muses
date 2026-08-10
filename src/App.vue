<template>
  <k-app theme="ios" class="flex flex-col h-full overflow-hidden">
    <div
      class="flex-1 relative overflow-hidden"
      :class="{ 'pointer-events-none': hasGlobalOverlay }"
    >
      <RouterView />
    </div>
    <MiniPlayer
      :class="{ 'pointer-events-none': hasGlobalOverlay, 'is-overlay-active': hasGlobalOverlay }"
      :aria-hidden="hasGlobalOverlay"
    />
    <!-- 常驻 PlayerPage：k-popup 关闭时 v-show 保活，避免关再开重建 AMLL 背景闪默认底（#22） -->
    <PlayerPage />
    <QueuePage />
  </k-app>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, onUnmounted, watch } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import { kApp } from 'konsta/vue'
import { App } from '@capacitor/app'
import type { PluginListenerHandle } from '@capacitor/core'
import { StatusBar, Style } from '@capacitor/status-bar'
import MiniPlayer from '@/components/MiniPlayer.vue'
import { initializePlayer } from '@/features/player/controller'
import { closePlayerOverlay, closeQueueOverlay, playerOverlayVisible, queueOverlayVisible } from '@/features/player/overlay'

const PlayerPage = defineAsyncComponent(() => import('@/views/PlayerPage.vue'))
const QueuePage = defineAsyncComponent(() => import('@/views/QueuePage.vue'))
const router = useRouter()
const hasGlobalOverlay = computed(() => playerOverlayVisible.value || queueOverlayVisible.value)
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

    // 路由可后退时返回上一页（专辑/歌单等详情页手势返回），否则退到后台
    if (router.options.history.state.back !== null) {
      router.back()
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
