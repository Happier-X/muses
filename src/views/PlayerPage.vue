<template>
  <div
    class="player-overlay fixed inset-0 z-[var(--muses-z-player)] overflow-hidden overscroll-behavior-none touch-action-none text-[var(--muses-immersive-ink)]"
    :aria-hidden="!playerOverlayVisible"
    @touchstart.passive="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
    @touchcancel="onTouchEnd"
  >
    <div
      class="immersive-shell relative h-dvh max-h-dvh overflow-hidden [background:var(--muses-immersive-void)] transition-[transform] duration-[var(--muses-duration-overlay)] ease-[var(--muses-ease-standard)]"
      :class="{ 'is-dragging': isDraggingVertically, '!transition-none': isDraggingVertically }"
      :style="{ transform: `translateY(${dragOffsetY}px)` }"
    >
      <!-- 背景与歌词解耦：切歌暂无词时不卸载，避免闪默认底（#20） -->
      <div v-if="showAlbumBackground" class="amll-background absolute inset-0 z-0 overflow-hidden opacity-75">
        <BackgroundRender
          :key="backgroundAlbumSrc || 'no-album'"
          class="amll-background-render absolute inset-0 block w-full h-full"
          :album="backgroundAlbumSrc || undefined"
          :album-is-video="false"
          :flow-speed="2"
          :has-lyric="hasLyrics"
          :renderer="meshGradientRenderer"
        />
      </div>
      <div class="fallback-background absolute inset-0 z-0" :class="{ 'opacity-0': showAlbumBackground }" />

      <section v-if="!playerState.currentSong" class="empty-state flex flex-col items-center justify-center text-center">
        <div class="placeholder-cover">♪</div>
        <h1>暂无播放歌曲</h1>
        <p>从歌曲列表选择一首音乐后，即可进入沉浸式播放。</p>
      </section>

      <div
        v-else
        class="panels relative z-10 flex w-[200%] h-full max-h-full overflow-hidden transition-[transform] duration-[var(--muses-duration-overlay)] ease-[var(--muses-ease-standard)]"
        :style="{ transform: `translateX(-${activePanel * 50}%)` }"
      >
        <section class="panel info-panel flex flex-col items-center justify-stretch text-center" aria-label="播放控制页">
          <div class="info-panel-inner flex flex-col items-center justify-between gap-[8px] w-[min(100%,420px)] h-full mx-auto min-h-0 overflow-hidden">
            <div class="cover-slot flex-[1_1_auto] flex items-center justify-center w-full min-h-0">
              <img v-if="displayCoverSrc" class="cover aspect-1 h-auto max-w-full max-h-full object-cover" :src="displayCoverSrc" alt="歌曲封面" />
              <div v-else class="cover placeholder-cover aspect-1 h-auto max-w-full max-h-full object-cover">♪</div>
            </div>

            <div class="song-info flex-none w-full m-0 text-left min-w-0">
              <h1>{{ playerState.currentSong.title }}</h1>
              <p>{{ subtitle }}</p>
            </div>

            <div
              class="progress-area flex-none w-full"
              @touchstart.stop="onProgressGestureStart"
              @touchmove.stop
              @touchend.stop="onProgressGestureEnd"
              @touchcancel.stop="onProgressGestureEnd"
              @pointerdown.stop="onProgressGestureStart"
              @pointerup.stop="onProgressGestureEnd"
              @pointercancel.stop="onProgressGestureEnd"
            >
              <h-range
                class="progress-range w-full cursor-pointer touch-manipulation"
                :min="0"
                :max="durationForSlider"
                :step="0.1"
                :model-value="effectiveSeekPosition"
                :disabled="!canSeek"
                aria-label="播放进度"
                @update:model-value="onSeekInput"
                @change="onSeek"
              />
              <div class="time-row flex justify-between items-center mt-[2px] text-[12px] tabular-nums text-[var(--muses-immersive-ink-soft)]">
                <span>{{ formatTime(playerState.position) }}</span>
                <span v-if="bufferHintVisible" class="buffer-hint text-[rgba(255,255,255,0.55)] text-[11px]">缓冲中</span>
                <span>{{ playerState.duration ? formatTime(playerState.duration) : '--:--' }}</span>
              </div>
            </div>

            <div class="controls flex-none flex items-center justify-center gap-[clamp(16px,5vw,28px)] w-full m-0 touch-manipulation">
              <h-button variant="ghost" is-icon-only shape="circle" aria-label="上一曲" @click="onPrevious">
                <h-icon :icon="previousIcon" variant="fill" />
              </h-button>
              <h-button
                class="play-toggle"
                variant="ghost"
                is-icon-only
                shape="circle"
                :disabled="playerState.status === 'loading'"
                aria-label="播放或暂停"
                @click="togglePlayback"
              >
                <h-icon :icon="isPlaying ? pause : play" variant="fill" />
              </h-button>
              <h-button variant="ghost" is-icon-only shape="circle" aria-label="下一曲" @click="onNext">
                <h-icon :icon="nextIcon" variant="fill" />
              </h-button>
            </div>

            <div class="mode-bar flex-none flex justify-between items-center w-full max-w-[280px] m-0 touch-manipulation">
              <h-button
                variant="ghost"
                is-icon-only
                shape="circle"
                class="mode-button"
                :class="{ 'is-active': queueState.repeatMode === 'one' }"
                :aria-label="repeatModeLabel"
                @click="onToggleRepeat"
              >
                <h-icon :icon="repeatIcon" />
              </h-button>

              <h-button
                variant="ghost"
                is-icon-only
                shape="circle"
                class="mode-button"
                :class="{ 'is-active': queueState.shuffleEnabled }"
                :aria-label="shuffleModeLabel"
                @click="onToggleShuffle"
              >
                <h-icon :icon="shuffleIcon" />
              </h-button>

              <h-button
                variant="ghost"
                is-icon-only
                shape="circle"
                class="mode-button"
                aria-label="播放队列"
                @click="goToQueue"
              >
                <h-icon :icon="listIcon" />
              </h-button>
            </div>
          </div>
        </section>

        <section
          class="panel lyric-panel relative flex flex-col items-stretch justify-start overflow-hidden"
          aria-label="歌词页"
          @pointerup="onLyricPanelPointerUp"
        >
          <header v-if="playerState.currentSong" class="lyric-header flex-none w-full text-left min-w-0">
            <h2 class="lyric-title m-0 font-bold tracking-[0.01em] truncate">{{ playerState.currentSong.title }}</h2>
            <p v-if="lyricArtist" class="lyric-artist m-0 truncate">{{ lyricArtist }}</p>
          </header>

          <template v-if="hasLyrics">
            <LyricPlayer
              :key="lyricPlayerKey"
              class="lyric-player block relative flex-[1_1_auto] w-full min-h-0 h-auto"
              :data-translation-visible="showLyricTranslation ? 'true' : 'false'"
              :lyric-lines="displayLyricLines"
              :current-time="lyricRenderTime"
              align-anchor="center"
              :align-position="0.5"
              :enable-spring="true"
              :enable-blur="true"
              :enable-scale="true"
              :word-fade-width="0.5"
              @line-click="onLyricLineClick"
            />
          </template>
          <div v-else class="lyric-empty flex flex-col items-center justify-center text-center flex-[1_1_auto] w-full min-h-0 overflow-hidden">
            <h2>{{ lyricEmptyTitle }}</h2>
            <p>{{ lyricEmptyDescription }}</p>
          </div>

          <div
            v-if="playerState.currentSong"
            class="lyric-floating-actions absolute left-[12px] right-[12px] bottom-[calc(8px+env(safe-area-inset-bottom,0px))] z-[3] flex items-center justify-between opacity-0 pointer-events-none transition-[opacity] duration-[var(--muses-duration-fab)] ease-[var(--muses-ease-standard)]"
            :class="{ 'is-visible': lyricChromeVisible }"
            aria-label="歌词快捷操作"
            :aria-hidden="!lyricChromeVisible"
          >
            <h-button
              variant="ghost"
              is-icon-only
              shape="circle"
              class="lyric-fab lyric-translate-toggle w-[40px] h-[40px] min-w-[40px] min-h-[40px] m-0 text-[20px] pointer-events-none backdrop-blur-[10px] [--padding-start:0] [--padding-end:0] [--padding-top:0] [--padding-bottom:0] [--background:rgba(0,0,0,0.16)] [--background-hover:rgba(255,255,255,0.14)] [--background-activated:rgba(255,255,255,0.2)] [--color:rgba(255,255,255,0.78)] [--border-radius:var(--muses-radius-pill)]"
              :class="{ 'is-active': showLyricTranslation, '!pointer-events-auto [--color:#fff] [--background:rgba(255,255,255,0.22)]': showLyricTranslation }"
              :aria-label="showLyricTranslation ? '隐藏翻译' : '显示翻译'"
              :tabindex="lyricChromeVisible ? 0 : -1"
              @click.stop="onLyricTranslateClick"
            >
              <h-icon :icon="translationIcon" aria-hidden="true" />
            </h-button>

            <h-button
              v-if="!isTabletLayout"
              variant="ghost"
              is-icon-only
              shape="circle"
              class="lyric-fab lyric-play-toggle w-[40px] h-[40px] min-w-[40px] min-h-[40px] m-0 text-[20px] pointer-events-none backdrop-blur-[10px] [--padding-start:0] [--padding-end:0] [--padding-top:0] [--padding-bottom:0] [--background:rgba(0,0,0,0.16)] [--background-hover:rgba(255,255,255,0.14)] [--background-activated:rgba(255,255,255,0.2)] [--color:rgba(255,255,255,0.72)] [--border-radius:var(--muses-radius-pill)]"
              :aria-label="isPlaying ? '暂停播放' : '继续播放'"
              :disabled="playerState.status === 'loading'"
              :tabindex="lyricChromeVisible ? 0 : -1"
              @click.stop="onLyricPlayClick"
            >
              <h-icon :icon="isPlaying ? pause : play" variant="fill" />
            </h-button>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { HButton, HIcon, HRange } from '@/components/ui'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Capacitor } from '@capacitor/core'
import { languageOffOutline, languageOutline, list, listOutline, pause, play, playSkipBack, playSkipForward, repeat, repeatOutline, shuffle } from '@/icons'
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
/** 隐藏态冻结传给 AMLL 的时间输入；重新打开时由当前播放进度立即刷新。 */
const hiddenLyricTime = ref(0)
const lyricRenderTime = computed(() => playerOverlayVisible.value ? playerState.position * 1000 : hiddenLyricTime.value)
const touchStartX = ref<number | null>(null)
const touchStartY = ref<number | null>(null)
const dragOffsetY = ref(0)
const gestureDirection = ref<'horizontal' | 'vertical' | null>(null)
const isDraggingVertically = ref(false)
const canDragDown = ref(false)
const showLyricTranslation = ref(true)
/** 歌词页浮动 chrome：默认隐藏，交互后显示，空闲 3s 再藏。 */
const lyricChromeVisible = ref(false)
let lyricChromeIdleTimer: ReturnType<typeof setTimeout> | null = null
const LYRIC_FAB_IDLE_MS = 3000
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
const translationIcon = computed(() => showLyricTranslation.value ? languageOutline : languageOffOutline)
const previousIcon = playSkipBack
const nextIcon = playSkipForward
const isTabletLayout = computed(() => viewportWidth.value >= 768)

const updateViewportWidth = () => {
  viewportWidth.value = window.innerWidth
}

const clearLyricChromeIdleTimer = () => {
  if (lyricChromeIdleTimer !== null) {
    clearTimeout(lyricChromeIdleTimer)
    lyricChromeIdleTimer = null
  }
}

const scheduleLyricChromeHide = () => {
  clearLyricChromeIdleTimer()
  lyricChromeIdleTimer = setTimeout(() => {
    lyricChromeVisible.value = false
    lyricChromeIdleTimer = null
  }, LYRIC_FAB_IDLE_MS)
}

const revealLyricChrome = () => {
  if (activePanel.value !== 1) {
    return
  }
  lyricChromeVisible.value = true
  scheduleLyricChromeHide()
}

const hideLyricChromeImmediate = () => {
  lyricChromeVisible.value = false
  clearLyricChromeIdleTimer()
}

const toggleLyricTranslation = () => {
  showLyricTranslation.value = !showLyricTranslation.value
}

const onLyricTranslateClick = () => {
  revealLyricChrome()
  toggleLyricTranslation()
}

const onLyricPlayClick = async () => {
  revealLyricChrome()
  await togglePlayback()
}

const onLyricPanelPointerUp = (event: PointerEvent) => {
  if (activePanel.value !== 1 || !playerState.currentSong) {
    return
  }
  // 已显示的 fab 自身 click 会重置 timer；此处避免重复与误触路径。
  if (event.target instanceof Element && event.target.closest('.lyric-fab')) {
    return
  }
  revealLyricChrome()
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
watch(playerOverlayVisible, (visible) => {
  if (visible) {
    // 重新打开时直接跳到最新播放位置，避免歌词从关闭前的旧行开始。
    hiddenLyricTime.value = playerState.position * 1000
  } else {
    hideLyricChromeImmediate()
  }
  resetDragState()
})

watch(activePanel, (panel) => {
  if (panel !== 1) {
    hideLyricChromeImmediate()
  }
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

/** 拖动中视觉 clamp 到已缓冲终点，并用本地 preview 驱动 h-range value。
 * 仅用户进度条手势写入 preview：ion-range 在 value 属性变化时也会 emit ionInput，
 * 若误写 preview 会盖住 playerState.position，导致进度条冻结（#47）。
 */
const onSeekInput = (value: number) => {
  if (!seekGestureLocked.value) {
    return
  }
  const requested = value
  if (!Number.isFinite(requested)) {
    return
  }
  const clamped = clampSeekTarget(requested)
  seekPreviewPosition.value = clamped
  if (requested > clamped + 0.05) {
    showBufferHint()
  }
}

const onSeek = async (value: number) => {
  // ionChange 可能在 pointerup 之后触发；再锁一次并续期 debounce，覆盖 click 穿透窗口。
  lockSeekGesture()
  scheduleSeekUnlock()
  const requested = value
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

  // 歌词面板内滑动（含 AMLL 上下浏览）时短暂露出浮动按钮。
  if (
    activePanel.value === 1
    && isLyricPanelTarget(event)
    && Math.max(Math.abs(deltaX), Math.abs(deltaY)) > 8
  ) {
    revealLyricChrome()
  }

  if (gestureDirection.value !== 'vertical' || !canDragDown.value) {
    return
  }

  const nextOffset = Math.max(0, deltaY)
  dragOffsetY.value = nextOffset
  isDraggingVertically.value = nextOffset > 0
}

const INTERACTIVE_SELECTOR =
  'input, textarea, select, button, a, [role="button"], [contenteditable="true"], .progress-range'

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
    // 歌词面板轻点：显示浮动 chrome（pointerup 也会兜底；touch 路径保证移动端一致）。
    if (
      activePanel.value === 1
      && isLyricPanelTarget(event)
      && !isNativeInteractiveEvent(event)
    ) {
      revealLyricChrome()
    }
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
  clearLyricChromeIdleTimer()
  if (bufferHintTimer !== null) {
    clearTimeout(bufferHintTimer)
    bufferHintTimer = null
  }
  seekGestureLocked.value = false
  resetDragState()
})
</script>

