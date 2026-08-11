<template>
  <k-page class="k-page m-page flex flex-col overflow-hidden !h-auto !bottom-0">
    <k-navbar rightClass="!h-8">
      <template #title>设置</template>
    </k-navbar>
    <div class="m-content pb-40">
      <div class="">
        <k-block-title>关于</k-block-title>
        <k-list inset>
          <k-list-item
            title="Muses"
            :subtitle="`应用版本 ${currentVersion}`"
          />
          <k-list-item
            title="检查更新"
            :subtitle="checking ? '正在检查更新…' : undefined"
            link
            aria-label="检查更新"
            @click="checkUpdate"
          />
        </k-list>

        <k-block-title>音频</k-block-title>
        <k-list inset>
          <k-list-item
            title="音量均衡"
            subtitle="根据歌曲自带的 ReplayGain 等标签统一响度（含 +6 dB 听感补偿）。无标签不改变；过静曲无法超过系统满幅。若整体仍偏小可关闭本开关。"
          >
            <template #after>
              <k-toggle
                :checked="loudnessNormalizeEnabled"
                aria-label="音量均衡"
                @change="onLoudnessToggle"
              />
            </template>
          </k-list-item>
        </k-list>
      </div>
    </div>

    <k-toast :opened="toast.visible" position="center">
      {{ toast.message }}
    </k-toast>
  </k-page>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { kBlockTitle, kList, kListItem, kNavbar, kPage, kToast, kToggle } from '@/components/ui'
import {
  isLoudnessNormalizeEnabled,
  setLoudnessNormalizeEnabled,
} from '@/features/player/controller'
import pkg from '../../package.json'

const currentVersion = pkg.version
const checking = ref(false)
const loudnessNormalizeEnabled = ref(isLoudnessNormalizeEnabled())

const toast = ref<{
  visible: boolean
  message: string
  variant: 'default' | 'success' | 'warning' | 'danger'
  duration: number
}>({
  visible: false,
  message: '',
  variant: 'default',
  duration: 2000,
})

let toastTimer: number | undefined

const showToast = (
  message: string,
  variant: 'default' | 'success' | 'warning' | 'danger' = 'default',
  duration = 2000,
): void => {
  toast.value = { visible: true, message, variant, duration }
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.value.visible = false
  }, duration)
}

const onLoudnessToggle = (e: Event): void => {
  loudnessNormalizeEnabled.value = (e.target as HTMLInputElement).checked
}

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
  if (checking.value) return
  checking.value = true
  try {
    const res = await fetch('https://api.github.com/repos/Happier-X/muses/releases/latest')
    if (!res.ok) {
      if (res.status === 403) {
        showToast('检查更新失败，请稍后重试', 'danger')
      } else {
        showToast('检查更新失败，请检查网络连接', 'danger')
      }
      return
    }
    const data = await res.json()
    const tag: string = data.tag_name ?? ''
    const match = tag.match(/^v(\d+\.\d+\.\d+)$/)
    if (!match) {
      showToast('检查更新失败，版本格式异常', 'danger')
      return
    }
    const latestVer = match[1]
    const cmp = compareVersions(latestVer, currentVersion)
    if (cmp <= 0) {
      showToast('已是最新版本', 'success')
    } else {
      showToast(`发现新版本 v${latestVer}`, 'default', 3000)
      window.open(data.html_url, '_system')
    }
  } catch {
    showToast('检查更新失败，请检查网络连接', 'danger')
  } finally {
    checking.value = false
  }
}
</script>

