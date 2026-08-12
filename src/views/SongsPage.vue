<template>
  <div ref="pageRef" class="m-page songs-page">
    <div class="songs-page__navbar-wrap">
      <m-navbar right-class="songs-page__right">
        <template #title>歌曲</template>
      </m-navbar>
    </div>
    <div class="m-content songs-page__content">
      <div v-if="songs.length === 0" class="songs-page__empty">
        <m-empty
          title="还没有歌曲"
          description="请先到音源页添加并扫描音源。"
        />
      </div>

      <div
        v-else
        ref="listParentRef"
        class="songs-page__list"
      >
        <div class="songs-page__shuffle-bar">
          <div class="relative h-full w-full">
            <div class="songs-page__shuffle-blur" aria-hidden="true" />
            <div class="songs-page__shuffle-glass" aria-hidden="true" />
            <m-button
              component="button"
              variant="clear"
              class="songs-page__shuffle-btn"
              aria-label="随机播放全部"
              @click="onShuffleAll"
            >
              <component :is="shuffle" aria-hidden="true" class="songs-page__shuffle-icon" />
              <span>{{ songs.length }} 首</span>
            </m-button>
          </div>
        </div>
        <m-list :dividers="false" class="songs-page__list-root">
          <div class="relative w-full" :style="{ height: `${totalSize}px` }">
            <div
              v-for="virtualRow in virtualRows"
              :key="songs[virtualRow.index].id"
              :ref="measureVirtualRow"
              class="songs-page__virtual-row"
              :data-index="virtualRow.index"
              :style="{ transform: `translateY(${virtualRow.start}px)` }"
            >
              <m-list-item
                :chevron="false"
                link
                :title="songs[virtualRow.index].title"
                :subtitle="`${getSongArtistName(songs[virtualRow.index])} - ${getSongAlbumName(songs[virtualRow.index])}`"
                title-class="songs-page__row-title"
                subtitle-class="songs-page__row-subtitle"
                class="songs-page__row"
                :class="songItemClass(songs[virtualRow.index].id)"
                :data-song-id="songs[virtualRow.index].id"
                role="button"
                tabindex="0"
                @click="playSong(songs[virtualRow.index])"
              >
                <template #media>
                  <m-cover :src="getSongCoverSrc(songs[virtualRow.index])" :size="48" radius="sm" alt="" />
                </template>
                <template #after>
                  <m-button
                    component="button"
                    variant="clear"
                    rounded
                    class="songs-page__more-btn"
                    aria-label="更多歌曲操作"
                    @click.stop="openSongActions(songs[virtualRow.index])"
                  >
                    <component :is="ellipsisVertical" aria-hidden="true" class="songs-page__more-icon" />
                  </m-button>
                </template>
              </m-list-item>
            </div>
          </div>
        </m-list>
      </div>

      <m-actions :opened="isSongActionsOpen" @backdropclick="isSongActionsOpen = false">
        <m-actions-group>
          <m-actions-label>歌曲操作</m-actions-label>
          <m-actions-button @click="onAddToQueue">添加到队列</m-actions-button>
          <m-actions-button @click="onPickPlaylist">加入歌单…</m-actions-button>
          <m-actions-button @click="isSongActionsOpen = false">取消</m-actions-button>
        </m-actions-group>
      </m-actions>

      <m-actions :opened="isPlaylistPickOpen" @backdropclick="isPlaylistPickOpen = false">
        <m-actions-group>
          <m-actions-label>加入歌单</m-actions-label>
          <m-actions-button
            v-for="pl in playlistList"
            :key="pl.id"
            @click="onAddToPlaylist(pl.id)"
          >
            {{ pl.name }}
          </m-actions-button>
          <m-actions-button @click="onCreateNewPlaylist">新建歌单</m-actions-button>
          <m-actions-button @click="isPlaylistPickOpen = false">取消</m-actions-button>
        </m-actions-group>
      </m-actions>

      <m-dialog :opened="isCreatePlaylistOpen" title="新建歌单">
        <m-list inset>
          <m-list-input
            label="歌单名称"
            type="text"
            :value="newPlaylistName"
            placeholder="歌单名称"
            maxlength="80"
            clear-button
            @input="onNewPlaylistNameInput"
          />
        </m-list>
        <template #buttons>
          <m-dialog-button @click="isCreatePlaylistOpen = false">取消</m-dialog-button>
          <m-dialog-button strong @click="onConfirmCreatePlaylist">创建并加入</m-dialog-button>
        </template>
      </m-dialog>

      <m-fab
        v-if="showJumpBubble"
        class="songs-page__jump-fab"
        aria-label="跳转到当前播放"
        @click="scrollToCurrentSong"
      >
        <component :is="crosshair" aria-hidden="true" class="songs-page__jump-icon" />
      </m-fab>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, type ComponentPublicInstance } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { Capacitor } from '@capacitor/core'
import { crosshair, ellipsisVertical, shuffle } from '@/icons'
import {
  MActions, MActionsButton, MActionsGroup, MActionsLabel,
  MButton, MDialog, MDialogButton, MFab, MList, MListItem, MListInput,
  MNavbar, MCover, MEmpty,
} from '@/components/ui'
import { loadSongs, SONGS_UPDATED_EVENT } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { getSongAlbumName, getSongArtistName, sortSongsForDisplay } from '@/features/library/views'
import {
  addSongToPlaylist,
  createPlaylist,
  loadPlaylists,
} from '@/features/playlist'
import {
  clearQueue,
  enqueueSong,
  enqueueSongs,
  playerState,
  playSong,
  selectSongAtIndex,
  shuffleEnabled,
  toggleShuffle,
} from '@/features/player/controller'

const songs = ref<SongItem[]>([])
const listParentRef = ref<HTMLElement | null>(null)
/** tab 切换时的滚动位置保留（sessionStorage 持久，组件卸载/重挂载与懒加载 chunk 重执行均安全） */
const SCROLL_SAVE_KEY = 'muses:songs-scroll-top'
let savedSongsScrollTop = Number(sessionStorage.getItem(SCROLL_SAVE_KEY) || 0)
const actionSong = ref<SongItem | null>(null)
const isSongActionsOpen = ref(false)
const isPlaylistPickOpen = ref(false)
const isCreatePlaylistOpen = ref(false)
const songItemClass = (songId: string): string => {
  const classes: string[] = []
  if (playerState.currentSong?.id === songId) {
    classes.push('is-playing')
  }
  return classes.join(' ')
}

/** 大曲库只渲染可视行，降低滚动/卡顿（#50） */
const rowVirtualizer = useVirtualizer(
  computed(() => ({
    count: songs.value.length,
    getScrollElement: () => listParentRef.value,
    estimateSize: () => 72,
    overscan: 8,
  })),
)

const measureVirtualRow = (element: Element | ComponentPublicInstance | null): void => {
  rowVirtualizer.value.measureElement(element instanceof HTMLElement ? element : null)
}

const virtualRows = computed(() => {
  const items = rowVirtualizer.value.getVirtualItems()
  if (items.length > 0) {
    return items
  }
// 虚拟列表只剩可视行；首帧 / stub 退化逻辑保留（已无单测但为审美/强健性保留）
  return songs.value.map((_, index) => ({
    index,
    start: index * 72,
    size: 72,
    end: (index + 1) * 72,
    key: index,
  }))
})
const totalSize = computed(() => {
  const measured = rowVirtualizer.value.getTotalSize()
  if (measured > 0) {
    return measured
  }
  return songs.value.length * 72
})

const currentPlayingInList = computed(() => {
  const currentId = playerState.currentSong?.id
  if (!currentId) {
    return false
  }
  return songs.value.some((song) => song.id === currentId)
})

/** 当前歌曲在 songs 中的下标（-1 表示不在列表） */
const currentSongIndex = computed(() => {
  const currentId = playerState.currentSong?.id
  if (!currentId) return -1
  return songs.value.findIndex((song) => song.id === currentId)
})

/** 当前歌曲行是否在可视区（渲染行含 overscan） */
const currentSongInViewport = computed(() => {
  const idx = currentSongIndex.value
  if (idx < 0) return false
  return virtualRows.value.some((item) => item.index === idx)
})

/** 列表滚动中（防抖 300ms）：滚动时隐藏气泡，避免遮挡行的更多按钮 */
const isListScrolling = ref(false)
let scrollIdleTimer: ReturnType<typeof setTimeout> | null = null
const onListScroll = (): void => {
  isListScrolling.value = true
  if (scrollIdleTimer) clearTimeout(scrollIdleTimer)
  scrollIdleTimer = setTimeout(() => {
    isListScrolling.value = false
  }, 300)
}

/** 跳转气泡：当前歌曲在列表且不在可视区且未滚动中才显示（跳转后自动隐藏，不挡更多按钮） */
const showJumpBubble = computed(
  () => currentPlayingInList.value && !currentSongInViewport.value && !isListScrolling.value,
)

const onNewPlaylistNameInput = (e: Event): void => {
  newPlaylistName.value = (e.target as HTMLInputElement).value
}

const refreshSongs = () => {
  songs.value = sortSongsForDisplay(loadSongs())
}

const openSongActions = (song: SongItem) => {
  actionSong.value = song
  isSongActionsOpen.value = true
}

const newPlaylistName = ref('')

const onAddToQueue = () => {
  if (actionSong.value) {
    enqueueSong(actionSong.value)
  }
  isSongActionsOpen.value = false
}

const onPickPlaylist = () => {
  isSongActionsOpen.value = false
  // 等主 sheet 关闭后再开，避免叠层冲突
  window.setTimeout(() => {
    isPlaylistPickOpen.value = true
  }, 180)
}

const playlistList = computed(() => {
  return loadPlaylists().slice().sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
})

const onAddToPlaylist = (playlistId: string) => {
  if (actionSong.value) {
    addSongToPlaylist(playlistId, actionSong.value.id)
  }
  isPlaylistPickOpen.value = false
}

const onCreateNewPlaylist = () => {
  isPlaylistPickOpen.value = false
  newPlaylistName.value = ''
  window.setTimeout(() => {
    isCreatePlaylistOpen.value = true
  }, 180)
}

const onConfirmCreatePlaylist = () => {
  const name = newPlaylistName.value.trim()
  if (!name || !actionSong.value) return
  const created = createPlaylist(name)
  if (created) {
    addSongToPlaylist(created.id, actionSong.value.id)
  }
  isCreatePlaylistOpen.value = false
}

const onShuffleAll = () => {
  if (songs.value.length === 0) {
    return
  }

  clearQueue()
  enqueueSongs(songs.value)
  if (!shuffleEnabled()) {
    toggleShuffle()
  }
  const first = selectSongAtIndex(0)
  if (first) {
    void playSong(first)
  }
}

const scrollToCurrentSong = async () => {
  const currentId = playerState.currentSong?.id
  if (!currentId) {
    return
  }

  const index = songs.value.findIndex((song) => song.id === currentId)
  if (index < 0) {
    return
  }

  // 仅用 virtualizer 定位；禁止再 scrollIntoView + scroll-margin（滚动端口已在 navbar/shuffle 下，二次偏移会连点下移）
  rowVirtualizer.value.scrollToIndex(index, { align: 'start', behavior: 'smooth' })
  await nextTick()
  await new Promise<void>((resolve) => {
    requestAnimationFrame(() => resolve())
  })
  // measure 完成后兜底一次，仍不走 scrollIntoView
  rowVirtualizer.value.scrollToIndex(index, { align: 'start' })
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

const getSongCoverSrc = (song: SongItem): string => {
  return song.coverUri ? toDisplayableUri(song.coverUri) : ''
}

onMounted(() => {
  refreshSongs()
  if (typeof window !== 'undefined') {
    window.addEventListener(SONGS_UPDATED_EVENT, refreshSongs)
  }
  const mountAt = Date.now()
  let interacted = false
  const stop = () => { interacted = true }
  // 挂载后 4 秒防漂移 + 挂滚动位置保存。
  // 背景：WebView 首屏布局未稳时虚拟列表可能被误滚到底（scrollToCurrentSong 未调用、
  // overflow-anchor:none 无效；疑 TanStack Virtual measure 期间布局漂移），冷启动与
  // tab 切回（重新挂载）均会触发。统一处理：期望位置 = 保存值（有则恢复）或 0（顶部），
  // 4 秒内无用户交互且 scrollTop 漂移远离期望位置时拉回；用户一交互（touchstart/wheel）
  // 立即停止，避免打断手动滚动。
  let attached = false
  const attachScrollSave = () => {
    const cur = listParentRef.value
    if (!cur || attached) return
    attached = true
    cur.addEventListener('touchstart', stop, { once: true, passive: true })
    cur.addEventListener('wheel', stop, { once: true, passive: true })
    cur.addEventListener('scroll', onListScroll, { passive: true })
    cur.addEventListener('scroll', () => {
      if (Date.now() - mountAt < 4000) return
      savedSongsScrollTop = cur.scrollTop
      sessionStorage.setItem(SCROLL_SAVE_KEY, String(cur.scrollTop))
    }, { passive: true })
  }
  const guard = () => {
    const cur = listParentRef.value
    if (!cur || interacted || Date.now() - mountAt > 4000) return
    const max = cur.scrollHeight - cur.clientHeight
    if (max <= 0) return
    // 期望位置：有保存值恢复（clamp 到当前列表范围），否则顶部
    const target = savedSongsScrollTop > 0 ? Math.min(savedSongsScrollTop, max) : 0
    if (Math.abs(cur.scrollTop - target) > 500) {
      cur.scrollTop = target
    }
  }
  requestAnimationFrame(guard)
  const iv = window.setInterval(() => { attachScrollSave(); guard() }, 250)
  window.setTimeout(() => window.clearInterval(iv), 4000)
})

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener(SONGS_UPDATED_EVENT, refreshSongs)
  }
  if (scrollIdleTimer) {
    clearTimeout(scrollIdleTimer)
    scrollIdleTimer = null
  }
})
</script>

<style scoped lang="scss">
.songs-page {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;

  &__navbar-wrap {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    z-index: 20;
  }

  &__right {
    height: 32px;
  }

  &__content {
    overflow: hidden;
  }

  &__empty {
    height: 100%;
    box-sizing: border-box;
    padding-top: calc(max(16px, var(--m-safe-area-top, 0px)) + 44px);
  }

  &__list {
    height: 100%;
    overflow-y: auto;
    box-sizing: border-box;
    padding-top: calc(max(16px, var(--m-safe-area-top, 0px)) + 44px);
    padding-bottom: var(--m-content-pb);
    overflow-anchor: none;

    @media (min-width: 768px) {
      padding-bottom: var(--m-content-pb-md);
    }
  }

  /* 随机播放吸顶条（矮条玻璃：blur 8px + 白渐变 + 暗色黑渐变） */
  &__shuffle-bar {
    position: sticky;
    top: 0;
    z-index: 10;
    box-sizing: border-box;
    width: 100%;
    height: 44px;
  }

  &__shuffle-blur {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    -webkit-backdrop-filter: blur(8px);
    backdrop-filter: blur(8px);
  }

  &__shuffle-glass {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    background: linear-gradient(
      to bottom,
      #fff 0%,
      rgba(255, 255, 255, 0.55) 50%,
      rgba(255, 255, 255, 0) 100%
    );
  }

  &__shuffle-btn {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 10px;
    width: 100%;
    height: 100%;
    padding: 0 16px;
    border-radius: 0;
    font-size: 15px;
    color: var(--m-text);
    background-color: transparent;

    &:active {
      background-color: rgba(0, 0, 0, 0.1);
    }
  }

  &__shuffle-icon {
    width: 16px;
    height: 16px;
    flex: none;
  }

  &__list-root {
    margin: 0;
  }

  &__virtual-row {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    box-sizing: border-box;
    min-height: 72px;
  }

  &__row {
    height: 100%;
    position: relative;
  }

  &__row-title {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__row-subtitle {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__more-btn {
    position: absolute !important;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
    width: 32px;
    height: 32px;
    padding: 0;
    flex: none;
    margin-left: auto;
    color: var(--m-text);
  }

  &__more-icon {
    width: 16px;
    height: 16px;
  }

  &__jump-fab {
    position: fixed;
    z-index: 1100;
    right: 16px;
    bottom: 176px;
  }

  &__jump-icon {
    width: 20px;
    height: 20px;
  }
}

/* 正在播放行高亮 */
.songs-page :deep(.is-playing) {
  background-color: rgba(0, 0, 0, 0.05);
}

:global(.dark) .songs-page__shuffle-glass {
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.6) 0%,
    rgba(0, 0, 0, 0.35) 50%,
    rgba(0, 0, 0, 0) 100%
  );
}

:global(.dark) .songs-page__shuffle-btn:active {
  background-color: rgba(255, 255, 255, 0.1);
}

:global(.dark) .songs-page :deep(.is-playing) {
  background-color: rgba(255, 255, 255, 0.1);
}
</style>