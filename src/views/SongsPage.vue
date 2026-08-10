<template>
  <k-page ref="pageRef" class="k-page m-page flex flex-col overflow-hidden !h-auto !bottom-safe-24 md:!bottom-0">
    <k-navbar rightClass="!h-8">
      <template #title>歌曲</template>
      <template #right>
        <k-button component="button" clear rounded class="size-8" aria-label="搜索歌曲">
          <component :is="searchOutline" aria-hidden="true" class="size-4" />
        </k-button>
      </template>
    </k-navbar>
    <div class="m-content" style="overflow: hidden;">
      <m-empty
        v-if="songs.length === 0"
        title="还没有歌曲"
        description="请先到音源页添加并扫描音源。"
      />

      <div v-else ref="listParentRef" class="h-full overflow-auto box-border pb-[64px] md:pb-safe-16 md:max-w-[720px] md:mx-auto [overflow-anchor:none]">
        <k-button
          component="button"
          clear
          :colors="{ textIos: 'text-black dark:text-white' }"
          class="sticky top-0 z-20 flex items-center justify-start gap-[10px] w-full h-11 bg-ios-light-surface-1 dark:bg-ios-dark-surface-1 px-4 rounded-none text-[15px]"
          aria-label="随机播放全部"
          @click="onShuffleAll"
        >
          <component :is="shuffle" aria-hidden="true" class="size-4 flex-none" />
          <span>{{ songs.length }} 首</span>
        </k-button>
        <k-list strong-ios outline-ios :dividers-ios="false" class="!my-0">          <div class="relative w-full" :style="{ height: `${totalSize}px` }">
            <div
              v-for="virtualRow in virtualRows"
              :key="songs[virtualRow.index].id"
              :ref="measureVirtualRow"
              class="absolute top-0 left-0 right-0 box-border min-h-[72px]"
              :data-index="virtualRow.index"
              :style="{ transform: `translateY(${virtualRow.start}px)` }"
            >
              <k-list-item
                :chevron="false"
                link
                :title="songs[virtualRow.index].title"
              :subtitle="`${getSongArtistName(songs[virtualRow.index])} - ${getSongAlbumName(songs[virtualRow.index])}`"
              titleClass="min-w-0 truncate"
              subtitleClass="truncate"
              class="h-full relative"
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
                <k-button
                  component="button"
                  clear
                  rounded
                  class="flex-none m-0 ml-auto size-8 !absolute !right-2 !top-1/2 !-translate-y-1/2"
                  aria-label="更多歌曲操作"
                  @click.stop="openSongActions(songs[virtualRow.index])"
                >
                  <component :is="ellipsisVertical" aria-hidden="true" class="size-4" />
                </k-button>
              </template>
            </k-list-item>
          </div>
        </div>
        </k-list>
      </div>

      <k-actions :opened="isSongActionsOpen" @backdropclick="isSongActionsOpen = false">
        <k-actions-group>
          <k-actions-label>歌曲操作</k-actions-label>
          <k-actions-button @click="onAddToQueue">添加到队列</k-actions-button>
          <k-actions-button @click="onPickPlaylist">加入歌单…</k-actions-button>
        </k-actions-group>
        <k-actions-group>
          <k-actions-button @click="isSongActionsOpen = false">取消</k-actions-button>
        </k-actions-group>
      </k-actions>

      <k-actions :opened="isPlaylistPickOpen" @backdropclick="isPlaylistPickOpen = false">
        <k-actions-group>
          <k-actions-label>加入歌单</k-actions-label>
          <k-actions-button
            v-for="pl in playlistList"
            :key="pl.id"
            @click="onAddToPlaylist(pl.id)"
          >
            {{ pl.name }}
          </k-actions-button>
          <k-actions-button @click="onCreateNewPlaylist">新建歌单</k-actions-button>
        </k-actions-group>
        <k-actions-group>
          <k-actions-button @click="isPlaylistPickOpen = false">取消</k-actions-button>
        </k-actions-group>
      </k-actions>

      <k-dialog :opened="isCreatePlaylistOpen" title="新建歌单">
        <k-list inset>
          <k-list-input
            label="歌单名称"
            type="text"
            :value="newPlaylistName"
            placeholder="歌单名称"
            maxlength="80"
            clear-button
            @input="onNewPlaylistNameInput"
          />
        </k-list>
        <template #buttons>
          <k-dialog-button @click="isCreatePlaylistOpen = false">取消</k-dialog-button>
          <k-dialog-button strong @click="onConfirmCreatePlaylist">创建并加入</k-dialog-button>
        </template>
      </k-dialog>

      <k-fab
        v-if="showJumpBubble"
        class="fixed z-[1100]"
        style="right: 16px; bottom: 176px"
        aria-label="跳转到当前播放"
        @click="scrollToCurrentSong"
      >
        <component :is="crosshair" aria-hidden="true" class="size-5" />
      </k-fab>
    </div>
  </k-page>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, type ComponentPublicInstance } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { Capacitor } from '@capacitor/core'
import { crosshair, ellipsisVertical, searchOutline, shuffle } from '@/icons'
import { kActions, kActionsButton, kActionsGroup, kActionsLabel, kButton, kDialog, kDialogButton, kFab, kList, kListItem, kListInput, kNavbar, kPage, MCover, MEmpty } from '@/components/ui'
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
const highlightedSongId = ref<string | null>(null)
let jumpHighlightTimer: ReturnType<typeof setTimeout> | null = null

const songItemClass = (songId: string): string => {
  const classes: string[] = []
  if (playerState.currentSong?.id === songId) {
    classes.push('is-playing bg-black/5 dark:bg-white/10')
  }
  if (highlightedSongId.value === songId) {
    classes.push('bg-primary/10')
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
  if (jumpHighlightTimer) {
    clearTimeout(jumpHighlightTimer)
    jumpHighlightTimer = null
  }
})
</script>
