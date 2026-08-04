<template>
  <h-popup
    v-model="playerOverlayVisible"
    position="fullscreen"
    :keep-alive="true"
    :swipe-close="false"
    :close-on-overlay="false"
    :close-on-esc="false"
  >
    <div
      class="player-overlay h-full overflow-hidden overscroll-behavior-none touch-action-none text-[var(--muses-immersive-ink)]"
      @touchstart.passive="onTouchStart"
      @touchmove="onTouchMove"
      @touchend="onTouchEnd"
      @touchcancel="onTouchEnd"
    >
      <div
        class="relative h-full overflow-hidden [background:var(--muses-immersive-void)] transition-[transform] duration-[var(--muses-duration-overlay)] ease-[var(--muses-ease-standard)]"
        :class="{ 'is-dragging': isDraggingVertically, '!transition-none': isDraggingVertically }"
        :style="{ transform: `translateY(${dragOffsetY}px)` }"
      >
      <!-- 背景与歌词解耦：切歌暂无词时不卸载，避免闪默认底（#20） -->
      <div v-if="showAlbumBackground" class="absolute inset-0 z-0 overflow-hidden opacity-75">
        <BackgroundRender
          :key="backgroundAlbumSrc || 'no-album'"
          class="absolute inset-0 block w-full h-full"
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
          <div class="info-panel-inner flex flex-col items-center justify-center gap-[12px] w-[min(100%,420px)] h-full mx-auto min-h-0 overflow-hidden">
            <div class="cover-slot flex-[0_1_auto] flex items-center justify-center w-full min-h-0">
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
                ref="progressRangeRef"
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
                <span v-if="bufferHintVisible" class="text-[rgba(255,255,255,0.55)] text-[11px]">缓冲中</span>
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

            <div class="mode-bar flex-none flex justify-between items-center w-full max-w-[320px] m-0 touch-manipulation">
              <h-button
                variant="ghost"
                is-icon-only
                shape="circle"
                :aria-label="repeatModeLabel"
                @click="onToggleRepeat"
              >
                <h-icon :icon="repeatIcon" />
              </h-button>

              <h-button
                variant="ghost"
                is-icon-only
                shape="circle"
                :aria-label="shuffleModeLabel"
                @click="onToggleShuffle"
              >
                <h-icon :icon="shuffleIcon" />
              </h-button>

              <h-button
                variant="ghost"
                is-icon-only
                shape="circle"

                aria-label="播放队列"
                @click="goToQueue"
              >
                <h-icon :icon="listIcon" />
              </h-button>

              <h-button
                variant="ghost"
                is-icon-only
                shape="circle"
                aria-label="更多"
                @click="openPlayerActions"
              >
                <h-icon :icon="moreIcon" />
              </h-button>
            </div>
          </div>
        </section>

        <section
          ref="lyricPanelRef"
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
              ref="lyricPlayerRef"
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
          <div v-else class="flex flex-col items-center justify-center text-center flex-[1_1_auto] w-full min-h-0 overflow-hidden">
            <h2>{{ lyricEmptyTitle }}</h2>
            <p>{{ lyricEmptyDescription }}</p>
          </div>

          <div
            v-if="showLyricFloatingActions"
            class="lyric-floating-actions absolute left-[12px] right-[12px] bottom-[calc(8px+var(--safe-area-inset-bottom,env(safe-area-inset-bottom,0px)))] z-[3] flex items-center opacity-0 pointer-events-none transition-[opacity] duration-[var(--muses-duration-fab)] ease-[var(--muses-ease-standard)]"
            :class="[
              { 'is-visible': lyricChromeVisible },
              hasLyricTranslation ? 'justify-between' : 'justify-end',
            ]"
            aria-label="歌词快捷操作"
            :aria-hidden="!lyricChromeVisible"
          >
            <h-button
              v-if="hasLyricTranslation"
              variant="ghost"
              is-icon-only
              shape="circle"
              class="lyric-fab w-[40px] h-[40px] min-w-[40px] min-h-[40px] m-0 text-[20px] pointer-events-none"
              :class="{ 'is-active': showLyricTranslation }"
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
              class="lyric-fab lyric-play-toggle w-[40px] h-[40px] min-w-[40px] min-h-[40px] m-0 text-[20px] pointer-events-none"
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

    <!-- 歌曲操作：仅「编辑歌曲信息」（D2） -->
    <h-bottom-sheet v-model="isPlayerActionsOpen" title="歌曲操作">
      <div class="flex flex-col gap-[var(--muses-space-xs)] pb-[var(--muses-space-lg)] px-[var(--muses-space-lg)]">
        <button :class="actionSheetItemClass" type="button" @click="onOpenSongEdit">编辑歌曲信息</button>
        <button :class="[actionSheetItemClass, actionSheetCancelClass]" type="button" @click="isPlayerActionsOpen = false">取消</button>
      </div>
    </h-bottom-sheet>

    <!-- 编辑歌曲信息：title/artist/album/封面/歌词/RG -->
    <h-bottom-sheet v-model="isSongEditOpen" title="编辑歌曲信息">
      <form
        class="flex flex-col gap-[var(--muses-space-md)] pb-[var(--muses-space-lg)] px-[var(--muses-space-lg)]"
        @submit.prevent="editForm.handleSubmit"
      >
        <editForm.Field
          name="title"
          :validators="{
            onSubmit: ({ value }) => (String(value ?? '').trim() ? undefined : '请填写歌曲标题'),
          }"
        >
          <template #default="{ field }">
            <h-input
              :model-value="field.state.value"
              label="标题"
              :error="typeof field.state.meta.errors[0] === 'string' ? field.state.meta.errors[0] : undefined"
              :invalid="field.state.meta.errors.length > 0"
              :disabled="isEditSubmitting"
              @update:model-value="field.handleChange"
              @blur="field.handleBlur"
            />
          </template>
        </editForm.Field>

        <editForm.Field name="artist">
          <template #default="{ field }">
            <h-input
              :model-value="field.state.value"
              label="艺术家"
              :disabled="isEditSubmitting"
              @update:model-value="field.handleChange"
              @blur="field.handleBlur"
            />
          </template>
        </editForm.Field>

        <editForm.Field name="album">
          <template #default="{ field }">
            <h-input
              :model-value="field.state.value"
              label="专辑"
              :disabled="isEditSubmitting"
              @update:model-value="field.handleChange"
              @blur="field.handleBlur"
            />
          </template>
        </editForm.Field>

        <div class="flex flex-col gap-[var(--muses-space-xs)]">
          <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--muses-immersive-ink-soft,#aaa)]">封面</p>
          <div class="flex items-center gap-[var(--muses-space-md)]">
            <img
              v-if="editCoverPreviewSrc"
              class="w-16 h-16 rounded-[10px] object-cover flex-none"
              :src="editCoverPreviewSrc"
              alt="封面预览"
            >
            <div
              v-else
              class="w-16 h-16 rounded-[10px] flex-none flex items-center justify-center bg-[rgba(255,255,255,0.08)] text-[24px]"
              aria-hidden="true"
            >
              ♪
            </div>
            <div class="flex flex-col gap-[var(--muses-space-xs)] min-w-0">
              <h-button
                variant="ghost"
                type="button"
                size="sm"
                :disabled="isEditSubmitting"
                @click="onPickCover"
              >
                选择图片
              </h-button>
              <h-button
                v-if="editCoverPreviewSrc"
                variant="ghost"
                type="button"
                size="sm"
                :disabled="isEditSubmitting"
                @click="onClearCover"
              >
                清除封面
              </h-button>
            </div>
          </div>
          <input
            ref="coverFileInputRef"
            class="hidden"
            type="file"
            accept="image/*"
            @change="onCoverFileChange"
          >
        </div>

        <editForm.Field name="lyrics">
          <template #default="{ field }">
            <h-textarea
              :model-value="field.state.value"
              label="歌词（LRC 文本）"
              :rows="6"
              :disabled="isEditSubmitting"
              @update:model-value="field.handleChange"
              @blur="field.handleBlur"
            />
          </template>
        </editForm.Field>

        <editForm.Field
          name="replayGainDb"
          :validators="{
            onSubmit: ({ value }) => validateReplayGainInput(String(value ?? '')),
          }"
        >
          <template #default="{ field }">
            <h-input
              :model-value="field.state.value"
              label="音量均衡（ReplayGain dB）"
              placeholder="如 -6.5，空=清除"
              :error="typeof field.state.meta.errors[0] === 'string' ? field.state.meta.errors[0] : undefined"
              :invalid="field.state.meta.errors.length > 0"
              :disabled="isEditSubmitting"
              @update:model-value="field.handleChange"
              @blur="field.handleBlur"
            />
          </template>
        </editForm.Field>

        <p v-if="editFormError" class="m-0 text-[length:var(--muses-font-body-sm)] text-[var(--h-color-danger,#f31260)]">
          {{ editFormError }}
        </p>

        <div class="flex justify-end gap-[var(--muses-space-sm)] pt-[var(--muses-space-sm)]">
          <h-button variant="ghost" type="button" :disabled="isEditSubmitting" @click="closeSongEdit">
            取消
          </h-button>
          <h-button variant="primary" type="submit" :disabled="isEditSubmitting">
            {{ isEditSubmitting ? '保存中…' : '保存' }}
          </h-button>
        </div>
      </form>
    </h-bottom-sheet>

    <h-toast
      v-model="toast.visible"
      :variant="toast.variant"
      :duration="toast.duration"
    >
      {{ toast.message }}
    </h-toast>
  </h-popup>
</template>

<script setup lang="ts">
import {
  HBottomSheet,
  HButton,
  HIcon,
  HInput,
  HPopup,
  HRange,
  HTextarea,
  HToast,
} from '@/components/ui'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useForm } from '@tanstack/vue-form'
import { Capacitor } from '@capacitor/core'
import {
  ellipsisVertical,
  languageOffOutline,
  languageOutline,
  list,
  listOutline,
  pause,
  play,
  playSkipBack,
  playSkipForward,
  repeat,
  repeatOutline,
  shuffle,
} from '@/icons'
import { BackgroundRender, LyricPlayer } from '@applemusic-like-lyrics/vue'
import { MeshGradientRenderer } from '@applemusic-like-lyrics/core'
import type { LyricLine, LyricLineMouseEvent } from '@applemusic-like-lyrics/core'
import { parseLrc, parseQrc, parseTTML, parseYrc } from '@applemusic-like-lyrics/lyric'
import '@applemusic-like-lyrics/core/style.css'
import { applyLyricTranslationVisibility } from '@/features/lyrics/display'
import { prepareLyricLinesForDisplay } from '@/features/lyrics/mergeTranslation'
import { loadSongs } from '@/features/library/storage'
import { cacheCoverBytes } from '@/features/library/native'
import {
  isPlaying,
  pausePlayback,
  playerState,
  playNextFromQueue,
  playPreviousFromQueue,
  queueState,
  resumePlayback,
  saveCurrentSongUserEdit,
  seekPlayback,
  setRepeatMode,
  toggleShuffle,
} from '@/features/player/controller'
import { closePlayerOverlay, openQueueOverlay, playerOverlayVisible } from '@/features/player/overlay'
import { actionSheetCancelClass, actionSheetItemClass } from '@/theme/action-sheet'

const activePanel = ref(0)
/** 手势落点判断用的 template ref（取代 closest / class 选择器查询）。 */
const lyricPanelRef = ref<HTMLElement | null>(null)
const lyricPlayerRef = ref<HTMLElement | { $el?: HTMLElement } | null>(null)
const progressRangeRef = ref<HTMLElement | { $el?: HTMLElement } | null>(null)
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
const moreIcon = ellipsisVertical
const translationIcon = computed(() => showLyricTranslation.value ? languageOutline : languageOffOutline)
const previousIcon = playSkipBack
const nextIcon = playSkipForward
const isTabletLayout = computed(() => viewportWidth.value >= 768)

// ── 更多 / 编辑歌曲信息 ──────────────────────────────────────────
const isPlayerActionsOpen = ref(false)
const isSongEditOpen = ref(false)
const editFormError = ref('')
const coverFileInputRef = ref<HTMLInputElement | null>(null)
/** 编辑中的安全封面 URI；null + cleared 表示用户清除 */
const editCoverUri = ref<string | null>(null)
const editCoverCleared = ref(false)
/** 仅用户改过封面时才写入 patch，避免误标 userEditedFields.cover */
const editCoverDirty = ref(false)
/** 打开编辑时的基线，保存时仅提交相对基线有变化的字段 */
const editBaseline = ref({
  title: '',
  artist: '',
  album: '',
  lyrics: '',
  replayGainDb: '',
})
const editCoverPreviewSrc = computed(() => {
  if (editCoverCleared.value) {
    return ''
  }
  const uri = editCoverUri.value || ''
  return uri ? toDisplayableUri(uri) : ''
})

const toast = ref<{
  visible: boolean
  message: string
  variant: 'default' | 'success' | 'warning' | 'danger'
  duration: number
}>({
  visible: false,
  message: '',
  variant: 'default',
  duration: 2200,
})

const showToast = (
  message: string,
  variant: 'default' | 'success' | 'warning' | 'danger' = 'default',
  duration = 2200,
): void => {
  toast.value = { visible: true, message, variant, duration }
}

const validateReplayGainInput = (value: string): string | undefined => {
  const trimmed = value.trim()
  if (!trimmed) {
    return undefined
  }
  const normalized = trimmed.replace(/\s*dB\s*$/i, '').trim()
  const num = Number(normalized)
  if (!Number.isFinite(num)) {
    return '请输入合法的 dB 数值'
  }
  if (Math.abs(num) > 30) {
    return 'ReplayGain 应在 ±30 dB 内'
  }
  return undefined
}

const parseReplayGainInput = (value: string): number | null => {
  const trimmed = value.trim()
  if (!trimmed) {
    return null
  }
  const normalized = trimmed.replace(/\s*dB\s*$/i, '').trim()
  const num = Number(normalized)
  return Number.isFinite(num) ? num : null
}

const editForm = useForm({
  defaultValues: {
    title: '',
    artist: '',
    album: '',
    lyrics: '',
    replayGainDb: '',
  },
  onSubmit: async ({ value }) => {
    editFormError.value = ''
    const songId = playerState.currentSong?.id
    if (!songId) {
      editFormError.value = '当前没有播放中的歌曲。'
      return
    }

    const title = value.title.trim()
    if (!title) {
      editFormError.value = '请填写歌曲标题'
      return
    }

    const rgRaw = value.replayGainDb.trim()
    const rgError = validateReplayGainInput(rgRaw)
    if (rgError) {
      editFormError.value = rgError
      return
    }

    const baseline = editBaseline.value
    const artist = value.artist.trim()
    const album = value.album.trim()
    const lyrics = value.lyrics
    const nextRg = parseReplayGainInput(rgRaw)
    const baseRg = parseReplayGainInput(baseline.replayGainDb)

    // 仅提交相对打开时基线有变化的字段，避免「只改 title 也永久锁死 lyrics/RG」
    const patch: Parameters<typeof saveCurrentSongUserEdit>[0] = {}
    if (title !== baseline.title.trim()) {
      patch.title = title
    }
    if (artist !== baseline.artist.trim()) {
      patch.artist = artist
    }
    if (album !== baseline.album.trim()) {
      patch.album = album
    }
    if (lyrics !== baseline.lyrics) {
      patch.lyrics = lyrics
      patch.lyricsFormat = 'lrc'
    }
    if (nextRg !== baseRg) {
      patch.replayGainTrackDb = nextRg
    }
    if (editCoverDirty.value) {
      patch.coverUri = editCoverCleared.value ? null : (editCoverUri.value || null)
    }

    if (Object.keys(patch).length === 0) {
      isSongEditOpen.value = false
      showToast('没有需要保存的修改', 'default')
      return
    }

    // title 必填：若本次未改 title 但基线为空，仍须带上非空 title 才能过库校验；否则只传变更字段
    if (patch.title === undefined && !baseline.title.trim()) {
      patch.title = title
    }

    const result = await saveCurrentSongUserEdit(patch)

    if (!result.libraryOk) {
      editFormError.value = result.fileError || '保存失败'
      showToast(result.fileError || '保存失败', 'danger')
      return
    }

    isSongEditOpen.value = false
    if (result.fileOk) {
      showToast('已保存', 'success')
    } else {
      const reason = result.fileError ? `：${result.fileError}` : ''
      showToast(`已更新曲库，写入音频文件失败${reason}`, 'warning', 3200)
    }
  },
})

const isEditSubmitting = editForm.useSelector((state) => state.isSubmitting)

const openPlayerActions = () => {
  if (!playerState.currentSong) {
    return
  }
  isPlayerActionsOpen.value = true
}

const onOpenSongEdit = () => {
  isPlayerActionsOpen.value = false
  window.setTimeout(() => {
    seedSongEditForm()
    isSongEditOpen.value = true
  }, 180)
}

const seedSongEditForm = () => {
  const current = playerState.currentSong
  if (!current) {
    return
  }
  const latest = loadSongs().find((item) => item.id === current.id)
  const title = latest?.title ?? current.title ?? ''
  const artist = latest?.artist ?? current.artist ?? ''
  const album = latest?.album ?? current.album ?? ''
  const lyrics = latest?.lyrics ?? playerState.lyrics ?? current.lyrics ?? ''
  const rg = latest?.replayGainTrackDb
  const replayGainDb = rg !== undefined && Number.isFinite(rg) ? String(rg) : ''
  const seed = {
    title,
    artist: artist || '',
    album: album || '',
    lyrics: lyrics || '',
    replayGainDb,
  }
  editForm.reset(seed)
  editBaseline.value = { ...seed }
  editCoverUri.value = latest?.coverUri || current.coverUri || playerState.coverUri || null
  editCoverCleared.value = false
  editCoverDirty.value = false
  editFormError.value = ''
}

const closeSongEdit = () => {
  if (isEditSubmitting.value) {
    return
  }
  isSongEditOpen.value = false
}

const onPickCover = () => {
  coverFileInputRef.value?.click()
}

const onClearCover = () => {
  editCoverUri.value = null
  editCoverCleared.value = true
  editCoverDirty.value = true
}

const onCoverFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  // 允许重复选同一文件
  input.value = ''
  if (!file) {
    return
  }
  try {
    const buffer = await file.arrayBuffer()
    const bytes = new Uint8Array(buffer)
    if (bytes.byteLength === 0 || bytes.byteLength > 5 * 1024 * 1024) {
      showToast('图片过大或为空', 'warning')
      return
    }
    let binary = ''
    const chunk = 0x8000
    for (let i = 0; i < bytes.length; i += chunk) {
      binary += String.fromCharCode(...bytes.subarray(i, i + chunk))
    }
    const base64Data = btoa(binary)
    const songId = playerState.currentSong?.id || 'edit-cover'
    const uri = await cacheCoverBytes({
      cacheKey: `user-cover:${songId}:${Date.now()}`,
      base64Data,
    })
    if (!uri) {
      showToast('封面保存失败', 'danger')
      return
    }
    editCoverUri.value = uri
    editCoverCleared.value = false
    editCoverDirty.value = true
  } catch {
    showToast('读取图片失败', 'danger')
  }
}

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

/** 有译文/音译才出翻译键；prepare 后的行或独立 tlyric 任一即可 */
const hasLyricTranslation = computed(() => {
  if (playerState.lyricsTranslation?.trim()) {
    return true
  }
  return lyricLines.value.some(
    (line) => !!line.translatedLyric?.trim() || !!line.romanLyric?.trim(),
  )
})

/** 宽屏无播放键且无译时不挂空 chrome */
const showLyricFloatingActions = computed(
  () => !!playerState.currentSong && (hasLyricTranslation.value || !isTabletLayout.value),
)

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

/** 取出组件实例或原生元素的真实 DOM。 */
const unwrapRef = (value: HTMLElement | { $el?: HTMLElement } | null): HTMLElement | null => {
  if (!value) {
    return null
  }
  return value instanceof HTMLElement ? value : (value.$el ?? null)
}

/** 进度条范围元素（取代 `.progress-range` 选择器查询）。 */
const progressRangeEl = computed<HTMLElement | null>(() => unwrapRef(progressRangeRef.value))

/** 原生交互控件选择器——全是标准元素/属性选择器，非 class 标记，属合法声明式查询。 */
const INTERACTIVE_SELECTOR =
  'input, textarea, select, button, a, [role="button"], [contenteditable="true"]'

const isInteractiveElement = (el: Element): boolean => {
  // 进度条用 ref 识别（取代原 `.progress-range` class 选择器）
  const progressEl = progressRangeEl.value
  if (progressEl && progressEl.contains(el)) {
    return true
  }
  return Boolean(el.closest(INTERACTIVE_SELECTOR))
}

const isNativeInteractiveTarget = (target: EventTarget | null): boolean => {
  if (!(target instanceof Element)) {
    return false
  }
  if (isInteractiveElement(target)) {
    return true
  }
  return false
}

const isNativeInteractiveEvent = (event: TouchEvent | Event): boolean => {
  if (isNativeInteractiveTarget(event.target)) {
    return true
  }
  // Shadow DOM / 合成事件路径里目标可能是组件宿主（h-button 原生按钮上有 Shadow DOM）；
  // composedPath 能穿透 shadow，closest/contains 在 light DOM 上能识别。元素级 API，非标记类查询。
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
  // 用 template ref 的 contains 判断触点是否落在歌词面板/歌词播放器内，取代 closest + classList.contains 标记类查询。
  const target = event.target
  const lyricPanelEl = lyricPanelRef.value
  if (lyricPanelEl instanceof HTMLElement && target instanceof Node && lyricPanelEl.contains(target)) {
    return true
  }
  const lyricPlayerEl = unwrapRef(lyricPlayerRef.value)
  return Boolean(lyricPlayerEl && target instanceof Node && lyricPlayerEl.contains(target))
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

