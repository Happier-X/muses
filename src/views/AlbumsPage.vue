<template>
  <m-page>
    <template #title>专辑</template>

    <h-empty
      v-if="albums.length === 0"
      title="还没有专辑"
      description="请先到音源页添加并扫描音源。"
    />

    <div v-else class="album-grid tablet-content-limit">
      <article
        v-for="album in albums"
        :key="album.name"
        class="album-card"
      >
        <m-cover class="album-card__cover" :src="getAlbumCoverSrc(album.songs)" alt="" />
        <div class="album-card__info">
          <h2 class="album-card__name">{{ album.name }}</h2>
          <p class="album-card__count">{{ album.songCount }} 首歌曲</p>
          <p class="album-card__artists">{{ album.artistSummary }}</p>
        </div>
      </article>
    </div>
  </m-page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Capacitor } from '@capacitor/core'
import { onIonViewWillEnter } from '@ionic/vue'
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
.album-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--muses-space-lg);
  padding: var(--muses-space-lg);
}

.album-card {
  display: flex;
  flex-direction: column;
  gap: var(--muses-space-sm);
  min-width: 0;
}

/* MCover 会内联设置默认尺寸，卡片场景需显式覆盖为容器宽度。 */
.album-card > .album-card__cover {
  --m-cover-size: 100% !important;
  aspect-ratio: 1;
  flex: 0 0 auto;
}

.album-card__info {
  display: flex;
  flex-direction: column;
  gap: var(--muses-space-xs);
  min-width: 0;
}

.album-card__name {
  margin: 0;
  font-size: var(--muses-font-title);
  line-height: var(--muses-line-height-title);
  color: var(--muses-color-ink);
  /* 最多两行，超出省略，不撑破卡片 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.album-card__count,
.album-card__artists {
  margin: 0;
  font-size: var(--muses-font-body-sm);
  color: var(--muses-color-ink-muted);
  /* 单行省略 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (min-width: 768px) {
  .album-grid {
    /* 宽屏在内容宽度上限内按可用宽度自动增列 */
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
</style>
