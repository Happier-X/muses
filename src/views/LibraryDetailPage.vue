<template>
  <div class="m-page library-detail-page">
    <div class="library-detail-page__navbar-wrap">
      <m-navbar right-class="library-detail-page__right">
        <template #left>
          <m-navbar-back-link text="返回" @click="goBack" />
        </template>
        <template #title>{{ pageTitle }}</template>
      </m-navbar>
    </div>
    <div class="m-content library-detail-page__content">
      <div class="library-detail-page__fill">
        <div v-if="songs.length === 0" class="library-detail-page__empty">
          <m-empty
            title="没有歌曲"
            description="没有找到相关歌曲。"
          />
        </div>

        <div
          v-else
          ref="listParentRef"
          class="library-detail-page__list"
          role="list"
          :aria-label="`${pageTitle} 歌曲`"
          @scroll="onListScroll"
        >
          <div class="library-detail-page__shuffle-bar">
            <div class="library-detail-page__shuffle-inner">
              <div class="library-detail-page__shuffle-blur" aria-hidden="true" />
              <div class="library-detail-page__shuffle-glass" aria-hidden="true" />
              <m-button
                component="button"
                size="small"
                class="library-detail-page__shuffle-btn"
                aria-label="随机播放全部"
                :disabled="songs.length === 0"
                @click="onShuffleAll"
              >
                <component :is="shuffle" aria-hidden="true" />
                随机播放全部
              </m-button>
            </div>
          </div>
          <m-list strong outline class="library-detail-page__list-root">
            <div class="library-detail-page__vlist" :style="{ height: `${totalSize}px` }">
              <div
                v-for="row in visibleRows"
                :key="row.song.id"
                :ref="measureVirtualRow"
                class="library-detail-page__virtual-row"
                role="listitem"
                :data-index="row.virtualRow.index"
                :style="{ transform: `translateY(${row.virtualRow.start}px)` }"
              >
                <m-list-item
                  :chevron="false"
                  link
                  :title="row.song.title"
                  :subtitle="`${getSongArtistName(row.song)} - ${getSongAlbumName(row.song)}`"
                  title-class="library-detail-page__row-title"
                  subtitle-class="library-detail-page__row-subtitle"
                  class="library-detail-page__row"
                  :class="songItemClass(row.song.id)"
                  role="button"
                  tabindex="0"
                  @click="onPlaySong(row.song)"
                >
                  <template #media>
                    <m-cover :src="getSongCoverSrc(row.song)" :size="48" radius="sm" alt="" />
                  </template>
                </m-list-item>
              </div>
            </div>
          </m-list>
        </div>
      </div>
    </div>

    <m-fab
      v-if="showJumpBubble"
      class="library-detail-page__jump-fab"
      :style="{ position: 'fixed', left: `${fabOffset.x}px`, top: `${fabOffset.y}px` }"
      aria-label="跳转到当前播放"
      @click="scrollToCurrentSong"
    >
      <template #icon>
        <component :is="crosshair" aria-hidden="true" />
      </template>
    </m-fab>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch, type ComponentPublicInstance } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { useRoute, useRouter } from 'vue-router'
import { Capacitor } from '@capacitor/core'
import { crosshair, shuffle } from '@/icons'
import {
  MButton, MFab, MList, MListItem, MNavbar, MNavbarBackLink, MCover, MEmpty,
} from '@/components/ui'
import { loadSongs, SONGS_UPDATED_EVENT } from '@/features/library/storage'
import { setCategoriesSegment } from '@/features/player/categoriesSegment'
import type { SongItem } from '@/features/library/types'
import { getSongAlbumName, getSongArtistName, groupSongsByAlbum, groupSongsByArtist } from '@/features/library/views'
import {
  clearQueue,
  enqueueSongs,
  playerState,
  playSong,
  selectSongAtIndex,
  shuffleEnabled,
  toggleShuffle,
} from '@/features/player/controller'

const route = useRoute()
const router = useRouter()
const allSongs = ref<SongItem[]>([])
const listParentRef = ref<HTMLElement | null>(null)

const kind = computed(() => (route.params.kind === 'artist' ? 'artist' : 'album'))
const groupName = computed(() => {
  const raw = route.params.name
  return typeof raw === 'string' ? decodeURIComponent(raw) : ''
})

const pageTitle = computed(() => groupName.value || (kind.value === 'artist' ? '艺术家' : '专辑'))

const songs = computed(() => {
  const name = groupName.value
  if (!name) {
    return [] as SongItem[]
  }
  const groups = kind.value === 'artist'
    ? groupSongsByArtist(allSongs.value)
    : groupSongsByAlbum(allSongs.value)
  return groups.find((group) => group.name === name)?.songs ?? []
})

const rowVirtualizer = useVirtualizer(
  computed(() => ({
    count: songs.value.length,
    getScrollElement: () => listParentRef.value,
    estimateSize: () => 72,
    overscan: 8,
  })),
)

const visibleRows = computed(() => rowVirtualizer.value.getVirtualItems().flatMap((virtualRow) => {
  const song = songs.value[virtualRow.index]
  return song ? [{ virtualRow, song }] : []
}))
const totalSize = computed(() => rowVirtualizer.value.getTotalSize())

const measureVirtualRow = (element: Element | ComponentPublicInstance | null): void => {
  rowVirtualizer.value.measureElement(element instanceof HTMLElement ? element : null)
}

const highlightedSongId = ref<string | null>(null)
let jumpHighlightTimer: ReturnType<typeof setTimeout> | null = null

const songItemClass = (songId: string): string => {
  const classes: string[] = []
  if (playerState.currentSong?.id === songId) {
    classes.push('is-playing')
  }
  if (highlightedSongId.value === songId) {
    classes.push('library-detail-page__jump-highlight')
  }
  return classes.join(' ')
}

const currentPlayingInList = computed(() => {
  const currentId = playerState.currentSong?.id
  if (!currentId) {
    return false
  }
  return songs.value.some((song) => song.id === currentId)
})

const currentSongIndex = computed(() => {
  const currentId = playerState.currentSong?.id
  if (!currentId) return -1
  return songs.value.findIndex((song) => song.id === currentId)
})

const currentSongInViewport = computed(() => {
  const idx = currentSongIndex.value
  if (idx < 0) return false
  return visibleRows.value.some((row) => row.virtualRow.index === idx)
})

const isListScrolling = ref(false)
let scrollIdleTimer: ReturnType<typeof setTimeout> | null = null
const onListScroll = (): void => {
  isListScrolling.value = true
  if (scrollIdleTimer) clearTimeout(scrollIdleTimer)
  scrollIdleTimer = setTimeout(() => {
    isListScrolling.value = false
  }, 300)
}

const showJumpBubble = computed(
  () => currentPlayingInList.value && !currentSongInViewport.value && !isListScrolling.value,
)

const fabOffset = computed(() => {
  const miniPlayerH = 64
  return { x: window.innerWidth - 48, y: window.innerHeight - miniPlayerH - 48 }
})

const onShuffleAll = () => {
  const list = songs.value
  if (list.length === 0) {
    return
  }
  clearQueue()
  enqueueSongs(list)
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
  rowVirtualizer.value.scrollToIndex(index, { align: 'start', behavior: 'smooth' })
  await nextTick()
  await new Promise<void>((resolve) => {
    requestAnimationFrame(() => resolve())
  })
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

const refresh = () => {
  allSongs.value = loadSongs()
  void nextTick(() => {
    if (songs.value.length > 0) {
      rowVirtualizer.value.measure()
    }
  })
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

const goBack = (): void => {
  // 专辑/艺术家详情属于「音乐」页分段，返回时恢复对应段
  setCategoriesSegment(kind.value === 'artist' ? 'artists' : 'albums')
  router.back()
  window.setTimeout(() => {
    router.replace('/tabs/categories')
  }, 100)
}

const onPlaySong = (song: SongItem): void => {
  void playSong(song)
}

onMounted(() => {
  refresh()
  window.addEventListener(SONGS_UPDATED_EVENT, refresh)
  // 与 SongsPage 同款兜底：冷启动首屏布局未稳时虚拟列表可能被误滚到底
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
  window.removeEventListener(SONGS_UPDATED_EVENT, refresh)
  if (scrollIdleTimer) {
    clearTimeout(scrollIdleTimer)
    scrollIdleTimer = null
  }
  if (jumpHighlightTimer) {
    clearTimeout(jumpHighlightTimer)
    jumpHighlightTimer = null
  }
})

// 路由参数变化时刷新数据（同一组件复用场景）
watch(groupName, () => {
  refresh()
})
</script>

<style scoped lang="scss">
.library-detail-page {
  &__fill { height: 100%; }
  &__shuffle-inner { position: relative; }
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
    overscroll-behavior: contain;
    box-sizing: border-box;
    padding-top: calc(max(16px, var(--m-safe-area-top, 0px)) + 44px);
    padding-bottom: var(--m-content-pb);
    overflow-anchor: none;

    @media (min-width: 768px) {
      padding-bottom: var(--m-content-pb-md);
    }
  }

  &__shuffle-bar {
    position: sticky;
    top: 0;
    z-index: 10;
    box-sizing: border-box;
    width: 100%;
    padding: 4px 8px;
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
    margin: 0;
  }

  &__list-root {
    margin: 0;
  }

  &__virtual-row {
    position: absolute;
    inset-inline: 0;
    top: 0;
    box-sizing: border-box;
    min-height: 72px;
  }

  &__row {
    height: 100%;
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

  &__jump-fab {
    z-index: 1100;
  }

  &__jump-highlight {
    background-color: rgba(0, 122, 255, 0.1);
  }
}

/* 正在播放行高亮 */
.library-detail-page :deep(.is-playing) {
  background-color: rgba(0, 0, 0, 0.05);
}

:global(.dark) .library-detail-page__shuffle-glass {
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.6) 0%,
    rgba(0, 0, 0, 0.35) 50%,
    rgba(0, 0, 0, 0) 100%
  );
}

:global(.dark) .library-detail-page :deep(.is-playing) {
  background-color: rgba(255, 255, 255, 0.1);
}
</style>