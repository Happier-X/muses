<template>
  <div ref="pageRef" class="m-page">
    <h-nav-bar title="歌曲" :fixed="false">
      <template #right>
        <h-button variant="ghost" is-icon-only shape="square" aria-label="搜索歌曲">
          <h-icon :icon="searchOutline" />
        </h-button>
      </template>
    </h-nav-bar>
    <div class="flex-[0_0_48px] min-h-[48px] bg-[var(--h-color-surface-secondary)]">
      <div class="box-border w-full px-[8px] py-[4px] md:max-w-[var(--muses-content-max-width)] md:mx-auto">
        <h-button
          variant="ghost"
          size="sm"
          class="m-0"
          aria-label="随机播放全部"
          :disabled="songs.length === 0"
          @click="onShuffleAll"
        >
          <template #leading><h-icon :icon="shuffle" /></template>
          随机播放全部
        </h-button>
      </div>
    </div>
    <div class="m-content" style="overflow: hidden;">
      <h-empty
        v-if="songs.length === 0"
        title="还没有歌曲"
        description="请先到音源页添加并扫描音源。"
      />

      <div v-else ref="listParentRef" class="h-full overflow-auto box-border pb-[calc(var(--muses-mini-player-height)+var(--muses-space-lg))] md:pb-[calc(var(--muses-mini-player-height)+var(--muses-space-lg)+env(safe-area-inset-bottom,0px))] md:max-w-[var(--muses-content-max-width)] md:mx-auto [overflow-anchor:none]">
        <div class="relative w-full" :style="{ height: `${totalSize}px` }">
          <div
            v-for="virtualRow in virtualRows"
            :key="songs[virtualRow.index].id"
            :ref="measureVirtualRow"
            class="absolute top-0 left-0 right-0 box-border min-h-[var(--muses-song-row-height)]"
            :data-index="virtualRow.index"
            :style="{ transform: `translateY(${virtualRow.start}px)` }"
          >
            <div
              class="flex items-center gap-[var(--muses-space-md)] p-[var(--muses-space-md)]"
              :class="songItemClass(songs[virtualRow.index].id)"
              :data-song-id="songs[virtualRow.index].id"
              role="button"
              tabindex="0"
              @click="playSong(songs[virtualRow.index])"
            >
              <m-cover class="!w-12 !h-12 !flex-none !rounded-md" :src="getSongCoverSrc(songs[virtualRow.index])" alt="" />
              <div class="flex-1 min-w-0 flex flex-col gap-[var(--muses-space-xs)]">
                <h2 class="m-0 text-[length:var(--muses-font-title)] leading-[var(--muses-line-height-title)] text-[color:var(--muses-color-ink)] truncate">{{ songs[virtualRow.index].title }}</h2>
                <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--muses-color-ink-muted)] truncate">{{ getSongArtistName(songs[virtualRow.index]) }} - {{ getSongAlbumName(songs[virtualRow.index]) }}</p>
              </div>
              <h-button
                variant="ghost"
                is-icon-only
                shape="square"
                class="flex-none m-0 ml-auto"
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
        <div class="flex flex-col gap-[var(--muses-space-xs)] pb-[var(--muses-space-lg)] px-[var(--muses-space-lg)]">
          <button :class="actionSheetItemClass" type="button" @click="onAddToQueue">添加到队列</button>
          <button :class="actionSheetItemClass" type="button" @click="onPickPlaylist">加入歌单…</button>
          <button :class="[actionSheetItemClass, actionSheetCancelClass]" type="button" @click="isSongActionsOpen = false">取消</button>
        </div>
      </h-bottom-sheet>

      <h-bottom-sheet v-model="isPlaylistPickOpen" title="加入歌单">
        <div class="flex flex-col gap-[var(--muses-space-xs)] pb-[var(--muses-space-lg)] px-[var(--muses-space-lg)]">
          <button
            v-for="pl in playlistList"
            :key="pl.id"
            :class="actionSheetItemClass"
            type="button"
            @click="onAddToPlaylist(pl.id)"
          >
            {{ pl.name }}
          </button>
          <button :class="actionSheetItemClass" type="button" @click="onCreateNewPlaylist">新建歌单</button>
          <button :class="[actionSheetItemClass, actionSheetCancelClass]" type="button" @click="isPlaylistPickOpen = false">取消</button>
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
import { actionSheetCancelClass, actionSheetItemClass } from '@/theme/action-sheet'
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
const actionSong = ref<SongItem | null>(null)
const isSongActionsOpen = ref(false)
const isPlaylistPickOpen = ref(false)
const isCreatePlaylistOpen = ref(false)
const highlightedSongId = ref<string | null>(null)
let jumpHighlightTimer: ReturnType<typeof setTimeout> | null = null

const songItemClass = (songId: string): string => {
  const classes: string[] = []
  if (playerState.currentSong?.id === songId) {
    classes.push('is-playing bg-[var(--muses-color-playing-bg)]')
  }
  if (highlightedSongId.value === songId) {
    classes.push('bg-[var(--muses-color-jump-highlight)]')
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

  highlightedSongId.value = currentId
  if (jumpHighlightTimer) {
    clearTimeout(jumpHighlightTimer)
  }
  jumpHighlightTimer = setTimeout(() => {
    highlightedSongId.value = null
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
  // 冷启动/重进页面时 WebView 首屏布局未稳，虚拟列表可能被误滚到底（scrollToCurrentSong 未调用、
  // overflow-anchor:none 无效；疑 TanStack Virtual measure 期间布局漂移）。挂载后短暂周期回顶兜底，
  // 用户一交互立即停止，避免打断手动滚动。
  let interacted = false
  const stop = () => { interacted = true }
  const el = listParentRef.value
  el?.addEventListener('touchstart', stop, { once: true, passive: true })
  el?.addEventListener('wheel', stop, { once: true, passive: true })
  const resetTop = () => {
    if (interacted) return
    const cur = listParentRef.value
    if (cur && cur.scrollTop > 0) cur.scrollTop = 0
  }
  requestAnimationFrame(resetTop)
  const iv = window.setInterval(resetTop, 250)
  window.setTimeout(() => { window.clearInterval(iv); el?.removeEventListener('touchstart', stop); el?.removeEventListener('wheel', stop) }, 4000)
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
