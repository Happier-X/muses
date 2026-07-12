<template>
  <ion-page ref="pageRef">
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-button
            fill="clear"
            aria-label="随机播放全部"
            :disabled="songs.length === 0"
            @click="onShuffleAll"
          >
            <ion-icon slot="icon-only" :icon="shuffle" aria-hidden="true" />
          </ion-button>
        </ion-buttons>
        <ion-title class="page-title">歌曲</ion-title>
        <ion-buttons slot="end">
          <ion-button fill="clear" aria-label="搜索歌曲">
            <ion-icon slot="icon-only" :icon="searchOutline" aria-hidden="true" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>
    <ion-content :fullscreen="true">
      <ion-header collapse="condense">
        <ion-toolbar>
          <ion-title class="page-title" size="large">歌曲</ion-title>
        </ion-toolbar>
      </ion-header>

      <div v-if="songs.length === 0" class="empty-state">
        <h2>还没有歌曲</h2>
        <p>请先到音源页添加并扫描音源。</p>
      </div>

      <div v-else class="list-grid tablet-content-limit">
        <ion-list>
          <ion-item
            v-for="song in songs"
            :key="song.id"
            button
            :detail="false"
            lines="none"
            class="song-item"
            :class="{ 'is-playing': playerState.currentSong?.id === song.id }"
            :data-song-id="song.id"
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
              aria-label="更多歌曲操作"
              @click.stop="enqueueSingleSong(song)"
            >
              <ion-icon slot="icon-only" :icon="ellipsisVertical" aria-hidden="true" />
            </ion-button>
          </ion-item>
        </ion-list>
      </div>

      <ion-fab
        v-if="currentPlayingInList"
        vertical="bottom"
        horizontal="end"
        slot="fixed"
        class="jump-current-fab"
      >
        <ion-fab-button
          aria-label="跳转到当前播放"
          @click="scrollToCurrentSong"
        >
          <ion-icon :icon="locateOutline" aria-hidden="true" />
        </ion-fab-button>
      </ion-fab>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Capacitor } from '@capacitor/core'
import {
  IonButton,
  IonButtons,
  IonContent,
  IonFab,
  IonFabButton,
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
import { ellipsisVertical, locateOutline, musicalNotesOutline, searchOutline, shuffle } from 'ionicons/icons'
import { loadSongs, SONGS_UPDATED_EVENT } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { getSongAlbumName, getSongArtistName, sortSongsForDisplay } from '@/features/library/views'
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
const pageRef = ref<HTMLElement | { $el?: HTMLElement } | null>(null)
let jumpHighlightTimer: ReturnType<typeof setTimeout> | null = null

const currentPlayingInList = computed(() => {
  const currentId = playerState.currentSong?.id
  if (!currentId) {
    return false
  }
  return songs.value.some((song) => song.id === currentId)
})

const refreshSongs = () => {
  songs.value = sortSongsForDisplay(loadSongs())
}

const enqueueSingleSong = (song: SongItem) => {
  enqueueSong(song)
}

const onShuffleAll = () => {
  if (songs.value.length === 0) {
    return
  }

  clearQueue()
  enqueueSongs(songs.value)
  if (!shuffleEnabled()) {
    toggleShuffle()
  }
  const first = selectSongAtIndex(0)
  if (first) {
    void playSong(first)
  }
}

const resolvePageRoot = (): ParentNode | null => {
  const value = pageRef.value
  if (!value) {
    return typeof document !== 'undefined' ? document : null
  }
  if (value instanceof HTMLElement) {
    return value
  }
  if (value.$el instanceof HTMLElement) {
    return value.$el
  }
  return typeof document !== 'undefined' ? document : null
}

const findSongRow = (songId: string): HTMLElement | null => {
  const root = resolvePageRoot()
  if (!root) {
    return null
  }

  const rows = Array.from(root.querySelectorAll<HTMLElement>('[data-song-id]'))
  return rows.find((row) => row.getAttribute('data-song-id') === songId) ?? null
}

const scrollToCurrentSong = () => {
  const currentId = playerState.currentSong?.id
  if (!currentId) {
    return
  }

  const row = findSongRow(currentId)
  if (!row) {
    return
  }

  row.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })

  row.classList.add('jump-highlight')
  if (jumpHighlightTimer) {
    clearTimeout(jumpHighlightTimer)
  }
  jumpHighlightTimer = setTimeout(() => {
    row.classList.remove('jump-highlight')
    jumpHighlightTimer = null
  }, 1200)
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
})

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener(SONGS_UPDATED_EVENT, refreshSongs)
  }
  if (jumpHighlightTimer) {
    clearTimeout(jumpHighlightTimer)
    jumpHighlightTimer = null
  }
})

onIonViewWillEnter(refreshSongs)
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
  --min-height: 72px;
}

.song-cover {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: rgba(var(--ion-color-medium-rgb), 0.16);
  color: var(--ion-color-medium);
  flex-shrink: 0;
}

.song-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.song-cover ion-icon {
  font-size: 24px;
}

.song-item h2 {
  font-weight: 600;
}

.more-button {
  --padding-start: 8px;
  --padding-end: 8px;
  color: var(--ion-color-medium);
}

.is-playing {
  --background: rgba(var(--ion-color-primary-rgb), 0.1);
}

.jump-highlight {
  --background: rgba(var(--ion-color-primary-rgb), 0.22);
}

/* 避开底部 Tab Bar（~64）+ MiniPlayer（~64）+ 间距，保留安全区 */
.jump-current-fab {
  bottom: calc(144px + var(--ion-safe-area-bottom, 0px));
  right: 12px;
}

@media (min-width: 768px) {
  .jump-current-fab {
    /* 宽屏无底部 Tab Bar，仍避开 MiniPlayer */
    bottom: calc(80px + var(--ion-safe-area-bottom, 0px));
  }

  /* 歌曲页宽屏始终单列，仅保留内容最大宽度居中 */
  .list-grid {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
</style>
