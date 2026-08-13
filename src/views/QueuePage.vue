<template>
  <m-popup :opened="queueOverlayVisible">
    <div ref="popupPanelRef" class="queue-popup-panel queue-page__panel">
      <div class="queue-page__header">
        <h2 class="queue-page__title">播放队列</h2>
        <div class="queue-page__header-actions">
          <m-button
            v-if="queueState.hasItems"
            component="button"
            variant="clear"
            rounded
            class="queue-page__clear-btn"
            aria-label="清空队列"
            @click="onClearQueue"
          >
            <component :is="trash" aria-hidden="true" class="queue-page__icon" />
          </m-button>
          <m-button
            component="button"
            variant="clear"
            rounded
            class="queue-page__close-btn"
            aria-label="关闭队列"
            @click="goBack"
          >
            <component :is="close" aria-hidden="true" class="queue-page__icon" />
          </m-button>
        </div>
      </div>

      <div class="queue-page__body">
        <m-empty
          v-if="!queueState.hasItems"
          title="队列为空"
          description="从歌曲列表中添加歌曲即可开始播放。"
        />

        <div v-else ref="listParentRef" class="queue-page__list" role="list" aria-label="播放队列歌曲" @touchstart.stop @touchmove.stop @touchend.stop @touchcancel.stop>
          <m-list strong outline class="queue-page__list-root">
            <div class="queue-page__vlist" :style="{ height: `${totalSize}px` }">
              <div
                v-for="row in visibleRows"
                :key="row.song.id"
                :ref="measureVirtualRow"
                class="queue-page__virtual-row"
                role="listitem"
                :data-index="row.virtualRow.index"
                :style="{ transform: `translateY(${row.virtualRow.start}px)` }"
              >
                <m-list-item
                  :chevron="false"
                  link
                  :title="row.song.title"
                  :subtitle="row.song.artist || '未知歌手'"
                  title-class="queue-page__row-title"
                  subtitle-class="queue-page__row-subtitle"
                  class="queue-page__row"
                  :class="row.virtualRow.index === queueState.currentIndex ? 'queue-page__row--playing' : ''"
                  :aria-current="row.virtualRow.index === queueState.currentIndex ? 'true' : undefined"
                  role="button"
                  tabindex="0"
                  @click="onSelectSong(row.virtualRow.index)"
                >
                  <template #after>
                    <span class="queue-page__row-index">{{ row.virtualRow.index + 1 }}</span>
                    <m-button
                      component="button"
                      variant="clear"
                      rounded
                      class="queue-page__remove-btn"
                      :aria-label="`从队列删除 ${row.song.title}`"
                      @click.stop="onRemoveSong(row.song.id)"
                    >
                      <component :is="close" aria-hidden="true" class="queue-page__icon" />
                    </m-button>
                  </template>
                </m-list-item>
              </div>
            </div>
          </m-list>
        </div>
      </div>
    </div>
  </m-popup>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, type ComponentPublicInstance, watch } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { close, trash } from '@/icons'
import { MButton, MList, MListItem, MPopup, MEmpty } from '@/components/ui'
import {
  clearQueue,
  playSong,
  queueState,
  removeSongFromQueue,
  selectSongAtIndex,
} from '@/features/player/controller'
import { closeQueueOverlay, queueOverlayVisible } from '@/features/player/overlay'

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
  if (
    !listParentRef.value
    || queueState.currentIndex < 0
    || queueState.currentIndex >= queueState.items.length
  ) {
    return
  }
  rowVirtualizer.value.scrollToIndex(queueState.currentIndex, { align: 'center' })
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

const onSelectSong = async (index: number): Promise<void> => {
  const song = selectSongAtIndex(index)
  if (song) {
    await playSong(song)
  }
}
</script>

<style scoped lang="scss">
.queue-page {
  &__vlist { position: relative; width: 100%; }
  &__panel {
    flex-direction: column;
    overflow: hidden;
  }

  &__header {
    flex: none;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: var(--m-spacing-sub) var(--m-spacing) 8px;
  }

  &__title {
    margin: 0;
    font-size: 17px;
    line-height: 1.3;
    font-weight: 700;
    color: var(--m-text);
  }

  &__header-actions {
    display: flex;
    align-items: center;
    gap: 2px;
  }

  &__clear-btn,
  &__close-btn {
    width: 32px;
    height: 32px;
    padding: 0;
  }

  &__clear-btn {
    color: #ff3b30;

    &:active {
      background-color: rgba(255, 59, 48, 0.15);
    }
  }

  &__close-btn {
    color: var(--m-text);
  }

  &__icon {
    width: 16px;
    height: 16px;
  }

  &__body {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  &__list {
    height: 100%;
    overflow-y: auto;
    overscroll-behavior: contain;
    box-sizing: border-box;
    padding-bottom: var(--m-safe-area-bottom, 0px);
    overflow-anchor: none;
  }

  &__list-root {
    margin: 0;
  }

  &__virtual-row {
    position: absolute;
    inset-inline: 0;
    top: 0;
    box-sizing: border-box;
    min-height: var(--m-list-row-h);
  }

  &__row {
    height: 100%;
  }

  &__row-title {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__row-subtitle {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__row-index {
    flex: none;
    margin-right: 2px;
    margin-left: 4px;
    font-size: 12px;
    opacity: 0.6;
    color: var(--m-text-secondary);
  }

  &__remove-btn {
    flex: none;
    margin: 0;
    width: 32px;
    height: 32px;
    padding: 0;
    color: #ff3b30;

    &:active {
      background-color: rgba(255, 59, 48, 0.15);
    }
  }
}

/* 正在播放行高亮与 Salt 分割线 */
.queue-page :deep(.queue-page__row) {
  border-bottom: 1px solid var(--m-hairline);
}

.queue-page :deep(.queue-page__row--playing) {
  background-color: rgba(var(--m-primary-rgb), 0.1);
}
</style>