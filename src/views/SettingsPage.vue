<template>
  <div class="m-page settings-page">
    <div class="settings-page__navbar-wrap">
      <m-navbar right-class="settings-page__right">
        <template #title>设置</template>
      </m-navbar>
    </div>
    <div class="m-content settings-page__content">
      <m-block-title>关于</m-block-title>
      <m-list inset>
        <m-list-item
          title="Muses"
          :subtitle="`应用版本 ${currentVersion}`"
        >
          <template #media>
            <span class="settings-page__item-icon" aria-hidden="true">
              <Info :size="20" />
            </span>
          </template>
        </m-list-item>
        <m-list-item
          title="检查更新"
          :subtitle="checking ? '正在检查更新…' : undefined"
          link
          aria-label="检查更新"
          @click="checkUpdate"
        >
          <template #media>
            <span class="settings-page__item-icon" aria-hidden="true">
              <RefreshCw :size="20" />
            </span>
          </template>
        </m-list-item>
      </m-list>

      <m-block-title>音频</m-block-title>
      <m-list inset>
        <m-list-item
          title="音量均衡"
          subtitle="根据歌曲自带的 ReplayGain 等标签统一响度（含 +6 dB 听感补偿）。无标签不改变；过静曲无法超过系统满幅。若整体仍偏小可关闭本开关。"
        >
          <template #media>
            <span class="settings-page__item-icon" aria-hidden="true">
              <Volume2 :size="20" />
            </span>
          </template>
          <template #after>
            <m-toggle
              :checked="loudnessNormalizeEnabled"
              aria-label="音量均衡"
              @change="onLoudnessToggle"
            />
          </template>
        </m-list-item>
      </m-list>
    </div>

    <m-toast :opened="toast.visible" position="center">
      {{ toast.message }}
    </m-toast>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Info, RefreshCw, Volume2 } from '@lucide/vue'
import { MBlockTitle, MList, MListItem, MNavbar, MToast, MToggle } from '@/components/ui'
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

<style scoped lang="scss">
.settings-page {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;

  &__navbar-wrap {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    z-index: 20;
  }

  &__right {
    height: 32px;
  }

  &__content {
    padding-top: calc(var(--m-navbar-pt, 16px) + 44px);
    padding-bottom: var(--m-content-pb);

    @media (min-width: 768px) {
      padding-bottom: var(--m-content-pb-md);
    }
  }

  :deep(.m-list-item) {
    min-height: var(--m-list-row-h);
  }

  /* 列表项左侧图标容器：统一规格圆角方形 + 主色调浅底（rgba 主色 RGB，明暗主题自动跟随） */
  &__item-icon {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    margin-right: var(--m-spacing-sub);
    border-radius: var(--m-radius-sm);
    background-color: rgba(var(--m-primary-rgb), 0.12);
    color: var(--m-primary);
  }

  /* 分组标题层次微调：缩小字号拉开与行标题（17px）的层级差，仅本页生效 */
  :deep(.m-block-title--default) {
    padding-top: 16px;
    font-size: var(--m-font-size-sm);
    font-weight: 600;
    line-height: 1.4;
    letter-spacing: 0.02em;
    color: var(--m-text-2);
  }
}
</style>