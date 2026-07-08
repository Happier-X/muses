<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title class="page-title">歌曲</ion-title>
        <ion-buttons slot="end">
          <ion-button
            v-if="songs.length > 0"
            fill="clear"
            aria-label="将当前歌曲列表全部加入播放队列"
            @click="enqueueAllSongs"
          >
            全部入队
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

      <ion-list v-else>
        <ion-item
          v-for="song in songs"
          :key="song.id"
          button
          :detail="false"
          :class="{ 'is-playing': playerState.currentSong?.id === song.id }"
          @click="playSong(song)"
        >
          <ion-label>
            <h2>{{ song.title }}</h2>
            <p>{{ getSongArtistName(song) }} · {{ getSongAlbumName(song) }}</p>
            <p>{{ formatSongDetail(song) }}</p>
            <p v-if="playerState.currentSong?.id === song.id" class="playing-label">正在播放</p>
          </ion-label>
          <ion-button
            slot="end"
            fill="clear"
            aria-label="追加到播放队列"
            @click.stop="enqueueSingleSong(song)"
          >
            入队
          </ion-button>
        </ion-item>
      </ion-list>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { IonButton, IonButtons, IonContent, IonHeader, IonItem, IonLabel, IonList, IonPage, IonTitle, IonToolbar, onIonViewWillEnter } from '@ionic/vue'
import { loadSongs } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { formatDuration, getSongAlbumName, getSongArtistName, sortSongsForDisplay } from '@/features/library/views'
import { enqueueSong, enqueueSongs, playerState, playSong } from '@/features/player/controller'

const songs = ref<SongItem[]>([])

const refreshSongs = () => {
  songs.value = sortSongsForDisplay(loadSongs())
}

const enqueueAllSongs = () => {
  enqueueSongs(songs.value)
}

const enqueueSingleSong = (song: SongItem) => {
  enqueueSong(song)
}

const formatSongDetail = (song: SongItem): string => {
  const duration = formatDuration(song.duration)
  if (duration) {
    return duration
  }

  return song.sourceType === 'webdav' ? 'WebDAV 音源' : '本地音源'
}

onMounted(refreshSongs)
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

.is-playing {
  --background: rgba(var(--ion-color-primary-rgb), 0.1);
}

.playing-label {
  color: var(--ion-color-primary);
  font-weight: 600;
}
</style>
