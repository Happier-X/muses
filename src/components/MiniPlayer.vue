<template>
  <div
    class="mini-player fixed left-0 right-0 bottom-[calc(var(--muses-tab-bar-height)+env(safe-area-inset-bottom,0px))] z-[var(--muses-z-mini-player)] flex items-center gap-[var(--muses-space-md)] w-full min-h-[var(--muses-mini-player-height)] py-[var(--muses-space-sm)] px-[var(--muses-space-md)] border-t border-t-[rgba(0,0,0,0.08)] text-[color:var(--muses-color-ink)] bg-[var(--muses-color-surface)] dark:border-t-[rgba(255,255,255,0.12)] dark:bg-[var(--muses-color-surface-dark)] md:bottom-[env(safe-area-inset-bottom,0px)]"
    :class="{
      'cursor-pointer': !!playerState.currentSong,
      'cursor-default': !playerState.currentSong,
      'is-empty': !playerState.currentSong
    }"
    role="button"
    tabindex="0"
    aria-label="打开沉浸式播放器"
    :aria-disabled="!playerState.currentSong"
    @click="openPlayerPage"
    @keyup.enter="openPlayerPage"
    @keyup.space="openPlayerPage"
  >
    <m-cover :src="coverSrc" :size="48" alt="" />

    <div class="track-info min-w-0 flex flex-1 flex-col gap-[3px]">
      <strong class="truncate text-[length:var(--muses-font-title)] leading-[var(--muses-line-height-title)]">{{ titleText }}</strong>
      <span class="truncate text-[color:var(--muses-color-ink-muted)] text-[length:var(--muses-font-body-sm)]">{{ subtitleText }}</span>
    </div>

    <div class="player-actions flex shrink-0 items-center gap-[var(--muses-space-xs)]">
      <h-button
        variant="ghost"
        is-icon-only
        shape="circle"
        :aria-label="isPlaying ? '暂停播放' : '继续播放'"
        :disabled="!playerState.currentSong || playerState.status === 'loading'"
        @click.stop="togglePlayback"
      >
        <h-icon :icon="isPlaying ? pause : play" variant="fill" />
      </h-button>
      <h-button variant="ghost" is-icon-only shape="circle" aria-label="打开播放队列" @click.stop="openQueuePage">
        <h-icon :icon="list" />
      </h-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Capacitor } from '@capacitor/core'
import { list, pause, play } from '@/icons'
import { HButton, HIcon, MCover } from '@/components/ui'
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
