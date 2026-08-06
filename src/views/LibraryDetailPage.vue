<template>
  <div class="m-page">
    <h-nav-bar
      :title="pageTitle"
      show-back
      back-aria-label="返回"
      :fixed="false"
      @handle-left-click="goBack"
    >
      <template #right>
        <h-button
          variant="ghost"
          is-icon-only
          shape="square"
          aria-label="播放全部"
          :disabled="songs.length === 0"
          @click="onPlayAll"
        >
          <h-icon :icon="playOutline" />
        </h-button>
      </template>
    </h-nav-bar>
    <div class="m-content" style="overflow: hidden;">
      <div class="h-full md:max-w-[var(--muses-content-max-width)] md:mx-auto">
        <h-empty
          v-if="songs.length === 0"
          title="没有歌曲"
          description="没有找到相关歌曲。"
        />

        <div
          v-else
          ref="listParentRef"
          class="h-full overflow-auto overscroll-contain box-border pb-[var(--muses-mini-player-height)] md:pb-[calc(var(--muses-mini-player-height)+env(safe-area-inset-bottom,0px))] [overflow-anchor:none]"
          role="list"
          :aria-label="`${pageTitle} 歌曲`"
        >
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
                @click="onPlaySong(row.song)"
              >
                <m-cover class="!flex-none" :src="getSongCoverSrc(row.song)" :size="48" radius="sm" alt="" />
                <div class="flex-1 min-w-0 flex flex-col gap-[var(--muses-space-xs)]">
                  <h2 class="m-0 text-[length:var(--muses-font-title)] leading-[var(--muses-line-height-title)] text-[color:var(--muses-color-ink)] truncate">{{ row.song.title }}</h2>
                  <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--muses-color-ink-muted)] truncate">{{ getSongArtistName(row.song) }} - {{ getSongAlbumName(row.song) }}</p>
                </div>
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
import { playOutline } from '@/icons'
import { HButton, HEmpty, HIcon, HNavBar, MCover } from '@/components/ui'
import { loadSongs, SONGS_UPDATED_EVENT } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { getSongAlbumName, getSongArtistName, groupSongsByAlbum, groupSongsByArtist } from '@/features/library/views'
import {
  clearQueue,
  enqueueSongs,
  playerState,
  playSong,
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

const onPlayAll = () => {
  const list = songs.value
  if (list.length === 0) {
    return
  }
  clearQueue()
  enqueueSongs(list)
  void playSong(list[0])
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
})

// 路由参数变化时刷新数据（同一组件复用场景）
watch(groupName, () => {
  refresh()
})
</script>
