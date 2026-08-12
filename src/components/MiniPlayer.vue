<template>
  <div
    class="mini-player"
    :class="{
      'mini-player--empty': !playerState.currentSong,
    }"
    role="button"
    tabindex="0"
    aria-label="打开沉浸式播放器"
    :aria-disabled="!playerState.currentSong"
    @click="openPlayerPage"
    @keyup.enter="openPlayerPage"
    @keyup.space="openPlayerPage"
  >
    <div class="mini-player__blur" aria-hidden="true" />
    <div class="mini-player__glass" aria-hidden="true" />

    <div class="mini-player__row">
      <m-cover :src="coverSrc" :size="48" alt="" />

      <div class="mini-player__info">
        <strong class="mini-player__title">{{ titleText }}</strong>
        <span class="mini-player__subtitle">{{ subtitleText }}</span>
      </div>

      <div class="mini-player__controls">
        <m-button
          component="button"
          variant="clear"
          rounded-full
          class="mini-player__btn"
          :aria-label="isPlaying ? '暂停播放' : '继续播放'"
          :disabled="!playerState.currentSong || playerState.status === 'loading'"
          @click.stop="togglePlayback"
        >
          <component :is="isPlaying ? pause : play" aria-hidden="true" class="mini-player__icon" />
        </m-button>
        <m-button
          component="button"
          variant="clear"
          rounded-full
          class="mini-player__btn"
          aria-label="打开播放队列"
          @click.stop="openQueuePage"
        >
          <component :is="list" aria-hidden="true" class="mini-player__icon" />
        </m-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { Capacitor } from '@capacitor/core'
import { list, pause, play } from '@/icons'
import { MButton, MCover } from '@/components/ui'
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

<style scoped lang="scss">
/* 玻璃胶囊 MiniPlayer：白玻璃（blur 2px）+ surface 渐变 + 暗色黑玻璃（WebView 兼容值写法） */
.mini-player {
  position: fixed;
  left: 16px;
  right: 16px;
  bottom: calc(96px + var(--m-safe-area-bottom, 0px));
  z-index: 1000;
  height: 64px;
  border-radius: 9999px;
  color: var(--m-text);
  background-color: transparent;
  cursor: pointer;
  box-sizing: border-box;

  @media (min-width: 768px) {
    bottom: calc(8px + var(--m-safe-area-bottom, 0px));
  }

  &--empty {
    cursor: default;
  }

  &__blur,
  &__glass {
    position: absolute;
    inset: 0;
    border-radius: 9999px;
    pointer-events: none;
  }

  &__blur {
    -webkit-backdrop-filter: blur(2px);
    backdrop-filter: blur(2px);
  }

  &__glass {
    background: linear-gradient(
      to bottom,
      var(--m-surface) 0%,
      rgba(239, 239, 244, 0.4) 50%,
      rgba(255, 255, 255, 0) 100%
    );
  }

  &__row {
    position: relative;
    display: flex;
    align-items: center;
    height: 100%;
    gap: 12px;
    padding: 0 12px;
    box-sizing: border-box;
  }

  &__info {
    display: flex;
    flex-direction: column;
    gap: 3px;
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: 15px;
    line-height: 1.25;
    font-weight: 600;
    color: var(--m-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__subtitle {
    font-size: 13px;
    line-height: 1.3;
    color: var(--m-text-2);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__controls {
    display: flex;
    align-items: center;
    gap: 2px;
    flex-shrink: 0;
  }

  /* clear 变体按钮：MiniPlayer 场景用文字色而非主题色（覆盖 MButton 默认） */
  &__btn {
    width: 40px;
    height: 40px;
    padding: 0;
    flex: 0 0 40px;
    color: var(--m-text);

    &:active {
      background-color: rgba(0, 0, 0, 0.1);
    }
  }

  &__icon {
    width: 20px;
    height: 20px;
  }
}

:global(.dark) .mini-player__glass {
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.5) 0%,
    rgba(0, 0, 0, 0.4) 50%,
    rgba(0, 0, 0, 0) 100%
  );
}

:global(.dark) .mini-player__btn:active {
  background-color: rgba(255, 255, 255, 0.1);
}
</style>