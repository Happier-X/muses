<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>艺术家</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content :fullscreen="true">
      <ion-header collapse="condense">
        <ion-toolbar>
          <ion-title size="large">艺术家</ion-title>
        </ion-toolbar>
      </ion-header>

      <div v-if="artists.length === 0" class="empty-state">
        <h2>还没有艺术家</h2>
        <p>请先到音源页添加并扫描音源。</p>
      </div>

      <div v-else class="list-grid tablet-content-limit">
        <ion-list>
        <ion-item v-for="artist in artists" :key="artist.name">
          <ion-label>
            <h2>{{ artist.name }}</h2>
            <p>{{ artist.songCount }} 首歌曲</p>
            <p>{{ artist.albumCount }} 张专辑</p>
          </ion-label>
        </ion-item>
      </ion-list>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { IonContent, IonHeader, IonItem, IonLabel, IonList, IonPage, IonTitle, IonToolbar, onIonViewWillEnter } from '@ionic/vue'
import { loadSongs } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { groupSongsByArtist } from '@/features/library/views'

const songs = ref<SongItem[]>([])
const artists = computed(() => groupSongsByArtist(songs.value))

const refreshSongs = () => {
  songs.value = loadSongs()
}

onMounted(refreshSongs)
onIonViewWillEnter(refreshSongs)
</script>

<style scoped>
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
@media (min-width: 768px) {
  .list-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 1px;
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }

  .list-grid > ion-list {
    display: contents;
  }

  .list-grid ion-item {
    width: 100%;
  }
}
</style>
