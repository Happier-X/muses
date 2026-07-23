<template>
  <div class="queue-overlay">
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-button fill="clear" aria-label="返回" @click="goBack">
            <h-icon :icon="chevronBack" />
          </ion-button>
        </ion-buttons>
        <ion-title>播放队列</ion-title>
        <ion-buttons slot="end">
          <ion-button v-if="queueState.hasItems" fill="clear" color="danger" aria-label="清空队列" @click="onClearQueue">
            <h-icon :icon="trash" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen>
      <h-empty
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
            <ion-item
              button
              :detail="false"
              lines="none"
              class="queue-item"
              :class="{ 'is-playing': row.virtualRow.index === queueState.currentIndex }"
              :aria-current="row.virtualRow.index === queueState.currentIndex ? 'true' : undefined"
              @click="onSelectSong(row.virtualRow.index, $event)"
            >
              <ion-label>
                <h2>{{ row.song.title }}</h2>
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
                <h-icon :icon="close" />
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
  IonItem,
  IonLabel,
  IonNote,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { chevronBack, close, trash } from '@/icons'
import { HEmpty, HIcon } from '@/components/ui'
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

/* 当前队列项：替代已删除 HListRow 的 playing 背景。 */
:deep(.queue-item.is-playing) {
  --background: var(--muses-color-playing-bg);
  background: var(--muses-color-playing-bg);
}

@media (prefers-color-scheme: dark) {
  .queue-overlay {
    background: var(--muses-color-surface-dark);
  }
}
</style>
