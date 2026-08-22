<template>
  <div class="webdav-browser">
    <div class="webdav-browser__nav">
      <m-button
        component="button"
        variant="clear"
        size="small"
        rounded
        :disabled="!parentPath || isLoading"
        @click="goToParentDirectory"
      >
        返回上级
      </m-button>
      <span class="webdav-browser__path">{{ currentPath }}</span>
    </div>

    <div v-if="isLoading" class="webdav-browser__loading">
      <m-preloader aria-label="正在读取目录" />
    </div>

    <template v-else>
      <div v-if="directories.length > 0" class="webdav-browser__list">
        <div v-for="directory in directories" :key="directory.path" class="webdav-browser__row">
          <m-checkbox
            v-if="mode === 'multiple'"
            :checked="selectedPaths.has(directory.path)"
            :aria-label="`选择 ${directory.basename}`"
            @change="setSelectionFromEvent(directory.path, $event)"
          />
          <button type="button" class="webdav-browser__row-btn" @click="openDirectory(directory.path)">
            <strong>{{ directory.basename }}</strong>
            <span>{{ directory.path }}</span>
          </button>
          <m-button
            component="button"
            variant="clear"
            size="small"
            rounded
            @click="mode === 'single' ? confirmSingle(directory.path) : openDirectory(directory.path)"
          >
            {{ mode === 'single' ? '选择' : '进入' }}
          </m-button>
        </div>
      </div>

      <p v-else class="webdav-browser__empty">
        {{ mode === 'multiple' ? '当前目录没有可添加的子文件夹。' : '当前目录没有子文件夹。' }}
      </p>

      <m-button
        v-if="mode === 'multiple'"
        component="button"
        rounded
        :disabled="selectedPaths.size === 0"
        @click="confirmMultiple"
      >
        添加选中的 {{ selectedPaths.size }} 个文件夹
      </m-button>
    </template>
  </div>
</template>

<script setup lang="ts">
/**
 * WebDAV 目录浏览器（feature 级组件）：
 * 给定连接信息与初始路径，内部自管浏览状态（当前路径/列表/loading/勾选）。
 * - multiple：checkbox 多选 + 底部批量确认（添加流程）
 * - single：行尾「选择」单选确认（编辑回填流程）
 * 错误统一经 emit('error') 上报，由页面统一 toast；组件内保留已浏览状态。
 */
import { computed, ref } from 'vue'
import { MButton, MCheckbox, MPreloader } from '@/components/ui'
import type { WebDavConnectionInput, WebDavDirectoryItem } from '@/features/sources/types'
import {
  getParentWebDavPath,
  listWebDavDirectories,
  normalizeWebDavPath,
} from '@/features/sources/webdav'

const props = withDefaults(
  defineProps<{
    connection: WebDavConnectionInput
    mode: 'single' | 'multiple'
    initialPath?: string
  }>(),
  {
    mode: 'multiple',
    initialPath: '/',
  },
)

const emit = defineEmits<{
  confirm: [payload: { paths: string[] }]
  error: [message: string]
}>()

const currentPath = ref(normalizeWebDavPath(props.initialPath))
const directories = ref<WebDavDirectoryItem[]>([])
const selectedPaths = ref(new Set<string>())
const isLoading = ref(false)

const parentPath = computed(() => getParentWebDavPath(currentPath.value))

const getErrorMessage = (error: unknown): string => {
  return error instanceof Error ? error.message : '读取 WebDAV 目录失败。'
}

/** 重置到 initialPath 并加载首屏；失败经 emit('error') 上报后保持关闭前状态可重试。 */
const open = async (): Promise<void> => {
  const targetPath = normalizeWebDavPath(props.initialPath)
  currentPath.value = targetPath
  directories.value = []
  selectedPaths.value = new Set()
  isLoading.value = true
  try {
    directories.value = await listWebDavDirectories(props.connection, targetPath)
  } catch (error) {
    emit('error', getErrorMessage(error))
  } finally {
    isLoading.value = false
  }
}

const loadDirectories = async (path: string): Promise<void> => {
  isLoading.value = true
  try {
    const normalizedPath = normalizeWebDavPath(path)
    // 先请求成功再切换状态，失败时保留已浏览内容供用户返回上级重试。
    const nextDirectories = await listWebDavDirectories(props.connection, normalizedPath)
    currentPath.value = normalizedPath
    directories.value = nextDirectories
  } catch (error) {
    emit('error', getErrorMessage(error))
  } finally {
    isLoading.value = false
  }
}

const openDirectory = async (path: string): Promise<void> => {
  await loadDirectories(path)
}

const goToParentDirectory = async (): Promise<void> => {
  if (!parentPath.value) {
    return
  }

  await loadDirectories(parentPath.value)
}

const setSelection = (path: string, selected: boolean): void => {
  const nextSelectedPaths = new Set(selectedPaths.value)
  if (selected) {
    nextSelectedPaths.add(path)
  } else {
    nextSelectedPaths.delete(path)
  }
  selectedPaths.value = nextSelectedPaths
}

const setSelectionFromEvent = (path: string, event: Event): void => {
  setSelection(path, (event.target as HTMLInputElement).checked)
}

const confirmMultiple = (): void => {
  if (selectedPaths.value.size === 0) {
    return
  }

  emit('confirm', { paths: [...selectedPaths.value] })
}

const confirmSingle = (path: string): void => {
  emit('confirm', { paths: [path] })
}

defineExpose({
  open,
})
</script>

<style scoped lang="scss">
.webdav-browser {
  display: flex;
  flex-direction: column;
  gap: var(--m-spacing-sub);

  &__nav {
    display: flex;
    align-items: center;
    gap: 8px;

    /* 同 __row：MButton 默认 width:100% 会把路径挤到只剩零头
     （08-22-fix-webdav-browser-nav-layout） */
    :deep(.m-button) {
      width: auto;
      flex: 0 0 auto;
    }
  }

  &__path {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px;
    color: var(--m-text-secondary);
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 16px 0;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: var(--m-spacing-sub);
    min-height: var(--m-list-row-h);
    padding: 8px 0;
    border-bottom: 1px solid var(--m-hairline);

    /* MButton 默认 width:100% 会以整行宽度参与 flex 分配，
       把 flex:1 的行按钮挤成 0 宽（08-21-fix-webdav-browser-row-layout） */
    :deep(.m-button) {
      width: auto;
      flex: 0 0 auto;
    }
  }

  &__row-btn {
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

    > strong {
      font-weight: 600;
      color: var(--m-text);
    }

    > span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 13px;
      color: var(--m-text-secondary);
    }
  }

  &__empty {
    margin: 0;
    text-align: center;
    font-size: 13px;
    color: var(--m-text-secondary);
  }
}
</style>
