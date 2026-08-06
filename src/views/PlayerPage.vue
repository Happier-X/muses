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
              <img v-if="displayCoverSrc" class="cover aspect-1 h-auto max-w-full object-cover" :src="displayCoverSrc" alt="歌曲封面" />
              <div v-else class="cover placeholder-cover aspect-1 h-auto max-w-full object-cover">♪</div>
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
              @wheel="onLyricUserScroll"
              @touchmove="onLyricUserScroll"
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
              class="lyric-fab pointer-events-none"
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
              class="lyric-fab lyric-play-toggle pointer-events-none"
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

    <!-- 编辑歌曲信息：title/artist/album/封面/歌词/RG + 云端获取 -->
    <h-bottom-sheet v-model="isSongEditOpen" title="编辑歌曲信息">
      <form
        class="flex flex-col gap-[var(--muses-space-md)] pb-[var(--muses-space-lg)] px-[var(--muses-space-lg)]"
        @submit.prevent="editForm.handleSubmit"
      >
        <!-- 云端强制搜：预览 + 勾选应用，不自动覆盖表单 -->
        <section
          class="flex flex-col gap-[var(--muses-space-sm)] rounded-[12px] border border-[color:var(--h-color-border,rgba(0,0,0,0.08))] p-[var(--muses-space-md)]"
          aria-label="从云端获取元信息"
        >
          <div class="flex items-center justify-between gap-[var(--muses-space-sm)]">
            <p class="m-0 text-[length:var(--muses-font-body-sm)] font-medium">云端元信息</p>
            <h-button
              variant="ghost"
              type="button"
              size="sm"
              :disabled="isEditSubmitting || cloudFetching || cloudApplying"
              aria-label="从云端获取标题艺人专辑封面与歌词"
              @click="onFetchCloudMeta"
            >
              {{ cloudFetching ? '获取中…' : '从云端获取' }}
            </h-button>
          </div>

          <p
            v-if="cloudStatusMessage"
            class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--h-color-ink-muted,#888)]"
          >
            {{ cloudStatusMessage }}
          </p>

          <template v-if="cloudResult">
            <div class="flex flex-col gap-[var(--muses-space-xs)]">
              <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--h-color-ink-muted,#888)]">
                文本 · {{ dimStatusLabel(cloudResult.text.status) }}
                <template v-if="selectedTextHit">
                  ：{{ selectedTextHit.title || '—' }} / {{ selectedTextHit.artist || '—' }} / {{ selectedTextHit.album || '—' }}
                  <span v-if="selectedTextHit.source">（{{ selectedTextHit.source }}）</span>
                </template>
              </p>
              <h-button
                v-if="cloudResult.text.items.length > 1"
                variant="ghost"
                type="button"
                size="sm"
                class="self-start"
                :disabled="cloudFetching || cloudApplying"
                aria-label="更换文本候选"
                @click="cloudExpandText = !cloudExpandText"
              >
                {{ cloudExpandText ? '收起文本候选' : '更换文本' }}
              </h-button>
              <div v-if="cloudExpandText" class="flex flex-col gap-[4px] max-h-36 overflow-y-auto">
                <button
                  v-for="(item, idx) in cloudResult.text.items"
                  :key="`text-${idx}-${item.source}`"
                  type="button"
                  class="text-left text-[length:var(--muses-font-body-sm)] p-[6px] rounded-[8px] border-0 bg-transparent"
                  :class="idx === cloudTextIndex ? 'bg-[rgba(0,0,0,0.06)]' : ''"
                  @click="cloudTextIndex = idx"
                >
                  {{ item.title || '—' }} · {{ item.artist || '—' }} · {{ item.album || '—' }}
                  <span class="opacity-60">（{{ item.source }}）</span>
                </button>
              </div>

              <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--h-color-ink-muted,#888)]">
                封面 · {{ dimStatusLabel(cloudResult.cover.status) }}
                <template v-if="selectedCoverHit">（{{ selectedCoverHit.source }}）</template>
              </p>
              <img
                v-if="selectedCoverHit"
                class="w-14 h-14 rounded-[8px] object-cover"
                :src="selectedCoverHit.remoteUrl"
                alt="云端封面预览"
              >
              <h-button
                v-if="cloudResult.cover.items.length > 1"
                variant="ghost"
                type="button"
                size="sm"
                class="self-start"
                :disabled="cloudFetching || cloudApplying"
                aria-label="更换封面候选"
                @click="cloudExpandCover = !cloudExpandCover"
              >
                {{ cloudExpandCover ? '收起封面候选' : '更换封面' }}
              </h-button>
              <div v-if="cloudExpandCover" class="flex flex-wrap gap-[8px] max-h-40 overflow-y-auto">
                <button
                  v-for="(item, idx) in cloudResult.cover.items"
                  :key="`cover-${idx}-${item.source}`"
                  type="button"
                  class="p-0 border-0 bg-transparent rounded-[8px] overflow-hidden ring-offset-1"
                  :class="idx === cloudCoverIndex ? 'ring-2 ring-[var(--h-color-primary,#0070f0)]' : ''"
                  :aria-label="`封面候选 ${idx + 1} ${item.source}`"
                  @click="cloudCoverIndex = idx"
                >
                  <img class="w-12 h-12 object-cover" :src="item.remoteUrl" alt="">
                </button>
              </div>

              <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--h-color-ink-muted,#888)]">
                歌词 · {{ dimStatusLabel(cloudResult.lyrics.status) }}
                <template v-if="selectedLyricsHit">
                  ：{{ selectedLyricsHit.source }} / {{ selectedLyricsHit.format }}
                  <span v-if="selectedLyricsHit.translationText">（含译文，应用仅主词）</span>
                </template>
              </p>
              <p
                v-if="selectedLyricsHit"
                class="m-0 text-[length:var(--muses-font-body-sm)] whitespace-pre-wrap line-clamp-3 opacity-80"
              >
                {{ lyricsPreview(selectedLyricsHit.text) }}
              </p>
              <h-button
                v-if="cloudResult.lyrics.items.length > 1"
                variant="ghost"
                type="button"
                size="sm"
                class="self-start"
                :disabled="cloudFetching || cloudApplying"
                aria-label="更换歌词候选"
                @click="cloudExpandLyrics = !cloudExpandLyrics"
              >
                {{ cloudExpandLyrics ? '收起歌词候选' : '更换歌词' }}
              </h-button>
              <div v-if="cloudExpandLyrics" class="flex flex-col gap-[4px] max-h-36 overflow-y-auto">
                <button
                  v-for="(item, idx) in cloudResult.lyrics.items"
                  :key="`lyrics-${idx}-${item.source}`"
                  type="button"
                  class="text-left text-[length:var(--muses-font-body-sm)] p-[6px] rounded-[8px] border-0 bg-transparent"
                  :class="idx === cloudLyricsIndex ? 'bg-[rgba(0,0,0,0.06)]' : ''"
                  @click="cloudLyricsIndex = idx"
                >
                  {{ item.source }} · {{ item.format }}
                  <span class="opacity-60">{{ lyricsPreview(item.text, 40) }}</span>
                </button>
              </div>
            </div>

            <div class="flex flex-col gap-[6px] pt-[var(--muses-space-xs)]">
              <p class="m-0 text-[length:var(--muses-font-body-sm)]">应用到表单的字段</p>
              <div class="flex flex-wrap gap-x-[12px] gap-y-[6px]">
                <label class="inline-flex items-center gap-[6px] text-[length:var(--muses-font-body-sm)]">
                  <h-checkbox
                    :model-value="cloudChecks.title"
                    :disabled="!selectedTextHit?.title?.trim() || cloudFetching || cloudApplying"
                    aria-label="应用标题"
                    @update:model-value="cloudChecks.title = $event"
                  />
                  标题
                </label>
                <label class="inline-flex items-center gap-[6px] text-[length:var(--muses-font-body-sm)]">
                  <h-checkbox
                    :model-value="cloudChecks.artist"
                    :disabled="!selectedTextHit?.artist?.trim() || cloudFetching || cloudApplying"
                    aria-label="应用艺术家"
                    @update:model-value="cloudChecks.artist = $event"
                  />
                  艺术家
                </label>
                <label class="inline-flex items-center gap-[6px] text-[length:var(--muses-font-body-sm)]">
                  <h-checkbox
                    :model-value="cloudChecks.album"
                    :disabled="!selectedTextHit?.album?.trim() || cloudFetching || cloudApplying"
                    aria-label="应用专辑"
                    @update:model-value="cloudChecks.album = $event"
                  />
                  专辑
                </label>
                <label class="inline-flex items-center gap-[6px] text-[length:var(--muses-font-body-sm)]">
                  <h-checkbox
                    :model-value="cloudChecks.cover"
                    :disabled="!selectedCoverHit || cloudFetching || cloudApplying"
                    aria-label="应用封面"
                    @update:model-value="cloudChecks.cover = $event"
                  />
                  封面
                </label>
                <label class="inline-flex items-center gap-[6px] text-[length:var(--muses-font-body-sm)]">
                  <h-checkbox
                    :model-value="cloudChecks.lyrics"
                    :disabled="!selectedLyricsHit || cloudFetching || cloudApplying"
                    aria-label="应用歌词"
                    @update:model-value="cloudChecks.lyrics = $event"
                  />
                  歌词
                </label>
              </div>
              <h-button
                variant="primary"
                type="button"
                size="sm"
                class="self-start"
                :disabled="!canApplyCloud || cloudFetching || cloudApplying || isEditSubmitting"
                aria-label="将勾选的云端字段应用到表单"
                @click="onApplyCloudMeta"
              >
                {{ cloudApplying ? '应用中…' : '应用到表单' }}
              </h-button>
            </div>
          </template>
        </section>

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
              label="歌词文本"
              :rows="6"
              :disabled="isEditSubmitting"
              @update:model-value="onEditLyricsInput(field, $event)"
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
  HCheckbox,
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
  searchEditCloudMeta,
  type EditCloudMetaResult,
  type EditDimStatus,
} from '@/features/editMeta'
import { cacheRemoteCover } from '@/features/player/native'
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
import type { SongLyricsFormat } from '@/features/library/types'
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

/** 歌词滚动后自动回高亮行：停止滚动 2 秒后调 AMLL resetScroll + calcLayout */
let lyricScrollBackTimer: ReturnType<typeof setTimeout> | null = null
const getLyricPlayerInstance = ():
  | { resetScroll(): void; calcLayout(sync?: boolean, force?: boolean): Promise<void> }
  | null => {
  // expose() 返回对象经 proxyRefs 自动解包：lyricPlayer 直接是 player 实例（非 Ref，无 .value）
  const comp = lyricPlayerRef.value as unknown as {
    lyricPlayer?: { resetScroll(): void; calcLayout(sync?: boolean, force?: boolean): Promise<void> }
  } | null
  return comp?.lyricPlayer ?? null
}
const onLyricUserScroll = (): void => {
  if (lyricScrollBackTimer) clearTimeout(lyricScrollBackTimer)
  lyricScrollBackTimer = setTimeout(() => {
    lyricScrollBackTimer = null
    // AMLL 自带 5 秒归位（仅播放中时间更新时生效）；此处 2 秒主动归位，暂停时也生效
    const player = getLyricPlayerInstance()
    if (player) {
      player.resetScroll()
      void player.calcLayout()
    }
  }, 2000)
}
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
/** 应用歌词时的 format；保存 dirty 时写入 patch；手改文本回落 lrc */
const editLyricsFormat = ref<SongLyricsFormat | null>(null)
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
      // 云端应用可带 ttml/yrc/qrc；手改文本默认 lrc
      patch.lyricsFormat = editLyricsFormat.value || 'lrc'
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
  editLyricsFormat.value = (latest?.lyricsFormat as SongLyricsFormat | undefined)
    || (playerState.lyricsFormat as SongLyricsFormat | null)
    || null
  editFormError.value = ''
  resetCloudMetaState({ abort: true })
}

const closeSongEdit = () => {
  if (isEditSubmitting.value) {
    return
  }
  abortCloudFetch()
  isSongEditOpen.value = false
}

// ── 云端元信息（强制搜 + 勾选应用）──────────────────────────────
const cloudFetching = ref(false)
const cloudApplying = ref(false)
const cloudResult = ref<EditCloudMetaResult | null>(null)
const cloudStatusMessage = ref('')
const cloudTextIndex = ref(0)
const cloudCoverIndex = ref(0)
const cloudLyricsIndex = ref(0)
const cloudExpandText = ref(false)
const cloudExpandCover = ref(false)
const cloudExpandLyrics = ref(false)
const cloudChecks = ref({
  title: false,
  artist: false,
  album: false,
  cover: false,
  lyrics: false,
})
let cloudAbort: AbortController | null = null
let cloudFetchGeneration = 0

const abortCloudFetch = (): void => {
  cloudAbort?.abort()
  cloudAbort = null
  cloudFetchGeneration += 1
  cloudFetching.value = false
}

const resetCloudMetaState = (options?: { abort?: boolean }): void => {
  if (options?.abort) {
    abortCloudFetch()
  }
  cloudResult.value = null
  cloudStatusMessage.value = ''
  cloudTextIndex.value = 0
  cloudCoverIndex.value = 0
  cloudLyricsIndex.value = 0
  cloudExpandText.value = false
  cloudExpandCover.value = false
  cloudExpandLyrics.value = false
  cloudChecks.value = {
    title: false,
    artist: false,
    album: false,
    cover: false,
    lyrics: false,
  }
  cloudApplying.value = false
}

const dimStatusLabel = (status: EditDimStatus): string => {
  switch (status) {
    case 'ok':
      return '已命中'
    case 'network':
      return '网络异常'
    case 'aborted':
      return '已取消'
    default:
      return '无匹配'
  }
}

const lyricsPreview = (text: string, max = 80): string => {
  const oneLine = text.replace(/\s+/g, ' ').trim()
  if (oneLine.length <= max) {
    return oneLine
  }
  return `${oneLine.slice(0, max)}…`
}

const selectedTextHit = computed(() => {
  const items = cloudResult.value?.text.items
  if (!items?.length) {
    return null
  }
  return items[cloudTextIndex.value] ?? items[0] ?? null
})

const selectedCoverHit = computed(() => {
  const items = cloudResult.value?.cover.items
  if (!items?.length) {
    return null
  }
  return items[cloudCoverIndex.value] ?? items[0] ?? null
})

const selectedLyricsHit = computed(() => {
  const items = cloudResult.value?.lyrics.items
  if (!items?.length) {
    return null
  }
  return items[cloudLyricsIndex.value] ?? items[0] ?? null
})

const canApplyCloud = computed(() => {
  const c = cloudChecks.value
  if (c.title && selectedTextHit.value?.title?.trim()) {
    return true
  }
  if (c.artist && selectedTextHit.value?.artist?.trim()) {
    return true
  }
  if (c.album && selectedTextHit.value?.album?.trim()) {
    return true
  }
  if (c.cover && selectedCoverHit.value) {
    return true
  }
  if (c.lyrics && selectedLyricsHit.value) {
    return true
  }
  return false
})

const seedCloudChecksFromSelection = (): void => {
  const text = selectedTextHit.value
  cloudChecks.value = {
    title: !!text?.title?.trim(),
    artist: !!text?.artist?.trim(),
    album: !!text?.album?.trim(),
    cover: !!selectedCoverHit.value,
    lyrics: !!selectedLyricsHit.value,
  }
}

const onFetchCloudMeta = async (): Promise<void> => {
  const song = playerState.currentSong
  if (!song?.id || cloudFetching.value || isEditSubmitting.value) {
    return
  }

  const title = String(editForm.getFieldValue('title') ?? '').trim() || song.title?.trim() || ''
  if (!title) {
    cloudStatusMessage.value = '请先填写标题再获取'
    showToast('请先填写标题再获取', 'warning')
    return
  }

  const artist = String(editForm.getFieldValue('artist') ?? '').trim() || undefined
  const album = String(editForm.getFieldValue('album') ?? '').trim() || undefined

  abortCloudFetch()
  const controller = new AbortController()
  cloudAbort = controller
  const generation = cloudFetchGeneration
  const songId = song.id
  cloudFetching.value = true
  cloudResult.value = null
  cloudStatusMessage.value = '正在从多平台获取…'
  cloudExpandText.value = false
  cloudExpandCover.value = false
  cloudExpandLyrics.value = false

  try {
    const result = await searchEditCloudMeta(
      {
        songId,
        title,
        artist,
        album,
        durationSec: Number.isFinite(playerState.duration) && playerState.duration > 0
          ? playerState.duration
          : undefined,
      },
      { signal: controller.signal },
    )

    if (
      generation !== cloudFetchGeneration
      || controller.signal.aborted
      || !isSongEditOpen.value
      || playerState.currentSong?.id !== songId
    ) {
      return
    }

    cloudResult.value = result
    cloudTextIndex.value = result.text.defaultIndex
    cloudCoverIndex.value = result.cover.defaultIndex
    cloudLyricsIndex.value = result.lyrics.defaultIndex
    seedCloudChecksFromSelection()

    const anyOk =
      result.text.status === 'ok'
      || result.cover.status === 'ok'
      || result.lyrics.status === 'ok'
    const anyNetwork =
      result.text.status === 'network'
      || result.cover.status === 'network'
      || result.lyrics.status === 'network'

    if (anyOk) {
      const parts = [
        result.text.status === 'ok' ? `文本 ${result.text.items.length}` : null,
        result.cover.status === 'ok' ? `封面 ${result.cover.items.length}` : null,
        result.lyrics.status === 'ok' ? `歌词 ${result.lyrics.items.length}` : null,
      ].filter(Boolean)
      cloudStatusMessage.value = `已获取：${parts.join(' · ')}（勾选后应用到表单）`
    } else if (anyNetwork) {
      cloudStatusMessage.value = '网络异常，请稍后重试'
      showToast('云端获取网络异常', 'warning')
    } else {
      cloudStatusMessage.value = '未找到匹配结果'
      showToast('未找到云端匹配', 'default')
    }
  } catch (error) {
    if (controller.signal.aborted || generation !== cloudFetchGeneration) {
      return
    }
    cloudStatusMessage.value = '获取失败，请重试'
    showToast('云端获取失败', 'danger')
    console.warn('[edit-cloud-meta] fetch failed', error)
  } finally {
    if (generation === cloudFetchGeneration) {
      cloudFetching.value = false
    }
    if (cloudAbort === controller) {
      cloudAbort = null
    }
  }
}

const onEditLyricsInput = (
  field: { handleChange: (value: string) => void },
  value: string | number | null | undefined,
): void => {
  field.handleChange(String(value ?? ''))
  // 手改文本默认 lrc；云端应用走 setFieldValue，不会触发本 handler
  editLyricsFormat.value = 'lrc'
}

const onApplyCloudMeta = async (): Promise<void> => {
  if (!canApplyCloud.value || cloudApplying.value || isEditSubmitting.value) {
    return
  }

  const songId = playerState.currentSong?.id
  if (!songId || !isSongEditOpen.value) {
    return
  }

  cloudApplying.value = true
  const checks = { ...cloudChecks.value }
  const text = selectedTextHit.value
  const cover = selectedCoverHit.value
  const lyrics = selectedLyricsHit.value
  const applied: string[] = []
  const failed: string[] = []

  const stillActive = (): boolean =>
    isSongEditOpen.value && playerState.currentSong?.id === songId

  try {
    if (checks.title && text?.title?.trim()) {
      editForm.setFieldValue('title', text.title.trim())
      applied.push('标题')
    }
    if (checks.artist && text?.artist?.trim()) {
      editForm.setFieldValue('artist', text.artist.trim())
      applied.push('艺术家')
    }
    if (checks.album && text?.album?.trim()) {
      editForm.setFieldValue('album', text.album.trim())
      applied.push('专辑')
    }
    if (checks.lyrics && lyrics?.text?.trim()) {
      editForm.setFieldValue('lyrics', lyrics.text)
      const fmt = lyrics.format
      editLyricsFormat.value =
        fmt === 'ttml' || fmt === 'yrc' || fmt === 'qrc' || fmt === 'lrc' ? fmt : 'lrc'
      applied.push('歌词')
    }
    if (checks.cover && cover?.remoteUrl) {
      const localUri = await cacheRemoteCover({
        url: cover.remoteUrl,
        cacheKey: `edit-cloud-cover:${songId}:${cover.source}:${Date.now()}`,
      })
      // 关 sheet / 切歌后丢弃封面回写，避免串曲
      if (!stillActive()) {
        return
      }
      if (localUri) {
        editCoverUri.value = localUri
        editCoverCleared.value = false
        editCoverDirty.value = true
        applied.push('封面')
      } else {
        failed.push('封面')
      }
    }

    if (!stillActive()) {
      return
    }

    if (applied.length === 0 && failed.length === 0) {
      showToast('请勾选要应用的字段', 'default')
      return
    }
    if (failed.length && applied.length) {
      showToast(`已应用 ${applied.join('、')}；${failed.join('、')}失败`, 'warning', 3200)
    } else if (failed.length) {
      showToast(`${failed.join('、')}应用失败`, 'danger')
    } else {
      showToast(`已应用到表单：${applied.join('、')}`, 'success')
    }
  } finally {
    cloudApplying.value = false
  }
}

watch(isSongEditOpen, (open) => {
  if (!open) {
    // 关 sheet：作废在途请求并清空未应用预览
    resetCloudMetaState({ abort: true })
  }
})

watch(
  () => playerState.currentSong?.id,
  (nextId, prevId) => {
    if (prevId && nextId !== prevId && isSongEditOpen.value) {
      resetCloudMetaState({ abort: true })
      isSongEditOpen.value = false
    }
  },
)

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
  // 播放前歌词未加载（无翻译数据），移动端按钮仍常显：点击时提示
  if (!hasLyricTranslation.value) {
    showToast('当前歌曲暂无翻译歌词', 'default')
    return
  }
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
 * 仅用户进度条手势写入 preview：程序化更新 value 时也可能触发 input，
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
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0')
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
  if (lyricScrollBackTimer) {
    clearTimeout(lyricScrollBackTimer)
    lyricScrollBackTimer = null
  }
  if (bufferHintTimer !== null) {
    clearTimeout(bufferHintTimer)
    bufferHintTimer = null
  }
  seekGestureLocked.value = false
  resetDragState()
})
</script>

