<template>
  <div class="m-app">
    <div
      class="m-app__stage"
      :class="{ 'm-app__stage--locked': hasGlobalOverlay }"
    >
      <RouterView />
    </div>
    <MiniPlayer
      :class="{ 'm-app__mini-locked': hasGlobalOverlay }"
      :aria-hidden="hasGlobalOverlay"
    />
    <!-- 常驻 PlayerPage：m-popup 关闭时 v-show 保活，避免关再开重建 AMLL 背景闪默认底（#22） -->
    <PlayerPage />
    <QueuePage />
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, onUnmounted, watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { App } from '@capacitor/app'
import type { PluginListenerHandle } from '@capacitor/core'
import { StatusBar, Style } from '@capacitor/status-bar'
import MiniPlayer from '@/components/MiniPlayer.vue'
import { initializePlayer } from '@/features/player/controller'
import { closePlayerOverlay, closeQueueOverlay, playerOverlayVisible, queueOverlayVisible } from '@/features/player/overlay'

const PlayerPage = defineAsyncComponent(() => import('@/views/PlayerPage.vue'))
const QueuePage = defineAsyncComponent(() => import('@/views/QueuePage.vue'))
const router = useRouter()
const route = useRoute()
/** 一级 tab 页（歌曲/分类/音源/设置）返回 = 退出应用；二级页（详情等）返回上一页 */
const topLevelPaths = ['/tabs/songs', '/tabs/categories', '/tabs/sources', '/tabs/settings']
const isTopLevelPage = computed(() => topLevelPaths.includes(route.path))
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

    // 二级页（专辑/歌单详情）返回上一页；一级 tab 页直接退出应用（退到桌面，不销毁进程保播放）
    if (!isTopLevelPage.value) {
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

<style scoped lang="scss">
.m-app {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  color: var(--m-text);
  background-color: var(--m-surface);

  &__stage {
    position: relative;
    flex: 1;
    overflow: hidden;
  }

  &__stage--locked,
  &__mini-locked {
    pointer-events: none;
  }
}
</style>