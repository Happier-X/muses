<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title class="source-title">音源</ion-title>
        <ion-buttons slot="end">
          <ion-button aria-label="添加音源" @click="isAddActionSheetOpen = true">
            <ion-icon slot="icon-only" :icon="add" aria-hidden="true" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content :fullscreen="true">
      <div v-if="sources.length === 0" class="empty-state">
        <h2>还没有音源</h2>
        <p>点击右上角加号添加本地文件夹或 WebDAV 文件夹。</p>
      </div>

      <div v-else ref="listParentRef" class="source-list">
        <div class="source-list-spacer" :style="{ height: `${totalSize}px` }">
          <ion-card
            v-for="virtualRow in virtualRows"
            :key="sources[virtualRow.index].id"
            class="source-card"
            :style="{
              transform: `translateY(${virtualRow.start}px)`,
            }"
          >
            <ion-card-header>
              <ion-card-title>{{ sources[virtualRow.index].name }}</ion-card-title>
              <ion-card-subtitle>{{ getSourceSubtitle(sources[virtualRow.index]) }}</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content>
              <p class="source-path">{{ sources[virtualRow.index].path }}</p>
            </ion-card-content>
          </ion-card>
        </div>
      </div>

      <ion-action-sheet
        :is-open="isAddActionSheetOpen"
        header="添加音源"
        :buttons="addSourceButtons"
        @didDismiss="isAddActionSheetOpen = false"
      />

      <ion-modal :is-open="isWebDavModalOpen" @didDismiss="closeWebDavModal">
        <ion-header>
          <ion-toolbar>
            <ion-title>添加 WebDAV</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="closeWebDavModal">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="ion-padding">
          <form class="webdav-form" @submit.prevent="connectWebDav">
            <ion-list inset>
              <ion-item>
                <ion-input
                  v-model="webDavForm.serverUrl"
                  label="服务器地址"
                  label-placement="stacked"
                  placeholder="https://example.com/dav"
                  type="url"
                  required
                />
              </ion-item>
              <ion-item>
                <ion-input
                  v-model="webDavForm.username"
                  label="用户名"
                  label-placement="stacked"
                  autocomplete="username"
                  required
                />
              </ion-item>
              <ion-item>
                <ion-input
                  v-model="webDavForm.password"
                  label="密码"
                  label-placement="stacked"
                  autocomplete="current-password"
                  type="password"
                  required
                />
              </ion-item>
            </ion-list>

            <ion-button expand="block" type="submit" :disabled="isWebDavLoading">
              {{ isWebDavConnected ? '重新连接' : '连接并浏览' }}
            </ion-button>
          </form>

          <ion-text v-if="errorMessage" color="danger">
            <p class="message-text">{{ errorMessage }}</p>
          </ion-text>
          <ion-text v-if="successMessage" color="success">
            <p class="message-text">{{ successMessage }}</p>
          </ion-text>

          <section v-if="isWebDavConnected" class="webdav-browser">
            <div class="browser-header">
              <ion-button fill="clear" size="small" :disabled="!parentWebDavPath || isWebDavLoading" @click="goToParentDirectory">
                返回上级
              </ion-button>
              <span class="current-path">{{ currentWebDavPath }}</span>
            </div>

            <ion-list v-if="webDavDirectories.length > 0" inset>
              <ion-item v-for="directory in webDavDirectories" :key="directory.path">
                <ion-checkbox
                  slot="start"
                  :checked="selectedWebDavPaths.has(directory.path)"
                  @ionChange="toggleWebDavSelection(directory.path)"
                />
                <ion-label @click="openWebDavDirectory(directory.path)">
                  <h2>{{ directory.basename }}</h2>
                  <p>{{ directory.path }}</p>
                </ion-label>
                <ion-button fill="clear" slot="end" @click="openWebDavDirectory(directory.path)">
                  进入
                </ion-button>
              </ion-item>
            </ion-list>

            <p v-else class="empty-directory">当前目录没有可添加的子文件夹。</p>

            <ion-button
              expand="block"
              :disabled="selectedWebDavPaths.size === 0 || isWebDavLoading"
              @click="addSelectedWebDavSources"
            >
              添加选中的 {{ selectedWebDavPaths.size }} 个文件夹
            </ion-button>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { FilePicker } from '@capawesome/capacitor-file-picker'
import {
  IonActionSheet,
  IonButton,
  IonButtons,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardSubtitle,
  IonCardTitle,
  IonCheckbox,
  IonContent,
  IonHeader,
  IonIcon,
  IonInput,
  IonItem,
  IonLabel,
  IonList,
  IonModal,
  IonPage,
  IonText,
  IonTitle,
  IonToolbar,
  type ActionSheetButton,
} from '@ionic/vue'
import { add } from 'ionicons/icons'
import { createSourceId, getWebDavPasswordKey, loadSources, saveSources, saveWebDavPassword } from '@/features/sources/storage'
import type { SourceItem, WebDavConnectionInput, WebDavDirectoryItem } from '@/features/sources/types'
import { getParentWebDavPath, getWebDavDisplayName, listWebDavDirectories, normalizeWebDavPath } from '@/features/sources/webdav'

const sources = ref<SourceItem[]>(loadSources())
const listParentRef = ref<HTMLElement | null>(null)
const isAddActionSheetOpen = ref(false)
const isWebDavModalOpen = ref(false)
const isWebDavLoading = ref(false)
const isWebDavConnected = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const currentWebDavPath = ref('/')
const webDavDirectories = ref<WebDavDirectoryItem[]>([])
const selectedWebDavPaths = ref(new Set<string>())
const webDavForm = ref<WebDavConnectionInput>({
  serverUrl: '',
  username: '',
  password: '',
})

const rowVirtualizer = useVirtualizer(
  computed(() => ({
    count: sources.value.length,
    getScrollElement: () => listParentRef.value,
    estimateSize: () => 116,
    overscan: 6,
  })),
)

const virtualRows = computed(() => rowVirtualizer.value.getVirtualItems())
const totalSize = computed(() => rowVirtualizer.value.getTotalSize())
const parentWebDavPath = computed(() => getParentWebDavPath(currentWebDavPath.value))

const persistSources = (): void => {
  saveSources(sources.value)
}

const showError = (message: string): void => {
  errorMessage.value = message
  successMessage.value = ''
}

const showSuccess = (message: string): void => {
  successMessage.value = message
  errorMessage.value = ''
}

const getSourceSubtitle = (source: SourceItem): string => {
  if (source.type === 'local') {
    return '本地文件夹'
  }

  return `WebDAV · ${source.username}@${source.serverUrl}`
}

const getLocalSourceName = (path: string): string => {
  return path.split(/[\\/]/).filter(Boolean).at(-1) || path
}

const addLocalSource = async (): Promise<void> => {
  try {
    const result = await FilePicker.pickDirectory()
    const id = createSourceId()
    const source: SourceItem = {
      id,
      type: 'local',
      name: getLocalSourceName(result.path),
      path: result.path,
      createdAt: new Date().toISOString(),
    }

    sources.value = [source, ...sources.value]
    persistSources()
    showSuccess('已添加本地文件夹。')
  } catch (error) {
    showError(error instanceof Error ? error.message : '选择本地文件夹失败。')
  }
}

const openWebDavModal = (): void => {
  isWebDavModalOpen.value = true
  errorMessage.value = ''
  successMessage.value = ''
}

const closeWebDavModal = (): void => {
  isWebDavModalOpen.value = false
  isWebDavConnected.value = false
  isWebDavLoading.value = false
  currentWebDavPath.value = '/'
  webDavDirectories.value = []
  selectedWebDavPaths.value = new Set<string>()
  webDavForm.value = {
    serverUrl: '',
    username: '',
    password: '',
  }
}

const loadWebDavDirectories = async (path: string): Promise<void> => {
  isWebDavLoading.value = true
  try {
    const normalizedPath = normalizeWebDavPath(path)
    webDavDirectories.value = await listWebDavDirectories(webDavForm.value, normalizedPath)
    currentWebDavPath.value = normalizedPath
    isWebDavConnected.value = true
    errorMessage.value = ''
  } catch (error) {
    showError(error instanceof Error ? error.message : '读取 WebDAV 目录失败。')
  } finally {
    isWebDavLoading.value = false
  }
}

const connectWebDav = async (): Promise<void> => {
  if (!webDavForm.value.serverUrl || !webDavForm.value.username || !webDavForm.value.password) {
    showError('请完整填写 WebDAV 连接信息。')
    return
  }

  selectedWebDavPaths.value = new Set<string>()
  await loadWebDavDirectories('/')
}

const openWebDavDirectory = async (path: string): Promise<void> => {
  await loadWebDavDirectories(path)
}

const goToParentDirectory = async (): Promise<void> => {
  if (!parentWebDavPath.value) {
    return
  }

  await loadWebDavDirectories(parentWebDavPath.value)
}

const toggleWebDavSelection = (path: string): void => {
  const nextSelectedPaths = new Set(selectedWebDavPaths.value)
  if (nextSelectedPaths.has(path)) {
    nextSelectedPaths.delete(path)
  } else {
    nextSelectedPaths.add(path)
  }

  selectedWebDavPaths.value = nextSelectedPaths
}

const addSelectedWebDavSources = async (): Promise<void> => {
  if (selectedWebDavPaths.value.size === 0) {
    return
  }

  isWebDavLoading.value = true
  try {
    const createdAt = new Date().toISOString()
    const newSources: SourceItem[] = []

    for (const path of selectedWebDavPaths.value) {
      const id = createSourceId()
      const credentialKey = getWebDavPasswordKey(id)
      await saveWebDavPassword(credentialKey, webDavForm.value.password)
      newSources.push({
        id,
        type: 'webdav',
        name: getWebDavDisplayName(path),
        serverUrl: webDavForm.value.serverUrl,
        username: webDavForm.value.username,
        path,
        credentialKey,
        createdAt,
      })
    }

    sources.value = [...newSources, ...sources.value]
    persistSources()
    showSuccess(`已添加 ${newSources.length} 个 WebDAV 文件夹。`)
    closeWebDavModal()
  } catch (error) {
    showError(error instanceof Error ? error.message : '保存 WebDAV 音源失败。')
  } finally {
    isWebDavLoading.value = false
  }
}

const addSourceButtons: ActionSheetButton[] = [
  {
    text: '添加本地文件夹',
    handler: () => {
      void addLocalSource()
    },
  },
  {
    text: '添加 WebDAV 文件夹',
    handler: () => {
      openWebDavModal()
    },
  },
  {
    text: '取消',
    role: 'cancel',
  },
]
</script>

<style scoped>
.source-title {
  text-align: center;
}

.empty-state {
  display: flex;
  min-height: 60%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: var(--ion-color-medium);
  text-align: center;
}

.source-list {
  height: 100%;
  overflow: auto;
  padding: 8px 12px 24px;
}

.source-list-spacer {
  position: relative;
}

.source-card {
  position: absolute;
  top: 0;
  left: 12px;
  right: 12px;
  min-height: 100px;
  margin: 8px 0;
}

.source-path {
  overflow: hidden;
  margin: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.webdav-form {
  margin-bottom: 16px;
}

.message-text {
  margin: 12px 0;
}

.webdav-browser {
  margin-top: 20px;
}

.browser-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.current-path {
  overflow: hidden;
  color: var(--ion-color-medium);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-directory {
  color: var(--ion-color-medium);
  text-align: center;
}
</style>
