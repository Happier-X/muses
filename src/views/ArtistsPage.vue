<template>
  <m-page>
    <template #title>艺术家</template>

    <m-empty-state
      v-if="artists.length === 0"
      title="还没有艺术家"
      description="请先到音源页添加并扫描音源。"
    />

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
  </m-page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { IonItem, IonLabel, IonList, onIonViewWillEnter } from '@ionic/vue'
import { MEmptyState, MPage } from '@/components/ui'
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
