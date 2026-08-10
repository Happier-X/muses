<template>
  <k-page class="k-page m-page flex flex-col overflow-hidden !h-auto !bottom-safe-24 md:!bottom-0">
    <k-navbar rightClass="!h-8">
      <template #title>音源</template>
      <template #right>
        <k-button component="button" clear rounded class="size-8" aria-label="添加音源" @click="isAddActionSheetOpen = true">
          <component :is="add" aria-hidden="true" class="size-4" />
        </k-button>
      </template>
    </k-navbar>

    <div class="m-content" id="source-page-content">
      <m-empty
        v-if="sources.length === 0"
        title="还没有音源"
        description="点击右上角加号添加本地文件夹或 WebDAV 文件夹。"
        :icon="radio"
      />

      <div v-else class="h-full md:max-w-[720px] md:mx-auto">
        <div ref="listParentRef" class="h-full overflow-auto box-border pt-[8px] px-[12px] pb-[88px] md:pb-safe-22">
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
              <k-card class="min-h-[100px] m-0">
                <div class="text-[17px] leading-[1.3] font-semibold text-black dark:text-white">{{ sources[virtualRow.index].name }}</div>
                <div class="mt-[2px] text-black/55 dark:text-white/55 text-[13px]">{{ getSourceSubtitle(sources[virtualRow.index]) }}</div>
                <p class="truncate mt-[8px] text-black/55 dark:text-white/55">{{ sources[virtualRow.index].path }}</p>
                <div class="flex justify-end gap-[8px] mt-[8px]">
                  <k-button component="button" small outline rounded @click="openEditSource(sources[virtualRow.index])">编辑</k-button>
                  <k-button component="button" small rounded :colors="{ fillBgIos: 'bg-[#ff3b30] active:bg-[#e03428]' }" @click="confirmDeleteSource(sources[virtualRow.index])">删除</k-button>
                  <k-button component="button" small rounded @click="openScanSettings(sources[virtualRow.index])">扫描</k-button>
                </div>
              </k-card>
            </div>
          </div>
        </div>
      </div>

      <k-actions :opened="isAddActionSheetOpen" @backdropclick="isAddActionSheetOpen = false">
        <k-actions-group>
          <k-actions-label>添加音源</k-actions-label>
          <k-actions-button @click="handleAddLocal">添加本地文件夹</k-actions-button>
          <k-actions-button @click="handleAddWebDav">添加 WebDAV 文件夹</k-actions-button>
          <k-actions-button bold @click="isAddActionSheetOpen = false">取消</k-actions-button>
        </k-actions-group>
      </k-actions>

      <k-dialog :opened="isDeleteAlertOpen" title="删除音源">
        <p class="m-0 text-center text-black/55 dark:text-white/55 text-[15px] leading-[1.4]">{{ deleteAlertMessage }}</p>
        <template #buttons>
          <k-dialog-button @click="isDeleteAlertOpen = false">取消</k-dialog-button>
          <k-dialog-button strong :colors="{ fillBgIos: 'bg-[#ff3b30] active:bg-[#e03428]' }" @click="onConfirmDeleteSource">删除</k-dialog-button>
        </template>
      </k-dialog>

      <k-dialog :opened="isEditModalOpen" title="编辑音源" @backdropclick="closeEditSource">

        
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
                  <k-list-input
                    :value="field.state.value"
                    label="显示名称"
                    :error="firstFieldError(field.state.meta.errors)"
                    @input="onFormInput(field.handleChange)"
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
                    <k-list-input
                      :value="field.state.value"
                      label="服务器地址"
                      type="url"
                      :error="firstFieldError(field.state.meta.errors)"
                      @input="onFormInput(field.handleChange)"
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
                    <k-list-input
                      :value="field.state.value"
                      label="用户名"
                      autocomplete="username"
                      :error="firstFieldError(field.state.meta.errors)"
                      @input="onFormInput(field.handleChange)"
                      @blur="field.handleBlur"
                    />
                  </template>
                </editSourceForm.Field>
                <editSourceForm.Field name="password">
                  <template #default="{ field }">
                    <k-list-input
                      :value="field.state.value"
                      label="新密码"
                      type="password"
                      autocomplete="new-password"
                      info="留空则保留原密码"
                      @input="onFormInput(field.handleChange)"
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
                  <k-list-input
                    :value="field.state.value"
                    label="目录"
                    :error="firstFieldError(field.state.meta.errors)"
                    @input="onFormInput(field.handleChange)"
                    @blur="field.handleBlur"
                  />
                </template>
              </editSourceForm.Field>
            </div>

            <k-button
              v-if="sourcePendingEdit?.type === 'local'"
              component="button"
              outline
              rounded
              :disabled="isEditSaving"
              @click="pickEditedLocalDirectory"
            >
              重新选择目录
            </k-button>
            <!-- editErrorMessage 已改为 showToast -->
            <k-button component="button" type="submit" rounded :disabled="isEditSaving">
              {{ isEditSaving ? '正在保存…' : '保存修改' }}
            </k-button>
          </form>
        </div>
      </k-dialog>

      <k-dialog :opened="isScanSettingsOpen" @backdropclick="closeScanSettings">
        <template #title>
          <span>扫描设置</span>
        </template>
        <k-list inset>
          <k-list-item title="读取音乐标签">
            <template #after>
              <k-toggle
                :checked="scanOptions.readTags"
                aria-label="读取音乐标签"
                @change="onScanReadTagsToggle"
              />
            </template>
          </k-list-item>
        </k-list>
        <p class="m-0 text-center text-black/55 dark:text-white/55 text-[13px] leading-[1.4]">开启后会逐个文件读取标题、歌手、专辑和时长；读取失败会回退为文件名。</p>
        <k-button component="button" rounded :disabled="!selectedScanSource" class="mt-[8px]" @click="startScan">开始扫描</k-button>
      </k-dialog>

      <k-dialog :opened="isScanProgressOpen" @backdropclick="closeScanProgress">
        <template #title>
          <span>扫描进度</span>
          <k-button
            component="button"
            clear
            small
            rounded
            :disabled="scanProgress.stage === 'processing' || scanProgress.stage === 'discovering'"
            @click="closeScanProgress"
          >
            关闭
          </k-button>
        </template>

        <k-preloader
          v-if="scanProgress.stage === 'discovering' || scanProgress.stage === 'processing'"
          class="mx-auto"
          aria-label="扫描进行中"
        />
        <section class="mt-[16px]">
          <h2>{{ getScanStageText(scanProgress.stage) }}</h2>
          <p v-if="scanProgress.message">{{ scanProgress.message }}</p>
          <p v-if="scanProgress.currentItem" class="truncate">当前：{{ scanProgress.currentItem }}</p>
          <div>
            <div>
              <span>已发现 / 已处理</span>
              <span>{{ scanProgress.discovered }} / {{ scanProgress.processed }}</span>
            </div>
            <div>
              <span>入库 / 更新 / 跳过</span>
              <span>{{ scanProgress.inserted }} / {{ scanProgress.updated }} / {{ scanProgress.skipped }}</span>
            </div>
            <div>
              <span>降级 / 失败</span>
              <span>{{ scanProgress.degraded }} / {{ scanProgress.failed }}</span>
            </div>
            <div>
              <span>移除</span>
              <span>{{ scanProgress.removed }}</span>
            </div>
          </div>
        </section>
      </k-dialog>

      <k-sheet :opened="isWebDavModalOpen" @backdropclick="closeWebDavModal">
        <div class="px-[16px] pb-safe-0">
          <div class="text-[17px] font-semibold text-center pt-[16px] pb-[8px] text-black dark:text-white">添加 WebDAV</div>
          <form class="flex flex-col gap-[16px] mb-[16px]" @submit.prevent="webDavForm.handleSubmit">
            <div class="flex flex-col gap-[12px]">
              <webDavForm.Field
                name="serverUrl"
                :validators="{
                  onSubmit: ({ value }) => requiredTrimmed(value, '请填写服务器地址'),
                }"
              >
                <template #default="{ field }">
                  <k-list-input
                    :value="field.state.value"
                    label="服务器地址"
                    placeholder="https://example.com/dav"
                    type="url"
                    :error="firstFieldError(field.state.meta.errors)"
                    @input="onFormInput(field.handleChange)"
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
                  <k-list-input
                    :value="field.state.value"
                    label="用户名"
                    autocomplete="username"
                    :error="firstFieldError(field.state.meta.errors)"
                    @input="onFormInput(field.handleChange)"
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
                  <k-list-input
                    :value="field.state.value"
                    label="密码"
                    type="password"
                    autocomplete="current-password"
                    :error="firstFieldError(field.state.meta.errors)"
                    @input="onFormInput(field.handleChange)"
                    @blur="field.handleBlur"
                  />
                </template>
              </webDavForm.Field>
            </div>

            <k-button component="button" type="submit" rounded :disabled="isWebDavLoading || isWebDavSubmitting">
              {{ isWebDavConnected ? '重新连接' : '连接并浏览' }}
            </k-button>
          </form>

          <!-- errorMessage/successMessage 已改为 showToast -->

          <section v-if="isWebDavConnected" class="mt-[20px]">
            <div class="flex items-center gap-[8px] mb-[8px]">
              <k-button component="button" clear small rounded :disabled="!parentWebDavPath || isWebDavLoading" @click="goToParentDirectory">
                返回上级
              </k-button>
              <span class="truncate text-black/55 dark:text-white/55">{{ currentWebDavPath }}</span>
            </div>

            <div v-if="webDavDirectories.length > 0">
              <div v-for="directory in webDavDirectories" :key="directory.path" class="flex items-center gap-[12px] py-[10px] border-b border-black/10 dark:border-white/15">
                <k-checkbox
                  :checked="selectedWebDavPaths.has(directory.path)"
                  :aria-label="`选择 ${directory.basename}`"
                  @change="setWebDavSelectionFromEvent(directory.path, $event)"
                />
                <button type="button" class="flex flex-1 min-w-0 flex-col gap-[2px] p-0 border-0 text-inherit bg-transparent text-left [&>span]:truncate [&>span]:text-black/55 dark:[&>span]:text-white/55" @click="openWebDavDirectory(directory.path)">
                  <strong>{{ directory.basename }}</strong>
                  <span>{{ directory.path }}</span>
                </button>
                <k-button component="button" clear small rounded @click="openWebDavDirectory(directory.path)">进入</k-button>
              </div>
            </div>

            <p v-else class="text-black/55 dark:text-white/55 text-center">当前目录没有可添加的子文件夹。</p>

            <k-button
              component="button"
              rounded
              :disabled="selectedWebDavPaths.size === 0 || isWebDavLoading"
              @click="addSelectedWebDavSources"
            >
              添加选中的 {{ selectedWebDavPaths.size }} 个文件夹
            </k-button>
          </section>
        </div>
      </k-sheet>
    </div>

    <k-toast :opened="toast.visible" position="center">
      {{ toast.message }}
    </k-toast>
  </k-page>
</template>

<script setup lang="ts">
import { computed, ref, type ComponentPublicInstance } from 'vue'
import { useForm } from '@tanstack/vue-form'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { FilePicker } from '@capawesome/capacitor-file-picker'
import { add, radio } from '@/icons'
import { kActions, kActionsButton, kActionsGroup, kActionsLabel, kButton, kCard, kCheckbox, kDialog, kDialogButton, kList, kListItem, kListInput, kNavbar, kPage, kPreloader, kSheet, kToast, kToggle, MEmpty } from '@/components/ui'
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

/** k-list-input 的 @input 事件适配 TanStack Form 的 handleChange */
const onFormInput =
  (handleChange: (value: string) => void) => (e: Event): void => {
    handleChange((e.target as HTMLInputElement).value)
  }

const onScanReadTagsToggle = (e: Event): void => {
  scanOptions.value.readTags = (e.target as HTMLInputElement).checked
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

const setWebDavSelectionFromEvent = (path: string, e: Event): void => {
  setWebDavSelection(path, (e.target as HTMLInputElement).checked)
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
