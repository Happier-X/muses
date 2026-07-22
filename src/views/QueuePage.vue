<template>
  <div class="queue-overlay">
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <m-icon-button
            :icon="chevronBack"
            ariaLabel="返回"
            @click="goBack"
          />
        </ion-buttons>
        <ion-title>播放队列</ion-title>
        <ion-buttons slot="end">
          <m-icon-button
            v-if="queueState.hasItems"
            :icon="trash"
            ariaLabel="清空队列"
            color="danger"
            @click="onClearQueue"
          />
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen>
      <m-empty-state
        v-if="!queueState.hasItems"
        title="队列为空"
        description="从歌曲列表中添加歌曲即可开始播放。"
      />

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
            <m-list-row
              :title="row.song.title"
              :subtitle="row.song.artist || '未知歌手'"
              :cover="false"
              :playing="row.virtualRow.index === queueState.currentIndex"
              :show-playing-indicator="true"
              class="queue-item"
              @click="onSelectSong(row.virtualRow.index, $event)"
            >
              <template #end>
                <ion-note class="queue-index">{{ row.virtualRow.index + 1 }}</ion-note>
                <m-icon-button
                  class="remove-button"
                  :icon="close"
                  :ariaLabel="`从队列删除 ${row.song.title}`"
                  color="danger"
                  stop-propagation
                  @click="onRemoveSong(row.song.id)"
                />
              </template>
            </m-list-row>
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
  IonButtons,
  IonContent,
  IonHeader,
  IonNote,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { chevronBack, close, trash } from '@/icons/ion-lucide'
import { MEmptyState, MIconButton, MListRow } from '@/components/ui'
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
    estimateSize: () => 72,
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

const onSelectSong = async (index: number, event: MouseEvent | KeyboardEvent): Promise<void> => {
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
  /* 队列叠在沉浸播放页之上（player=1100） */
  z-index: 1200;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  overscroll-behavior: none;
  background: var(--muses-color-surface);
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
  padding-bottom: calc(var(--muses-mini-player-height) + var(--muses-space-xl) + var(--ion-safe-area-bottom, 0px));
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
  min-height: var(--muses-song-row-height);
}

.queue-index {
  font-size: var(--muses-font-label);
  opacity: 0.6;
  margin-inline-end: var(--muses-space-xs);
}

@media (prefers-color-scheme: dark) {
  .queue-overlay {
    background: var(--muses-color-surface-dark);
  }
}
</style>
