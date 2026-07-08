<template>
  <ion-page>
    <ion-content fullscreen class="player-page">
      <div class="immersive-shell">
        <div v-if="hasLyrics" class="amll-background">
          <BackgroundRender
            class="amll-background-render"
            :album="coverSrc || undefined"
            :album-is-video="false"
            :flow-speed="2"
            :has-lyric="hasLyrics"
            :renderer="meshGradientRenderer"
          />
        </div>
        <div class="fallback-background" />

        <header class="player-header">
          <ion-button fill="clear" color="light" aria-label="返回" @click="goBack">
            <ion-icon slot="icon-only" :icon="chevronDown" />
          </ion-button>
          <span>正在播放</span>
        </header>

        <section v-if="!playerState.currentSong" class="empty-state">
          <div class="placeholder-cover">♪</div>
          <h1>暂无播放歌曲</h1>
          <p>从歌曲列表选择一首音乐后，即可进入沉浸式播放。</p>
          <ion-button color="light" @click="goBack">返回</ion-button>
        </section>

        <div
          v-else
          class="panels"
          :style="{ transform: `translateX(-${activePanel * 50}%)` }"
          @touchstart="onTouchStart"
          @touchend="onTouchEnd"
        >
          <section class="panel info-panel" aria-label="播放控制页">
            <img v-if="coverSrc" class="cover" :src="coverSrc" alt="歌曲封面" />
            <div v-else class="cover placeholder-cover">♪</div>

            <div class="song-info">
              <h1>{{ playerState.currentSong.title }}</h1>
              <p>{{ subtitle }}</p>
              <small v-if="playerState.metadataStatus === 'scanning'">正在补充歌曲信息…</small>
              <small v-else-if="playerState.metadataStatus === 'failed'">歌曲信息补充失败，已使用当前信息播放。</small>
            </div>

            <div class="progress-area">
              <input
                class="progress-slider"
                type="range"
                min="0"
                :max="durationForSlider"
                step="0.1"
                :value="playerState.position"
                :disabled="!canSeek"
                aria-label="播放进度"
                @change="onSeek"
              />
              <div class="time-row">
                <span>{{ formatTime(playerState.position) }}</span>
                <span>{{ playerState.duration ? formatTime(playerState.duration) : '--:--' }}</span>
              </div>
            </div>

            <div class="controls">
              <ion-button fill="clear" color="light" shape="round" aria-label="上一曲" @click="onPrevious">
                <ion-icon slot="icon-only" :icon="previousIcon" />
              </ion-button>
              <ion-button class="play-toggle" fill="solid" color="light" shape="round" :disabled="playerState.status === 'loading'" aria-label="播放或暂停" @click="togglePlayback">
                <ion-icon slot="icon-only" :icon="isPlaying ? pause : play" />
              </ion-button>
              <ion-button fill="clear" color="light" shape="round" aria-label="下一曲" @click="onNext">
                <ion-icon slot="icon-only" :icon="nextIcon" />
              </ion-button>
            </div>

            <div class="mode-bar">
              <ion-button
                fill="clear"
                size="small"
                color="light"
                :aria-label="repeatModeLabel"
                @click="onToggleRepeat"
              >
                <ion-icon slot="start" :icon="repeatIcon" />
                {{ repeatModeLabel }}
              </ion-button>

              <ion-button
                fill="clear"
                size="small"
                color="light"
                :aria-label="shuffleModeLabel"
                @click="onToggleShuffle"
              >
                <ion-icon slot="start" :icon="shuffleIcon" />
                {{ shuffleModeLabel }}
              </ion-button>

              <ion-button
                fill="clear"
                size="small"
                color="light"
                aria-label="播放队列"
                @click="goToQueue"
              >
                <ion-icon slot="icon-only" :icon="listIcon" />
              </ion-button>
            </div>
          </section>

          <section class="panel lyric-panel" aria-label="歌词页">
            <template v-if="hasLyrics && activePanel === 1">
              <LyricPlayer class="lyric-player" :lyric-lines="lyricLines" :current-time="playerState.position * 1000" />
            </template>
            <div v-else class="lyric-empty">
              <h2>暂无歌词</h2>
              <p>当前歌曲没有内嵌歌词，也没有找到同目录同名 .lrc 文件。</p>
            </div>
          </section>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Capacitor } from '@capacitor/core'
import { IonButton, IonContent, IonIcon, IonPage } from '@ionic/vue'
import { chevronDown, listOutline, pause, play, playSkipBack, playSkipForward, repeat, repeatOutline, shuffle } from 'ionicons/icons'
import { useRouter } from 'vue-router'
import { BackgroundRender, LyricPlayer } from '@applemusic-like-lyrics/vue'
import { MeshGradientRenderer } from '@applemusic-like-lyrics/core'
import type { LyricLine } from '@applemusic-like-lyrics/core'
import { parseLrc } from '@applemusic-like-lyrics/lyric'
import '@applemusic-like-lyrics/core/style.css'
import { isPlaying, pausePlayback, playerState, playNextFromQueue, playPreviousFromQueue, queueState, resumePlayback, seekPlayback, setRepeatMode, toggleShuffle } from '@/features/player/controller'

const router = useRouter()
const activePanel = ref(0)
const touchStartX = ref<number | null>(null)
const meshGradientRenderer = MeshGradientRenderer

const repeatModeLabel = computed(() => queueState.repeatMode === 'one' ? '单曲循环' : '列表循环')
const repeatIcon = computed(() => queueState.repeatMode === 'one' ? repeat : repeatOutline)
const shuffleModeLabel = computed(() => queueState.shuffleEnabled ? '随机播放' : '顺序播放')
const shuffleIcon = computed(() => shuffle)
const listIcon = listOutline
const previousIcon = playSkipBack
const nextIcon = playSkipForward

const onToggleRepeat = () => {
  setRepeatMode(queueState.repeatMode === 'one' ? 'all' : 'one')
}

const onToggleShuffle = () => {
  toggleShuffle()
}

const goToQueue = () => {
  void router.push('/queue')
}

const onPrevious = () => {
  void playPreviousFromQueue()
}

const onNext = () => {
  void playNextFromQueue()
}

const subtitle = computed(() => {
  const song = playerState.currentSong
  return [song?.artist, song?.album].filter(Boolean).join(' · ') || '未知歌手'
})

const currentLyrics = computed(() => playerState.lyrics || playerState.currentSong?.lyrics || '')
const currentCoverUri = computed(() => playerState.coverUri || playerState.currentSong?.coverUri || '')
const coverSrc = computed(() => toDisplayableUri(currentCoverUri.value))

const lyricLines = computed<LyricLine[]>(() => {
  if (!currentLyrics.value) {
    return []
  }

  try {
    return parseLrc(normalizeLrc(currentLyrics.value))
  } catch {
    return []
  }
})

const hasLyrics = computed(() => lyricLines.value.length > 0)
const canSeek = computed(() => playerState.duration > 0)
const durationForSlider = computed(() => playerState.duration || 1)

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  void router.push('/tabs/songs')
}

const togglePlayback = async () => {
  if (isPlaying.value) {
    await pausePlayback()
    return
  }
  await resumePlayback()
}

const onSeek = async (event: Event) => {
  const target = event.target as HTMLInputElement
  await seekPlayback(Number(target.value))
}

const onTouchStart = (event: TouchEvent) => {
  touchStartX.value = event.changedTouches[0]?.clientX ?? null
}

const onTouchEnd = (event: TouchEvent) => {
  const startX = touchStartX.value
  const endX = event.changedTouches[0]?.clientX
  touchStartX.value = null
  if (startX === null || endX === undefined || Math.abs(startX - endX) < 40) {
    return
  }
  activePanel.value = endX < startX ? 1 : 0
}

const toDisplayableUri = (uri: string): string => {
  if (!uri) {
    return ''
  }
  return uri.startsWith('data:')
    ? ''
    : uri.startsWith('http://') || uri.startsWith('https://')
      ? uri
      : Capacitor.convertFileSrc(uri)
}

const normalizeLrc = (lyrics: string): string => {
  return lyrics.replace(/\[((?:\d+:)*\d+),(\d+)\]/g, '[$1.$2]')
}

const formatTime = (value: number): string => {
  const totalSeconds = Math.max(0, Math.floor(value))
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}
</script>

<style scoped>
.player-page {
  --background: #05070d;
  color: #fff;
}

.immersive-shell {
  position: relative;
  min-height: 100%;
  overflow: hidden;
}

.amll-background,
.fallback-background {
  position: absolute;
  inset: 0;
}

.amll-background {
  z-index: 0;
  overflow: hidden;
  opacity: 0.75;
}

.amll-background-render {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
}

.fallback-background {
  z-index: 0;
  background: radial-gradient(circle at top, rgba(122, 92, 255, 0.35), transparent 35%), linear-gradient(160deg, #15192a, #05070d 70%);
}

.amll-background + .fallback-background {
  opacity: 0;
}

.player-header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: calc(18px + var(--ion-safe-area-top, 0px)) 16px 10px;
  font-weight: 700;
}

.empty-state,
.panel {
  min-height: calc(100vh - 84px);
  padding: 24px;
}

.empty-state,
.info-panel,
.lyric-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.panels {
  position: relative;
  z-index: 1;
  display: flex;
  width: 200%;
  min-height: calc(100vh - 84px);
  overflow: visible;
  transition: transform 220ms ease;
}

.panel {
  width: 50%;
  min-width: 50%;
  flex: 0 0 50%;
}

.cover {
  width: min(72vw, 320px);
  aspect-ratio: 1;
  border-radius: 28px;
  object-fit: cover;
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.45);
}

.placeholder-cover {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0.06));
  color: rgba(255, 255, 255, 0.8);
  font-size: 72px;
}

.song-info {
  width: 100%;
  margin: 28px 0 18px;
}

.song-info h1 {
  margin: 0 0 8px;
  font-size: 28px;
}

.song-info p,
.song-info small,
.time-row {
  color: rgba(255, 255, 255, 0.72);
}

.progress-area {
  width: min(86vw, 420px);
}

.progress-slider {
  width: 100%;
}

.time-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.controls {
  display: flex;
  gap: 18px;
  margin-top: 24px;
}

.controls ion-button {
  width: 50px;
  height: 50px;
}

.controls .play-toggle {
  width: 64px;
  height: 64px;
}

.mode-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  margin-top: 16px;
}

.mode-bar ion-button {
  --color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
}

.lyric-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.lyric-player {
  display: block;
  width: 100%;
  height: 70vh;
  min-height: 420px;
}

.lyric-empty pre {
  max-width: 88vw;
  max-height: 48vh;
  overflow: auto;
  padding: 16px;
  border-radius: 16px;
  text-align: left;
  white-space: pre-wrap;
  background: rgba(255, 255, 255, 0.1);
}
</style>
