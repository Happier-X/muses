<template>
  <div ref="pageRef" class="m-page">
    <h-nav-bar title="歌曲" :fixed="false">
      <template #right>
        <h-button variant="ghost" is-icon-only shape="square" aria-label="搜索歌曲">
          <h-icon :icon="searchOutline" />
        </h-button>
      </template>
    </h-nav-bar>
    <div class="shuffle-bar">
      <div class="shuffle-actions tablet-content-limit">
        <h-button
          variant="ghost"
          size="sm"
          class="shuffle-all-button"
          aria-label="随机播放全部"
          :disabled="songs.length === 0"
          @click="onShuffleAll"
        >
          <template #leading><h-icon :icon="shuffle" /></template>
          随机播放全部
        </h-button>
      </div>
    </div>
    <div class="m-content songs-content" style="overflow: hidden;">
      <h-empty
        v-if="songs.length === 0"
        title="还没有歌曲"
        description="请先到音源页添加并扫描音源。"
      />

      <div v-else ref="listParentRef" class="song-list list-grid tablet-content-limit">
        <div class="song-list-spacer" :style="{ height: `${totalSize}px` }">
          <div
            v-for="virtualRow in virtualRows"
            :key="songs[virtualRow.index].id"
            :ref="measureVirtualRow"
            class="song-row"
            :data-index="virtualRow.index"
            :style="{ transform: `translateY(${virtualRow.start}px)` }"
          >
            <div
              class="song-item"
              :class="{ 'is-playing': playerState.currentSong?.id === songs[virtualRow.index].id }"
              :data-song-id="songs[virtualRow.index].id"
              role="button"
              tabindex="0"
              @click="playSong(songs[virtualRow.index])"
            >
              <m-cover :src="getSongCoverSrc(songs[virtualRow.index])" alt="" />
              <div class="song-item-label">
                <h2>{{ songs[virtualRow.index].title }}</h2>
                <p>{{ getSongArtistName(songs[virtualRow.index]) }} - {{ getSongAlbumName(songs[virtualRow.index]) }}</p>
              </div>
              <h-button
                variant="ghost"
                is-icon-only
                shape="square"
                class="more-button"
                aria-label="更多歌曲操作"
                @click.stop="openSongActions(songs[virtualRow.index])"
              >
                <h-icon :icon="ellipsisVertical" />
              </h-button>
            </div>
          </div>
        </div>
      </div>

      <h-bottom-sheet v-model="isSongActionsOpen" title="歌曲操作">
        <div class="action-sheet-list">
          <button class="action-sheet-item" type="button" @click="onAddToQueue">添加到队列</button>
          <button class="action-sheet-item" type="button" @click="onPickPlaylist">加入歌单…</button>
          <button class="action-sheet-item action-cancel" type="button" @click="isSongActionsOpen = false">取消</button>
        </div>
      </h-bottom-sheet>

      <h-bottom-sheet v-model="isPlaylistPickOpen" title="加入歌单">
        <div class="action-sheet-list">
          <button
            v-for="pl in playlistList"
            :key="pl.id"
            class="action-sheet-item"
            type="button"
            @click="onAddToPlaylist(pl.id)"
          >
            {{ pl.name }}
          </button>
          <button class="action-sheet-item" type="button" @click="onCreateNewPlaylist">新建歌单</button>
          <button class="action-sheet-item action-cancel" type="button" @click="isPlaylistPickOpen = false">取消</button>
        </div>
      </h-bottom-sheet>

      <h-dialog v-model="isCreatePlaylistOpen" title="新建歌单">
        <h-input v-model="newPlaylistName" placeholder="歌单名称" maxlength="80" />
        <template #actions>
          <h-button variant="ghost" @click="isCreatePlaylistOpen = false">取消</h-button>
          <h-button variant="primary" @click="onConfirmCreatePlaylist">创建并加入</h-button>
        </template>
      </h-dialog>

      <h-floating-bubble
        v-if="currentPlayingInList"
        axis="lock"
        :offset="fabOffset"
        :ariaLabel="'跳转到当前播放'"
        @click="scrollToCurrentSong"
      >
        <h-icon :icon="locateOutline" aria-hidden="true" />
      </h-floating-bubble>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, type ComponentPublicInstance } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { Capacitor } from '@capacitor/core'
import { ellipsisVertical, locateOutline, searchOutline, shuffle } from '@/icons'
import { HBottomSheet, HButton, HDialog, HEmpty, HFloatingBubble, HIcon, HInput, HNavBar, MCover } from '@/components/ui'
import type { HFloatingBubbleOffset } from '@/components/ui'
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
const pageRef = ref<HTMLElement | { $el?: HTMLElement } | null>(null)
const listParentRef = ref<HTMLElement | null>(null)
const actionSong = ref<SongItem | null>(null)
const isSongActionsOpen = ref(false)
const isPlaylistPickOpen = ref(false)
const isCreatePlaylistOpen = ref(false)
let jumpHighlightTimer: ReturnType<typeof setTimeout> | null = null

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
  // 无滚动容器时（首帧 / 单测 stub）退化为全量行，避免空白与测试失败
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

const fabOffset = computed<HFloatingBubbleOffset>(() => {
  const miniPlayerH = 64
  const tabBarH = 64
  return { x: window.innerWidth - 56, y: window.innerHeight - miniPlayerH - tabBarH - 56 }
})

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

const resolvePageRoot = (): ParentNode | null => {
  const value = pageRef.value
  if (!value) {
    return typeof document !== 'undefined' ? document : null
  }
  if (value instanceof HTMLElement) {
    return value
  }
  if (value.$el instanceof HTMLElement) {
    return value.$el
  }
  return typeof document !== 'undefined' ? document : null
}

const findSongRow = (songId: string): HTMLElement | null => {
  const root = resolvePageRoot()
  if (!root) {
    return null
  }

  const rows = Array.from(root.querySelectorAll<HTMLElement>('[data-song-id]'))
  return rows.find((row) => row.getAttribute('data-song-id') === songId) ?? null
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

  // 虚拟列表：先滚到索引，再高亮 DOM 行
  rowVirtualizer.value.scrollToIndex(index, { align: 'start', behavior: 'smooth' })
  await nextTick()
  // 等 layout 一帧，确保目标行已挂载
  await new Promise<void>((resolve) => {
    requestAnimationFrame(() => resolve())
  })

  const row = findSongRow(currentId)
  if (!row) {
    return
  }

  // 保留既有 scrollIntoView 合约（stub/首帧 virtualizer 尚未拿到容器时也可跳转）
  const scrollableRow = row as HTMLElement & { scrollIntoView?: (options?: ScrollIntoViewOptions) => void }
  scrollableRow.scrollIntoView?.({ behavior: 'smooth', block: 'start', inline: 'nearest' })
  row.classList.add('jump-highlight')
  if (jumpHighlightTimer) {
    clearTimeout(jumpHighlightTimer)
  }
  jumpHighlightTimer = setTimeout(() => {
    row.classList.remove('jump-highlight')
    jumpHighlightTimer = null
  }, 1200)
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
})

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener(SONGS_UPDATED_EVENT, refreshSongs)
  }
  if (jumpHighlightTimer) {
    clearTimeout(jumpHighlightTimer)
    jumpHighlightTimer = null
  }
})
</script>

<style scoped>
/* 列表自管 padding-bottom；content 不再重复加底内边距 */
.songs-content {
  --padding-bottom: 0;
}

.shuffle-bar {
  flex: 0 0 48px;
  min-height: 48px;
  background: var(--muses-color-surface);
}

.shuffle-actions {
  box-sizing: border-box;
  width: 100%;
  padding: 4px 8px;
}

.shuffle-all-button {
  margin: 0;
}

/* 自建滚动容器：虚拟列表需要固定高度 + overflow（#50） */
.song-list {
  height: 100%;
  overflow: auto;
  box-sizing: border-box;
  /* 仅为 MiniPlayer 与 Tab Bar 预留滚动空间 */
  padding-bottom: calc(var(--muses-tab-bar-height) + var(--muses-mini-player-height) + env(safe-area-inset-bottom, 0px));
}

.song-list-spacer {
  position: relative;
  width: 100%;
}

.song-row {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  box-sizing: border-box;
  min-height: var(--muses-song-row-height);
  /* 避开 navbar 与随机播放操作区，scrollIntoView block=start 时标题完整可见 */
  scroll-margin-top: 108px;
}

/* 当前播放行 */
.song-item.is-playing {
  background: var(--muses-color-playing-bg);
}

/* 跳转高亮 */
.song-item.jump-highlight {
  background: var(--muses-color-jump-highlight);
}



@media (min-width: 768px) {
  /* 歌曲页宽屏始终单列，仅保留内容最大宽度居中 */
  .list-grid {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }

  .song-list {
    padding-bottom: calc(var(--muses-mini-player-height) + env(safe-area-inset-bottom, 0px));
  }

  .songs-content {
    --padding-bottom: 0;
  }

  .shuffle-actions {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
</style>
