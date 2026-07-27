<template>
  <div class="m-page">
    <h-nav-bar title="音源" :fixed="false">
      <template #right>
        <h-button variant="ghost" is-icon-only shape="square" aria-label="添加音源" @click="isAddActionSheetOpen = true">
          <h-icon :icon="add" />
        </h-button>
      </template>
    </h-nav-bar>

    <div class="m-content" id="source-page-content">
      <h-empty
        v-if="sources.length === 0"
        title="还没有音源"
        description="点击右上角加号添加本地文件夹或 WebDAV 文件夹。"
      />

      <div v-else class="h-full md:max-w-[var(--muses-content-max-width)] md:mx-auto">
        <div ref="listParentRef" class="h-full overflow-auto pt-[8px] px-[12px] pb-[24px]">
          <div class="relative" :style="{ height: `${totalSize}px` }">
            <div
              v-for="virtualRow in virtualRows"
              :key="sources[virtualRow.index].id"
              :ref="measureVirtualRow"
              class="absolute top-0 left-[12px] right-[12px] box-border py-[8px]"
              :data-index="virtualRow.index"
              :style="{
                transform: `translateY(${virtualRow.start}px)`,
              }"
            >
              <h-card class="min-h-[100px] m-0">
                <div class="text-[length:var(--muses-font-title)] leading-[var(--muses-line-height-title)] font-semibold">{{ sources[virtualRow.index].name }}</div>
                <div class="mt-[2px] text-[color:var(--muses-color-ink-muted)] text-[length:var(--muses-font-body-sm)]">{{ getSourceSubtitle(sources[virtualRow.index]) }}</div>
                <p class="truncate mt-[8px]">{{ sources[virtualRow.index].path }}</p>
                <div class="flex justify-end gap-[8px] mt-[8px]">
                  <h-button size="sm" variant="outline" @click="openEditSource(sources[virtualRow.index])">编辑</h-button>
                  <h-button size="sm" variant="danger-soft" @click="confirmDeleteSource(sources[virtualRow.index])">删除</h-button>
                  <h-button size="sm" variant="primary" @click="openScanSettings(sources[virtualRow.index])">扫描</h-button>
                </div>
              </h-card>
            </div>
          </div>
        </div>
      </div>

      <h-bottom-sheet v-model="isAddActionSheetOpen" title="添加音源" :show-handle="true" @close="isAddActionSheetOpen = false">
        <div class="flex flex-col gap-[var(--h-space-xs,2px)] pb-[env(safe-area-inset-bottom,0px)]">
          <button :class="actionSheetItemClass" type="button" @click="handleAddLocal">
            添加本地文件夹
          </button>
          <button :class="actionSheetItemClass" type="button" @click="handleAddWebDav">
            添加 WebDAV 文件夹
          </button>
          <button :class="actionSheetCancelClass" type="button" @click="isAddActionSheetOpen = false">
            取消
          </button>
        </div>
      </h-bottom-sheet>

      <h-dialog v-model="isDeleteAlertOpen" title="删除音源">
        <p>{{ deleteAlertMessage }}</p>
        <template #actions>
          <h-button variant="ghost" @click="isDeleteAlertOpen = false">取消</h-button>
          <h-button variant="danger" @click="onConfirmDeleteSource">删除</h-button>
        </template>
      </h-dialog>

      <h-bottom-sheet v-model="isEditModalOpen" title="编辑音源" @close="closeEditSource">

        
        <div>
          <form class="flex flex-col gap-[16px] mb-[16px]" @submit.prevent="editSourceForm.handleSubmit">
            <div class="flex flex-col gap-[12px]">
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
            <!-- editErrorMessage 已改为 showToast -->
            <h-button variant="primary" type="submit" :disabled="isEditSaving">
              {{ isEditSaving ? '正在保存…' : '保存修改' }}
            </h-button>
          </form>
        </div>
      </h-bottom-sheet>

      <h-bottom-sheet v-model="isScanSettingsOpen" @close="closeScanSettings">
        <template #title>
          <span>扫描设置</span>
        </template>
        <div class="flex items-center gap-[12px]">
          <span>读取音乐标签</span>
          <h-switch v-model="scanOptions.readTags" aria-label="读取音乐标签" />
        </div>
        <p class="text-[color:var(--h-color-ink-muted)] text-[14px] leading-[1.4]">开启后会逐个文件读取标题、歌手、专辑和时长；读取失败会回退为文件名。</p>
        <h-button variant="primary" :disabled="!selectedScanSource" @click="startScan">开始扫描</h-button>
      </h-bottom-sheet>

      <h-bottom-sheet v-model="isScanProgressOpen" @close="closeScanProgress">
        <template #title>
          <span>扫描进度</span>
          <h-button
            variant="ghost"
            size="sm"
            :disabled="scanProgress.stage === 'processing' || scanProgress.stage === 'discovering'"
            @click="closeScanProgress"
          >
            关闭
          </h-button>
        </template>

        <h-progress v-if="scanProgress.stage === 'discovering' || scanProgress.stage === 'processing'" indeterminate aria-label="扫描进行中" />
        <section class="mt-[16px]">
          <h2>{{ getScanStageText(scanProgress.stage) }}</h2>
          <p v-if="scanProgress.message">{{ scanProgress.message }}</p>
          <p v-if="scanProgress.currentItem" class="truncate">当前：{{ scanProgress.currentItem }}</p>
          <div class="scan-stats">
            <div class="scan-stat-row">
              <span>已发现 / 已处理</span>
              <span class="source-note">{{ scanProgress.discovered }} / {{ scanProgress.processed }}</span>
            </div>
            <div class="scan-stat-row">
              <span>入库 / 更新 / 跳过</span>
              <span class="source-note">{{ scanProgress.inserted }} / {{ scanProgress.updated }} / {{ scanProgress.skipped }}</span>
            </div>
            <div class="scan-stat-row">
              <span>降级 / 失败</span>
              <span class="source-note">{{ scanProgress.degraded }} / {{ scanProgress.failed }}</span>
            </div>
            <div class="scan-stat-row">
              <span>移除</span>
              <span class="source-note">{{ scanProgress.removed }}</span>
            </div>
          </div>
        </section>
      </h-bottom-sheet>

      <h-bottom-sheet v-model="isWebDavModalOpen" title="添加 WebDAV" @close="closeWebDavModal">
          <form class="flex flex-col gap-[16px] mb-[16px]" @submit.prevent="webDavForm.handleSubmit">
            <div class="flex flex-col gap-[12px]">
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

          <!-- errorMessage/successMessage 已改为 showToast -->

          <section v-if="isWebDavConnected" class="mt-[20px]">
            <div class="flex items-center gap-[8px] mb-[8px]">
              <h-button variant="ghost" size="sm" :disabled="!parentWebDavPath || isWebDavLoading" @click="goToParentDirectory">
                返回上级
              </h-button>
              <span class="truncate text-[color:var(--h-color-ink-muted)]">{{ currentWebDavPath }}</span>
            </div>

            <div v-if="webDavDirectories.length > 0">
              <div v-for="directory in webDavDirectories" :key="directory.path" class="flex items-center gap-[12px] py-[10px] border-b border-[var(--muses-color-border-subtle)]">
                <h-checkbox
                  :model-value="selectedWebDavPaths.has(directory.path)"
                  :aria-label="`选择 ${directory.basename}`"
                  @update:model-value="setWebDavSelection(directory.path, $event)"
                />
                <button type="button" class="flex flex-1 min-w-0 flex-col gap-[2px] p-0 border-0 text-inherit bg-transparent text-left [&>span]:truncate [&>span]:text-[color:var(--h-color-ink-muted)]" @click="openWebDavDirectory(directory.path)">
                  <strong>{{ directory.basename }}</strong>
                  <span>{{ directory.path }}</span>
                </button>
                <h-button variant="ghost" size="sm" @click="openWebDavDirectory(directory.path)">进入</h-button>
              </div>
            </div>

            <p v-else class="text-[color:var(--h-color-ink-muted)] text-center">当前目录没有可添加的子文件夹。</p>

            <h-button
              variant="primary"
              :disabled="selectedWebDavPaths.size === 0 || isWebDavLoading"
              @click="addSelectedWebDavSources"
            >
              添加选中的 {{ selectedWebDavPaths.size }} 个文件夹
            </h-button>
          </section>
      </h-bottom-sheet>
    </div>

    <h-toast
      v-model="toast.visible"
      :variant="toast.variant"
      :duration="toast.duration"
      position="bottom"
    >
      {{ toast.message }}
    </h-toast>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, type ComponentPublicInstance } from 'vue'
import { useForm } from '@tanstack/vue-form'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { FilePicker } from '@capawesome/capacitor-file-picker'
import { add } from '@/icons'
import { HBottomSheet, HButton, HCard, HCheckbox, HDialog, HEmpty, HIcon, HInput, HNavBar, HProgress, HSwitch, HToast } from '@/components/ui'
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

import { actionSheetCancelClass, actionSheetItemClass } from '@/theme/action-sheet'

const sources = ref<SourceItem[]>(loadSources())
const listParentRef = ref<HTMLElement | null>(null)
const isAddActionSheetOpen = ref(false)
const isDeleteAlertOpen = ref(false)
const sourcePendingDelete = ref<SourceItem | null>(null)
const sourcePendingEdit = ref<SourceItem | null>(null)
const isEditModalOpen = ref(false)
// editErrorMessage → showToast
const isWebDavModalOpen = ref(false)
const isWebDavLoading = ref(false)
const isWebDavConnected = ref(false)
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

const showToast = (
  message: string,
  variant: 'default' | 'success' | 'warning' | 'danger' = 'default',
  duration = 2000,
): void => {
  toast.value = { visible: true, message, variant, duration }
}
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
            showToast('WebDAV 密码不存在，请输入新密码。', 'danger')
            return
          }
          try {
            await listWebDavDirectories({ serverUrl, username, password: verificationPassword }, path)
          } catch {
            showToast('WebDAV 连接或目标目录验证失败，请检查编辑信息。', 'danger')
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
      showToast('音源修改已保存。', 'success')
    } catch {
      showToast('保存音源修改失败，请稍后重试。', 'danger')
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

const onConfirmDeleteSource = (): void => {
  if (sourcePendingDelete.value) {
    void executeDeleteSource(sourcePendingDelete.value)
  }
  closeDeleteAlert()
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
  
  isEditModalOpen.value = true
}

const closeEditSource = (): void => {
  if (isEditSaving.value) {
    return
  }
  isEditModalOpen.value = false
  sourcePendingEdit.value = null
  
  editSourceForm.reset(emptyEditSourceFormValues())
}

const pickEditedLocalDirectory = async (): Promise<void> => {
  try {
    const result = await FilePicker.pickDirectory()
    editSourceForm.setFieldValue('path', result.path)
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    if (!/cancel|取消/i.test(message)) {
      showToast('选择本地文件夹失败。', 'danger')
    }
  }
}

const executeDeleteSource = async (source: SourceItem): Promise<void> => {
  try {
    const result = await deleteSource(source.id, sources.value)
    if (!result.deleted) {
      showToast('找不到要删除的音源。', 'danger')
      return
    }

    sources.value = result.sources
    reconcileSourceSongs(result.deleted.id, [])
    showToast(`已删除音源「${result.deleted.name}」。`, 'success')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '删除音源失败。', 'danger')
  }
}

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
    showToast(
      `扫描完成：入库 ${result.summary.inserted} 首，更新 ${result.summary.updated} 首，跳过 ${result.summary.skipped} 首，降级 ${result.summary.degraded} 首，移除 ${result.summary.removed} 首。`,
      'success',
      4000,
    )
  } catch (error) {
    scanProgress.value = {
      ...scanProgress.value,
      stage: 'failed',
      message: error instanceof Error ? error.message : '扫描失败。',
    }
    showToast(scanProgress.value.message ?? '扫描失败。', 'danger')
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
    showToast('已添加本地文件夹。', 'success')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '选择本地文件夹失败。', 'danger')
  }
}

const openWebDavModal = (): void => {
  isWebDavModalOpen.value = true
}

const closeWebDavModal = (): void => {
  isWebDavModalOpen.value = false
  isWebDavConnected.value = false
  isWebDavLoading.value = false
  currentWebDavPath.value = '/'
  webDavDirectories.value = []
  selectedWebDavPaths.value = new Set<string>()
  webDavForm.reset(emptyWebDavFormValues())
}

const loadWebDavDirectories = async (path: string): Promise<void> => {
  isWebDavLoading.value = true
  try {
    const normalizedPath = normalizeWebDavPath(path)
    webDavDirectories.value = await listWebDavDirectories(getWebDavConnectionFromForm(), normalizedPath)
    currentWebDavPath.value = normalizedPath
    isWebDavConnected.value = true
  } catch (error) {
    showToast(error instanceof Error ? error.message : '读取 WebDAV 目录失败。', 'danger')
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
    showToast(`已添加 ${newSources.length} 个 WebDAV 文件夹。`, 'success')
    closeWebDavModal()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '保存 WebDAV 音源失败。', 'danger')
  } finally {
    isWebDavLoading.value = false
  }
}

const handleAddLocal = (): void => {
  isAddActionSheetOpen.value = false
  void addLocalSource()
}

const handleAddWebDav = (): void => {
  isAddActionSheetOpen.value = false
  openWebDavModal()
}
</script>
