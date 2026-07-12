<template>
  <div
    class="player-overlay"
    @touchstart.passive="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
    @touchcancel="onTouchEnd"
  >
    <div
      class="immersive-shell"
      :class="{ 'is-dragging': isDraggingVertically }"
      :style="{ transform: `translateY(${dragOffsetY}px)` }"
    >
      <div v-if="hasLyrics" class="amll-background">
        <BackgroundRender
          class="amll-background-render"
          :album="coverSrc || undefined"
          :album-is-video="false"
          :flow-speed="2"
          :has-lyric="hasLyrics"
          :renderer="meshGradientRenderer"
        />
      </div>
      <div class="fallback-background" />

      <section v-if="!playerState.currentSong" class="empty-state">
        <div class="placeholder-cover">♪</div>
        <h1>暂无播放歌曲</h1>
        <p>从歌曲列表选择一首音乐后，即可进入沉浸式播放。</p>
      </section>

      <div
        v-else
        class="panels"
        :style="{ transform: `translateX(-${activePanel * 50}%)` }"
      >
        <section class="panel info-panel" aria-label="播放控制页">
          <div class="info-panel-inner">
            <div class="cover-slot">
              <img v-if="coverSrc" class="cover" :src="coverSrc" alt="歌曲封面" />
              <div v-else class="cover placeholder-cover">♪</div>
            </div>

            <div class="song-info">
              <h1>{{ playerState.currentSong.title }}</h1>
              <p>{{ subtitle }}</p>
              <small v-if="playerState.metadataStatus === 'scanning'">正在补充歌曲信息…</small>
              <small v-else-if="playerState.metadataStatus === 'failed'">歌曲信息补充失败，已使用当前信息播放。</small>
            </div>

            <div
              class="progress-area"
              @touchstart.stop="onProgressGestureStart"
              @touchmove.stop
              @touchend.stop="onProgressGestureEnd"
              @touchcancel.stop="onProgressGestureEnd"
              @pointerdown.stop="onProgressGestureStart"
              @pointerup.stop="onProgressGestureEnd"
              @pointercancel.stop="onProgressGestureEnd"
            >
              <input
                class="progress-slider"
                type="range"
                min="0"
                :max="durationForSlider"
                step="0.1"
                :value="playerState.position"
                :disabled="!canSeek"
                :style="{ '--progress': progressPercent }"
                aria-label="播放进度"
                @change="onSeek"
              />
              <div class="time-row">
                <span>{{ formatTime(playerState.position) }}</span>
                <span>{{ playerState.duration ? formatTime(playerState.duration) : '--:--' }}</span>
              </div>
            </div>

            <div class="controls">
              <ion-button fill="clear" color="light" shape="round" aria-label="上一曲" @click="onPrevious">
                <ion-icon slot="icon-only" :icon="previousIcon" />
              </ion-button>
              <ion-button
                class="play-toggle"
                fill="clear"
                color="light"
                shape="round"
                :disabled="playerState.status === 'loading'"
                aria-label="播放或暂停"
                @click="togglePlayback"
              >
                <ion-icon slot="icon-only" :icon="isPlaying ? pause : play" />
              </ion-button>
              <ion-button fill="clear" color="light" shape="round" aria-label="下一曲" @click="onNext">
                <ion-icon slot="icon-only" :icon="nextIcon" />
              </ion-button>
            </div>

            <div class="mode-bar">
              <ion-button
                fill="clear"
                color="light"
                shape="round"
                class="mode-button"
                :class="{ 'is-active': queueState.repeatMode === 'one' }"
                :aria-label="repeatModeLabel"
                @click="onToggleRepeat"
              >
                <ion-icon slot="icon-only" :icon="repeatIcon" />
              </ion-button>

              <ion-button
                fill="clear"
                color="light"
                shape="round"
                class="mode-button"
                :class="{ 'is-active': queueState.shuffleEnabled }"
                :aria-label="shuffleModeLabel"
                @click="onToggleShuffle"
              >
                <ion-icon slot="icon-only" :icon="shuffleIcon" />
              </ion-button>

              <ion-button
                fill="clear"
                color="light"
                shape="round"
                class="mode-button"
                aria-label="播放队列"
                @click="goToQueue"
              >
                <ion-icon slot="icon-only" :icon="listIcon" />
              </ion-button>
            </div>
          </div>
        </section>

        <section class="panel lyric-panel" aria-label="歌词页">
          <header v-if="playerState.currentSong" class="lyric-header">
            <h2 class="lyric-title">{{ playerState.currentSong.title }}</h2>
            <p v-if="lyricArtist" class="lyric-artist">{{ lyricArtist }}</p>
          </header>

          <template v-if="hasLyrics">
            <LyricPlayer
              class="lyric-player"
              :lyric-lines="lyricLines"
              :current-time="playerState.position * 1000"
              align-anchor="center"
              :align-position="0.38"
              :enable-spring="true"
              :enable-blur="true"
              :enable-scale="true"
              :word-fade-width="0.5"
            />
          </template>
          <div v-else class="lyric-empty">
            <h2>暂无歌词</h2>
            <p>当前歌曲没有内嵌歌词，也没有找到同目录同名 .lrc 文件。</p>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { Capacitor } from '@capacitor/core'
import { IonButton, IonIcon } from '@ionic/vue'
import { listOutline, pause, play, playSkipBack, playSkipForward, repeat, repeatOutline, shuffle } from 'ionicons/icons'
import { BackgroundRender, LyricPlayer } from '@applemusic-like-lyrics/vue'
import { MeshGradientRenderer } from '@applemusic-like-lyrics/core'
import type { LyricLine } from '@applemusic-like-lyrics/core'
import { parseLrc } from '@applemusic-like-lyrics/lyric'
import '@applemusic-like-lyrics/core/style.css'
import { isPlaying, pausePlayback, playerState, playNextFromQueue, playPreviousFromQueue, queueState, resumePlayback, seekPlayback, setRepeatMode, toggleShuffle } from '@/features/player/controller'
import { closePlayerOverlay, openQueueOverlay } from '@/features/player/overlay'

const activePanel = ref(0)
const touchStartX = ref<number | null>(null)
const touchStartY = ref<number | null>(null)
const dragOffsetY = ref(0)
const gestureDirection = ref<'horizontal' | 'vertical' | null>(null)
const isDraggingVertically = ref(false)
const canDragDown = ref(false)
/** 进度条交互中或结束后的短保护期，防止松手穿透到上一曲/下一曲或横向切面板。 */
const seekGestureLocked = ref(false)
let seekUnlockTimer: ReturnType<typeof setTimeout> | null = null
const SEEK_CLICK_GUARD_MS = 300
const meshGradientRenderer = MeshGradientRenderer

const repeatModeLabel = computed(() => queueState.repeatMode === 'one' ? '单曲循环' : '列表循环')
const repeatIcon = computed(() => queueState.repeatMode === 'one' ? repeat : repeatOutline)
const shuffleModeLabel = computed(() => queueState.shuffleEnabled ? '随机播放' : '顺序播放')
const shuffleIcon = computed(() => shuffle)
const listIcon = listOutline
const previousIcon = playSkipBack
const nextIcon = playSkipForward

const onToggleRepeat = () => {
  setRepeatMode(queueState.repeatMode === 'one' ? 'all' : 'one')
}

const onToggleShuffle = () => {
  toggleShuffle()
}

const goToQueue = () => {
  openQueueOverlay()
}

const clearSeekUnlockTimer = () => {
  if (seekUnlockTimer !== null) {
    clearTimeout(seekUnlockTimer)
    seekUnlockTimer = null
  }
}

const lockSeekGesture = () => {
  seekGestureLocked.value = true
  clearSeekUnlockTimer()
}

const scheduleSeekUnlock = () => {
  clearSeekUnlockTimer()
  seekUnlockTimer = setTimeout(() => {
    seekGestureLocked.value = false
    seekUnlockTimer = null
  }, SEEK_CLICK_GUARD_MS)
}

const onProgressGestureStart = () => {
  lockSeekGesture()
  // 进度条手势与 overlay 全局手势隔离：清空已记录的触点，避免半成品横向/纵向手势。
  touchStartX.value = null
  touchStartY.value = null
  gestureDirection.value = null
  canDragDown.value = false
  isDraggingVertically.value = false
  dragOffsetY.value = 0
}

const onProgressGestureEnd = () => {
  scheduleSeekUnlock()
}

const onPrevious = () => {
  if (seekGestureLocked.value) {
    return
  }
  void playPreviousFromQueue()
}

const onNext = () => {
  if (seekGestureLocked.value) {
    return
  }
  void playNextFromQueue()
}

const subtitle = computed(() => {
  const song = playerState.currentSong
  return [song?.artist, song?.album].filter(Boolean).join(' · ') || '未知歌手'
})

const lyricArtist = computed(() => playerState.currentSong?.artist?.trim() || '')

const currentLyrics = computed(() => playerState.lyrics || playerState.currentSong?.lyrics || '')
const currentCoverUri = computed(() => playerState.coverUri || playerState.currentSong?.coverUri || '')
const coverSrc = computed(() => toDisplayableUri(currentCoverUri.value))

const lyricLines = computed<LyricLine[]>(() => {
  if (!currentLyrics.value) {
    return []
  }

  try {
    return parseLrc(normalizeLrc(currentLyrics.value))
  } catch {
    return []
  }
})

const hasLyrics = computed(() => lyricLines.value.length > 0)
const canSeek = computed(() => playerState.duration > 0)
const durationForSlider = computed(() => playerState.duration || 1)
const progressPercent = computed(() => {
  const duration = durationForSlider.value
  if (duration <= 0) {
    return '0%'
  }
  const ratio = Math.min(1, Math.max(0, playerState.position / duration))
  return `${ratio * 100}%`
})

const resetDragState = () => {
  touchStartX.value = null
  touchStartY.value = null
  gestureDirection.value = null
  canDragDown.value = false
  isDraggingVertically.value = false
  dragOffsetY.value = 0
}

const goBack = () => {
  closePlayerOverlay()
}

const togglePlayback = async () => {
  if (isPlaying.value) {
    await pausePlayback()
    return
  }
  await resumePlayback()
}

const onSeek = async (event: Event) => {
  // change 可能在 pointerup 之后触发；再锁一次并续期 debounce，覆盖 click 穿透窗口。
  lockSeekGesture()
  scheduleSeekUnlock()
  const target = event.target as HTMLInputElement
  await seekPlayback(Number(target.value))
}

const onTouchStart = (event: TouchEvent) => {
  // 原生控件（进度条）或 seek 锁定期内，不启动 overlay 面板/下滑手势。
  if (seekGestureLocked.value || isNativeInteractiveTarget(event.target)) {
    touchStartX.value = null
    touchStartY.value = null
    gestureDirection.value = null
    isDraggingVertically.value = false
    canDragDown.value = false
    dragOffsetY.value = 0
    return
  }

  const touch = event.changedTouches[0]
  touchStartX.value = touch?.clientX ?? null
  touchStartY.value = touch?.clientY ?? null
  gestureDirection.value = null
  isDraggingVertically.value = false
  canDragDown.value = canStartVerticalDismiss(event)
}

const onTouchMove = (event: TouchEvent) => {
  const startX = touchStartX.value
  const startY = touchStartY.value
  const touch = event.changedTouches[0]
  if (startX === null || startY === null || !touch) {
    return
  }

  // 进度条需要原生拖动；其余区域一律拦截默认滚动，防止穿透到底层歌曲列表。
  if (!isNativeInteractiveTarget(event.target)) {
    event.preventDefault()
  }

  const deltaX = touch.clientX - startX
  const deltaY = touch.clientY - startY

  if (!gestureDirection.value && Math.max(Math.abs(deltaX), Math.abs(deltaY)) > 8) {
    gestureDirection.value = Math.abs(deltaY) > Math.abs(deltaX) ? 'vertical' : 'horizontal'
  }

  if (gestureDirection.value !== 'vertical' || !canDragDown.value) {
    return
  }

  const nextOffset = Math.max(0, deltaY)
  dragOffsetY.value = nextOffset
  isDraggingVertically.value = nextOffset > 0
}

const isNativeInteractiveTarget = (target: EventTarget | null): boolean => {
  if (!(target instanceof Element)) {
    return false
  }

  return Boolean(target.closest('input, textarea, select, [contenteditable="true"]'))
}

const onTouchEnd = (event: TouchEvent) => {
  const startX = touchStartX.value
  const endX = event.changedTouches[0]?.clientX
  const shouldDismiss = gestureDirection.value === 'vertical' && dragOffsetY.value >= getDismissThreshold()
  const skipPanelSwitch = seekGestureLocked.value || isNativeInteractiveTarget(event.target)

  touchStartX.value = null
  touchStartY.value = null
  gestureDirection.value = null
  canDragDown.value = false
  isDraggingVertically.value = false

  if (shouldDismiss) {
    goBack()
    return
  }

  if (dragOffsetY.value > 0) {
    dragOffsetY.value = 0
    return
  }

  // 进度条拖动期间/刚结束时，忽略横向位移，避免误切控制/歌词面板。
  if (skipPanelSwitch) {
    dragOffsetY.value = 0
    return
  }

  if (startX === null || endX === undefined || Math.abs(startX - endX) < 40) {
    return
  }
  activePanel.value = endX < startX ? 1 : 0
}

const canStartVerticalDismiss = (event: TouchEvent): boolean => {
  return !event.composedPath().some((target) => {
    if (!(target instanceof HTMLElement)) {
      return false
    }
    return target.scrollHeight > target.clientHeight && target.scrollTop > 0
  })
}

const getDismissThreshold = (): number => {
  return Math.min(160, Math.max(96, window.innerHeight * 0.18))
}

const toDisplayableUri = (uri: string): string => {
  if (!uri) {
    return ''
  }
  const normalizedUri = uri.trim().toLowerCase()
  if (normalizedUri.startsWith('data:') || normalizedUri.startsWith('blob:') || normalizedUri.includes(';base64,')) {
    return ''
  }

  return normalizedUri.startsWith('http://') || normalizedUri.startsWith('https://')
    ? uri
    : Capacitor.convertFileSrc(uri)
}

const normalizeLrc = (lyrics: string): string => {
  return lyrics.replace(/\[((?:\d+:)*\d+),(\d+)\]/g, '[$1.$2]')
}

const formatTime = (value: number): string => {
  const totalSeconds = Math.max(0, Math.floor(value))
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}

onUnmounted(() => {
  clearSeekUnlockTimer()
  seekGestureLocked.value = false
  resetDragState()
})
</script>

<style scoped>
.player-overlay {
  position: fixed;
  inset: 0;
  z-index: 1100;
  overflow: hidden;
  overscroll-behavior: none;
  /* 手势统一由脚本处理，避免浏览器把垂直滑动交给底层 ion-content。 */
  touch-action: none;
  color: #fff;
}

.immersive-shell {
  position: relative;
  height: 100dvh;
  max-height: 100dvh;
  overflow: hidden;
  background: #05070d;
  transition: transform 220ms ease;
}

.immersive-shell.is-dragging {
  transition: none;
}

.amll-background,
.fallback-background {
  position: absolute;
  inset: 0;
}

.amll-background {
  z-index: 0;
  overflow: hidden;
  opacity: 0.75;
}

.amll-background-render {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
}

.fallback-background {
  z-index: 0;
  background:
    radial-gradient(circle at 50% 18%, rgba(148, 120, 255, 0.28), transparent 42%),
    linear-gradient(165deg, #171b2b 0%, #0a0c14 48%, #05070d 100%);
}

.amll-background + .fallback-background {
  opacity: 0;
}

.empty-state,
.panel {
  box-sizing: border-box;
  height: 100%;
  max-height: 100%;
  overflow: hidden;
  padding:
    calc(16px + var(--ion-safe-area-top, 0px))
    24px
    calc(16px + var(--ion-safe-area-bottom, 0px));
}

.empty-state,
.info-panel,
.lyric-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.info-panel {
  justify-content: stretch;
}

.info-panel-inner {
  width: min(100%, 420px);
  height: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 0;
  overflow: hidden;
}

.panels {
  position: relative;
  z-index: 1;
  display: flex;
  width: 200%;
  height: 100%;
  max-height: 100%;
  overflow: hidden;
  transition: transform 220ms ease;
}

.panel {
  width: 50%;
  min-width: 50%;
  flex: 0 0 50%;
}

.cover-slot {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 0;
  max-height: min(52dvh, 340px);
}

.cover {
  width: min(72vw, 100%, 340px);
  max-width: 100%;
  max-height: 100%;
  aspect-ratio: 1;
  height: auto;
  border-radius: clamp(18px, 4vw, 28px);
  object-fit: cover;
}

.placeholder-cover {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0.06));
  color: rgba(255, 255, 255, 0.8);
  font-size: clamp(48px, 12vw, 72px);
}

.song-info {
  flex: 0 0 auto;
  width: 100%;
  margin: 0;
  text-align: left;
  min-width: 0;
}

.song-info h1 {
  margin: 0 0 4px;
  font-size: clamp(20px, 5.6vw, 28px);
  line-height: 1.2;
  font-weight: 700;
  letter-spacing: 0.01em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-info p,
.song-info small,
.time-row {
  color: rgba(255, 255, 255, 0.68);
}

.song-info p {
  margin: 0;
  font-size: clamp(13px, 3.6vw, 15px);
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-info small {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-area {
  flex: 0 0 auto;
  width: 100%;
}

.progress-slider {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 24px;
  margin: 0;
  background: transparent;
  cursor: pointer;
  touch-action: manipulation;
}

.progress-slider:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.progress-slider::-webkit-slider-runnable-track {
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(
    to right,
    rgba(255, 255, 255, 0.92) 0%,
    rgba(255, 255, 255, 0.92) var(--progress, 0%),
    rgba(255, 255, 255, 0.22) var(--progress, 0%),
    rgba(255, 255, 255, 0.22) 100%
  );
}

.progress-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 14px;
  height: 14px;
  margin-top: -5px;
  border: 0;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.08);
}

.progress-slider::-moz-range-track {
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(
    to right,
    rgba(255, 255, 255, 0.92) 0%,
    rgba(255, 255, 255, 0.92) var(--progress, 0%),
    rgba(255, 255, 255, 0.22) var(--progress, 0%),
    rgba(255, 255, 255, 0.22) 100%
  );
}

.progress-slider::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border: 0;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.08);
}

.time-row {
  display: flex;
  justify-content: space-between;
  margin-top: 2px;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.controls {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: clamp(16px, 5vw, 28px);
  width: 100%;
  margin: 0;
}

.controls ion-button {
  --padding-start: 0;
  --padding-end: 0;
  width: 52px;
  height: 52px;
  margin: 0;
  font-size: 26px;
}

.controls .play-toggle {
  width: 68px;
  height: 68px;
  font-size: 30px;
}

.mode-bar {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  max-width: 280px;
  margin: 0;
}

.mode-bar ion-button {
  --padding-start: 0;
  --padding-end: 0;
  --color: rgba(255, 255, 255, 0.58);
  width: 44px;
  height: 44px;
  margin: 0;
  font-size: 20px;
}

.mode-bar .mode-button.is-active {
  --color: #ffffff;
}

.lyric-panel {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  gap: 12px;
  overflow: hidden;
}

.lyric-header {
  flex: 0 0 auto;
  width: 100%;
  text-align: left;
  min-width: 0;
}

.lyric-title {
  margin: 0;
  font-size: clamp(20px, 5.4vw, 28px);
  line-height: 1.2;
  font-weight: 700;
  letter-spacing: 0.01em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lyric-artist {
  margin: 6px 0 0;
  color: rgba(255, 255, 255, 0.68);
  font-size: clamp(13px, 3.6vw, 15px);
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lyric-player {
  display: block;
  position: relative;
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
  height: auto;
  /* AMLL 通过 CSS 变量控制字号；窄屏偏大字左对齐 */
  --amll-lp-font-size: clamp(22px, 6.5vw, 32px);
  --amll-lp-line-padding-x: 0;
  --amll-lp-line-width-aspect: 1;
  --lyric-line-padding-x: 0;
}

/* AMLL 实际 player 挂在 wrapper 子节点；强制铺满 flex 槽位 */
.lyric-player :deep(.amll-lyric-player) {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

/* 去掉 AMLL 默认行左右 padding，使歌词左缘与顶部歌名对齐（panel 已有 24px 边距） */
.lyric-player :deep(.FmKaba_lyricLine) {
  padding-left: 0;
  padding-right: 0;
}

.lyric-empty {
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
  overflow: hidden;
}

@media (min-width: 768px) {
  .panels {
    width: auto;
    display: flex;
    flex-direction: row;
    height: 100%;
    overflow: hidden;
    transform: none !important;
  }

  .panel {
    flex: 1;
    width: auto;
    min-width: 0;
    min-height: 0;
  }

  .info-panel {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 24px;
  }

  .info-panel-inner {
    height: 100%;
    max-height: 100%;
    justify-content: center;
    gap: 12px;
  }

  .cover-slot {
    max-height: min(48dvh, 320px);
  }

  .cover {
    width: min(40vw, 320px);
  }

  .song-info {
    text-align: center;
  }

  .lyric-panel {
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
  }

  /* 宽屏左侧控制页已有歌名/歌手，右侧不重复顶部信息 */
  .lyric-header {
    display: none;
  }

  .lyric-player {
    flex: 1;
    min-height: 0;
    height: auto;
    --amll-lp-font-size: clamp(20px, 2.4vw, 30px);
  }
}

@media (max-height: 720px) {
  .controls ion-button {
    width: 46px;
    height: 46px;
    font-size: 22px;
  }

  .controls .play-toggle {
    width: 58px;
    height: 58px;
    font-size: 26px;
  }

  .mode-bar ion-button {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .cover-slot {
    max-height: min(42dvh, 260px);
  }
}
</style>
