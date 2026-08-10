<template>
  <k-page class="k-page m-page flex flex-col overflow-hidden !h-auto !bottom-safe-24 md:!bottom-0">
    <k-navbar rightClass="!h-8">
      <template #left>
        <k-navbar-back-link text="返回" @click="goBack" />
      </template>
      <template #title>{{ pageTitle }}</template>
    </k-navbar>
    <div class="flex-[0_0_48px] min-h-[48px] bg-white dark:bg-black">
      <div class="box-border w-full px-[8px] py-[4px] md:max-w-[720px] md:mx-auto">
        <k-button
          component="button"
          small
          class="m-0"
          aria-label="随机播放全部"
          :disabled="songs.length === 0"
          @click="onShuffleAll"
        >
          <component :is="shuffle" aria-hidden="true" />
          随机播放全部
        </k-button>
      </div>
    </div>
    <div class="m-content" style="overflow: hidden;">
      <div class="h-full md:max-w-[720px] md:mx-auto">
        <m-empty
          v-if="songs.length === 0"
          title="没有歌曲"
          description="没有找到相关歌曲。"
        />

        <div
          v-else
          ref="listParentRef"
          class="h-full overflow-auto overscroll-contain box-border pb-[64px] md:pb-safe-16 [overflow-anchor:none]"
          role="list"
          :aria-label="`${pageTitle} 歌曲`"
          @scroll="onListScroll"
        >
          <div class="relative w-full" :style="{ height: `${totalSize}px` }">
            <div
              v-for="row in visibleRows"
              :key="row.song.id"
              :ref="measureVirtualRow"
              class="absolute inset-x-0 top-0 box-border min-h-[72px]"
              role="listitem"
              :data-index="row.virtualRow.index"
              :style="{ transform: `translateY(${row.virtualRow.start}px)` }"
            >
              <k-list-item
                :title="row.song.title"
                :subtitle="`${getSongArtistName(row.song)} - ${getSongAlbumName(row.song)}`"
                titleClass="min-w-0 truncate"
                subtitleClass="truncate"
                class="h-full"
                :class="songItemClass(row.song.id)"
                role="button"
                tabindex="0"
                @click="onPlaySong(row.song)"
              >
                <template #media>
                  <m-cover :src="getSongCoverSrc(row.song)" :size="48" radius="sm" alt="" />
                </template>
              </k-list-item>
            </div>
          </div>
        </div>
      </div>
    </div>

    <k-fab
      v-if="showJumpBubble"
      class="z-[1100]"
      :style="{ position: 'fixed', left: `${fabOffset.x}px`, top: `${fabOffset.y}px` }"
      aria-label="跳转到当前播放"
      @click="scrollToCurrentSong"
    >
      <template #icon>
        <component :is="crosshair" aria-hidden="true" />
      </template>
    </k-fab>
  </k-page>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch, type ComponentPublicInstance } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { useRoute, useRouter } from 'vue-router'
import { Capacitor } from '@capacitor/core'
import { crosshair, shuffle } from '@/icons'
import { kButton, kFab, kListItem, kNavbar, kPage, kNavbarBackLink, MCover, MEmpty } from '@/components/ui'
import { loadSongs, SONGS_UPDATED_EVENT } from '@/features/library/storage'
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
    classes.push('is-playing bg-black/5 dark:bg-white/10')
  }
  if (highlightedSongId.value === songId) {
    classes.push('bg-primary/10')
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
  router.back()
  window.setTimeout(() => {
    router.replace(kind.value === 'artist' ? '/tabs/artists' : '/tabs/albums')
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
