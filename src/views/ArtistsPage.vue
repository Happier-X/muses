<template>
  <m-page>
    <template #title>艺术家</template>

    <h-empty
      v-if="artists.length === 0"
      title="还没有艺术家"
      description="请先到音源页添加并扫描音源。"
    />

    <div v-else class="artist-grid tablet-content-limit">
      <article
        v-for="artist in artists"
        :key="artist.name"
        class="artist-card"
      >
        <m-cover
          class="artist-card__avatar"
          :src="getArtistCoverSrc(artist.songs)"
          alt=""
        />
        <div class="artist-card__info">
          <h2 class="artist-card__name">{{ artist.name }}</h2>
          <p class="artist-card__count">{{ artist.songCount }} 首歌曲</p>
          <p class="artist-card__count">{{ artist.albumCount }} 张专辑</p>
        </div>
      </article>
    </div>
  </m-page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Capacitor } from '@capacitor/core'
import { onIonViewWillEnter } from '@ionic/vue'
import { HEmpty, MCover, MPage } from '@/components/ui'
import { loadSongs } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { groupSongsByArtist } from '@/features/library/views'

const songs = ref<SongItem[]>([])
const artists = computed(() => groupSongsByArtist(songs.value))

const refreshSongs = () => {
  songs.value = loadSongs()
}

const getArtistCoverSrc = (artistSongs: SongItem[]): string => {
  const coverUri = artistSongs
    .map((song) => song.coverUri?.trim())
    .find((uri): uri is string => {
      if (!uri) return false
      const normalized = uri.toLowerCase()
      return !normalized.startsWith('data:')
        && !normalized.startsWith('blob:')
        && !normalized.includes(';base64,')
    })

  if (!coverUri) return ''
  const normalizedUri = coverUri.toLowerCase()
  return normalizedUri.startsWith('http://') || normalizedUri.startsWith('https://')
    ? coverUri
    : Capacitor.convertFileSrc(coverUri)
}

onMounted(refreshSongs)
onIonViewWillEnter(refreshSongs)
</script>

<style scoped>
.artist-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--muses-space-lg);
  padding: var(--muses-space-lg);
}

.artist-card {
  display: flex;
  flex-direction: column;
  gap: var(--muses-space-sm);
  min-width: 0;
}

.artist-card > .artist-card__avatar {
  --m-cover-size: 100% !important;
  height: auto;
  aspect-ratio: 1;
  flex: 0 0 auto;
  border-radius: 50%;
}

.artist-card__info {
  display: flex;
  flex-direction: column;
  gap: var(--muses-space-xs);
  min-width: 0;
  text-align: center;
}

.artist-card__name {
  margin: 0;
  font-size: var(--muses-font-title);
  line-height: var(--muses-line-height-title);
  color: var(--muses-color-ink);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.artist-card__count {
  margin: 0;
  font-size: var(--muses-font-body-sm);
  color: var(--muses-color-ink-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (min-width: 768px) {
  .artist-grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
</style>
