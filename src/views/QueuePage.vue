<template>
  <div class="queue-overlay">
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-button fill="clear" aria-label="返回" @click="goBack">
            <ion-icon slot="icon-only" :icon="chevronBack" aria-hidden="true" />
          </ion-button>
        </ion-buttons>
        <ion-title>播放队列</ion-title>
        <ion-buttons slot="end">
          <ion-button
            v-if="queueState.hasItems"
            fill="clear"
            color="danger"
            aria-label="清空队列"
            @click="onClearQueue"
          >
            <ion-icon slot="icon-only" :icon="trash" aria-hidden="true" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen>
      <div v-if="!queueState.hasItems" class="empty-state">
        <ion-icon class="empty-icon" :icon="musicalNotes" aria-hidden="true" />
        <h2>队列为空</h2>
        <p>从歌曲列表中添加歌曲即可开始播放。</p>
      </div>

      <div v-else ref="listParentRef" class="queue-list" role="list" aria-label="播放队列歌曲">
        <div class="queue-list-spacer" :style="{ height: `${totalSize}px` }">
          <div
            v-for="row in visibleRows"
            :key="row.song.id"
            :ref="measureVirtualRow"
            class="queue-row"
            role="listitem"
            :data-index="row.virtualRow.index"
            :style="{ transform: `translateY(${row.virtualRow.start}px)` }"
          >
            <ion-item
              :class="{ 'current-song': row.virtualRow.index === queueState.currentIndex }"
              :aria-current="row.virtualRow.index === queueState.currentIndex ? 'true' : undefined"
              button
              :detail="false"
              @click="onSelectSong(row.virtualRow.index, $event)"
            >
              <ion-label>
                <h2>
                  <span v-if="row.virtualRow.index === queueState.currentIndex" class="current-indicator">♪ </span>
                  {{ row.song.title }}
                </h2>
                <p>{{ row.song.artist || '未知歌手' }}</p>
              </ion-label>
              <ion-note slot="end" class="queue-index">{{ row.virtualRow.index + 1 }}</ion-note>
              <ion-button
                slot="end"
                fill="clear"
                color="danger"
                class="remove-button"
                :aria-label="`从队列删除 ${row.song.title}`"
                @click.stop="onRemoveSong(row.song.id)"
              >
                <ion-icon slot="icon-only" :icon="close" aria-hidden="true" />
              </ion-button>
            </ion-item>
          </div>
        </div>
      </div>
    </ion-content>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, type ComponentPublicInstance, watch } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonNote,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { chevronBack, close, musicalNotes, trash } from '@/icons/ion-lucide'
import {
  clearQueue,
  playSong,
  queueState,
  removeSongFromQueue,
  selectSongAtIndex,
} from '@/features/player/controller'
import { closeQueueOverlay } from '@/features/player/overlay'

const listParentRef = ref<HTMLElement | null>(null)

const rowVirtualizer = useVirtualizer(
  computed(() => ({
    count: queueState.items.length,
    getScrollElement: () => listParentRef.value,
    estimateSize: () => 56,
    overscan: 8,
  })),
)

const visibleRows = computed(() => rowVirtualizer.value.getVirtualItems().flatMap((virtualRow) => {
  const song = queueState.items[virtualRow.index]
  return song ? [{ virtualRow, song }] : []
}))
const totalSize = computed(() => rowVirtualizer.value.getTotalSize())

const measureVirtualRow = (element: Element | ComponentPublicInstance | null): void => {
  rowVirtualizer.value.measureElement(element instanceof HTMLElement ? element : null)
}

const scrollToCurrent = async (): Promise<void> => {
  await nextTick()
  if (queueState.currentIndex >= 0 && queueState.currentIndex < queueState.items.length) {
    rowVirtualizer.value.scrollToIndex(queueState.currentIndex, { align: 'center' })
  }
}

watch(
  [listParentRef, () => queueState.items.length, () => queueState.currentIndex],
  () => void scrollToCurrent(),
  { flush: 'post', immediate: true },
)

const goBack = () => {
  closeQueueOverlay()
}

const onClearQueue = () => {
  clearQueue()
}

const onRemoveSong = (songId: string) => {
  removeSongFromQueue(songId)
}

const onSelectSong = async (index: number, event: MouseEvent): Promise<void> => {
  if (event.composedPath().some((target) => target instanceof Element && target.classList.contains('remove-button'))) {
    return
  }
  const song = selectSongAtIndex(index)
  if (song) {
    await playSong(song)
  }
}
</script>

<style scoped>
.queue-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  overscroll-behavior: none;
  background: var(--ion-background-color, #fff);
}

.queue-overlay ion-content {
  flex: 1;
  min-height: 0;
}

.queue-list {
  height: 100%;
  overflow: auto;
  overscroll-behavior: contain;
  box-sizing: border-box;
  padding-bottom: calc(96px + var(--ion-safe-area-bottom, 0px));
}

.queue-list-spacer {
  position: relative;
  width: 100%;
}

.queue-row {
  position: absolute;
  inset-inline: 0;
  top: 0;
  box-sizing: border-box;
  min-height: 56px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 48px 24px;
  color: var(--ion-color-medium);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.current-song {
  --background: var(--ion-color-light);
}

.current-indicator {
  color: var(--ion-color-primary);
}

.queue-index {
  font-size: 12px;
  opacity: 0.6;
}
</style>