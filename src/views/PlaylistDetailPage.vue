<template>
  <div class="m-page playlist-detail-page">
    <div class="playlist-detail-page__navbar-wrap">
      <m-navbar right-class="playlist-detail-page__right">
        <template #left>
          <m-navbar-back-link text="返回" @click="goBack" />
        </template>
        <template #title>{{ playlist?.name ?? '歌单' }}</template>
        <template #right>
          <m-button
            component="button"
            variant="clear"
            rounded
            class="playlist-detail-page__play-all-btn"
            aria-label="播放全部"
            :disabled="resolvedSongs.length === 0"
            @click="onPlayAll"
          >
            <component :is="playOutline" aria-hidden="true" class="playlist-detail-page__play-all-icon" />
          </m-button>
        </template>
      </m-navbar>
    </div>
    <div class="m-content playlist-detail-page__content">
      <div class="playlist-detail-page__fill">
        <div v-if="!playlist" class="playlist-detail-page__empty">
          <m-empty title="歌单不存在" description="可能已被删除。" />
        </div>

        <div v-else-if="resolvedSongs.length === 0" class="playlist-detail-page__empty">
          <m-empty
            title="歌单是空的"
            description="在歌曲页点「更多」→「加入歌单」添加歌曲。"
          />
        </div>

        <div v-else ref="listParentRef" class="playlist-detail-page__list" role="list" aria-label="歌单歌曲">
          <m-list strong outline class="playlist-detail-page__list-root">
            <div class="playlist-detail-page__vlist" :style="{ height: `${totalSize}px` }">
              <div
                v-for="row in visibleRows"
                :key="row.song.id"
                :ref="measureVirtualRow"
                class="playlist-detail-page__virtual-row"
                role="listitem"
                :data-index="row.virtualRow.index"
                :style="{ transform: `translateY(${row.virtualRow.start}px)` }"
              >
                <m-list-item
                  :chevron="false"
                  link
                  :title="row.song.title"
                  :subtitle="`${getSongArtistName(row.song)} - ${getSongAlbumName(row.song)}`"
                  title-class="playlist-detail-page__row-title"
                  subtitle-class="playlist-detail-page__row-subtitle"
                  class="playlist-detail-page__row"
                  :class="playerState.currentSong?.id === row.song.id ? 'playlist-detail-page__row--playing' : ''"
                  role="button"
                  tabindex="0"
                  @click="onPlaySong(row.song)"
                >
                  <template #media>
                    <m-cover :src="getSongCoverSrc(row.song)" :size="48" radius="sm" alt="" />
                  </template>
                  <template #after>
                    <m-button
                      component="button"
                      size="small"
                      variant="clear"
                      rounded
                      class="playlist-detail-page__remove-btn"
                      :aria-label="`从歌单移除 ${row.song.title}`"
                      @click.stop="onRemove(row.song.id)"
                    >
                      <component :is="removeCircleOutline" aria-hidden="true" class="playlist-detail-page__remove-icon" />
                    </m-button>
                  </template>
                </m-list-item>
              </div>
            </div>
          </m-list>
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
import {
  MButton, MList, MListItem, MNavbar, MNavbarBackLink, MCover, MEmpty,
} from '@/components/ui'
import { loadSongs, SONGS_UPDATED_EVENT } from '@/features/library/storage'
import { setCategoriesSegment } from '@/features/player/categoriesSegment'
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
  // 歌单详情属于「音乐」页歌单段，返回时恢复对应段
  setCategoriesSegment('playlists')
  router.back()
  // vue-router 的 back() 在无历史时无操作，用超时兜底
  window.setTimeout(() => {
    router.replace('/tabs/categories')
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

<style scoped lang="scss">
.playlist-detail-page {
  &__fill { height: 100%; }
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
    display: flex;
    align-items: center;
  }

  &__play-all-btn {
    width: 32px;
    height: 32px;
    padding: 0;
  }

  &__play-all-icon {
    width: 16px;
    height: 16px;
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

  &__remove-btn {
    flex: none;
    margin-left: auto;
    width: 32px;
    height: 32px;
    padding: 0;
  }

  &__remove-icon {
    width: 16px;
    height: 16px;
  }
}

/* 正在播放行高亮 */
.playlist-detail-page :deep(.is-playing),
.playlist-detail-page :deep(.playlist-detail-page__row--playing) {
  background-color: rgba(0, 0, 0, 0.05);
}

:global(.dark) .playlist-detail-page :deep(.is-playing),
:global(.dark) .playlist-detail-page :deep(.playlist-detail-page__row--playing) {
  background-color: rgba(255, 255, 255, 0.1);
}
</style>