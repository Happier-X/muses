<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button default-href="/tabs/playlists" text="" />
        </ion-buttons>
        <ion-title>{{ playlist?.name ?? '歌单' }}</ion-title>
        <ion-buttons slot="end">
          <m-icon-button
            :icon="playOutline"
            ariaLabel="播放全部"
            :disabled="resolvedSongs.length === 0"
            @click="onPlayAll"
          />
        </ion-buttons>
      </ion-toolbar>
    </ion-header>
    <ion-content :fullscreen="true">
      <ion-header collapse="condense">
        <ion-toolbar>
          <ion-title size="large">{{ playlist?.name ?? '歌单' }}</ion-title>
        </ion-toolbar>
      </ion-header>

      <div class="tablet-content-limit">
        <m-empty-state v-if="!playlist" title="歌单不存在" description="可能已被删除。" />

        <m-empty-state
          v-else-if="resolvedSongs.length === 0"
          title="歌单是空的"
          description="在歌曲页点「更多」→「加入歌单」添加歌曲。"
        />

        <div v-else ref="listParentRef" class="playlist-list" role="list" aria-label="歌单歌曲">
          <div class="playlist-list-spacer" :style="{ height: `${totalSize}px` }">
            <div
              v-for="row in visibleRows"
              :key="row.song.id"
              :ref="measureVirtualRow"
              class="playlist-row"
              role="listitem"
              :data-index="row.virtualRow.index"
              :style="{ transform: `translateY(${row.virtualRow.start}px)` }"
            >
              <m-list-row
                class="song-item"
                :title="row.song.title"
                :subtitle="`${getSongArtistName(row.song)} - ${getSongAlbumName(row.song)}`"
                :cover-src="getSongCoverSrc(row.song)"
                :cover-size="48"
                cover-radius="sm"
                :playing="playerState.currentSong?.id === row.song.id"
                @click="onPlaySong(row.song, $event)"
              >
                <template #end>
                  <m-icon-button
                    class="more-button"
                    :icon="removeCircleOutline"
                    :ariaLabel="`从歌单移除 ${row.song.title}`"
                    stop-propagation
                    @click="onRemove(row.song.id)"
                  />
                </template>
              </m-list-row>
            </div>
          </div>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, type ComponentPublicInstance } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { useRoute } from 'vue-router'
import { Capacitor } from '@capacitor/core'
import {
  IonBackButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { playOutline, removeCircleOutline } from '@/icons/ion-lucide'
import { MEmptyState, MIconButton, MListRow } from '@/components/ui'
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

const onPlayAll = () => {
  const songs = resolvedSongs.value
  if (songs.length === 0) {
    return
  }
  clearQueue()
  enqueueSongs(songs)
  void playSong(songs[0])
}

const onPlaySong = (song: SongItem, event: MouseEvent): void => {
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

onIonViewWillEnter(() => {
  refresh()
})
</script>

<style scoped>
.tablet-content-limit {
  height: 100%;
}

.playlist-list {
  height: 100%;
  overflow: auto;
  overscroll-behavior: contain;
}

.playlist-list-spacer {
  position: relative;
  width: 100%;
}

.playlist-row {
  position: absolute;
  inset-inline: 0;
  top: 0;
  box-sizing: border-box;
  min-height: var(--muses-song-row-height);
}

@media (min-width: 768px) {
  .tablet-content-limit {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
</style>
