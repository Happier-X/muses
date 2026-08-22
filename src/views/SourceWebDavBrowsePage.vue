<template>
  <div class="m-page source-webdav-browse-page">
    <div class="source-webdav-browse-page__navbar-wrap">
      <m-navbar>
        <template #left>
          <m-navbar-back-link aria-label="返回" @click="goBack" />
        </template>
        <template #title>{{ sessionMode === 'single' ? '选择目录' : '选择文件夹' }}</template>
      </m-navbar>
    </div>

    <div class="m-content source-webdav-browse-page__content">
      <!-- 目录浏览器占满剩余高度，内部自滚动 -->
      <div v-if="hasSession" class="source-webdav-browse-page__browser">
        <WebDavDirectoryBrowser
          ref="browserRef"
          :mode="sessionMode"
          :connection="sessionConnection"
          :initial-path="sessionInitialPath"
          @confirm="onBrowserConfirm"
          @error="showToast($event, 'danger')"
        />
      </div>
    </div>

    <m-toast :opened="toast.visible" position="center">
      {{ toast.message }}
    </m-toast>
  </div>
</template>

<script setup lang="ts">
/**
 * WebDAV 目录浏览独立页（add/edit 共用）：
 * 会话由表单页经 webdavBrowseSession 传入（仅内存，不进 URL）。
 * - multiple：勾选多个目录确认 → 结果写入会话服务带回，表单页批量建源
 * - single：单选确认 → 结果写入会话服务带回，表单页回填目录字段
 * 刷新 / 深链直达时无会话，toast 后兜底回表单页。
 */
import { nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  MNavbar, MNavbarBackLink, MToast,
} from '@/components/ui'
import WebDavDirectoryBrowser from '@/components/webdav/WebDavDirectoryBrowser.vue'
import {
  setWebDavBrowseResult,
  takeWebDavBrowseSession,
} from '@/features/sources/webdavBrowseSession'
import type { WebDavConnectionInput } from '@/features/sources/types'

const router = useRouter()

const browserRef = ref<InstanceType<typeof WebDavDirectoryBrowser> | null>(null)
/** 是否拿到有效会话（决定是否渲染浏览器） */
const hasSession = ref(false)
const sessionMode = ref<'single' | 'multiple'>('multiple')
const sessionConnection = ref<WebDavConnectionInput>({ serverUrl: '', username: '', password: '' })
const sessionInitialPath = ref('/')

// ===== Toast（文案风格与表单页一致）=====

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

// ===== 导航 =====

/** 返回表单页；back() 无历史时无操作，超时后若仍在浏览页则 replace 兜底回表单页 */
const leaveBrowsePage = (): void => {
  router.back()
  window.setTimeout(() => {
    if (router.currentRoute.value.path === '/tabs/sources/webdav/browse') {
      void router.replace('/tabs/sources/webdav')
    }
  }, 100)
}

/** 未确认直接返回：不写入结果即离开，表单页零副作用 */
const goBack = (): void => {
  activeSession = null
  leaveBrowsePage()
}

// ===== 浏览器事件 =====

/** 确认选择：结果写入会话服务后返回；表单页重新挂载时消费（回填 / 建源） */
const onBrowserConfirm = ({ paths }: { paths: string[] }): void => {
  if (!activeSession || paths.length === 0) {
    return
  }
  setWebDavBrowseResult({
    connection: activeSession.connection,
    mode: activeSession.mode,
    paths,
  })
  activeSession = null
  leaveBrowsePage()
}

let activeSession: ReturnType<typeof takeWebDavBrowseSession> = null

// ===== 初始化 =====

onMounted(async () => {
  const session = takeWebDavBrowseSession()
  if (!session) {
    // 刷新 / 深链直达拿不到内存会话：提示后兜底回表单页
    showToast('浏览会话已失效，请重新连接。', 'danger')
    leaveBrowsePage()
    return
  }

  activeSession = session
  sessionMode.value = session.mode
  sessionConnection.value = session.connection
  sessionInitialPath.value = session.initialPath
  hasSession.value = true

  // 等 props 更新到子组件后再打开浏览器加载首屏
  await nextTick()
  await browserRef.value?.open()
})
</script>

<style scoped lang="scss">
.source-webdav-browse-page {
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

  &__content {
    display: flex;
    flex-direction: column;
    overflow: hidden;
    box-sizing: border-box;
    height: 100%;
    padding-top: calc(var(--m-navbar-pt, 16px) + 44px + 8px);
    padding-left: var(--m-spacing-sub);
    padding-right: var(--m-spacing-sub);
    padding-bottom: var(--m-content-pb);

    @media (min-width: 768px) {
      padding-bottom: var(--m-content-pb-md);
    }
  }

  /* 浏览器占满剩余高度并自滚动，保证路径导航与目录列表完整可见 */
  &__browser {
    flex: 1 1 0;
    min-height: 0;
    overflow-y: auto;
    box-sizing: border-box;
  }
}
</style>
