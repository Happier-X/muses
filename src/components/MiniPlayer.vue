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
    <div class="cover-wrap" aria-hidden="true">
      <img v-if="coverSrc" :src="coverSrc" alt="" />
      <ion-icon v-else :icon="musicalNotes" />
    </div>

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
import { list, musicalNotes, pause, play } from 'ionicons/icons'
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
  bottom: calc(64px + var(--ion-safe-area-bottom, 0px));
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 64px;
  padding: 8px 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  color: var(--ion-text-color);
  background: #ffffff;
}

.mini-player.is-empty {
  cursor: default;
}

.cover-wrap {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 10px;
  background: rgba(var(--ion-color-medium-rgb), 0.16);
  color: var(--ion-color-medium);
}

.cover-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-wrap ion-icon {
  font-size: 24px;
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
  font-size: 15px;
  line-height: 1.25;
}

.track-info span {
  color: var(--ion-color-medium);
  font-size: 13px;
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
    background: #1f1f1f;
  }
}

/* 宽屏无底部 Tab Bar，贴底仅保留安全区，避免 64px 悬空 */
@media (min-width: 768px) {
  .mini-player {
    bottom: var(--ion-safe-area-bottom, 0px);
  }
}
</style>
