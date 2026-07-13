<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button default-href="/tabs/playlists" text="" />
        </ion-buttons>
        <ion-title class="page-title">{{ playlist?.name ?? '歌单' }}</ion-title>
        <ion-buttons slot="end">
          <ion-button
            fill="clear"
            aria-label="播放全部"
            :disabled="resolvedSongs.length === 0"
            @click="onPlayAll"
          >
            <ion-icon slot="icon-only" :icon="playOutline" aria-hidden="true" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>
    <ion-content :fullscreen="true">
      <ion-header collapse="condense">
        <ion-toolbar>
          <ion-title class="page-title" size="large">{{ playlist?.name ?? '歌单' }}</ion-title>
        </ion-toolbar>
      </ion-header>

      <div class="tablet-content-limit">
        <div v-if="!playlist" class="empty-state">
          <h2>歌单不存在</h2>
          <p>可能已被删除。</p>
        </div>

        <div v-else-if="resolvedSongs.length === 0" class="empty-state">
          <h2>歌单是空的</h2>
          <p>在歌曲页点「更多」→「加入歌单」添加歌曲。</p>
        </div>

        <ion-list v-else>
          <ion-item
            v-for="song in resolvedSongs"
            :key="song.id"
            button
            :detail="false"
            lines="none"
            class="song-item"
            :class="{ 'is-playing': playerState.currentSong?.id === song.id }"
            @click="playSong(song)"
          >
            <div class="song-cover" slot="start" aria-hidden="true">
              <img v-if="getSongCoverSrc(song)" :src="getSongCoverSrc(song)" alt="" />
              <ion-icon v-else :icon="musicalNotesOutline" aria-hidden="true" />
            </div>
            <ion-label>
              <h2>{{ song.title }}</h2>
              <p>{{ getSongArtistName(song) }} - {{ getSongAlbumName(song) }}</p>
            </ion-label>
            <ion-button
              slot="end"
              fill="clear"
              class="more-button"
              aria-label="从歌单移除"
              @click.stop="onRemove(song.id)"
            >
              <ion-icon slot="icon-only" :icon="removeCircleOutline" aria-hidden="true" />
            </ion-button>
          </ion-item>
        </ion-list>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Capacitor } from '@capacitor/core'
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonPage,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { musicalNotesOutline, playOutline, removeCircleOutline } from 'ionicons/icons'
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

const refresh = () => {
  allSongs.value = loadSongs()
  playlist.value = playlistId.value ? getPlaylist(playlistId.value) : undefined
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
.page-title {
  text-align: center;
}

.empty-state {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 24px;
  color: var(--ion-color-medium);
}

.empty-state h2 {
  margin-bottom: 8px;
  color: var(--ion-text-color);
}

.song-item {
  --padding-start: 12px;
  --inner-padding-end: 4px;
  margin-bottom: 4px;
}

.song-item.is-playing {
  --background: rgba(var(--ion-color-primary-rgb), 0.08);
}

.song-cover {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(var(--ion-color-medium-rgb), 0.16);
  color: var(--ion-color-medium);
  font-size: 22px;
}

.song-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.more-button {
  margin: 0;
}

@media (min-width: 768px) {
  .tablet-content-limit {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
</style>
