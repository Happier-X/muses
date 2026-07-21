<template>
  <div
    class="player-overlay"
    :aria-hidden="!playerOverlayVisible"
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
      <!-- 背景与歌词解耦：切歌暂无词时不卸载，避免闪默认底（#20） -->
      <div v-if="showAlbumBackground" class="amll-background">
        <BackgroundRender
          :key="backgroundAlbumSrc || 'no-album'"
          class="amll-background-render"
          :album="backgroundAlbumSrc || undefined"
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
              <img v-if="displayCoverSrc" class="cover" :src="displayCoverSrc" alt="歌曲封面" />
              <div v-else class="cover placeholder-cover">♪</div>
            </div>

            <div class="song-info">
              <h1>{{ playerState.currentSong.title }}</h1>
              <p>{{ subtitle }}</p>
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
              <ion-range
                class="progress-range"
                :min="0"
                :max="durationForSlider"
                :step="0.1"
                :value="effectiveSeekPosition"
                :disabled="!canSeek"
                aria-label="播放进度"
                @ionInput="onSeekInput"
                @ionChange="onSeek"
              />
              <div class="time-row">
                <span>{{ formatTime(playerState.position) }}</span>
                <span v-if="bufferHintVisible" class="buffer-hint">缓冲中</span>
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
              :key="lyricPlayerKey"
              class="lyric-player"
              :data-translation-visible="showLyricTranslation ? 'true' : 'false'"
              :lyric-lines="displayLyricLines"
              :current-time="playerState.position * 1000"
              align-anchor="center"
              :align-position="0.5"
              :enable-spring="true"
              :enable-blur="true"
              :enable-scale="true"
              :word-fade-width="0.5"
              @line-click="onLyricLineClick"
            />
          </template>
          <div v-else class="lyric-empty">
            <h2>{{ lyricEmptyTitle }}</h2>
            <p>{{ lyricEmptyDescription }}</p>
          </div>

          <div v-if="playerState.currentSong" class="lyric-floating-actions" aria-label="歌词快捷操作">
            <ion-button
              fill="clear"
              shape="round"
              color="light"
              class="lyric-fab lyric-translate-toggle"
              :class="{ 'is-active': showLyricTranslation }"
              :aria-label="showLyricTranslation ? '隐藏翻译' : '显示翻译'"
              @click.stop="toggleLyricTranslation"
            >
              <ion-icon slot="icon-only" :icon="languageIcon" aria-hidden="true" />
            </ion-button>

            <ion-button
              v-if="!isTabletLayout"
              fill="clear"
              shape="round"
              color="light"
              class="lyric-fab lyric-play-toggle"
              :aria-label="isPlaying ? '暂停播放' : '继续播放'"
              :disabled="playerState.status === 'loading'"
              @click.stop="togglePlayback"
            >
              <ion-icon slot="icon-only" :icon="isPlaying ? pauseCircleIcon : playCircleIcon" aria-hidden="true" />
            </ion-button>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Capacitor } from '@capacitor/core'
import { IonButton, IonIcon, IonRange } from '@ionic/vue'
import { languageOutline, list, listOutline, pause, pauseCircleOutline, play, playCircleOutline, playSkipBack, playSkipForward, repeat, repeatOutline, shuffle } from '@/icons/ion-lucide'
import { BackgroundRender, LyricPlayer } from '@applemusic-like-lyrics/vue'
import { MeshGradientRenderer } from '@applemusic-like-lyrics/core'
import type { LyricLine, LyricLineMouseEvent } from '@applemusic-like-lyrics/core'
import { parseLrc, parseQrc, parseTTML, parseYrc } from '@applemusic-like-lyrics/lyric'
import '@applemusic-like-lyrics/core/style.css'
import { applyLyricTranslationVisibility } from '@/features/lyrics/display'
import { prepareLyricLinesForDisplay } from '@/features/lyrics/mergeTranslation'
import { isPlaying, pausePlayback, playerState, playNextFromQueue, playPreviousFromQueue, queueState, resumePlayback, seekPlayback, setRepeatMode, toggleShuffle } from '@/features/player/controller'
import { closePlayerOverlay, openQueueOverlay, playerOverlayVisible } from '@/features/player/overlay'

const activePanel = ref(0)
const touchStartX = ref<number | null>(null)
const touchStartY = ref<number | null>(null)
const dragOffsetY = ref(0)
const gestureDirection = ref<'horizontal' | 'vertical' | null>(null)
const isDraggingVertically = ref(false)
const canDragDown = ref(false)
const showLyricTranslation = ref(true)
const viewportWidth = ref(typeof window === 'undefined' ? 0 : window.innerWidth)
/** 进度条交互中或结束后的短保护期，防止松手穿透到上一曲/下一曲或横向切面板。 */
const seekGestureLocked = ref(false)
let seekUnlockTimer: ReturnType<typeof setTimeout> | null = null
const SEEK_CLICK_GUARD_MS = 300
const meshGradientRenderer = MeshGradientRenderer

const repeatModeLabel = computed(() => queueState.repeatMode === 'one' ? '单曲循环' : '列表循环')
const repeatIcon = computed(() => queueState.repeatMode === 'one' ? repeat : repeatOutline)
const shuffleModeLabel = computed(() => queueState.shuffleEnabled ? '随机播放' : '顺序播放')
const shuffleIcon = computed(() => queueState.shuffleEnabled ? shuffle : listOutline)
const listIcon = list
const languageIcon = languageOutline
const playCircleIcon = playCircleOutline
const pauseCircleIcon = pauseCircleOutline
const previousIcon = playSkipBack
const nextIcon = playSkipForward
const isTabletLayout = computed(() => viewportWidth.value >= 768)

const updateViewportWidth = () => {
  viewportWidth.value = window.innerWidth
}

const toggleLyricTranslation = () => {
  showLyricTranslation.value = !showLyricTranslation.value
}

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

const currentLyrics = computed(() => playerState.lyrics || playerState.currentSong?.lyrics || '')
const currentCoverUri = computed(() => playerState.coverUri || playerState.currentSong?.coverUri || '')
const coverSrc = computed(() => toDisplayableUri(currentCoverUri.value))

/** 切歌无封面时沿用上一张可展示封面，避免背景/封面闪默认（#20） */
const stickyCoverSrc = ref('')

/** PlayerPage 保活后，打开/关闭必须清掉上次下滑位移，避免再打开半屏（#25） */
watch(playerOverlayVisible, () => {
  resetDragState()
})

watch(
  [() => playerState.currentSong?.id, coverSrc],
  ([songId, nextCover]) => {
    if (!songId) {
      stickyCoverSrc.value = ''
      return
    }
    if (nextCover) {
      stickyCoverSrc.value = nextCover
    }
  },
  { immediate: true },
)

const displayCoverSrc = computed(() => coverSrc.value || stickyCoverSrc.value)
const backgroundAlbumSrc = computed(() => displayCoverSrc.value)
const showAlbumBackground = computed(
  () => !!playerState.currentSong && !!backgroundAlbumSrc.value,
)

const lyricLines = computed<LyricLine[]>(() => {
  if (!currentLyrics.value) {
    return []
  }

  try {
    // 格式解析归 AMLL；业务只做 tlyric 挂载 + 双行 plain LRC 主译（mergeTranslation）。
    let lines: LyricLine[]
    if (playerState.lyricsFormat === 'ttml') {
      lines = parseTTML(currentLyrics.value).lines
    } else if (playerState.lyricsFormat === 'yrc') {
      lines = parseYrc(currentLyrics.value)
    } else if (playerState.lyricsFormat === 'qrc') {
      lines = parseQrc(currentLyrics.value)
    } else {
      lines = parseLrc(normalizeLrc(currentLyrics.value))
    }
    // attach tlyric + 合并同时间戳双主行（库已填 translatedLyric 则跳过合并）
    return prepareLyricLinesForDisplay(lines, playerState.lyricsTranslation)
  } catch {
    return []
  }
})

const displayLyricLines = computed<LyricLine[]>(() => {
  return applyLyricTranslationVisibility(lyricLines.value, showLyricTranslation.value)
})

/** AMLL 内部会缓存行节点，翻译显隐切换时强制重建保证立即生效（#26） */
const lyricPlayerKey = computed(() => `${playerState.currentSong?.id ?? 'none'}:${playerState.lyricsFormat ?? 'lrc'}:${showLyricTranslation.value ? 'translation-on' : 'translation-off'}`)

const hasLyrics = computed(() => lyricLines.value.length > 0)

/** 匹配中且无本地词：显示匹配中；失败/无匹配且无本地：区分空态文案 */
const lyricEmptyTitle = computed(() => {
  if (playerState.onlineLyricsStatus === 'matching' && !currentLyrics.value) {
    return '正在匹配歌词'
  }
  return '暂无歌词'
})

const lyricEmptyDescription = computed(() => {
  if (playerState.onlineLyricsStatus === 'matching' && !currentLyrics.value) {
    return '正在匹配在线歌词…'
  }
  if (
    playerState.onlineLyricsStatus === 'miss'
    || playerState.onlineLyricsStatus === 'error'
    || (playerState.lyricsFormat === 'ttml' && !hasLyrics.value)
  ) {
    return '未匹配到可用的在线歌词，当前歌曲也没有内嵌歌词或同目录同名 .lrc 文件。'
  }
  return '当前歌曲没有内嵌歌词，也没有找到同目录同名 .lrc 文件。'
})
const canSeek = computed(() => playerState.duration > 0)
const durationForSlider = computed(() => playerState.duration || 1)
const seekPreviewPosition = ref<number | null>(null)
const effectiveSeekPosition = computed(() => seekPreviewPosition.value ?? playerState.position)

const bufferHintVisible = ref(false)
let bufferHintTimer: ReturnType<typeof setTimeout> | null = null

const showBufferHint = () => {
  bufferHintVisible.value = true
  if (bufferHintTimer !== null) {
    clearTimeout(bufferHintTimer)
  }
  bufferHintTimer = setTimeout(() => {
    bufferHintVisible.value = false
    bufferHintTimer = null
  }, 1200)
}

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

const clampSeekTarget = (raw: number): number => {
  if (!Number.isFinite(raw) || raw < 0) {
    return 0
  }
  const buffered = playerState.bufferedPosition
  const duration = playerState.duration
  let max = duration > 0 ? duration : Number.POSITIVE_INFINITY
  if (buffered != null && Number.isFinite(buffered) && buffered >= 0) {
    max = duration > 0 ? Math.min(duration, buffered) : buffered
  }
  return Number.isFinite(max) ? Math.min(raw, max) : raw
}

/** 从 ion-range 的 CustomEvent.detail.value 读取目标秒；兼容测试 stub 的 input 事件。 */
const readRangeEventValue = (event: Event): number => {
  const custom = event as CustomEvent<{ value?: number | { lower: number; upper: number } }>
  const raw = custom.detail?.value
  if (typeof raw === 'number') {
    return raw
  }
  if (raw && typeof raw === 'object' && typeof raw.lower === 'number') {
    return raw.lower
  }
  const target = event.target as { value?: string | number } | null
  if (target && target.value != null && target.value !== '') {
    return Number(target.value)
  }
  return Number.NaN
}

/** 拖动中视觉 clamp 到已缓冲终点，并用本地 preview 驱动 ion-range value。
 * 仅用户进度条手势写入 preview：ion-range 在 value 属性变化时也会 emit ionInput，
 * 若误写 preview 会盖住 playerState.position，导致进度条冻结（#47）。
 */
const onSeekInput = (event: Event) => {
  if (!seekGestureLocked.value) {
    return
  }
  const requested = readRangeEventValue(event)
  if (!Number.isFinite(requested)) {
    return
  }
  const clamped = clampSeekTarget(requested)
  seekPreviewPosition.value = clamped
  if (requested > clamped + 0.05) {
    showBufferHint()
  }
}

const onSeek = async (event: Event) => {
  // ionChange 可能在 pointerup 之后触发；再锁一次并续期 debounce，覆盖 click 穿透窗口。
  lockSeekGesture()
  scheduleSeekUnlock()
  const requested = readRangeEventValue(event)
  if (!Number.isFinite(requested)) {
    seekPreviewPosition.value = null
    return
  }
  const clamped = clampSeekTarget(requested)
  if (requested > clamped + 0.05) {
    showBufferHint()
  }
  const ok = await seekPlayback(clamped)
  seekPreviewPosition.value = null
  if (!ok && playerState.bufferedPosition != null) {
    showBufferHint()
  }
}

/** 点击有时间戳的歌词行，seek 到该行起始秒；无效 startTime / 未缓冲区间不处理。 */
const onLyricLineClick = async (event: LyricLineMouseEvent) => {
  // 阻止冒泡到 overlay 手势，避免点击 seek 误切面板或关闭播放页。
  event.stopPropagation()
  event.preventDefault()
  lockSeekGesture()
  scheduleSeekUnlock()
  touchStartX.value = null
  touchStartY.value = null
  gestureDirection.value = null
  canDragDown.value = false
  isDraggingVertically.value = false
  dragOffsetY.value = 0

  // LyricLineBase 通过 getLine() 暴露 LyricLine；startTime 单位为毫秒。
  const startMs = event.line?.getLine?.()?.startTime
  if (typeof startMs !== 'number' || !Number.isFinite(startMs) || startMs < 0) {
    return
  }
  const targetSec = startMs / 1000
  const ok = await seekPlayback(targetSec)
  if (!ok) {
    // 未缓冲区间：不 seek，轻提示（R3）
    showBufferHint()
  }
}

const onTouchStart = (event: TouchEvent) => {
  // 原生控件（进度条）或 seek 锁定期内，不启动 overlay 面板/下滑手势。
  if (seekGestureLocked.value || isNativeInteractiveEvent(event)) {
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
  if (!isNativeInteractiveEvent(event)) {
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

const INTERACTIVE_SELECTOR =
  'input, textarea, select, button, ion-button, ion-range, a, [role="button"], [contenteditable="true"], .progress-range'

const isInteractiveElement = (el: Element): boolean => {
  return Boolean(el.closest(INTERACTIVE_SELECTOR))
}

const isNativeInteractiveTarget = (target: EventTarget | null): boolean => {
  if (!(target instanceof Element)) {
    return false
  }

  // ion-button 使用 Shadow DOM：event.target 常在 shadow 内，closest 穿不过宿主，
  // 必须配合 composedPath 识别，否则 touchmove preventDefault 会吞掉 click（循环/随机按钮失效）。
  if (isInteractiveElement(target)) {
    return true
  }
  return false
}

const isNativeInteractiveEvent = (event: TouchEvent | Event): boolean => {
  if (isNativeInteractiveTarget(event.target)) {
    return true
  }
  if (!('composedPath' in event) || typeof event.composedPath !== 'function') {
    return false
  }
  return event.composedPath().some((node) => node instanceof Element && isInteractiveElement(node))
}

const onTouchEnd = (event: TouchEvent) => {
  const startX = touchStartX.value
  const endX = event.changedTouches[0]?.clientX
  const shouldDismiss = gestureDirection.value === 'vertical' && dragOffsetY.value >= getDismissThreshold()
  const skipPanelSwitch = seekGestureLocked.value || isNativeInteractiveEvent(event)

  touchStartX.value = null
  touchStartY.value = null
  gestureDirection.value = null
  canDragDown.value = false
  isDraggingVertically.value = false

  if (shouldDismiss) {
    dragOffsetY.value = 0
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

const isLyricPanelTarget = (event: TouchEvent): boolean => {
  // 优先用 target.closest：真实 DOM 与 @vue/test-utils trigger 都可靠；
  // 再兜底 composedPath，覆盖 Shadow DOM / 合成事件路径。
  if (event.target instanceof Element && event.target.closest('.lyric-panel, .lyric-player')) {
    return true
  }
  return event.composedPath().some((target) => {
    if (!(target instanceof Element)) {
      return false
    }
    return target.classList.contains('lyric-panel') || target.classList.contains('lyric-player')
  })
}

const canStartVerticalDismiss = (event: TouchEvent): boolean => {
  // AMLL LyricPlayer 内部滚动基于 transform，非原生 scroll，无法被下方的原生滚动检测识别。
  // 触点位于歌词面板/歌词播放器内时，禁止 overlay 下滑关闭，避免歌词上下滚动误触发收起。
  if (isLyricPanelTarget(event)) {
    return false
  }
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

const normalizeLrc = (lyrics: string): string => {
  return lyrics.replace(/\[((?:\d+:)*\d+),(\d+)\]/g, '[$1.$2]')
}

const formatTime = (value: number): string => {
  const totalSeconds = Math.max(0, Math.floor(value))
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}

onMounted(() => {
  updateViewportWidth()
  window.addEventListener('resize', updateViewportWidth)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewportWidth)
  clearSeekUnlockTimer()
  if (bufferHintTimer !== null) {
    clearTimeout(bufferHintTimer)
    bufferHintTimer = null
  }
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

/* 正方形边长 = min(水平上限, 垂直上限)；窄屏 width 必须含 dvh，避免仅靠 max-height clamp 拉成长方形 */
.cover,
.placeholder-cover {
  width: min(72vw, 100%, 340px, 52dvh);
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

/* ion-range 进度条：隐藏 knob，仅保留可拖动轨道与已播放填充。
 * 不再维护自绘缓冲色条层；缓冲 clamp 仍由 seek 业务逻辑处理。
 */
.progress-range {
  --knob-size: 0px;
  --knob-background: transparent;
  --knob-box-shadow: none;
  --bar-height: 4px;
  --bar-background: rgba(255, 255, 255, 0.2);
  --bar-background-active: #fff;
  --bar-border-radius: 999px;
  width: 100%;
  height: 24px;
  min-height: 24px;
  padding: 0;
  margin: 0;
  cursor: pointer;
  touch-action: manipulation;
}

.progress-range[disabled] {
  opacity: 0.55;
  cursor: not-allowed;
}

.time-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 2px;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.buffer-hint {
  color: rgba(255, 255, 255, 0.55);
  font-size: 11px;
}

.controls {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: clamp(16px, 5vw, 28px);
  width: 100%;
  margin: 0;
  touch-action: manipulation;
}

.controls ion-button {
  --padding-start: 0;
  --padding-end: 0;
  width: 52px;
  height: 52px;
  margin: 0;
  font-size: 26px;
  touch-action: manipulation;
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
  /* 覆盖 overlay 的 touch-action:none，保证模式按钮可点 */
  touch-action: manipulation;
}

.mode-bar ion-button {
  --padding-start: 0;
  --padding-end: 0;
  --color: rgba(255, 255, 255, 0.58);
  width: 44px;
  height: 44px;
  margin: 0;
  font-size: 20px;
  touch-action: manipulation;
}

.mode-bar .mode-button.is-active {
  --color: #ffffff;
}

.lyric-panel {
  position: relative;
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

.lyric-floating-actions {
  position: absolute;
  left: 0;
  right: 0;
  bottom: calc(8px + var(--ion-safe-area-bottom, 0px));
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  pointer-events: none;
}

.lyric-fab {
  --padding-start: 0;
  --padding-end: 0;
  --background: rgba(0, 0, 0, 0.16);
  --background-hover: rgba(255, 255, 255, 0.14);
  --background-activated: rgba(255, 255, 255, 0.2);
  --color: rgba(255, 255, 255, 0.72);
  width: 36px;
  height: 36px;
  margin: 0;
  font-size: 18px;
  pointer-events: auto;
  backdrop-filter: blur(10px);
}

.lyric-fab.is-active {
  --color: #ffffff;
}

/* AMLL 实际 player 挂在 wrapper 子节点；强制铺满 flex 槽位 */
.lyric-player :deep(.amll-lyric-player) {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

/* AMLL 实际类名：激活状态同时挂在行容器和主行，副行需跟随行容器同步高亮（#27） */
.lyric-player :deep(.FmKaba_lyricLine .FmKaba_lyricSubLine) {
  opacity: 0.3;
}

.lyric-player :deep(.FmKaba_lyricLine.FmKaba_active .FmKaba_lyricSubLine),
.lyric-player :deep(.FmKaba_lyricLine .FmKaba_lyricMainLine.FmKaba_active ~ .FmKaba_lyricSubLine) {
  opacity: 0.72;
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

  /* width 同时受 vw 与 dvh 约束，保证 aspect-ratio 1 时高度不超过 cover-slot max-height，避免被 clamp 拉伸 */
  .cover,
  .placeholder-cover {
    width: min(40vw, 48dvh, 320px);
  }

  .song-info {
    text-align: center;
  }

  .lyric-panel {
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
  }

  .lyric-play-toggle {
    display: none;
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

/* 矮屏/横屏：收紧控制区占位，释放垂直空间给封面；不隐藏控件；不改 lyric-panel */
@media (max-height: 720px) {
  .info-panel {
    padding:
      calc(10px + var(--ion-safe-area-top, 0px))
      24px
      calc(10px + var(--ion-safe-area-bottom, 0px));
  }

  .info-panel-inner {
    gap: 4px;
  }

  .cover-slot {
    max-height: min(42dvh, 260px);
  }

  .cover,
  .placeholder-cover {
    width: min(72vw, 100%, 260px, 42dvh);
  }

  .song-info h1 {
    font-size: clamp(18px, 5vw, 24px);
    margin-bottom: 2px;
  }

  .progress-range {
    height: 20px;
    min-height: 20px;
  }

  .controls {
    gap: clamp(12px, 4vw, 20px);
  }

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

  .mode-bar {
    max-width: 240px;
  }

  .mode-bar ion-button {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }
}

/* 宽屏 + 矮屏：cover width 与更紧的 cover-slot max-height 对齐，避免 48dvh 仍超过 42dvh 时被 clamp */
@media (min-width: 768px) and (max-height: 720px) {
  .info-panel {
    padding: 16px 24px;
  }

  .info-panel-inner {
    gap: 8px;
  }

  .cover,
  .placeholder-cover {
    width: min(40vw, 42dvh, 260px);
  }
}

/* 更矮（车机/横屏极限）：再收一档，仍保留全部控件 */
@media (max-height: 520px) {
  .info-panel {
    padding:
      calc(6px + var(--ion-safe-area-top, 0px))
      20px
      calc(6px + var(--ion-safe-area-bottom, 0px));
  }

  .info-panel-inner {
    gap: 2px;
  }

  .cover-slot {
    max-height: min(38dvh, 200px);
  }

  .cover,
  .placeholder-cover {
    width: min(72vw, 100%, 200px, 38dvh);
  }

  .song-info h1 {
    font-size: clamp(16px, 4.5vw, 20px);
    margin-bottom: 0;
  }

  .song-info p {
    font-size: 12px;
  }

  .song-info small {
    margin-top: 2px;
    font-size: 11px;
  }

  .progress-range {
    height: 18px;
    min-height: 18px;
  }

  .time-row {
    font-size: 11px;
  }

  .controls {
    gap: clamp(10px, 3.5vw, 16px);
  }

  .controls ion-button {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }

  .controls .play-toggle {
    width: 50px;
    height: 50px;
    font-size: 24px;
  }

  .mode-bar {
    max-width: 220px;
  }

  .mode-bar ion-button {
    width: 36px;
    height: 36px;
    font-size: 16px;
  }
}

@media (min-width: 768px) and (max-height: 520px) {
  .info-panel {
    padding: 12px 20px;
  }

  .cover,
  .placeholder-cover {
    width: min(40vw, 38dvh, 200px);
  }
}
</style>
