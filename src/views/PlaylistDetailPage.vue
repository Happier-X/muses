<template>
  <div class="m-page">
    <h-nav-bar
      :title="playlist?.name ?? '歌单'"
      show-back
      back-aria-label="返回歌单"
      :fixed="false"
      @handle-left-click="goBack"
    >
      <template #right>
        <h-button
          variant="ghost"
          is-icon-only
          shape="square"
          aria-label="播放全部"
          :disabled="resolvedSongs.length === 0"
          @click="onPlayAll"
        >
          <h-icon :icon="playOutline" />
        </h-button>
      </template>
    </h-nav-bar>
    <div class="m-content" style="overflow: hidden;">
      <div class="h-full md:max-w-[var(--muses-content-max-width)] md:mx-auto">
        <h-empty v-if="!playlist" title="歌单不存在" description="可能已被删除。" />

        <h-empty
          v-else-if="resolvedSongs.length === 0"
          title="歌单是空的"
          description="在歌曲页点「更多」→「加入歌单」添加歌曲。"
        />

        <div v-else ref="listParentRef" class="h-full overflow-auto overscroll-contain" role="list" aria-label="歌单歌曲">
          <div class="relative w-full" :style="{ height: `${totalSize}px` }">
            <div
              v-for="row in visibleRows"
              :key="row.song.id"
              :ref="measureVirtualRow"
              class="absolute inset-x-0 top-0 box-border min-h-[var(--muses-song-row-height)]"
              role="listitem"
              :data-index="row.virtualRow.index"
              :style="{ transform: `translateY(${row.virtualRow.start}px)` }"
            >
              <div
                class="flex items-center gap-[var(--muses-space-md)] p-[var(--muses-space-md)]"
                :class="playerState.currentSong?.id === row.song.id ? 'bg-[var(--muses-color-playing-bg)]' : ''"
                role="button"
                tabindex="0"
                @click="onPlaySong(row.song, $event)"
              >
                <m-cover class="!flex-none" :src="getSongCoverSrc(row.song)" :size="48" radius="sm" alt="" />
                <div class="flex-1 min-w-0 flex flex-col gap-[var(--muses-space-xs)]">
                  <h2 class="m-0 text-[length:var(--muses-font-title)] leading-[var(--muses-line-height-title)] text-[color:var(--muses-color-ink)] truncate">{{ row.song.title }}</h2>
                  <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--muses-color-ink-muted)] truncate">{{ getSongArtistName(row.song) }} - {{ getSongAlbumName(row.song) }}</p>
                </div>
                <h-button
                  variant="ghost"
                  is-icon-only
                  shape="square"
                  class="more-button flex-none m-0 ml-auto"
                  :aria-label="`从歌单移除 ${row.song.title}`"
                  @click.stop="onRemove(row.song.id)"
                >
                  <h-icon :icon="removeCircleOutline" />
                </h-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch, type ComponentPublicInstance } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { useRoute, useRouter } from 'vue-router'
import { Capacitor } from '@capacitor/core'
import { playOutline, removeCircleOutline } from '@/icons'
import { HButton, HEmpty, HIcon, HNavBar, MCover } from '@/components/ui'
import { loadSongs, SONGS_UPDATED_EVENT } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { getSongAlbumName, getSongArtistName } from '@/features/library/views'
import {
  getPlaylist,
  PLAYLISTS_UPDATED_EVENT,
  removeSongFromPlaylist,
  resolvePlaylistSongs,
  type Playlist,
} from '@/features/playlist'
import {
  clearQueue,
  enqueueSongs,
  playerState,
  playSong,
} from '@/features/player/controller'

const route = useRoute()
const router = useRouter()
const playlist = ref<Playlist | undefined>()
const allSongs = ref<SongItem[]>([])
const listParentRef = ref<HTMLElement | null>(null)

const playlistId = computed(() => {
  const raw = route.params.id
  return typeof raw === 'string' ? decodeURIComponent(raw) : ''
})

const resolvedSongs = computed(() => {
  if (!playlist.value) {
    return [] as SongItem[]
  }
  return resolvePlaylistSongs(playlist.value, allSongs.value)
})

const rowVirtualizer = useVirtualizer(
  computed(() => ({
    count: resolvedSongs.value.length,
    getScrollElement: () => listParentRef.value,
    estimateSize: () => 72,
    overscan: 8,
  })),
)

const visibleRows = computed(() => rowVirtualizer.value.getVirtualItems().flatMap((virtualRow) => {
  const song = resolvedSongs.value[virtualRow.index]
  return song ? [{ virtualRow, song }] : []
}))
const totalSize = computed(() => rowVirtualizer.value.getTotalSize())

const measureVirtualRow = (element: Element | ComponentPublicInstance | null): void => {
  rowVirtualizer.value.measureElement(element instanceof HTMLElement ? element : null)
}

const refresh = () => {
  allSongs.value = loadSongs()
  playlist.value = playlistId.value ? getPlaylist(playlistId.value) : undefined
  void nextTick(() => {
    if (resolvedSongs.value.length > 0) {
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
  // 优先回退历史栈，没有历史则回到歌单列表
  router.back()
  // vue-router 的 back() 在无历史时无操作，用超时兜底
  window.setTimeout(() => {
    router.replace('/tabs/playlists')
  }, 100)
}

const onPlayAll = () => {
  const songs = resolvedSongs.value
  if (songs.length === 0) {
    return
  }
  clearQueue()
  enqueueSongs(songs)
  void playSong(songs[0])
}

const onPlaySong = (song: SongItem, event: MouseEvent | KeyboardEvent): void => {
  if (event.composedPath().some((target) => target instanceof Element && target.classList.contains('more-button'))) {
    return
  }
  void playSong(song)
}

const onRemove = (songId: string) => {
  if (!playlist.value) {
    return
  }
  removeSongFromPlaylist(playlist.value.id, songId)
  refresh()
}

onMounted(() => {
  refresh()
  window.addEventListener(PLAYLISTS_UPDATED_EVENT, refresh)
  window.addEventListener(SONGS_UPDATED_EVENT, refresh)
})

onUnmounted(() => {
  window.removeEventListener(PLAYLISTS_UPDATED_EVENT, refresh)
  window.removeEventListener(SONGS_UPDATED_EVENT, refresh)
})

// 路由参数变化时刷新数据（同一组件复用场景）
watch(playlistId, () => {
  refresh()
})
</script>
