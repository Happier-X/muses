<template>
  <div
    class="fixed left-0 right-0 bottom-[calc(96px+var(--safe-area-inset-bottom,env(safe-area-inset-bottom,0px)))] z-[1000] flex items-center gap-[12px] w-full min-h-[64px] py-[8px] px-[12px] border-t border-t-black/10 text-black bg-white dark:border-t-white/15 dark:text-white dark:bg-[#1f1f1f] md:bottom-[var(--safe-area-inset-bottom,env(safe-area-inset-bottom,0px))]"
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

    <div class="min-w-0 flex flex-1 flex-col gap-[3px]">
      <strong class="truncate text-[15px] leading-[1.25] text-black dark:text-white">{{ titleText }}</strong>
      <span class="truncate text-[13px] text-black/55 dark:text-white/55">{{ subtitleText }}</span>
    </div>

    <div class="flex shrink-0 items-center gap-[2px]">
      <k-button
        component="button"
        clear
        rounded-full
        class="m-0 size-10 shrink-0 text-black dark:text-white"
        :aria-label="isPlaying ? '暂停播放' : '继续播放'"
        :disabled="!playerState.currentSong || playerState.status === 'loading'"
        @click.stop="togglePlayback"
      >
        <component :is="isPlaying ? pause : play" aria-hidden="true" class="size-5 fill-current stroke-none" />
      </k-button>
      <k-button
        component="button"
        clear
        rounded-full
        class="m-0 size-10 shrink-0 text-black dark:text-white"
        aria-label="打开播放队列"
        @click.stop="openQueuePage"
      >
        <component :is="list" aria-hidden="true" class="size-5" />
      </k-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Capacitor } from '@capacitor/core'
import { list, pause, play } from '@/icons'
import { kButton, MCover } from '@/components/ui'
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

const openPlayerPage = () => {
  if (!playerState.currentSong) {
    return
  }

  openPlayerOverlay()
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
