<template>
  <div
    class="mini-player"
    :class="{ 'is-empty': !playerState.currentSong }"
    role="button"
    tabindex="0"
    aria-label="打开沉浸式播放器"
    :aria-disabled="!playerState.currentSong"
    @click="openPlayerPage"
    @keyup.enter="openPlayerPage"
    @keyup.space="openPlayerPage"
  >
    <m-cover :src="coverSrc" :size="48" alt="" />

    <div class="track-info">
      <strong>{{ titleText }}</strong>
      <span>{{ subtitleText }}</span>
    </div>

    <div class="player-actions">
      <ion-button
        fill="clear"
        size="small"
        :disabled="!playerState.currentSong || playerState.status === 'loading'"
        :aria-label="isPlaying ? '暂停播放' : '继续播放'"
        @click.stop="togglePlayback"
        @keyup.enter.stop
        @keyup.space.stop
      >
        <ion-icon slot="icon-only" :icon="isPlaying ? pause : play" />
      </ion-button>
      <ion-button
        fill="clear"
        size="small"
        aria-label="打开播放队列"
        @click.stop="openQueuePage"
        @keyup.enter.stop
        @keyup.space.stop
      >
        <ion-icon slot="icon-only" :icon="list" />
      </ion-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Capacitor } from '@capacitor/core'
import { IonButton, IonIcon } from '@ionic/vue'
import { list, pause, play } from '@/icons/ion-lucide'
import { MCover } from '@/components/ui'
import { isPlaying, pausePlayback, playerState, resumePlayback } from '@/features/player/controller'
import { openPlayerOverlay, openQueueOverlay } from '@/features/player/overlay'

const titleText = computed(() => playerState.currentSong?.title || '暂无播放歌曲')
const subtitleText = computed(() => {
  const song = playerState.currentSong
  if (!song) {
    return '未知艺术家 - 未知专辑'
  }

  return `${song.artist || '未知艺术家'} - ${song.album || '未知专辑'}`
})
const currentCoverUri = computed(() => playerState.coverUri || playerState.currentSong?.coverUri || '')
const coverSrc = computed(() => toDisplayableUri(currentCoverUri.value))

const openPlayerPage = (event: MouseEvent | KeyboardEvent) => {
  if (isPlayerActionEvent(event)) {
    return
  }

  if (!playerState.currentSong) {
    return
  }

  openPlayerOverlay()
}

const isPlayerActionEvent = (event: MouseEvent | KeyboardEvent): boolean => {
  return event.composedPath().some((target) => {
    return target instanceof Element && target.classList.contains('player-actions')
  })
}

const openQueuePage = () => {
  openQueueOverlay()
}

const togglePlayback = async () => {
  if (!playerState.currentSong) {
    return
  }

  if (isPlaying.value) {
    await pausePlayback()
    return
  }

  await resumePlayback()
}

const toDisplayableUri = (uri: string): string => {
  const normalizedUri = uri.trim().toLowerCase()
  if (!uri || normalizedUri.startsWith('data:') || normalizedUri.startsWith('blob:') || normalizedUri.includes(';base64,')) {
    return ''
  }

  return normalizedUri.startsWith('http://') || normalizedUri.startsWith('https://')
    ? uri
    : Capacitor.convertFileSrc(uri)
}
</script>

<style scoped>
.mini-player {
  position: fixed;
  cursor: pointer;
  left: 0;
  right: 0;
  bottom: calc(var(--muses-tab-bar-height) + var(--ion-safe-area-bottom, 0px));
  z-index: var(--muses-z-mini-player);
  display: flex;
  align-items: center;
  gap: var(--muses-space-md);
  width: 100%;
  min-height: var(--muses-mini-player-height);
  padding: var(--muses-space-sm) var(--muses-space-md);
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  color: var(--muses-color-ink);
  background: var(--muses-color-surface);
}

.mini-player.is-empty {
  cursor: default;
}

.track-info {
  min-width: 0;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}

.track-info strong,
.track-info span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.track-info strong {
  font-size: var(--muses-font-title);
  line-height: var(--muses-line-height-title);
}

.track-info span {
  color: var(--ion-color-medium);
  font-size: var(--muses-font-body-sm);
}

.player-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 2px;
}

@media (prefers-color-scheme: dark) {
  .mini-player {
    border-top-color: rgba(255, 255, 255, 0.12);
    background: var(--muses-color-surface-dark);
  }
}

/* 宽屏无底部 Tab Bar，贴底仅保留安全区，避免 64px 悬空 */
@media (min-width: 768px) {
  .mini-player {
    bottom: var(--ion-safe-area-bottom, 0px);
  }
}
</style>
