<template>
  <div class="queue-overlay">
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-button fill="clear" aria-label="返回" @click="goBack">
            <ion-icon slot="icon-only" :icon="chevronBack" />
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
            <ion-icon slot="icon-only" :icon="trash" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen>
      <div v-if="!queueState.hasItems" class="empty-state">
        <ion-icon class="empty-icon" :icon="musicalNotes" />
        <h2>队列为空</h2>
        <p>从歌曲列表中添加歌曲即可开始播放。</p>
      </div>

      <ion-list v-else>
        <ion-item-sliding v-for="(song, index) in queueState.items" :key="song.id">
          <ion-item
            :class="{ 'current-song': index === queueState.currentIndex }"
            button
            @click="onSelectSong(index)"
          >
            <ion-label>
              <h2>
                <span v-if="index === queueState.currentIndex" class="current-indicator">♪ </span>
                {{ song.title }}
              </h2>
              <p>{{ song.artist || '未知歌手' }}</p>
            </ion-label>
            <ion-note slot="end" class="queue-index">{{ index + 1 }}</ion-note>
          </ion-item>

          <ion-item-options side="end">
            <ion-item-option color="danger" expandable @click="onRemoveSong(song.id)">
              <ion-icon slot="icon-only" :icon="close" />
            </ion-item-option>
          </ion-item-options>
        </ion-item-sliding>
      </ion-list>
    </ion-content>
  </div>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonItemOption,
  IonItemOptions,
  IonItemSliding,
  IonLabel,
  IonList,
  IonNote,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { chevronBack, close, musicalNotes, trash } from 'ionicons/icons'
import {
  clearQueue,
  playSong,
  queueState,
  removeSongFromQueue,
  selectSongAtIndex,
} from '@/features/player/controller'
import { closeQueueOverlay } from '@/features/player/overlay'

const goBack = () => {
  closeQueueOverlay()
}

const onClearQueue = () => {
  clearQueue()
}

const onRemoveSong = (songId: string) => {
  removeSongFromQueue(songId)
}

const onSelectSong = async (index: number) => {
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
  background: var(--ion-background-color, #fff);
}

.queue-overlay ion-content {
  flex: 1;
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