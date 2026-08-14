<template>
  <div ref="pageRef" class="m-page songs-page">
    <div class="songs-page__navbar-wrap">
      <m-navbar right-class="songs-page__right">
        <template #title>歌曲</template>
        <template #right>
          <m-button
            component="button"
            variant="clear"
            rounded
            class="songs-page__search-btn"
            aria-label="搜索歌曲"
            @click="enterSearch"
          >
            <component :is="searchOutline" aria-hidden="true" class="songs-page__search-icon" />
          </m-button>
        </template>
      </m-navbar>
    </div>
    <div class="m-content songs-page__content">
      <!-- 工具条：随机播放 + 歌曲数 / 排序 / 多选（椒盐结构） -->
      <div v-if="songs.length > 0 && !isSearching" class="songs-page__toolbar">
        <div class="songs-page__toolbar-wrap">
          <!-- 左侧：随机播放图标 + 歌曲总数 -->
          <div class="songs-page__toolbar-left">
            <button
              type="button"
              class="songs-page__toolbar-left-btn"
              aria-label="随机播放全部"
              @click="onShuffleAll"
            >
              <component :is="shuffle" aria-hidden="true" class="songs-page__toolbar-left-icon" />
            </button>
            <span class="songs-page__toolbar-count">{{ songs.length }}</span>
          </div>
          <!-- 多选模式计数 -->
          <div v-if="isMultiSelect" class="songs-page__toolbar-count">已选中 {{ selectedCount }} 项</div>
          <div class="songs-page__toolbar-right">
            <m-button
              component="button"
              variant="clear"
              inline
              rounded
              class="songs-page__toolbar-btn"
              aria-label="排序"
              @click="openSortMenu"
            >
              <component :is="arrowUpDown" aria-hidden="true" class="songs-page__toolbar-icon" />
            </m-button>
            <m-button
              component="button"
              variant="clear"
              inline
              rounded
              class="songs-page__toolbar-btn"
              :aria-label="isMultiSelect ? '取消多选' : '多选'"
              @click="isMultiSelect ? exitMultiSelect() : enterMultiSelect()"
            >
              <component :is="listChecks" aria-hidden="true" class="songs-page__toolbar-icon" />
            </m-button>
          </div>
        </div>
      </div>

      <!-- 搜索栏（替换工具条） -->
      <div v-if="songs.length > 0 && isSearching" class="songs-page__searchbar">
        <div class="songs-page__searchbar-wrap">
          <component :is="searchOutline" aria-hidden="true" class="songs-page__searchbar-icon" />
          <input
            ref="searchInputRef"
            v-model="searchQuery"
            class="songs-page__searchbar-input"
            :placeholder="`在 ${songs.length} 首歌曲中搜索`"
            @input="onSearchInput"
          />
          <m-button
            component="button"
            variant="clear"
            inline
            class="songs-page__searchbar-cancel"
            @click="exitSearch"
          >
            取消
          </m-button>
        </div>
      </div>

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
        <m-list :dividers="false" class="songs-page__list-root">
          <div class="songs-page__vlist" :style="{ height: `${totalSize}px` }">
            <div
              v-for="virtualRow in virtualRows"
              :key="visibleSongs[virtualRow.index].id"
              :ref="measureVirtualRow"
              class="songs-page__virtual-row"
              :data-index="virtualRow.index"
              :style="{ transform: `translateY(${virtualRow.start}px)` }"
            >
              <m-list-item
                :chevron="false"
                link
                :title="visibleSongs[virtualRow.index].title"
                :subtitle="`${getSongArtistName(visibleSongs[virtualRow.index])} - ${getSongAlbumName(visibleSongs[virtualRow.index])}`"
                title-class="songs-page__row-title"
                subtitle-class="songs-page__row-subtitle"
                class="songs-page__row"
                :class="songItemClass(visibleSongs[virtualRow.index].id)"
                :data-song-id="visibleSongs[virtualRow.index].id"
                role="button"
                tabindex="0"
                @click="onRowClick(visibleSongs[virtualRow.index])"
              >
                <template #media>
                  <!-- 多选时媒体区变选择框；否则封面 -->
                  <button
                    v-if="isMultiSelect"
                    type="button"
                    class="songs-page__select-box"
                    :class="{ 'is-checked': selectedIds.has(visibleSongs[virtualRow.index].id) }"
                    :aria-label="selectedIds.has(visibleSongs[virtualRow.index].id) ? '取消选择' : '选择'"
                    @click.stop="toggleSelectOne(visibleSongs[virtualRow.index].id)"
                  >
                    <component :is="checkCheck" aria-hidden="true" class="songs-page__select-check" />
                  </button>
                  <m-cover
                    v-else
                    :src="getSongCoverSrc(visibleSongs[virtualRow.index])"
                    :size="54"
                    radius="sm"
                    alt=""
                    class="songs-page__cover"
                  />
                </template>
                <template #after>
                  <div class="songs-page__row-actions">
                    <!-- 椒盐式实心三点菜单（椒盐歌曲页只保留 ⋮，无圆按钮） -->
                    <button
                      v-if="!isMultiSelect"
                      type="button"
                      class="songs-page__more-btn"
                      aria-label="更多歌曲操作"
                      @click.stop="openSongActions(visibleSongs[virtualRow.index])"
                    >
                      <span class="songs-page__more-dots" aria-hidden="true">
                        <i></i><i></i><i></i>
                      </span>
                    </button>
                  </div>
                </template>
              </m-list-item>
            </div>
          </div>
        </m-list>
      </div>

      <!-- 字母索引条（仅字母序排序） -->
      <div v-if="isAlphabeticalSort && songs.length > 0 && !isSearching" class="songs-page__index-bar">
        <button
          type="button"
          class="songs-page__index-item"
          aria-label="回到顶部"
          @click="scrollToIndexGroup(null)"
        >
          0
        </button>
        <button
          v-for="letter in indexLetters"
          :key="letter"
          type="button"
          class="songs-page__index-item"
          :class="{ 'is-empty': !indexGroups[letter] }"
          :aria-label="`跳转到 ${letter}`"
          @click="scrollToIndexGroup(letter)"
        >
          {{ letter }}
        </button>
      </div>

      <!-- 多选底部操作条 -->
      <div v-if="isMultiSelect" class="songs-page__multibar">
        <div class="songs-page__multibar-wrap">
          <m-button
            component="button"
            variant="clear"
            inline
            class="songs-page__multibar-btn songs-page__multibar-btn--danger"
            :disabled="selectedCount === 0"
            @click="onDeleteSelected"
          >
            永久删除
          </m-button>
          <m-button
            component="button"
            variant="clear"
            inline
            class="songs-page__multibar-btn"
            :disabled="selectedCount === 0"
            @click="onPickPlaylistForSelected"
          >
            添加到歌单
          </m-button>
          <m-button
            component="button"
            variant="clear"
            inline
            class="songs-page__multibar-btn"
            :disabled="selectedCount === 0"
            @click="onPlaySelected"
          >
            播放选中队列
          </m-button>
          <m-button
            component="button"
            variant="clear"
            inline
            class="songs-page__multibar-btn"
            @click="exitMultiSelect"
          >
            取消
          </m-button>
        </div>
      </div>

      <m-actions :opened="isSongActionsOpen" @backdropclick="isSongActionsOpen = false">
        <m-actions-group>
          <m-actions-label>歌曲操作</m-actions-label>
          <m-actions-button @click="onAddToQueue">添加到队列</m-actions-button>
          <m-actions-button @click="onPickPlaylist">加入歌单…</m-actions-button>
          <m-actions-button @click="isSongActionsOpen = false">取消</m-actions-button>
        </m-actions-group>
      </m-actions>

      <!-- 排序菜单 -->
      <m-actions :opened="isSortMenuOpen" @backdropclick="isSortMenuOpen = false">
        <m-actions-group>
          <m-actions-label>排序</m-actions-label>
          <m-actions-button
            v-for="item in SONG_SORT_MENU"
            :key="item.key"
            :class="{ 'songs-page__sort-item--disabled': !item.available }"
            @click="item.available && setSortMode(item.key as SongSortMode)"
          >
            <span class="songs-page__sort-label">
              {{ item.label }}
              <component
                v-if="sortMode === item.key"
                :is="checkCheck"
                aria-hidden="true"
                class="songs-page__sort-check"
              />
            </span>
          </m-actions-button>
          <m-actions-button @click="isSortMenuOpen = false">取消</m-actions-button>
        </m-actions-group>
      </m-actions>

      <m-actions :opened="isPlaylistPickOpen" @backdropclick="isPlaylistPickOpen = false">
        <m-actions-group>
          <m-actions-label>加入歌单</m-actions-label>
          <m-actions-button
            v-for="pl in playlistList"
            :key="pl.id"
            @click="isMultiSelect ? onAddSelectedToPlaylist(pl.id) : onAddToPlaylist(pl.id)"
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

      <m-toast :opened="toast.visible" position="center">
        {{ toast.message }}
      </m-toast>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, type ComponentPublicInstance } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { Capacitor } from '@capacitor/core'
import { arrowUpDown, checkCheck, crosshair, listChecks, searchOutline, shuffle } from '@/icons'
import {
  MActions, MActionsButton, MActionsGroup, MActionsLabel,
  MButton, MDialog, MDialogButton, MFab, MList, MListItem, MListInput,
  MNavbar, MCover, MEmpty, MToast,
} from '@/components/ui'
import { loadSongs, SONGS_UPDATED_EVENT } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import {
  getSongAlbumName, getSongArtistName, SONG_SORT_MENU, sortSongsByMode,
  type SongSortMode,
} from '@/features/library/views'
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
const SORT_SAVE_KEY = 'muses:songs-sort-mode'
let savedSongsScrollTop = Number(sessionStorage.getItem(SCROLL_SAVE_KEY) || 0)
const actionSong = ref<SongItem | null>(null)
const isSongActionsOpen = ref(false)
const isPlaylistPickOpen = ref(false)
const isCreatePlaylistOpen = ref(false)

/** 排序模式（sessionStorage 持久）；椒盐默认自定义顺序 */
const sortMode = ref<SongSortMode>((sessionStorage.getItem(SORT_SAVE_KEY) as SongSortMode | null) ?? 'custom')
const isSortMenuOpen = ref(false)

/** 多选模式 */
const isMultiSelect = ref(false)
const selectedIds = ref<Set<string>>(new Set())

/** 搜索 */
const isSearching = ref(false)
const searchQuery = ref('')
const searchInputRef = ref<HTMLInputElement | null>(null)

/** 列表排序是否字母序（决定索引条显示） */
const isAlphabeticalSort = computed(() => ['title', 'fileName', 'artist', 'album', 'folder'].includes(sortMode.value))

/** 索引分组：字母 -> 该组首行在 visibleSongs 中的 index */
const indexGroups = computed<Record<string, number>>(() => {
  const groups: Record<string, number> = {}
  visibleSongs.value.forEach((song, index) => {
    const letter = getTitleIndexLetter(song.title)
    if (letter && groups[letter] === undefined) {
      groups[letter] = index
    }
  })
  return groups
})

/** 索引条字母：A-Z + #，有歌的在前（保持 A-Z 顺序） */
const indexLetters = computed<string[]>(() => {
  return ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '#']
})

/** 标题首字符 -> 索引字母；数字/其他（含中文按 localeCompare 归属 # 组） */
const getTitleIndexLetter = (title: string): string | null => {
  const first = title.trim().charAt(0)
  if (!first) {
    return null
  }
  if (/[a-zA-Z]/.test(first)) {
    return first.toUpperCase()
  }
  if (/[0-9]/.test(first)) {
    return '#'
  }
  // 中文与其他字符：localeCompare 排序在字母后，归 # 组
  return '#'
}

const scrollToIndexGroup = (letter: string | null): void => {
  if (letter === null) {
    rowVirtualizer.value.scrollToIndex(0, { align: 'start' })
    return
  }
  const index = indexGroups.value[letter]
  if (index !== undefined) {
    rowVirtualizer.value.scrollToIndex(index, { align: 'start' })
  }
}

/** 过滤后的歌曲（搜索命中 title/artist/album） */
const visibleSongs = computed<SongItem[]>(() => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) {
    return songs.value
  }
  return songs.value.filter((song) => {
    return (
      song.title.toLowerCase().includes(query) ||
      (song.artist ?? '').toLowerCase().includes(query) ||
      (song.album ?? '').toLowerCase().includes(query)
    )
  })
})

const songItemClass = (songId: string): string => {
  const classes: string[] = []
  if (playerState.currentSong?.id === songId) {
    classes.push('is-playing')
  }
  if (isMultiSelect.value && selectedIds.value.has(songId)) {
    classes.push('is-selected')
  }
  return classes.join(' ')
}

const selectedCount = computed(() => selectedIds.value.size)
const toggleSelectOne = (songId: string): void => {
  const next = new Set(selectedIds.value)
  if (next.has(songId)) {
    next.delete(songId)
  } else {
    next.add(songId)
  }
  selectedIds.value = next
}

const enterMultiSelect = (): void => {
  isMultiSelect.value = true
  selectedIds.value = new Set()
}

const exitMultiSelect = (): void => {
  isMultiSelect.value = false
  selectedIds.value = new Set()
}

/** 随机播放全部（椒盐工具条左侧按钮） */
const onShuffleAll = (): void => {
  if (songs.value.length === 0) return
  clearQueue()
  enqueueSongs(songs.value)
  if (!shuffleEnabled()) {
    toggleShuffle()
  }
  selectSongAtIndex(0)
  void playSong(visibleSongs.value[0])
}

const openSortMenu = (): void => {
  isSortMenuOpen.value = true
}

const setSortMode = (mode: SongSortMode | 'durationDesc'): void => {
  sortMode.value = mode as SongSortMode
  sessionStorage.setItem(SORT_SAVE_KEY, String(sortMode.value))
  isSortMenuOpen.value = false
  // 排序变化后回到顶部，避免错位
  if (listParentRef.value) {
    listParentRef.value.scrollTop = 0
  }
  refreshSongs()
}

const enterSearch = (): void => {
  isSearching.value = true
  searchQuery.value = ''
  // 搜索激活时退出多选
  if (isMultiSelect.value) {
    exitMultiSelect()
  }
  void nextTick(() => {
    searchInputRef.value?.focus()
  })
}

const exitSearch = (): void => {
  isSearching.value = false
  searchQuery.value = ''
}


/** 大曲库只渲染可视行，降低滚动/卡顿（#50）；搜索/排序变化后列表基于 visibleSongs */
const rowVirtualizer = useVirtualizer(
  computed(() => ({
    count: visibleSongs.value.length,
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
  return visibleSongs.value.map((_, index) => ({
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
  return visibleSongs.value.length * 72
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
  songs.value = sortSongsByMode(loadSongs(), sortMode.value)
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

/** 行点击：多选时切换选择，否则播放 */
const onRowClick = (song: SongItem): void => {
  if (isMultiSelect.value) {
    toggleSelectOne(song.id)
    return
  }
  void playSong(song)
}

/** toast 通知（保留供其他功能使用） */
const toast = ref<{ visible: boolean; message: string }>({
  visible: false,
  message: '',
})

/** 多选：播放选中队列（按当前排序顺序，不清空已有队列） */
const onPlaySelected = (): void => {
  const selectedSongs = visibleSongs.value.filter((song) => selectedIds.value.has(song.id))
  if (selectedSongs.length === 0) {
    return
  }

  clearQueue()
  enqueueSongs(selectedSongs)
  const first = selectedSongs[0]
  if (first) {
    void playSong(first)
  }
  exitMultiSelect()
}

/** 多选：永久删除（移除库记录；文件本体不动，弹确认） */
const onDeleteSelected = async (): Promise<void> => {
  const selectedSongs = visibleSongs.value.filter((song) => selectedIds.value.has(song.id))
  if (selectedSongs.length === 0) {
    return
  }
  if (window.confirm(`确定从音乐库删除选中的 ${selectedSongs.length} 首歌曲吗？（文件不会被删除）`)) {
    const { loadSongs: loadSongsNow, saveSongs } = await import('@/features/library/storage')
    const removedIds = new Set(selectedSongs.map((song) => song.id))
    saveSongs(loadSongsNow().filter((song) => !removedIds.has(song.id)))
    exitMultiSelect()
    refreshSongs()
  }
}

/** 多选：添加到歌单（复用单曲加入逻辑） */
const onPickPlaylistForSelected = (): void => {
  isPlaylistPickOpen.value = true
}

const onAddSelectedToPlaylist = (playlistId: string): void => {
  const selectedIdsArray = Array.from(selectedIds.value)
  for (const songId of selectedIdsArray) {
    addSongToPlaylist(playlistId, songId)
  }
  isPlaylistPickOpen.value = false
  exitMultiSelect()
}

const onSearchInput = (e: Event): void => {
  searchQuery.value = (e.target as HTMLInputElement).value
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
  &__vlist { position: relative; width: 100%; }
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

    /* 对齐椒盐：navbar 背景与页面同色 #F3F3F3，含状态栏感知 */
    :deep(.m-navbar) {
      padding-top: 22px;
      background-color: #F3F3F3;
    }
    :deep(.m-navbar__bg) {
      background-color: #F3F3F3;
    }
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
    padding-top: calc(max(22px, var(--m-safe-area-top, 0px)) + 44px);
  }

  &__list {
    height: 100%;
    overflow-y: auto;
    box-sizing: border-box;
    padding-top: calc(max(22px, var(--m-safe-area-top, 0px)) + 44px);
    padding-bottom: var(--m-content-pb);
    overflow-anchor: none;

    @media (min-width: 768px) {
      padding-bottom: var(--m-content-pb-md);
    }
  }

  /* 工具条：48dp 干净表面，sticky 吸 navbar 正下方（对齐椒盐） */
  &__toolbar {
    position: sticky;
    top: calc(max(22px, var(--m-safe-area-top, 0px)) + 44px);
    z-index: 15;
    box-sizing: border-box;
    width: 100%;
    height: 48px;
    background: var(--m-surface);
  }

  &__toolbar-wrap {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 100%;
    padding: 0 8px;
  }

  &__toolbar-left {
    display: flex;
    align-items: center;
    gap: 6px;
    color: var(--m-text);
    font-size: 14px;
  }

  &__toolbar-left-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    padding: 0;
    border: none;
    border-radius: 50%;
    background: transparent;
    color: var(--m-text);
    cursor: pointer;
  }

  &__toolbar-left-icon {
    width: 20px;
    height: 20px;
    color: var(--m-text);
  }

  &__toolbar-left-text {
    font-size: 14px;
  }

  &__toolbar-count {
    flex: 1;
    text-align: center;
    font-size: 14px;
    color: var(--m-text-2);
    white-space: nowrap;
  }

  &__toolbar-right {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  &__toolbar-btn {
    width: 40px;
    height: 40px;
    padding: 0;
    color: var(--m-text-2);
  }

  &__toolbar-icon {
    width: 22px;
    height: 22px;
  }

  /* 搜索栏：替换工具条 */
  &__searchbar {
    position: sticky;
    top: calc(max(22px, var(--m-safe-area-top, 0px)) + 44px);
    z-index: 15;
    box-sizing: border-box;
    width: 100%;
    height: 48px;
    background: var(--m-surface-1);
  }

  &__searchbar-wrap {
    display: flex;
    align-items: center;
    gap: 8px;
    height: 100%;
    padding: 0 var(--m-spacing);
  }

  &__searchbar-icon {
    width: 18px;
    height: 18px;
    color: var(--m-text-2);
    flex: none;
  }

  &__searchbar-input {
    flex: 1;
    min-width: 0;
    height: 36px;
    padding: 0 12px;
    border: none;
    border-radius: var(--m-radius-card);
    background: var(--m-surface-2);
    color: var(--m-text);
    font-size: 15px;
    outline: none;
  }

  &__searchbar-cancel {
    flex: none;
    color: var(--m-primary);
    font-size: 15px;
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
    min-height: var(--m-list-row-h);
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

  /* 行内右侧操作区：圆形按钮 + 菜单（圆/⋮ 紧挨，对齐椒盐 x264-308 / x308-344） */
  &__row-actions {
    display: flex;
    align-items: center;
    gap: 0;
    flex: none;
    margin-left: auto;
    /* 48px 交互区在 28px 标题行内垂直溢出（负 margin 不撑高 72px 行；
       按钮中心仍对齐标题中心，与椒盐实测一致） */
    margin-top: -10px;
    margin-bottom: -10px;
  }

  /* 椒盐歌曲页只保留 ⋮，移除圆按钮 round-btn/round-icon */

  &__more-btn {
    /* 椒盐 ⋮ 三点按钮：小尺寸，紧贴右缘 */
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 48px;
    padding: 0;
    border: none;
    border-radius: 50%;
    background: transparent;
    color: #949fab; /* 椒盐 ⋮ 实心点蓝灰 (148,159,171) */
    cursor: pointer;
    flex: none;

    &:active {
      background: rgba(0, 0, 0, 0.06);
    }
  }

  &__more-dots {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px; /* 椒盐点中心距 5.3dp - 点径 3.3dp */

    i {
      display: block;
      width: 3.5px; /* 椒盐实心点 ~3.3dp */
      height: 3.5px;
      border-radius: 50%;
      background: currentColor;
    }
  }

  /* 多选选择框 */
  &__select-box {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    padding: 0;
    border: 1.5px solid var(--m-text-3);
    border-radius: 50%;
    background: transparent;
    color: transparent;
    cursor: pointer;
    flex: none;

    &.is-checked {
      border-color: var(--m-primary);
      background: var(--m-primary);
      color: var(--m-on-primary);
    }
  }

  &__select-check {
    width: 14px;
    height: 14px;
  }

  /* 多选底部操作条 */
  &__multibar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: calc(64px + var(--m-safe-area-bottom, 0px));
    z-index: 25;
    background: var(--m-surface-1);
    border-top: 1px solid var(--m-hairline);
  }

  &__multibar-wrap {
    display: flex;
    align-items: center;
    justify-content: space-around;
    height: 52px;
    padding: 0 8px;
  }

  &__multibar-btn {
    height: 36px;
    padding: 0 10px;
    font-size: 13px;
    color: var(--m-primary);

    &:disabled {
      color: var(--m-text-3);
      cursor: default;
    }

    &--danger {
      color: var(--m-danger);
    }
  }

  /* 字母索引条：右侧窄条，从工具条下方延伸到列表底（对齐椒盐：15dp 字母、密集排列） */
  &__index-bar {
    position: fixed;
    top: calc(max(22px, var(--m-safe-area-top, 0px)) + 44px + 48px);
    bottom: calc(64px + var(--m-safe-area-bottom, 0px));
    right: 2px;
    z-index: 15;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 4px 0;
  }

  &__index-item {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 15px;
    padding: 0;
    border: none;
    background: transparent;
    color: var(--m-text-3);
    font-size: 10px;
    line-height: 15px;
    cursor: pointer;
    border-radius: 2px;

    &:active {
      background: rgba(var(--m-primary-rgb), 0.12);
      color: var(--m-primary);
    }

    &.is-empty {
      color: rgba(140, 140, 140, 0.35);
    }
  }

  &__jump-fab {
    position: fixed;
    z-index: 1100;
    right: 16px;
    bottom: 96px; /* 对齐椒盐：FAB 底距播放条顶 ~24dp（椒盐实测） */

    /* 对齐椒盐：白色圆底 + 深灰定位图标（非主色底） */
    background-color: #f9f9f9;
    color: #666;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  }

  &__jump-icon {
    width: 20px;
    height: 20px;
  }

  &__sort-label {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    width: 100%;
  }

  &__sort-check {
    width: 18px;
    height: 18px;
    color: var(--m-primary);
  }
}

/* 行高对齐椒盐 72dp（覆盖全局 56px）；无行间分割线（椒盐列表无分割线） */
.songs-page :deep(.m-list-item) {
  --m-list-row-h: 72px;
  min-height: 72px;

  .m-list-item__inner {
    padding-top: 6px;
    padding-bottom: 6px;
    padding-left: 12px; /* 封面-标题间距对齐椒盐（~12dp） */
  }

  .m-list-item__title-wrap {
    min-height: 24px;
  }

  .m-list-item__title {
    font-size: 16px;
    line-height: 1.3;
  }

  .m-list-item__subtitle {
    font-size: 12px;
    line-height: 1.3;
    margin-top: 2px; /* 标题-副文字间距 2dp（SaltUI Item 源码） */

    /* HQ 品质标签（椒盐：副标题前橙色小标签） */
    &::before {
      content: 'HQ';
      display: inline-block;
      font-size: 9px;
      font-weight: 600;
      line-height: 1;
      padding: 1px 3px;
      border-radius: 2px;
      background: #F5A623; /* 橙色（椒盐实测） */
      color: #fff;
      margin-right: 4px;
      vertical-align: middle;
    }
  }

  /* 按钮区紧贴文字区（椒盐：文字 x66-264 → 圆交互区 x264-308 无空隙） */
  .m-list-item__after {
    padding-left: 0;
    gap: 0;
  }
}

/* 排序菜单不可用项置灰 */
.songs-page :deep(.songs-page__sort-item--disabled) {
  opacity: 0.4;
}

/* 无封面时对齐椒盐：透明底 32dp 小占位图标（不显示 50dp 大框） */
.songs-page :deep(.songs-page__cover) {
  background-color: transparent;

  .m-cover__placeholder-icon {
    width: 32px;
    height: 32px;
    opacity: 0.45;
  }
}

/* 多选时行不加分割线高亮 */
.songs-page :deep(.songs-page__row.is-selected) {
  background-color: rgba(var(--m-primary-rgb), 0.08);
}

/* 播放行文字蓝色高亮（对齐椒盐：标题+副文字变蓝，无背景高亮） */
.songs-page :deep(.songs-page__row.is-playing) {
  .m-list-item__title,
  .m-list-item__subtitle {
    color: var(--m-primary) !important;
  }
}

/* 深色主题：⋮ 按钮颜色 */
:global(.dark) .songs-page__more-btn {
  color: rgba(225, 230, 235, 0.75); /* 椒盐深色 subText #BFE1E6EB */
}

:global(.dark) .songs-page__more-btn:active {
  background: rgba(255, 255, 255, 0.08);
}

</style>