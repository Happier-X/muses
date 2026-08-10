<template>
  <k-page class="k-page m-page flex flex-col overflow-hidden !h-auto !bottom-safe-24 md:!bottom-0">
    <k-navbar rightClass="!h-8">
      <template #left>
        <k-navbar-back-link text="返回" @click="goBack" />
      </template>
      <template #title>{{ playlist?.name ?? '歌单' }}</template>
      <template #right>
        <k-button
          component="button"
          clear
          rounded
          class="size-8"
          aria-label="播放全部"
          :disabled="resolvedSongs.length === 0"
          @click="onPlayAll"
        >
          <component :is="playOutline" aria-hidden="true" class="size-4" />
        </k-button>
      </template>
    </k-navbar>
    <div class="m-content" style="overflow: hidden;">
      <div class="h-full ">
        <m-empty v-if="!playlist" title="歌单不存在" description="可能已被删除。" />

        <m-empty
          v-else-if="resolvedSongs.length === 0"
          title="歌单是空的"
          description="在歌曲页点「更多」→「加入歌单」添加歌曲。"
        />

        <div v-else ref="listParentRef" class="h-full overflow-auto overscroll-contain box-border pb-[64px] md:pb-safe-16 [overflow-anchor:none]" role="list" aria-label="歌单歌曲">
          <k-list strong-ios outline-ios class="!my-0">
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
                :chevron="false"
                link
                :title="row.song.title"
                :subtitle="`${getSongArtistName(row.song)} - ${getSongAlbumName(row.song)}`"
                titleClass="min-w-0 truncate"
                subtitleClass="truncate"
                class="h-full"
                :class="playerState.currentSong?.id === row.song.id ? 'bg-black/5 dark:bg-white/10' : ''"
                role="button"
                tabindex="0"
                @click="onPlaySong(row.song)"
              >
                <template #media>
                  <m-cover :src="getSongCoverSrc(row.song)" :size="48" radius="sm" alt="" />
                </template>
                <template #after>
                  <k-button
                    component="button"
                    small
                    rounded
                    class="flex-none m-0 ml-auto size-8"
                    :aria-label="`从歌单移除 ${row.song.title}`"
                    @click.stop="onRemove(row.song.id)"
                  >
                    <component :is="removeCircleOutline" aria-hidden="true" class="size-4" />
                  </k-button>
                </template>
              </k-list-item>
            </div>
          </div>
        </k-list>
        </div>
      </div>
    </div>
  </k-page>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch, type ComponentPublicInstance } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { useRoute, useRouter } from 'vue-router'
import { Capacitor } from '@capacitor/core'
import { playOutline, removeCircleOutline } from '@/icons'
import { kButton, kList, kListItem, kNavbar, kPage, kNavbarBackLink, MCover, MEmpty } from '@/components/ui'
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

const onPlaySong = (song: SongItem): void => {
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
  window.removeEventListener(PLAYLISTS_UPDATED_EVENT, refresh)
  window.removeEventListener(SONGS_UPDATED_EVENT, refresh)
})

// 路由参数变化时刷新数据（同一组件复用场景）
watch(playlistId, () => {
  refresh()
})
</script>
