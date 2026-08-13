<template>
  <div class="m-page sources-page">
    <div class="sources-page__navbar-wrap">
      <m-navbar right-class="sources-page__right">
        <template #title>音源</template>
        <template #right>
          <m-button component="button" variant="clear" rounded class="sources-page__add-btn" aria-label="添加音源" @click="isAddActionSheetOpen = true">
            <component :is="add" aria-hidden="true" class="sources-page__add-icon" />
          </m-button>
        </template>
      </m-navbar>
    </div>

    <div class="m-content sources-page__content" id="source-page-content">
      <div v-if="sources.length === 0" class="sources-page__empty">
        <m-empty
          title="还没有音源"
          description="点击右上角加号添加本地文件夹或 WebDAV 文件夹。"
          :icon="radio"
        />
      </div>

      <div v-else class="sources-page__fill">
        <div ref="listParentRef" class="sources-page__list">
          <div class="sources-page__vlist" :style="{ height: `${totalSize}px` }">
            <div
              v-for="virtualRow in virtualRows"
              :key="sources[virtualRow.index].id"
              :ref="measureVirtualRow"
              class="sources-page__virtual-row"
              :data-index="virtualRow.index"
              :style="{
                transform: `translateY(${virtualRow.start}px)`,
              }"
            >
              <m-card class="sources-page__card">
                <div class="sources-page__card-name">{{ sources[virtualRow.index].name }}</div>
                <div class="sources-page__card-subtitle">{{ getSourceSubtitle(sources[virtualRow.index]) }}</div>
                <p class="sources-page__card-path">{{ sources[virtualRow.index].path }}</p>
                <div class="sources-page__card-actions">
                  <m-button component="button" size="small" variant="outline" rounded @click="openEditSource(sources[virtualRow.index])">编辑</m-button>
                  <m-button component="button" size="small" rounded danger @click="confirmDeleteSource(sources[virtualRow.index])">删除</m-button>
                  <m-button component="button" size="small" rounded @click="openScanSettings(sources[virtualRow.index])">扫描</m-button>
                </div>
              </m-card>
            </div>
          </div>
        </div>
      </div>

      <m-actions :opened="isAddActionSheetOpen" @backdropclick="isAddActionSheetOpen = false">
        <m-actions-group>
          <m-actions-label>添加音源</m-actions-label>
          <m-actions-button @click="handleAddLocal">添加本地文件夹</m-actions-button>
          <m-actions-button @click="handleAddWebDav">添加 WebDAV 文件夹</m-actions-button>
          <m-actions-button @click="isAddActionSheetOpen = false">取消</m-actions-button>
        </m-actions-group>
      </m-actions>

      <m-dialog :opened="isDeleteAlertOpen" title="删除音源">
        <p class="sources-page__dialog-text">{{ deleteAlertMessage }}</p>
        <template #buttons>
          <m-dialog-button @click="isDeleteAlertOpen = false">取消</m-dialog-button>
          <m-dialog-button strong danger @click="onConfirmDeleteSource">删除</m-dialog-button>
        </template>
      </m-dialog>

      <m-dialog :opened="isEditModalOpen" title="编辑音源" @backdropclick="closeEditSource">
        <div>
          <form class="sources-page__form" @submit.prevent="editSourceForm.handleSubmit">
            <div class="sources-page__form-fields">
              <editSourceForm.Field
                name="name"
                :validators="{
                  onSubmit: ({ value }) => requiredTrimmed(value, '请填写显示名称'),
                }"
              >
                <template #default="{ field }">
                  <m-list-input
                    label="显示名称"
                    :error="firstFieldError(field.state.meta.errors)"
                  >
                    <template #input>
                      <input
                        :value="field.state.value"
                        type="text"
                        placeholder="显示名称"
                        class="sources-page__input"
                        @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                        @blur="field.handleBlur"
                      />
                    </template>
                  </m-list-input>
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
                    <m-list-input
                      label="服务器地址"
                      :error="firstFieldError(field.state.meta.errors)"
                    >
                      <template #input>
                        <input
                          :value="field.state.value"
                          type="url"
                          placeholder="https://example.com/dav"
                          class="sources-page__input"
                          @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                          @blur="field.handleBlur"
                        />
                      </template>
                    </m-list-input>
                  </template>
                </editSourceForm.Field>
                <editSourceForm.Field
                  name="username"
                  :validators="{
                    onSubmit: ({ value }) => requiredTrimmed(value, '请填写用户名'),
                  }"
                >
                  <template #default="{ field }">
                    <m-list-input
                      label="用户名"
                      :error="firstFieldError(field.state.meta.errors)"
                    >
                      <template #input>
                        <input
                          :value="field.state.value"
                          type="text"
                          placeholder="用户名"
                          autocomplete="username"
                          class="sources-page__input"
                          @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                          @blur="field.handleBlur"
                        />
                      </template>
                    </m-list-input>
                  </template>
                </editSourceForm.Field>
                <editSourceForm.Field name="password">
                  <template #default="{ field }">
                    <m-list-input label="新密码" info="留空则保留原密码">
                      <template #input>
                        <input
                          :value="field.state.value"
                          type="password"
                          placeholder="新密码"
                          autocomplete="new-password"
                          class="sources-page__input"
                          @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                          @blur="field.handleBlur"
                        />
                      </template>
                    </m-list-input>
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
                  <m-list-input
                    label="目录"
                    :error="firstFieldError(field.state.meta.errors)"
                  >
                    <template #input>
                      <input
                        :value="field.state.value"
                        type="text"
                        placeholder="目录"
                        class="sources-page__input"
                        @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                        @blur="field.handleBlur"
                      />
                    </template>
                  </m-list-input>
                </template>
              </editSourceForm.Field>
            </div>

            <m-button
              v-if="sourcePendingEdit?.type === 'local'"
              component="button"
              variant="outline"
              rounded
              :disabled="isEditSaving"
              @click="pickEditedLocalDirectory"
            >
              重新选择目录
            </m-button>
            <!-- editErrorMessage 已改为 showToast -->
            <m-button component="button" type="submit" rounded :disabled="isEditSaving">
              {{ isEditSaving ? '正在保存…' : '保存修改' }}
            </m-button>
          </form>
        </div>
      </m-dialog>

      <m-dialog :opened="isScanSettingsOpen" @backdropclick="closeScanSettings">
        <template #title>
          <span>扫描设置</span>
        </template>
        <m-list inset>
          <m-list-item title="读取音乐标签">
            <template #after>
              <m-toggle
                :checked="scanOptions.readTags"
                aria-label="读取音乐标签"
                @change="onScanReadTagsToggle"
              />
            </template>
          </m-list-item>
        </m-list>
        <p class="sources-page__hint-text">开启后会逐个文件读取标题、歌手、专辑和时长；读取失败会回退为文件名。</p>
        <m-button component="button" rounded :disabled="!selectedScanSource" class="sources-page__scan-start-btn" @click="startScan">开始扫描</m-button>
      </m-dialog>

      <m-dialog :opened="isScanProgressOpen" @backdropclick="closeScanProgress">
        <template #title>
          <span>扫描进度</span>
          <m-button
            component="button"
            variant="clear"
            size="small"
            rounded
            :disabled="scanProgress.stage === 'processing' || scanProgress.stage === 'discovering'"
            @click="closeScanProgress"
          >
            关闭
          </m-button>
        </template>

        <m-preloader
          v-if="scanProgress.stage === 'discovering' || scanProgress.stage === 'processing'"
          class="sources-page__preloader"
          aria-label="扫描进行中"
        />
        <section class="sources-page__scan-section">
          <h2>{{ getScanStageText(scanProgress.stage) }}</h2>
          <p v-if="scanProgress.message">{{ scanProgress.message }}</p>
          <p v-if="scanProgress.currentItem" class="sources-page__scan-current">{{ scanProgress.currentItem }}</p>
          <div class="sources-page__scan-stats">
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
      </m-dialog>

      <m-sheet :opened="isWebDavModalOpen" @backdropclick="closeWebDavModal">
        <div class="sources-page__sheet-body">
          <div class="sources-page__sheet-title">添加 WebDAV</div>
          <form class="sources-page__form" @submit.prevent="webDavForm.handleSubmit">
            <div class="sources-page__form-fields">
              <webDavForm.Field
                name="serverUrl"
                :validators="{
                  onSubmit: ({ value }) => requiredTrimmed(value, '请填写服务器地址'),
                }"
              >
                <template #default="{ field }">
                  <m-list-input
                    label="服务器地址"
                    :error="firstFieldError(field.state.meta.errors)"
                  >
                    <template #input>
                      <input
                        :value="field.state.value"
                        type="url"
                        placeholder="https://example.com/dav"
                        class="sources-page__input"
                        @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                        @blur="field.handleBlur"
                      />
                    </template>
                  </m-list-input>
                </template>
              </webDavForm.Field>
              <webDavForm.Field
                name="username"
                :validators="{
                  onSubmit: ({ value }) => requiredTrimmed(value, '请填写用户名'),
                }"
              >
                <template #default="{ field }">
                  <m-list-input
                    label="用户名"
                    :error="firstFieldError(field.state.meta.errors)"
                  >
                    <template #input>
                      <input
                        :value="field.state.value"
                        type="text"
                        placeholder="用户名"
                        autocomplete="username"
                        class="sources-page__input"
                        @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                        @blur="field.handleBlur"
                      />
                    </template>
                  </m-list-input>
                </template>
              </webDavForm.Field>
              <webDavForm.Field
                name="password"
                :validators="{
                  onSubmit: ({ value }) => requiredTrimmed(value, '请填写密码'),
                }"
              >
                <template #default="{ field }">
                  <m-list-input
                    label="密码"
                    :error="firstFieldError(field.state.meta.errors)"
                  >
                    <template #input>
                      <input
                        :value="field.state.value"
                        type="password"
                        placeholder="密码"
                        autocomplete="current-password"
                        class="sources-page__input"
                        @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                        @blur="field.handleBlur"
                      />
                    </template>
                  </m-list-input>
                </template>
              </webDavForm.Field>
            </div>

            <m-button component="button" type="submit" rounded :disabled="isWebDavLoading || isWebDavSubmitting">
              {{ isWebDavConnected ? '重新连接' : '连接并浏览' }}
            </m-button>
          </form>

          <!-- errorMessage/successMessage 已改为 showToast -->

          <section v-if="isWebDavConnected" class="sources-page__webdav-section">
            <div class="sources-page__webdav-nav">
              <m-button component="button" variant="clear" size="small" rounded :disabled="!parentWebDavPath || isWebDavLoading" @click="goToParentDirectory">
                返回上级
              </m-button>
              <span class="sources-page__webdav-path">{{ currentWebDavPath }}</span>
            </div>

            <div v-if="webDavDirectories.length > 0">
              <div v-for="directory in webDavDirectories" :key="directory.path" class="sources-page__webdav-row">
                <m-checkbox
                  :checked="selectedWebDavPaths.has(directory.path)"
                  :aria-label="`选择 ${directory.basename}`"
                  @change="setWebDavSelectionFromEvent(directory.path, $event)"
                />
                <button type="button" class="sources-page__webdav-row-btn" @click="openWebDavDirectory(directory.path)">
                  <strong>{{ directory.basename }}</strong>
                  <span>{{ directory.path }}</span>
                </button>
                <m-button component="button" variant="clear" size="small" rounded @click="openWebDavDirectory(directory.path)">进入</m-button>
              </div>
            </div>

            <p v-else class="sources-page__webdav-empty">当前目录没有可添加的子文件夹。</p>

            <m-button
              component="button"
              rounded
              :disabled="selectedWebDavPaths.size === 0 || isWebDavLoading"
              @click="addSelectedWebDavSources"
            >
              添加选中的 {{ selectedWebDavPaths.size }} 个文件夹
            </m-button>
          </section>
        </div>
      </m-sheet>
    </div>

    <m-toast :opened="toast.visible" position="center">
      {{ toast.message }}
    </m-toast>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, type ComponentPublicInstance } from 'vue'
import { useForm } from '@tanstack/vue-form'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { FilePicker } from '@capawesome/capacitor-file-picker'
import { add, radio } from '@/icons'
import {
  MActions, MActionsButton, MActionsGroup, MActionsLabel,
  MButton, MCard, MCheckbox, MDialog, MDialogButton, MList, MListItem, MListInput,
  MNavbar, MPreloader, MSheet, MToast, MToggle, MEmpty,
} from '@/components/ui'
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

/** m-list-input 的 @input 事件适配 TanStack Form 的 handleChange */
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

<style scoped lang="scss">
.sources-page {
  &__fill { height: 100%; }
  &__vlist { position: relative; }
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

  &__add-btn {
    width: 32px;
    height: 32px;
    padding: 0;
  }

  &__add-icon {
    width: 16px;
    height: 16px;
  }

  &__content {
    overflow: hidden;
  }

  &__empty {
    height: 100%;
    box-sizing: border-box;
    padding-top: calc(max(16px, var(--m-safe-area-top, 0px)) + 44px);
  }

  &__list {
    height: 100%;
    overflow-y: auto;
    box-sizing: border-box;
    padding-top: calc(max(16px, var(--m-safe-area-top, 0px)) + 44px + 8px);
    padding-left: var(--m-spacing-sub);
    padding-right: var(--m-spacing-sub);
    padding-bottom: var(--m-content-pb);

    @media (min-width: 768px) {
      padding-bottom: var(--m-content-pb-md);
    }
  }

  &__virtual-row {
    position: absolute;
    top: 0;
    left: var(--m-spacing-sub);
    right: var(--m-spacing-sub);
    box-sizing: border-box;
    padding: var(--m-spacing-sub) 0;
  }

  &__card {
    min-height: 100px;
    margin: 0;
    border-radius: var(--m-radius-card);
    border: 1px solid var(--m-hairline);
    background: var(--m-surface-1);
  }

  &__card-name {
    font-size: 17px;
    line-height: 1.3;
    font-weight: 600;
    color: var(--m-text);
  }

  &__card-subtitle {
    margin-top: 2px;
    font-size: 13px;
    color: var(--m-text-secondary);
  }

  &__card-path {
    margin: 8px 0 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px;
    color: var(--m-text-secondary);
  }

  &__card-actions {
    display: flex;
    justify-content: flex-end;
    gap: var(--m-spacing-sub);
    margin-top: var(--m-spacing-sub);
  }

  &__dialog-text {
    margin: 0;
    text-align: center;
    font-size: 15px;
    line-height: 1.4;
    color: var(--m-text-secondary);
  }

  &__form {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-bottom: 16px;
  }

  &__form-fields {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__input {
    display: block;
    box-sizing: border-box;
    width: 100%;
    height: 40px;
    border: none;
    outline: none;
    background: transparent;
    font-size: 16px;
    color: var(--m-text);

    &::placeholder {
      color: var(--m-text-tertiary);
    }
  }

  &__hint-text {
    margin: 0;
    text-align: center;
    font-size: 13px;
    line-height: 1.4;
    color: var(--m-text-secondary);
  }

  &__scan-start-btn {
    margin-top: 8px;
  }

  &__preloader {
    margin-left: auto;
    margin-right: auto;
  }

  &__scan-section {
    margin-top: 16px;

    h2 {
      margin: 0 0 8px;
      font-size: 17px;
      font-weight: 600;
      color: var(--m-text);
    }

    p {
      margin: 0 0 8px;
      font-size: 14px;
      color: var(--m-text-secondary);
    }
  }

  &__scan-current {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__scan-stats {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-top: 8px;
    font-size: 14px;
    color: var(--m-text-secondary);

    > div {
      display: flex;
      justify-content: space-between;
      gap: 16px;
    }
  }

  &__sheet-body {
    padding: 0 16px 16px;
  }

  &__sheet-title {
    padding: 16px 0 8px;
    text-align: center;
    font-size: 17px;
    font-weight: 600;
    color: var(--m-text);
  }

  &__webdav-section {
    margin-top: 20px;
  }

  &__webdav-nav {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }

  &__webdav-path {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px;
    color: var(--m-text-secondary);
  }

  &__webdav-row {
    display: flex;
    align-items: center;
    gap: var(--m-spacing-sub);
    min-height: var(--m-list-row-h);
    padding: 8px 0;
    border-bottom: 1px solid var(--m-hairline);
  }

  &__webdav-row-btn {
    display: flex;
    flex: 1;
    min-width: 0;
    flex-direction: column;
    gap: 2px;
    padding: 0;
    border: 0;
    background: transparent;
    color: inherit;
    text-align: left;
    font-family: inherit;

    > span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 13px;
      color: var(--m-text-secondary);
    }
  }

  &__webdav-empty {
    margin: 0;
    text-align: center;
    font-size: 13px;
    color: var(--m-text-secondary);
  }
}
</style>