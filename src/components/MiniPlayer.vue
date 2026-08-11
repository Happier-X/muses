<template>
  <k-glass
    component="div"
    class="fixed left-4 right-4 bottom-safe-24 z-[1000] flex h-16 items-center gap-[12px] rounded-full px-[12px] text-black dark:text-white md:bottom-safe-2"
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
  </k-glass>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { Capacitor } from '@capacitor/core'
import { list, pause, play } from '@/icons'
import { kButton, kGlass, MCover } from '@/components/ui'
import { isPlaying, pausePlayback, playerState, resumePlayback } from '@/features/player/controller'
import { openPlayerOverlay, openQueueOverlay } from '@/features/player/overlay'

const titleText = computed(() => playerState.currentSong?.title || '暂无播放歌曲')

// MiniPlayer 可见性 → html.muses-mini-visible → 各页列表内容底部 padding 动态调整（CSS 变量 --content-pb）
// 对齐 iOS contentInset 做法：有播放 = MiniPlayer 顶(160px)，无播放 = tabbar 顶(80px)，无多余灰带
const syncMiniVisible = (hasSong: boolean) =>
  document.documentElement.classList.toggle('muses-mini-visible', hasSong)
onMounted(() => syncMiniVisible(!!playerState.currentSong))
watch(
  () => !!playerState.currentSong,
  (has) => syncMiniVisible(has),
)
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
