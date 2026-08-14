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
          <!-- lucide 实心图标（fill=currentColor 填充，对齐椒盐实心三角） -->
          <component
            :is="isPlaying ? pause : play"
            aria-hidden="true"
            class="mini-player__icon"
            fill="currentColor"
          />
        </m-button>
        <m-button
          component="button"
          variant="clear"
          rounded-full
          class="mini-player__btn"
          aria-label="打开播放队列"
          @click.stop="openQueuePage"
        >
          <!-- lucide 实心队列图标 -->
          <component
            :is="listMusic"
            aria-hidden="true"
            class="mini-player__icon"
            fill="currentColor"
          />
        </m-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { Capacitor } from '@capacitor/core'
import { list as listMusic, pause, play } from '@/icons'
import { MButton, MCover } from '@/components/ui'
import { isPlaying, pausePlayback, playerState, resumePlayback } from '@/features/player/controller'
import { openPlayerOverlay, openQueueOverlay } from '@/features/player/overlay'

const titleText = computed(() => playerState.currentSong?.title || '暂无播放歌曲')

// 保留 MiniPlayer 播放态标记供现有样式/扩展使用；组件无歌曲时仍显示空状态。
// 页面内容始终避让 64px MiniPlayer + 底部安全区，不再预留底部导航高度。
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
.mini-player {
  position: fixed;
  left: 18px;
  right: 18px;
  bottom: calc(var(--m-safe-area-bottom, 0px) + 8px);
  z-index: 1000;
  height: 64px;
  color: var(--m-text);
  background: rgba(255, 255, 255, 0.72); /* 液态玻璃：白色 72% 底 */
  border-radius: 40px; /* 大圆角胶囊（椒盐实测圆角 ~24dp，视觉近满圆） */
  cursor: pointer;
  box-sizing: border-box;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1); /* 悬浮阴影 */
  border: 1px solid rgba(255, 255, 255, 0.5); /* 微妙白边 */
  -webkit-backdrop-filter: blur(20px);
  backdrop-filter: blur(20px);


  &--empty {
    cursor: default;
  }

  &__row {
    position: relative;
    display: flex;
    align-items: center;
    height: 100%;
    gap: var(--m-spacing-sub);
    padding: 0 var(--m-spacing);
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
    position: relative;
    width: 40px;
    height: 40px;
    padding: 0;
    flex: 0 0 40px;
    /* 对齐椒盐：透明底，图标为深色实心（椒盐播放三角/队列图标无圆底） */
    color: #211715;

    /* 覆盖 MButton clear 的浅蓝 active 底：统一为透明度反馈 */
    &:active {
      opacity: 0.6;
      background-color: transparent !important;
    }
  }

  &__icon {
    width: 18px;
    height: 18px;
    z-index: 1;
  }
}

:global(.dark) .mini-player__btn:active {
  opacity: 0.6;
  background-color: transparent !important;
}
</style>