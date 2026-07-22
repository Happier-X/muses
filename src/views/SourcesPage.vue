<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>音源</ion-title>
        <ion-buttons slot="end">
          <ion-button aria-label="添加音源" @click="isAddActionSheetOpen = true">
            <ion-icon slot="icon-only" :icon="add" aria-hidden="true" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content :fullscreen="true">
      <m-empty-state
        v-if="sources.length === 0"
        title="还没有音源"
        description="点击右上角加号添加本地文件夹或 WebDAV 文件夹。"
      />

      <div v-else class="tablet-content-limit">
        <div ref="listParentRef" class="source-list">
          <div class="source-list-spacer" :style="{ height: `${totalSize}px` }">
            <div
              v-for="virtualRow in virtualRows"
              :key="sources[virtualRow.index].id"
              :ref="measureVirtualRow"
              class="source-card-row"
              :data-index="virtualRow.index"
              :style="{
                transform: `translateY(${virtualRow.start}px)`,
              }"
            >
              <ion-card class="source-card">
                <ion-card-header>
                  <ion-card-title>{{ sources[virtualRow.index].name }}</ion-card-title>
                  <ion-card-subtitle>{{ getSourceSubtitle(sources[virtualRow.index]) }}</ion-card-subtitle>
                </ion-card-header>
                <ion-card-content>
                  <p class="source-path">{{ sources[virtualRow.index].path }}</p>
                  <div class="source-actions">
                    <ion-button size="small" fill="outline" @click="openEditSource(sources[virtualRow.index])">
                      编辑
                    </ion-button>
                    <ion-button
                      size="small"
                      color="danger"
                      fill="outline"
                      @click="confirmDeleteSource(sources[virtualRow.index])"
                    >
                      删除
                    </ion-button>
                    <ion-button size="small" @click="openScanSettings(sources[virtualRow.index])">扫描</ion-button>
                  </div>
                </ion-card-content>
              </ion-card>
            </div>
          </div>
        </div>
      </div>

      <ion-action-sheet
        :is-open="isAddActionSheetOpen"
        header="添加音源"
        :buttons="addSourceButtons"
        @didDismiss="isAddActionSheetOpen = false"
      />

      <ion-alert
        :is-open="isDeleteAlertOpen"
        header="删除音源"
        :message="deleteAlertMessage"
        :buttons="deleteAlertButtons"
        @didDismiss="closeDeleteAlert"
      />

      <ion-modal :is-open="isEditModalOpen" @didDismiss="closeEditSource">
        <ion-header>
          <ion-toolbar>
            <ion-title>编辑音源</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="closeEditSource">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="ion-padding">
          <form class="edit-source-form" @submit.prevent="saveEditedSource">
            <ion-list inset>
              <ion-item>
                <ion-input v-model="editSourceForm.name" label="显示名称" label-placement="stacked" required />
              </ion-item>
              <template v-if="sourcePendingEdit?.type === 'webdav'">
                <ion-item>
                  <ion-input
                    v-model="editSourceForm.serverUrl"
                    label="服务器地址"
                    label-placement="stacked"
                    type="url"
                    required
                  />
                </ion-item>
                <ion-item>
                  <ion-input
                    v-model="editSourceForm.username"
                    label="用户名"
                    label-placement="stacked"
                    autocomplete="username"
                    required
                  />
                </ion-item>
                <ion-item>
                  <ion-input
                    v-model="editSourceForm.password"
                    label="新密码"
                    label-placement="stacked"
                    autocomplete="new-password"
                    type="password"
                    helper-text="留空则保留原密码"
                  />
                </ion-item>
              </template>
              <ion-item>
                <ion-input v-model="editSourceForm.path" label="目录" label-placement="stacked" required />
              </ion-item>
            </ion-list>

            <ion-button
              v-if="sourcePendingEdit?.type === 'local'"
              expand="block"
              fill="outline"
              type="button"
              :disabled="isEditSaving"
              @click="pickEditedLocalDirectory"
            >
              重新选择目录
            </ion-button>
            <ion-text v-if="editErrorMessage" color="danger">
              <p class="message-text">{{ editErrorMessage }}</p>
            </ion-text>
            <ion-button expand="block" type="submit" :disabled="isEditSaving">
              {{ isEditSaving ? '正在保存…' : '保存修改' }}
            </ion-button>
          </form>
        </ion-content>
      </ion-modal>

      <ion-modal :is-open="isScanSettingsOpen" @didDismiss="closeScanSettings">
        <ion-header>
          <ion-toolbar>
            <ion-title>扫描设置</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="closeScanSettings">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="ion-padding">
          <ion-list inset>
            <ion-item>
              <ion-toggle v-model="scanOptions.readTags">读取音乐标签</ion-toggle>
            </ion-item>
          </ion-list>
          <p class="scan-hint">开启后会逐个文件读取标题、歌手、专辑和时长；读取失败会回退为文件名。</p>
          <ion-button expand="block" :disabled="!selectedScanSource" @click="startScan">开始扫描</ion-button>
        </ion-content>
      </ion-modal>

      <ion-modal :is-open="isScanProgressOpen" :backdrop-dismiss="scanProgress.stage !== 'processing' && scanProgress.stage !== 'discovering'">
        <ion-header>
          <ion-toolbar>
            <ion-title>扫描进度</ion-title>
            <ion-buttons slot="end">
              <ion-button :disabled="scanProgress.stage === 'processing' || scanProgress.stage === 'discovering'" @click="closeScanProgress">
                关闭
              </ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="ion-padding">
          <ion-progress-bar v-if="scanProgress.stage === 'discovering' || scanProgress.stage === 'processing'" type="indeterminate" />
          <section class="scan-progress">
            <h2>{{ getScanStageText(scanProgress.stage) }}</h2>
            <p v-if="scanProgress.message">{{ scanProgress.message }}</p>
            <p v-if="scanProgress.currentItem" class="source-path">当前：{{ scanProgress.currentItem }}</p>
            <ion-list inset>
              <ion-item>
                <ion-label>已发现 / 已处理</ion-label>
                <ion-note slot="end">{{ scanProgress.discovered }} / {{ scanProgress.processed }}</ion-note>
              </ion-item>
              <ion-item>
                <ion-label>入库 / 更新 / 跳过</ion-label>
                <ion-note slot="end">{{ scanProgress.inserted }} / {{ scanProgress.updated }} / {{ scanProgress.skipped }}</ion-note>
              </ion-item>
              <ion-item>
                <ion-label>降级 / 失败</ion-label>
                <ion-note slot="end">{{ scanProgress.degraded }} / {{ scanProgress.failed }}</ion-note>
              </ion-item>
              <ion-item>
                <ion-label>移除</ion-label>
                <ion-note slot="end">{{ scanProgress.removed }}</ion-note>
              </ion-item>
            </ion-list>
          </section>
        </ion-content>
      </ion-modal>

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
import { computed, ref, type ComponentPublicInstance } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { FilePicker } from '@capawesome/capacitor-file-picker'
import {
  IonActionSheet,
  IonAlert,
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
  IonNote,
  IonPage,
  IonProgressBar,
  IonText,
  IonTitle,
  IonToggle,
  IonToolbar,
  type ActionSheetButton,
  type AlertButton,
} from '@ionic/vue'
import { add } from '@/icons/ion-lucide'
import { MEmptyState } from '@/components/ui'
import {
  createSourceId,
  deleteSource,
  getWebDavPassword,
  getWebDavPasswordKey,
  loadSources,
  saveSources,
  saveWebDavPassword,
  updateSource,
} from '@/features/sources/storage'
import type { SourceItem, WebDavConnectionInput, WebDavDirectoryItem } from '@/features/sources/types'
import { getParentWebDavPath, getWebDavDisplayName, listWebDavDirectories, normalizeWebDavPath } from '@/features/sources/webdav'
import { scanSourceLibrary } from '@/features/library/scanner'
import { reconcileSourceSongs } from '@/features/library/storage'
import type { ScanOptions, ScanProgress, ScanStage } from '@/features/library/types'

const sources = ref<SourceItem[]>(loadSources())
const listParentRef = ref<HTMLElement | null>(null)
const isAddActionSheetOpen = ref(false)
const isDeleteAlertOpen = ref(false)
const sourcePendingDelete = ref<SourceItem | null>(null)
const sourcePendingEdit = ref<SourceItem | null>(null)
const isEditModalOpen = ref(false)
const isEditSaving = ref(false)
const editErrorMessage = ref('')
const editSourceForm = ref({
  name: '',
  path: '',
  serverUrl: '',
  username: '',
  password: '',
})
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
const selectedScanSource = ref<SourceItem | null>(null)
const isScanSettingsOpen = ref(false)
const isScanProgressOpen = ref(false)
const scanOptions = ref<ScanOptions>({ readTags: true })
const scanProgress = ref<ScanProgress>({
  stage: 'idle',
  discovered: 0,
  processed: 0,
  inserted: 0,
  updated: 0,
  skipped: 0,
  failed: 0,
  degraded: 0,
  removed: 0,
})

const rowVirtualizer = useVirtualizer(
  computed(() => ({
    count: sources.value.length,
    getScrollElement: () => listParentRef.value,
    estimateSize: () => 148,
    overscan: 6,
  })),
)

const measureVirtualRow = (element: Element | ComponentPublicInstance | null): void => {
  rowVirtualizer.value.measureElement(element instanceof HTMLElement ? element : null)
}

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

const deleteAlertMessage = computed(() => {
  const source = sourcePendingDelete.value
  if (!source) {
    return '确定删除该音源吗？'
  }

  return `确定删除「${source.name}」吗？将同时清理该音源下的歌曲${source.type === 'webdav' ? '与安全存储凭据' : ''}。`
})

const closeDeleteAlert = (): void => {
  isDeleteAlertOpen.value = false
  sourcePendingDelete.value = null
}

const confirmDeleteSource = (source: SourceItem): void => {
  sourcePendingDelete.value = source
  isDeleteAlertOpen.value = true
}

const openEditSource = (source: SourceItem): void => {
  sourcePendingEdit.value = source
  editSourceForm.value = {
    name: source.name,
    path: source.path,
    serverUrl: source.type === 'webdav' ? source.serverUrl : '',
    username: source.type === 'webdav' ? source.username : '',
    password: '',
  }
  editErrorMessage.value = ''
  isEditModalOpen.value = true
}

const closeEditSource = (): void => {
  if (isEditSaving.value) {
    return
  }
  isEditModalOpen.value = false
  sourcePendingEdit.value = null
  editErrorMessage.value = ''
  editSourceForm.value.password = ''
}

const pickEditedLocalDirectory = async (): Promise<void> => {
  try {
    const result = await FilePicker.pickDirectory()
    editSourceForm.value.path = result.path
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    if (!/cancel|取消/i.test(message)) {
      editErrorMessage.value = '选择本地文件夹失败。'
    }
  }
}

const saveEditedSource = async (): Promise<void> => {
  const source = sourcePendingEdit.value
  if (!source || isEditSaving.value) {
    return
  }

  const name = editSourceForm.value.name.trim()
  const rawPath = editSourceForm.value.path.trim()
  if (!name || !rawPath) {
    editErrorMessage.value = '请完整填写音源信息。'
    return
  }
  const path = source.type === 'webdav' ? normalizeWebDavPath(rawPath) : rawPath

  isEditSaving.value = true
  editErrorMessage.value = ''
  try {
    if (source.type === 'local') {
      const result = await updateSource(source.id, { name, path }, sources.value)
      if (!result.updated) {
        throw new Error('找不到要编辑的音源。')
      }
      sources.value = result.sources
    } else {
      const serverUrl = editSourceForm.value.serverUrl.trim()
      const username = editSourceForm.value.username.trim()
      const password = editSourceForm.value.password
      if (!serverUrl || !username) {
        editErrorMessage.value = '请完整填写 WebDAV 连接信息。'
        return
      }

      const connectionChanged =
        serverUrl !== source.serverUrl || username !== source.username || path !== normalizeWebDavPath(source.path) || password.length > 0
      if (connectionChanged) {
        const verificationPassword = password || (await getWebDavPassword(source.credentialKey))
        if (!verificationPassword) {
          editErrorMessage.value = 'WebDAV 密码不存在，请输入新密码。'
          return
        }
        try {
          await listWebDavDirectories({ serverUrl, username, password: verificationPassword }, path)
        } catch {
          editErrorMessage.value = 'WebDAV 连接或目标目录验证失败，请检查编辑信息。'
          return
        }
      }

      const result = await updateSource(
        source.id,
        { name, serverUrl, username, path, ...(password ? { password } : {}) },
        sources.value,
      )
      if (!result.updated) {
        throw new Error('找不到要编辑的音源。')
      }
      sources.value = result.sources
    }

    isEditModalOpen.value = false
    sourcePendingEdit.value = null
    editSourceForm.value.password = ''
    showSuccess('音源修改已保存。')
  } catch {
    editErrorMessage.value = '保存音源修改失败，请稍后重试。'
  } finally {
    isEditSaving.value = false
  }
}

const executeDeleteSource = async (source: SourceItem): Promise<void> => {
  try {
    const result = await deleteSource(source.id, sources.value)
    if (!result.deleted) {
      showError('找不到要删除的音源。')
      return
    }

    sources.value = result.sources
    reconcileSourceSongs(result.deleted.id, [])
    showSuccess(`已删除音源「${result.deleted.name}」。`)
  } catch (error) {
    showError(error instanceof Error ? error.message : '删除音源失败。')
  }
}

const deleteAlertButtons = computed<AlertButton[]>(() => [
  {
    text: '取消',
    role: 'cancel',
  },
  {
    text: '删除',
    role: 'destructive',
    handler: () => {
      const source = sourcePendingDelete.value
      if (!source) {
        return
      }
      void executeDeleteSource(source)
    },
  },
])

const getLocalSourceName = (path: string): string => {
  return path.split(/[\\/]/).filter(Boolean).at(-1) || path
}

const getScanStageText = (stage: ScanStage): string => {
  const stageText: Record<ScanStage, string> = {
    idle: '等待扫描',
    discovering: '正在查找文件',
    processing: '正在扫描入库',
    completed: '扫描完成',
    failed: '扫描失败',
  }
  return stageText[stage]
}

const resetScanProgress = (): void => {
  scanProgress.value = {
    stage: 'idle',
    discovered: 0,
    processed: 0,
    inserted: 0,
    updated: 0,
    skipped: 0,
    failed: 0,
    degraded: 0,
    removed: 0,
  }
}

const openScanSettings = (source: SourceItem): void => {
  selectedScanSource.value = source
  // WebDAV 默认关闭读标签（避免网络逐文件读取导致慢/卡）；本地默认开启
  scanOptions.value = { readTags: source.type !== 'webdav' }
  resetScanProgress()
  isScanSettingsOpen.value = true
}

const closeScanSettings = (): void => {
  isScanSettingsOpen.value = false
}

const closeScanProgress = (): void => {
  isScanProgressOpen.value = false
  selectedScanSource.value = null
}

const startScan = async (): Promise<void> => {
  if (!selectedScanSource.value) {
    return
  }

  const source = selectedScanSource.value
  closeScanSettings()
  resetScanProgress()
  isScanProgressOpen.value = true

  try {
    const result = await scanSourceLibrary(source, scanOptions.value, (progress) => {
      scanProgress.value = progress
    })
    showSuccess(
      `扫描完成：入库 ${result.summary.inserted} 首，更新 ${result.summary.updated} 首，跳过 ${result.summary.skipped} 首，降级 ${result.summary.degraded} 首，移除 ${result.summary.removed} 首。`,
    )
  } catch (error) {
    scanProgress.value = {
      ...scanProgress.value,
      stage: 'failed',
      message: error instanceof Error ? error.message : '扫描失败。',
    }
    showError(scanProgress.value.message ?? '扫描失败。')
  }
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
.source-list {
  height: 100%;
  overflow: auto;
  padding: 8px 12px 24px;
}

.source-list-spacer {
  position: relative;
}

.source-card-row {
  position: absolute;
  top: 0;
  left: 12px;
  right: 12px;
  box-sizing: border-box;
  padding-block: 8px;
}

.source-card {
  min-height: 100px;
  margin: 0;
}

.source-path {
  overflow: hidden;
  margin: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.scan-hint {
  color: var(--ion-color-medium);
  font-size: 14px;
  line-height: 1.4;
}

.scan-progress {
  margin-top: 16px;
}

.edit-source-form,
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

.tablet-content-limit {
  height: 100%;
}

@media (min-width: 768px) {
  .tablet-content-limit {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
</style>
