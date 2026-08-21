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
import { useRouter } from 'vue-router'
import { FilePicker } from '@capawesome/capacitor-file-picker'
import { add, radio } from '@/icons'
import {
  MActions, MActionsButton, MActionsGroup, MActionsLabel,
  MButton, MCard, MDialog, MDialogButton, MList, MListItem, MListInput,
  MNavbar, MPreloader, MToast, MToggle, MEmpty,
} from '@/components/ui'
import {
  createSourceId,
  deleteSource,
  loadSources,
  saveSources,
  updateSource,
} from '@/features/sources/storage'
import type { SourceItem } from '@/features/sources/types'
import { scanSourceLibrary } from '@/features/library/scanner'
import { reconcileSourceSongs } from '@/features/library/storage'
import type { ScanOptions, ScanProgress, ScanStage } from '@/features/library/types'


const router = useRouter()

const sources = ref<SourceItem[]>(loadSources())
const listParentRef = ref<HTMLElement | null>(null)
const isAddActionSheetOpen = ref(false)
const isDeleteAlertOpen = ref(false)
const sourcePendingDelete = ref<SourceItem | null>(null)
const sourcePendingEdit = ref<SourceItem | null>(null)
const isEditModalOpen = ref(false)
// editErrorMessage → showToast
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
const emptyEditSourceFormValues = () => ({
  name: '',
  path: '',
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

const editSourceForm = useForm({
  defaultValues: emptyEditSourceFormValues(),
  onSubmit: async ({ value }) => {
    const source = sourcePendingEdit.value
    if (!source) {
      return
    }

    const name = value.name.trim()
    const path = value.path.trim()

    try {
      const result = await updateSource(source.id, { name, path }, sources.value)
      if (!result.updated) {
        throw new Error('找不到要编辑的音源。')
      }
      sources.value = result.sources

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
  if (source.type === 'webdav') {
    // WebDAV：跳转独立页面编辑（按 id 预填）
    void router.push(`/tabs/sources/webdav/${source.id}`)
    return
  }

  sourcePendingEdit.value = source
  editSourceForm.reset({
    name: source.name,
    path: source.path,
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

const handleAddLocal = (): void => {
  isAddActionSheetOpen.value = false
  void addLocalSource()
}

const handleAddWebDav = (): void => {
  isAddActionSheetOpen.value = false
  void router.push('/tabs/sources/webdav')
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
    padding-top: calc(var(--m-navbar-pt, 16px) + 44px);
  }

  &__list {
    height: 100%;
    overflow-y: auto;
    box-sizing: border-box;
    padding-top: calc(var(--m-navbar-pt, 16px) + 44px + 8px);
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
}
</style>
