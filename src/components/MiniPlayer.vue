<template>
  <div
    v-if="hasActiveSong || playerState.errorMessage"
    class="mini-player"
    role="button"
    tabindex="0"
    aria-label="打开沉浸式播放器"
    @click="openPlayerPage"
    @keyup.enter="openPlayerPage"
    @keyup.space="openPlayerPage"
  >
    <div class="track-info">
      <strong>{{ playerState.currentSong?.title || '播放失败' }}</strong>
      <span v-if="playerState.currentSong?.artist">{{ playerState.currentSong.artist }}</span>
      <span v-else-if="playerState.status === 'loading'">正在准备播放...</span>
      <span v-else-if="playerState.errorMessage" class="error-message">{{ playerState.errorMessage }}</span>
    </div>

    <div class="player-actions">
      <ion-button
        v-if="playerState.currentSong"
        fill="clear"
        size="small"
        :disabled="playerState.status === 'loading'"
        :aria-label="isPlaying ? '暂停播放' : '继续播放'"
        @click.stop="togglePlayback"
        @keyup.enter.stop
        @keyup.space.stop
      >
        <ion-icon slot="icon-only" :icon="isPlaying ? pause : play" />
      </ion-button>
      <ion-button
        fill="clear"
        size="small"
        color="medium"
        aria-label="停止播放"
        @click.stop="stopPlayback"
        @keyup.enter.stop
        @keyup.space.stop
      >
        <ion-icon slot="icon-only" :icon="close" />
      </ion-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { IonButton, IonIcon } from '@ionic/vue'
import { close, pause, play } from 'ionicons/icons'
import { useRouter } from 'vue-router'
import { hasActiveSong, isPlaying, pausePlayback, playerState, resumePlayback, stopPlayback } from '@/features/player/controller'

const router = useRouter()

const openPlayerPage = () => {
  void router.push('/player')
}

const togglePlayback = async () => {
  if (isPlaying.value) {
    await pausePlayback()
    return
  }

  await resumePlayback()
}
</script>

<style scoped>
.mini-player {
  position: fixed;
  cursor: pointer;
  left: 12px;
  right: 12px;
  bottom: calc(64px + var(--ion-safe-area-bottom, 0px));
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  color: var(--ion-text-color);
  background: #ffffff;
  background-clip: padding-box;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.18);
}

.track-info {
  min-width: 0;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.track-info strong,
.track-info span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.track-info span {
  color: var(--ion-color-medium);
  font-size: 13px;
}

.track-info .error-message {
  color: var(--ion-color-danger);
}

.player-actions {
  display: flex;
  align-items: center;
}

@media (prefers-color-scheme: dark) {
  .mini-player {
    border-color: rgba(255, 255, 255, 0.12);
    background: #1f1f1f;
  }
}
</style>
