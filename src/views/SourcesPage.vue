<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>音源</ion-title>
        <ion-buttons slot="end">
          <ion-button aria-label="添加音源" @click="isAddActionSheetOpen = true">
            <h-icon slot="icon-only" :icon="add" aria-hidden="true" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content :fullscreen="true">
      <h-empty
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
                    <h-button size="sm" variant="outline" @click="openEditSource(sources[virtualRow.index])">编辑</h-button>
                    <h-button size="sm" variant="danger-soft" @click="confirmDeleteSource(sources[virtualRow.index])">删除</h-button>
                    <h-button size="sm" variant="primary" @click="openScanSettings(sources[virtualRow.index])">扫描</h-button>
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

      <ion-modal :is-open="isEditModalOpen" :backdrop-dismiss="!isEditSaving" @didDismiss="closeEditSource">
        <ion-header>
          <ion-toolbar>
            <ion-title>编辑音源</ion-title>
            <ion-buttons slot="end">
              <ion-button :disabled="isEditSaving" @click="closeEditSource">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="ion-padding">
          <form class="edit-source-form" @submit.prevent="editSourceForm.handleSubmit">
            <div class="form-fields">
              <editSourceForm.Field
                name="name"
                :validators="{
                  onSubmit: ({ value }) => requiredTrimmed(value, '请填写显示名称'),
                }"
              >
                <template #default="{ field }">
                  <h-input
                    :model-value="field.state.value"
                    label="显示名称"
                    :error="firstFieldError(field.state.meta.errors)"
                    :invalid="field.state.meta.errors.length > 0"
                    @update:model-value="field.handleChange"
                    @blur="field.handleBlur"
                  />
                </template>
              </editSourceForm.Field>
              <template v-if="sourcePendingEdit?.type === 'webdav'">
                <editSourceForm.Field
                  name="serverUrl"
                  :validators="{
                    onSubmit: ({ value }) => requiredTrimmed(value, '请填写服务器地址'),
                  }"
                >
                  <template #default="{ field }">
                    <h-input
                      :model-value="field.state.value"
                      label="服务器地址"
                      type="url"
                      :error="firstFieldError(field.state.meta.errors)"
                      :invalid="field.state.meta.errors.length > 0"
                      @update:model-value="field.handleChange"
                      @blur="field.handleBlur"
                    />
                  </template>
                </editSourceForm.Field>
                <editSourceForm.Field
                  name="username"
                  :validators="{
                    onSubmit: ({ value }) => requiredTrimmed(value, '请填写用户名'),
                  }"
                >
                  <template #default="{ field }">
                    <h-input
                      :model-value="field.state.value"
                      label="用户名"
                      autocomplete="username"
                      :error="firstFieldError(field.state.meta.errors)"
                      :invalid="field.state.meta.errors.length > 0"
                      @update:model-value="field.handleChange"
                      @blur="field.handleBlur"
                    />
                  </template>
                </editSourceForm.Field>
                <editSourceForm.Field name="password">
                  <template #default="{ field }">
                    <h-input
                      :model-value="field.state.value"
                      label="新密码"
                      type="password"
                      autocomplete="new-password"
                      description="留空则保留原密码"
                      @update:model-value="field.handleChange"
                      @blur="field.handleBlur"
                    />
                  </template>
                </editSourceForm.Field>
              </template>
              <editSourceForm.Field
                name="path"
                :validators="{
                  onSubmit: ({ value }) => requiredTrimmed(value, '请填写目录'),
                }"
              >
                <template #default="{ field }">
                  <h-input
                    :model-value="field.state.value"
                    label="目录"
                    :error="firstFieldError(field.state.meta.errors)"
                    :invalid="field.state.meta.errors.length > 0"
                    @update:model-value="field.handleChange"
                    @blur="field.handleBlur"
                  />
                </template>
              </editSourceForm.Field>
            </div>

            <h-button
              v-if="sourcePendingEdit?.type === 'local'"
              variant="outline"
              type="button"
              :disabled="isEditSaving"
              @click="pickEditedLocalDirectory"
            >
              重新选择目录
            </h-button>
            <ion-text v-if="editErrorMessage" color="danger">
              <p class="message-text">{{ editErrorMessage }}</p>
            </ion-text>
            <h-button variant="primary" type="submit" :disabled="isEditSaving">
              {{ isEditSaving ? '正在保存…' : '保存修改' }}
            </h-button>
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
          <div class="form-fields">
            <div class="setting-inline">
              <span>读取音乐标签</span>
              <h-switch v-model="scanOptions.readTags" aria-label="读取音乐标签" />
            </div>
          </div>
          <p class="scan-hint">开启后会逐个文件读取标题、歌手、专辑和时长；读取失败会回退为文件名。</p>
          <h-button variant="primary" :disabled="!selectedScanSource" @click="startScan">开始扫描</h-button>
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
          <form class="webdav-form" @submit.prevent="webDavForm.handleSubmit">
            <div class="form-fields">
              <webDavForm.Field
                name="serverUrl"
                :validators="{
                  onSubmit: ({ value }) => requiredTrimmed(value, '请填写服务器地址'),
                }"
              >
                <template #default="{ field }">
                  <h-input
                    :model-value="field.state.value"
                    label="服务器地址"
                    placeholder="https://example.com/dav"
                    type="url"
                    :error="firstFieldError(field.state.meta.errors)"
                    :invalid="field.state.meta.errors.length > 0"
                    @update:model-value="field.handleChange"
                    @blur="field.handleBlur"
                  />
                </template>
              </webDavForm.Field>
              <webDavForm.Field
                name="username"
                :validators="{
                  onSubmit: ({ value }) => requiredTrimmed(value, '请填写用户名'),
                }"
              >
                <template #default="{ field }">
                  <h-input
                    :model-value="field.state.value"
                    label="用户名"
                    autocomplete="username"
                    :error="firstFieldError(field.state.meta.errors)"
                    :invalid="field.state.meta.errors.length > 0"
                    @update:model-value="field.handleChange"
                    @blur="field.handleBlur"
                  />
                </template>
              </webDavForm.Field>
              <webDavForm.Field
                name="password"
                :validators="{
                  onSubmit: ({ value }) => requiredTrimmed(value, '请填写密码'),
                }"
              >
                <template #default="{ field }">
                  <h-input
                    :model-value="field.state.value"
                    label="密码"
                    type="password"
                    autocomplete="current-password"
                    :error="firstFieldError(field.state.meta.errors)"
                    :invalid="field.state.meta.errors.length > 0"
                    @update:model-value="field.handleChange"
                    @blur="field.handleBlur"
                  />
                </template>
              </webDavForm.Field>
            </div>

            <h-button variant="primary" type="submit" :disabled="isWebDavLoading || isWebDavSubmitting">
              {{ isWebDavConnected ? '重新连接' : '连接并浏览' }}
            </h-button>
          </form>

          <ion-text v-if="errorMessage" color="danger">
            <p class="message-text">{{ errorMessage }}</p>
          </ion-text>
          <ion-text v-if="successMessage" color="success">
            <p class="message-text">{{ successMessage }}</p>
          </ion-text>

          <section v-if="isWebDavConnected" class="webdav-browser">
            <div class="browser-header">
              <h-button variant="ghost" size="sm" :disabled="!parentWebDavPath || isWebDavLoading" @click="goToParentDirectory">
                返回上级
              </h-button>
              <span class="current-path">{{ currentWebDavPath }}</span>
            </div>

            <div v-if="webDavDirectories.length > 0" class="webdav-dir-list">
              <div v-for="directory in webDavDirectories" :key="directory.path" class="webdav-dir-row">
                <h-checkbox
                  :model-value="selectedWebDavPaths.has(directory.path)"
                  :aria-label="`选择 ${directory.basename}`"
                  @update:model-value="setWebDavSelection(directory.path, $event)"
                />
                <button type="button" class="webdav-dir-meta" @click="openWebDavDirectory(directory.path)">
                  <strong>{{ directory.basename }}</strong>
                  <span>{{ directory.path }}</span>
                </button>
                <h-button variant="ghost" size="sm" @click="openWebDavDirectory(directory.path)">进入</h-button>
              </div>
            </div>

            <p v-else class="empty-directory">当前目录没有可添加的子文件夹。</p>

            <h-button
              variant="primary"
              :disabled="selectedWebDavPaths.size === 0 || isWebDavLoading"
              @click="addSelectedWebDavSources"
            >
              添加选中的 {{ selectedWebDavPaths.size }} 个文件夹
            </h-button>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { computed, ref, type ComponentPublicInstance } from 'vue'
import { useForm } from '@tanstack/vue-form'
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
  IonContent,
  IonHeader,
  IonItem,
  IonLabel,
  IonList,
  IonModal,
  IonNote,
  IonPage,
  IonProgressBar,
  IonText,
  IonTitle,
  IonToolbar,
  type ActionSheetButton,
  type AlertButton,
} from '@ionic/vue'
import { add } from '@/icons'
import { HButton, HCheckbox, HEmpty, HIcon, HInput, HSwitch } from '@/components/ui'
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
const editErrorMessage = ref('')
const isWebDavModalOpen = ref(false)
const isWebDavLoading = ref(false)
const isWebDavConnected = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const currentWebDavPath = ref('/')
const webDavDirectories = ref<WebDavDirectoryItem[]>([])
const selectedWebDavPaths = ref(new Set<string>())

const emptyWebDavFormValues = (): WebDavConnectionInput => ({
  serverUrl: '',
  username: '',
  password: '',
})

const emptyEditSourceFormValues = () => ({
  name: '',
  path: '',
  serverUrl: '',
  username: '',
  password: '',
})

const requiredTrimmed = (value: string, message: string): string | undefined =>
  value.trim() ? undefined : message

const firstFieldError = (errors: unknown[]): string | undefined => {
  const first = errors[0]
  return typeof first === 'string' ? first : undefined
}

const webDavForm = useForm({
  defaultValues: emptyWebDavFormValues(),
  onSubmit: async ({ value }) => {
    selectedWebDavPaths.value = new Set<string>()
    // 写回 trim 后的连接信息，供后续浏览/添加复用
    webDavForm.setFieldValue('serverUrl', value.serverUrl.trim())
    webDavForm.setFieldValue('username', value.username.trim())
    await loadWebDavDirectories('/')
  },
})

const isWebDavSubmitting = webDavForm.useSelector((state) => state.isSubmitting)

const getWebDavConnectionFromForm = (): WebDavConnectionInput => ({
  serverUrl: String(webDavForm.getFieldValue('serverUrl') ?? '').trim(),
  username: String(webDavForm.getFieldValue('username') ?? '').trim(),
  password: String(webDavForm.getFieldValue('password') ?? ''),
})

const editSourceForm = useForm({
  defaultValues: emptyEditSourceFormValues(),
  onSubmit: async ({ value }) => {
    const source = sourcePendingEdit.value
    if (!source) {
      return
    }

    const name = value.name.trim()
    const rawPath = value.path.trim()
    const path = source.type === 'webdav' ? normalizeWebDavPath(rawPath) : rawPath

    editErrorMessage.value = ''
    try {
      if (source.type === 'local') {
        const result = await updateSource(source.id, { name, path }, sources.value)
        if (!result.updated) {
          throw new Error('找不到要编辑的音源。')
        }
        sources.value = result.sources
      } else {
        const serverUrl = value.serverUrl.trim()
        const username = value.username.trim()
        const password = value.password

        const connectionChanged =
          serverUrl !== source.serverUrl ||
          username !== source.username ||
          path !== normalizeWebDavPath(source.path) ||
          password.length > 0
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
      editSourceForm.reset(emptyEditSourceFormValues())
      showSuccess('音源修改已保存。')
    } catch {
      editErrorMessage.value = '保存音源修改失败，请稍后重试。'
    }
  },
})

const isEditSaving = editSourceForm.useSelector((state) => state.isSubmitting)
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
  editSourceForm.reset({
    name: source.name,
    path: source.path,
    serverUrl: source.type === 'webdav' ? source.serverUrl : '',
    username: source.type === 'webdav' ? source.username : '',
    password: '',
  })
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
  editSourceForm.reset(emptyEditSourceFormValues())
}

const pickEditedLocalDirectory = async (): Promise<void> => {
  try {
    const result = await FilePicker.pickDirectory()
    editSourceForm.setFieldValue('path', result.path)
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    if (!/cancel|取消/i.test(message)) {
      editErrorMessage.value = '选择本地文件夹失败。'
    }
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
  webDavForm.reset(emptyWebDavFormValues())
  errorMessage.value = ''
  successMessage.value = ''
}

const loadWebDavDirectories = async (path: string): Promise<void> => {
  isWebDavLoading.value = true
  try {
    const normalizedPath = normalizeWebDavPath(path)
    webDavDirectories.value = await listWebDavDirectories(getWebDavConnectionFromForm(), normalizedPath)
    currentWebDavPath.value = normalizedPath
    isWebDavConnected.value = true
    errorMessage.value = ''
  } catch (error) {
    showError(error instanceof Error ? error.message : '读取 WebDAV 目录失败。')
  } finally {
    isWebDavLoading.value = false
  }
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

const setWebDavSelection = (path: string, selected: boolean): void => {
  const nextSelectedPaths = new Set(selectedWebDavPaths.value)
  if (selected) {
    nextSelectedPaths.add(path)
  } else {
    nextSelectedPaths.delete(path)
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

    const connection = getWebDavConnectionFromForm()
    for (const path of selectedWebDavPaths.value) {
      const id = createSourceId()
      const credentialKey = getWebDavPasswordKey(id)
      await saveWebDavPassword(credentialKey, connection.password)
      newSources.push({
        id,
        type: 'webdav',
        name: getWebDavDisplayName(path),
        serverUrl: connection.serverUrl,
        username: connection.username,
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
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 16px;
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.setting-inline,
.webdav-dir-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.webdav-dir-row {
  padding: 10px 0;
  border-bottom: 1px solid var(--muses-color-border-subtle);
}

.webdav-dir-meta {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
  padding: 0;
  border: 0;
  color: inherit;
  background: transparent;
  text-align: left;
}

.webdav-dir-meta span {
  overflow: hidden;
  color: var(--ion-color-medium);
  text-overflow: ellipsis;
  white-space: nowrap;
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
