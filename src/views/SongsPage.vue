<template>
  <div ref="pageRef" class="m-page songs-page">
    <div class="songs-page__navbar-wrap">
      <m-navbar right-class="songs-page__right">
        <template #title>歌曲</template>
        <template #right>
          <m-icon-button
            class="songs-page__search-btn"
            aria-label="搜索歌曲"
            @click="enterSearch"
          >
            <component :is="searchOutline" aria-hidden="true" class="songs-page__search-icon" />
          </m-icon-button>
        </template>
        <!-- 工具条/搜索栏并入 navbar subnavbar：与 navbar 同一块玻璃（无交界分界） -->
        <template #subnavbar>
          <!-- 工具条：随机播放 + 歌曲数（先只保留左侧，对齐椒盐） -->
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
                <button
                  type="button"
                  class="songs-page__toolbar-left-btn"
                  aria-label="筛选可疑歌曲批量入队"
                  :disabled="suspiciousCount === 0"
                  @click="onOpenSuspiciousBatch"
                >
                  <component :is="crosshair" aria-hidden="true" class="songs-page__toolbar-left-icon" />
                </button>
                <button
                  type="button"
                  class="songs-page__toolbar-left-btn songs-page__toolbar-left-btn--scrape"
                  aria-label="刮削队列"
                  @click="router.push('/scrape')"
                >
                  <component :is="listChecks" aria-hidden="true" class="songs-page__toolbar-left-icon" />
                  <span v-if="scrapeQueueCount > 0" class="songs-page__scrape-badge">
                    {{ scrapeQueueCount > 99 ? '99+' : scrapeQueueCount }}
                  </span>
                </button>
                <span class="songs-page__toolbar-count">{{ songs.length }}</span>
              </div>
              <!-- 多选模式计数 -->
              <div v-if="isMultiSelect" class="songs-page__toolbar-count">已选中 {{ selectedCount }} 项</div>
            </div>
          </div>

          <!-- 搜索栏（替换工具条） -->
          <div v-else-if="songs.length > 0 && isSearching" class="songs-page__searchbar">
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
        </template>
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
                    <m-icon-button
                      v-if="!isMultiSelect"
                      size="sm"
                      class="songs-page__more-btn"
                      aria-label="更多歌曲操作"
                      @click.stop="openSongActions(visibleSongs[virtualRow.index])"
                    >
                      <span class="songs-page__more-dots" aria-hidden="true">
                        <i></i><i></i><i></i>
                      </span>
                    </m-icon-button>
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
            :disabled="selectedCount === 0"
            @click="onMarkSelectedForScrape"
          >
            标记待刮削
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
          <m-actions-button @click="onAddToScrapeQueue">加入待刮削</m-actions-button>
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

      <!-- 筛选可疑歌曲批量入队确认框（child2 R2-3） -->
      <m-actions :opened="isSuspiciousConfirmOpen" @backdropclick="isSuspiciousConfirmOpen = false">
        <m-actions-group>
          <m-actions-label>筛选可疑歌曲</m-actions-label>
          <m-actions-button @click="onConfirmSuspiciousBatch">加入 {{ suspiciousCount }} 首到待刮削队列</m-actions-button>
          <m-actions-button @click="isSuspiciousConfirmOpen = false">取消</m-actions-button>
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
import { useRouter } from 'vue-router'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { Capacitor } from '@capacitor/core'
import { checkCheck, crosshair, listChecks, searchOutline, shuffle } from '@/icons'
import {
  MActions, MActionsButton, MActionsGroup, MActionsLabel,
  MButton, MDialog, MDialogButton, MFab, MIconButton, MList, MListItem, MListInput,
  MNavbar, MCover, MEmpty, MToast,
} from '@/components/ui'
import { loadSongs, SONGS_UPDATED_EVENT } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { enqueueScrapeSongs, loadScrapeQueue, onScrapeQueueChanged } from '@/features/scrape/queue'
import { pickSuspiciousSongs } from '@/features/scrape/suspicious'
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

const router = useRouter()
const songs = ref<SongItem[]>([])
const listParentRef = ref<HTMLElement | null>(null)
/** 挂载后防漂移 guard 的交互标记（touchstart/wheel/FAB 跳转都会置位，停止拉回 scrollTop） */
let mountInteracted = false
const stopMountGuard = (): void => {
  mountInteracted = true
}
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

/** child2：筛选可疑歌曲批量入队 */
const isSuspiciousConfirmOpen = ref(false)
const suspiciousCount = computed(() => pickSuspiciousSongs(songs.value).length)

/** 刮削队列计数 */
const scrapeQueueCount = ref(loadScrapeQueue().items.length)
const refreshScrapeQueueCount = (): void => {
  scrapeQueueCount.value = loadScrapeQueue().items.length
}

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
  // 与 currentSongIndex 同源（visibleSongs），搜索过滤后索引一致
  return visibleSongs.value.some((song) => song.id === currentId)
})

/** 当前歌曲在 visibleSongs 中的下标（-1 表示不在列表；virtualRows 基于 visibleSongs，必须同源） */
const currentSongIndex = computed(() => {
  const currentId = playerState.currentSong?.id
  if (!currentId) return -1
  return visibleSongs.value.findIndex((song) => song.id === currentId)
})

/** 当前歌曲行是否在真实可视区（不含 overscan：用行位置与滚动容器视口矩形相交判断） */
const currentSongInViewport = computed(() => {
  const idx = currentSongIndex.value
  if (idx < 0) return false
  const listEl = listParentRef.value
  if (!listEl) return false
  // 从 virtualRows 找当前歌曲行（含 overscan 渲染）
  const row = virtualRows.value.find((item) => item.index === idx)
  if (!row) return false
  // 行顶部/底部相对滚动容器的位置
  const rowTop = row.start - listEl.scrollTop
  const rowBottom = row.end - listEl.scrollTop
  const viewTop = 0
  const viewBottom = listEl.clientHeight
  // 行与可视区矩形有交集即可（overscan 行不视为在视口）
  return rowBottom > viewTop && rowTop < viewBottom
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

/** child2：长按菜单——加入待刮削 */
const onAddToScrapeQueue = (): void => {
  const song = actionSong.value
  isSongActionsOpen.value = false
  if (!song) {
    return
  }
  enqueueScrapeSongs([song.id])
  showToast('已加入待刮削队列')
}

/** child2：多选条——标记选中项为待刮削 */
const onMarkSelectedForScrape = (): void => {
  const ids = Array.from(selectedIds.value)
  if (ids.length === 0) {
    return
  }
  enqueueScrapeSongs(ids)
  showToast(`已标记 ${ids.length} 首为待刮削`)
  exitMultiSelect()
}

/** child2：顶部筛选按钮——打开确认框 */
const onOpenSuspiciousBatch = (): void => {
  if (suspiciousCount.value === 0) {
    showToast('未发现可疑歌曲')
    return
  }
  isSuspiciousConfirmOpen.value = true
}

/** child2：确认框——批量入队 */
const onConfirmSuspiciousBatch = (): void => {
  const picked = pickSuspiciousSongs(songs.value)
  isSuspiciousConfirmOpen.value = false
  if (picked.length === 0) {
    return
  }
  enqueueScrapeSongs(picked.map((song) => song.id))
  showToast(`已加入 ${picked.length} 首到待刮削队列`)
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
let toastTimer: number | undefined
const showToast = (message: string): void => {
  toast.value = { visible: true, message }
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.value.visible = false
  }, 1800)
}

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
  // 用户主动跳转 = 交互：停止挂载后 4s 防漂移 guard（否则跳转后的 scrollTop 会被 guard 拉回期望位置，首次点击失效）
  stopMountGuard()
  const currentId = playerState.currentSong?.id
  if (!currentId) {
    return
  }

  const index = visibleSongs.value.findIndex((song) => song.id === currentId)
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
  // 挂载后 4 秒防漂移 + 挂滚动位置保存。背景：WebView 首屏布局未稳时虚拟列表可能被误滚到底
  // （冷启动与 tab 切回重新挂载均会触发）；4 秒内无用户交互且 scrollTop 漂移远离期望位置时拉回，
  // 用户一交互立即停止（touchstart/wheel/FAB 跳转均置位 mountInteracted，见组件级变量）。
  let attached = false
  const attachScrollSave = () => {
    const cur = listParentRef.value
    if (!cur || attached) return
    attached = true
    cur.addEventListener('touchstart', stopMountGuard, { once: true, passive: true })
    cur.addEventListener('wheel', stopMountGuard, { once: true, passive: true })
    cur.addEventListener('scroll', onListScroll, { passive: true })
    cur.addEventListener('scroll', () => {
      if (Date.now() - mountAt < 4000) return
      savedSongsScrollTop = cur.scrollTop
      sessionStorage.setItem(SCROLL_SAVE_KEY, String(cur.scrollTop))
    }, { passive: true })
  }
  const guard = () => {
    const cur = listParentRef.value
    if (!cur || mountInteracted || Date.now() - mountAt > 4000) return
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

let scrapeQueueUnsubscribe: (() => void) | null = null
onMounted(() => {
  scrapeQueueUnsubscribe = onScrapeQueueChanged(refreshScrapeQueueCount)
})

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener(SONGS_UPDATED_EVENT, refreshSongs)
  }
  scrapeQueueUnsubscribe?.()
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

    /* 对齐椒盐：navbar 灰底磨砂玻璃（基底与列表同色，滚动透磨砂，08-16 定案） */
    :deep(.m-navbar) {
      padding-top: var(--m-navbar-pt, 16px); /* 紧贴状态栏（椒盐实测 0 间距，不再 +6px） */
      background: var(--m-navbar-glass-bg); /* 灰底磨砂：列表中经透 blur，无白色玻璃雾 */
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
      -webkit-backdrop-filter: blur(20px);
      backdrop-filter: blur(20px);
    }
    :deep(.m-navbar__bg) {
      background-color: transparent;
    }
    /* 工具条 subnavbar：48px 与 navbar 同玻璃 */
    :deep(.m-navbar__subnavbar) {
      height: 48px;
      padding: 0;
    }

    /* 对齐椒盐：汉堡图标更小巧（椒盐 ~20dp 宽图标），距左 20dp */
    :deep(.m-navbar__menu-button) {
      width: 36px;
      height: 36px;
      margin-left: 4px;

      svg {
        width: 20px;
        height: 20px;
      }
    }

    /* 对齐椒盐：搜索图标更小巧（椒盐 ~16dp 宽），距右 24dp，颜色深灰近黑 */
    :deep(.songs-page__search-btn) {
      min-width: 36px;
      height: 36px;
      color: var(--m-text); /* 对齐椒盐：搜索图标深灰近黑（椒盐 #0d0d0d），非主色蓝 */

      svg {
        width: 20px;
        height: 20px;
      }
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
    padding-top: calc(var(--m-navbar-pt, 16px) + 44px + 48px); /* navbar + subnavbar(工具条) */
  }

  &__list {
    height: 100%;
    overflow-y: auto;
    box-sizing: border-box;
    padding-top: calc(var(--m-navbar-pt, 16px) + 44px + 48px); /* navbar + subnavbar(工具条) */
    padding-bottom: var(--m-content-pb);
    overflow-anchor: none;

    @media (min-width: 768px) {
      padding-bottom: var(--m-content-pb-md);
    }
  }

  /* 工具条：48dp，位于 navbar subnavbar 内——与 navbar 同一块灰底磨砂表面（无独立背景/边框，消除交界分界） */
  &__toolbar {
    box-sizing: border-box;
    width: 100%;
    height: 48px;
  }

  &__toolbar-wrap {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 100%;
    padding: 0 8px 0 20px; /* 左对齐椒盐：工具条内容从 20dp 开始 */
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

  &__toolbar-left-btn--scrape {
    position: relative;
  }

  &__scrape-badge {
    position: absolute;
    top: 2px;
    right: 0;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 8px;
    background: var(--m-primary);
    color: #fff;
    font-size: 10px;
    font-weight: 600;
    line-height: 16px;
    text-align: center;
    pointer-events: none;
  }

  &__toolbar-left-text {
    font-size: 14px;
  }

  &__toolbar-count {
    flex: 1;
    text-align: center;
    font-size: 14px;
    color: var(--m-text); /* 对齐椒盐：计数深灰近黑（椒盐 #1a1a1a），非灰色 */
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

  /* 搜索栏：替换工具条（同样位于 subnavbar 内，共享 navbar 灰底磨砂） */
  &__searchbar {
    box-sizing: border-box;
    width: 100%;
    height: 48px;
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
    /* 椒盐 ⋮ 三点按钮：小尺寸，紧贴右缘（按下变暗反馈由 MIconButton 提供） */
    width: 36px;
    height: 48px;
    flex: none;
    color: #949fab; /* 椒盐 ⋮ 实心点蓝灰 (148,159,171) */
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

  /* 多选底部操作条：悬浮在 MiniPlayer 胶囊上方（胶囊顶距底 72px = 64px 高 + 8px 悬浮空隙） */
  &__multibar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: calc(72px + var(--m-safe-area-bottom, 0px));
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
    top: calc(var(--m-navbar-pt, 16px) + 44px + 48px);
    bottom: calc(72px + var(--m-safe-area-bottom, 0px));
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
    transition: transform 0.15s ease, opacity 0.15s ease;

    &:active {
      transform: scale(1.4);
      color: var(--m-text);
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

    /* 液态玻璃圆底（0.8 配方，与深色 0.8 一致；半透明白底 + 模糊 + 内高光 + 高光边）+ 主题图标色 */
    background: var(--m-glass-bg);
    color: var(--m-text-2);
    -webkit-backdrop-filter: blur(20px);
    backdrop-filter: blur(20px);
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.65),
      0 2px 8px rgba(0, 0, 0, 0.12);
    border: 1px solid rgba(255, 255, 255, 0.5);
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
:global(.dark .songs-page__more-btn) {
  color: rgba(225, 230, 235, 0.75); /* 椒盐深色 subText #BFE1E6EB */
}

/* 深色主题：跳转 FAB 与播放条同质感（液态玻璃深色配方） */
:global(.dark .songs-page__jump-fab) {
  background: var(--m-glass-bg);
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 2px 8px rgba(0, 0, 0, 0.3);
}

/* 深色主题：工具条随 navbar 灰底磨砂（subnavbar 内无独立背景，无需单独覆盖） */

/* 深色主题：navbar 灰底磨砂深色（随 --m-navbar-glass-bg，基底 #202020）。
 * 必须 :global(.dark .songs-page .m-navbar)（无 scope 属性、特异性 (0,3,0)）：
 * MNavbar 组件深色规则 .dark .m-navbar[data-v] 与页面 :deep(.m-navbar) 浅色规则同为
 * (0,2,0)，异步 chunk 后注入会覆盖组件深色规则，导致深色下 navbar 白雾。
 * 注意：不得写 :global(.dark) + :deep 组合——compiler-sfc 会丢弃 :global 之后的选择器。 */
:global(.dark .songs-page .m-navbar) {
  background: var(--m-navbar-glass-bg);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

</style>