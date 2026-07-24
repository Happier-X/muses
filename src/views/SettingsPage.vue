<template>
  <ion-page>
    <h-nav-bar title="设置" :fixed="false" />
    <ion-content :fullscreen="false">
      <div class="tablet-content-limit">
        <ion-list>
          <ion-item lines="none">
            <ion-label>
              <h2>Muses</h2>
              <p>应用版本 {{ currentVersion }}</p>
            </ion-label>
          </ion-item>

          <ion-item lines="none">
            <ion-label>
              <h2>音量均衡</h2>
              <p>根据歌曲自带的 ReplayGain 等标签统一响度（含 +6 dB 听感补偿）。无标签不改变；过静曲无法超过系统满幅。若整体仍偏小可关闭本开关。</p>
            </ion-label>
            <h-switch
              slot="end"
              v-model="loudnessNormalizeEnabled"
              aria-label="音量均衡"
            />
          </ion-item>
        </ion-list>

        <div class="update-section">
          <h-button
            variant="primary"
            size="lg"
            :disabled="checking"
            @click="checkUpdate"
          >
            {{ checking ? '检查中...' : '检查更新' }}
          </h-button>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  IonContent,
  IonItem,
  IonLabel,
  IonList,
  IonPage,
  toastController,
} from '@ionic/vue'
import { HButton, HNavBar, HSwitch } from '@/components/ui'
import {
  isLoudnessNormalizeEnabled,
  setLoudnessNormalizeEnabled,
} from '@/features/player/controller'
import pkg from '../../package.json'

const present = async (opts: { message: string; duration: number }) => {
  const toast = await toastController.create(opts)
  await toast.present()
}

const currentVersion = pkg.version
const checking = ref(false)
const loudnessNormalizeEnabled = ref(isLoudnessNormalizeEnabled())

watch(loudnessNormalizeEnabled, (enabled) => {
  setLoudnessNormalizeEnabled(enabled)
})

const compareVersions = (a: string, b: string): number => {
  const partsA = a.split('.').map(Number)
  const partsB = b.split('.').map(Number)
  const len = Math.max(partsA.length, partsB.length)
  for (let i = 0; i < len; i++) {
    const va = partsA[i] ?? 0
    const vb = partsB[i] ?? 0
    if (va > vb) return 1
    if (va < vb) return -1
  }
  return 0
}

const checkUpdate = async () => {
  checking.value = true
  try {
    const res = await fetch('https://api.github.com/repos/Happier-X/muses/releases/latest')
    if (!res.ok) {
      if (res.status === 403) {
        present({ message: '检查更新失败，请稍后重试', duration: 2000 })
      } else {
        present({ message: '检查更新失败，请检查网络连接', duration: 2000 })
      }
      return
    }
    const data = await res.json()
    const tag: string = data.tag_name ?? ''
    const match = tag.match(/^v(\d+\.\d+\.\d+)$/)
    if (!match) {
      present({ message: '检查更新失败，版本格式异常', duration: 2000 })
      return
    }
    const latestVer = match[1]
    const cmp = compareVersions(latestVer, currentVersion)
    if (cmp <= 0) {
      present({ message: '已是最新版本', duration: 2000 })
    } else {
      present({ message: `发现新版本 v${latestVer}`, duration: 3000 })
      window.open(data.html_url, '_system')
    }
  } catch {
    present({ message: '检查更新失败，请检查网络连接', duration: 2000 })
  } finally {
    checking.value = false
  }
}
</script>

<style scoped>
.update-section {
  padding: var(--muses-space-lg);
}

@media (min-width: 768px) {
  .tablet-content-limit {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
</style>
