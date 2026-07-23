<template>
  <m-page>
    <template #title>专辑</template>

    <h-empty
      v-if="albums.length === 0"
      title="还没有专辑"
      description="请先到音源页添加并扫描音源。"
    />

    <div v-else class="list-grid tablet-content-limit">
      <ion-list>
        <ion-item v-for="album in albums" :key="album.name">
          <m-cover slot="start" :src="getAlbumCoverSrc(album.songs)" alt="" />
          <ion-label>
            <h2>{{ album.name }}</h2>
            <p>{{ album.songCount }} 首歌曲</p>
            <p>{{ album.artistSummary }}</p>
          </ion-label>
        </ion-item>
      </ion-list>
    </div>
  </m-page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Capacitor } from '@capacitor/core'
import { IonItem, IonLabel, IonList, onIonViewWillEnter } from '@ionic/vue'
import { MCover, HEmpty, MPage } from '@/components/ui'
import { loadSongs } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { groupSongsByAlbum } from '@/features/library/views'

const songs = ref<SongItem[]>([])
const albums = computed(() => groupSongsByAlbum(songs.value))

const refreshSongs = () => {
  songs.value = loadSongs()
}

const getAlbumCoverSrc = (albumSongs: SongItem[]): string => {
  const coverUri = albumSongs.find((song) => song.coverUri)?.coverUri
  if (!coverUri) return ''
  const normalizedUri = coverUri.trim().toLowerCase()
  if (normalizedUri.startsWith('data:') || normalizedUri.startsWith('blob:') || normalizedUri.includes(';base64,')) return ''
  return normalizedUri.startsWith('http://') || normalizedUri.startsWith('https://')
    ? coverUri
    : Capacitor.convertFileSrc(coverUri)
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
