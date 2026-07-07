<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title class="page-title">专辑</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content :fullscreen="true">
      <ion-header collapse="condense">
        <ion-toolbar>
          <ion-title class="page-title" size="large">专辑</ion-title>
        </ion-toolbar>
      </ion-header>

      <div v-if="albums.length === 0" class="empty-state">
        <h2>还没有专辑</h2>
        <p>请先到音源页添加并扫描音源。</p>
      </div>

      <ion-list v-else>
        <ion-item v-for="album in albums" :key="album.name">
          <ion-label>
            <h2>{{ album.name }}</h2>
            <p>{{ album.songCount }} 首歌曲</p>
            <p>{{ album.artistSummary }}</p>
          </ion-label>
        </ion-item>
      </ion-list>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { IonContent, IonHeader, IonItem, IonLabel, IonList, IonPage, IonTitle, IonToolbar, onIonViewWillEnter } from '@ionic/vue'
import { loadSongs } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { groupSongsByAlbum } from '@/features/library/views'

const songs = ref<SongItem[]>([])
const albums = computed(() => groupSongsByAlbum(songs.value))

const refreshSongs = () => {
  songs.value = loadSongs()
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
</style>
