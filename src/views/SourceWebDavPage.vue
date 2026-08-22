<template>
  <div class="m-page source-webdav-page">
    <div class="source-webdav-page__navbar-wrap">
      <m-navbar>
        <template #left>
          <m-navbar-back-link aria-label="返回" @click="goBack" />
        </template>
        <template #title>{{ isEditMode ? '编辑 WebDAV' : '添加 WebDAV' }}</template>
      </m-navbar>
    </div>

    <div class="m-content source-webdav-page__content">
      <form class="source-webdav-page__form" @submit.prevent="webDavForm.handleSubmit">
        <div class="source-webdav-page__form-fields">
          <webDavForm.Field
            v-if="isEditMode"
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
                    class="source-webdav-page__input"
                    @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                    @blur="field.handleBlur"
                  />
                </template>
              </m-list-input>
            </template>
          </webDavForm.Field>
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
                    class="source-webdav-page__input"
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
                    class="source-webdav-page__input"
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
              onSubmit: ({ value }) =>
                isEditMode ? undefined : requiredTrimmed(value, '请填写密码'),
            }"
          >
            <template #default="{ field }">
              <m-list-input
                :label="isEditMode ? '新密码' : '密码'"
                :info="isEditMode ? '留空则保留原密码' : undefined"
                :error="firstFieldError(field.state.meta.errors)"
              >
                <template #input>
                  <input
                    :value="field.state.value"
                    type="password"
                    :placeholder="isEditMode ? '新密码' : '密码'"
                    :autocomplete="isEditMode ? 'new-password' : 'current-password'"
                    class="source-webdav-page__input"
                    @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
                    @blur="field.handleBlur"
                  />
                </template>
              </m-list-input>
            </template>
          </webDavForm.Field>
          <webDavForm.Field
            v-if="isEditMode"
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
                  <div class="source-webdav-page__path-row">
                    <!-- 只读展示：目录由目录浏览器单选回填 -->
                    <input
                      :value="field.state.value"
                      type="text"
                      placeholder="目录"
                      readonly
                      class="source-webdav-page__input"
                    />
                    <m-button
                      component="button"
                      variant="clear"
                      size="small"
                      rounded
                      @click="openBrowser"
                    >
                      浏览目录
                    </m-button>
                  </div>
                </template>
              </m-list-input>
            </template>
          </webDavForm.Field>
        </div>

        <div class="source-webdav-page__actions">
          <m-button
            v-if="isEditMode"
            component="button"
            type="button"
            variant="outline"
            rounded
            :disabled="isVerifying || isSubmitting"
            @click="openBrowser"
          >
            连接并浏览
          </m-button>
          <m-button
            v-else
            component="button"
            type="submit"
            rounded
            :disabled="isVerifying || isSubmitting"
          >
            {{ connectLabel }}
          </m-button>
          <m-button
            v-if="isEditMode"
            component="button"
            type="submit"
            rounded
            :disabled="isVerifying || isSubmitting"
          >
            {{ isSubmitting ? '正在保存…' : '保存修改' }}
          </m-button>
        </div>
      </form>

    </div>

    <m-toast :opened="toast.visible" position="center">
      {{ toast.message }}
    </m-toast>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useForm } from '@tanstack/vue-form'
import { useRoute, useRouter } from 'vue-router'
import {
  MButton, MListInput, MNavbar, MNavbarBackLink, MToast,
} from '@/components/ui'
import {
  setWebDavBrowseSession,
  setWebDavFormDraft,
  takeWebDavBrowseResult,
  takeWebDavFormDraft,
  type WebDavBrowseSession,
  type WebDavFormDraft,
} from '@/features/sources/webdavBrowseSession'
import {
  createSourceId,
  getWebDavPassword,
  getWebDavPasswordKey,
  loadSources,
  saveSources,
  saveWebDavPassword,
  updateSource,
} from '@/features/sources/storage'
import type { SourceItem, WebDavConnectionInput, WebDavSourceItem } from '@/features/sources/types'
import { getParentWebDavPath, getWebDavDisplayName, listWebDavDirectories, normalizeWebDavPath } from '@/features/sources/webdav'

const route = useRoute()
const router = useRouter()

/** 页面模式由路由决定：/tabs/sources/webdav 为添加，/tabs/sources/webdav/:id 为编辑 */
const isEditMode = computed(() => typeof route.params.id === 'string')

/** 编辑目标音源（进入页面时按 id 加载） */
const pendingSource = ref<WebDavSourceItem | null>(null)

// ===== 表单状态（自 SourcesPage webDavForm 双模式平移）=====

type WebDavFormValues = {
  name: string
  serverUrl: string
  username: string
  password: string
  path: string
}

const emptyWebDavFormValues = (): WebDavFormValues => ({
  name: '',
  serverUrl: '',
  username: '',
  password: '',
  path: '',
})

const requiredTrimmed = (value: string, message: string): string | undefined =>
  value.trim() ? undefined : message

const firstFieldError = (errors: unknown[]): string | undefined => {
  const first = errors[0]
  return typeof first === 'string' ? first : undefined
}

// ===== Toast =====

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

// ===== 目录浏览跨页会话 =====

const isVerifying = ref(false)

const toFormDraft = (): WebDavFormDraft => ({
  name: String(webDavForm.getFieldValue('name') ?? ''),
  serverUrl: String(webDavForm.getFieldValue('serverUrl') ?? ''),
  username: String(webDavForm.getFieldValue('username') ?? ''),
  password: String(webDavForm.getFieldValue('password') ?? ''),
  path: String(webDavForm.getFieldValue('path') ?? ''),
})

/** 打开全屏目录浏览页（快照表单草稿 + set 会话后跳转）；结果经会话服务带回，onMounted 消费 */
const openBrowseSession = (session: WebDavBrowseSession): void => {
  // 跳转会卸载本页，先快照未保存的表单输入，返回挂载时恢复
  setWebDavFormDraft(toFormDraft())
  setWebDavBrowseSession(session)
  void router.push('/tabs/sources/webdav/browse')
}

/** 消费浏览页带回的结果：edit 单选回填目录，add 批量建源（须在表单 reset 之后调用） */
const consumeBrowseResult = (): void => {
  const result = takeWebDavBrowseResult()
  if (!result) {
    return
  }
  if (result.mode === 'single') {
    if (result.paths.length > 0) {
      webDavForm.setFieldValue('path', result.paths[0])
    }
    return
  }
  void addSelectedWebDavSources(result.paths, result.connection)
}
/** 批量建源进行中（防双击重复建源） */
const isSavingSources = ref(false)

// ===== 表单与提交 =====

const webDavForm = useForm({
  defaultValues: emptyWebDavFormValues(),
  onSubmit: async ({ value }) => {
    if (isEditMode.value) {
      await submitWebDavEdit(value)
      return
    }

    // add 模式：先验证连接（列根目录），成功后跳转全屏目录浏览页多选
    const connection: WebDavConnectionInput = {
      serverUrl: value.serverUrl.trim(),
      username: value.username.trim(),
      password: value.password,
    }
    isVerifying.value = true
    try {
      await listWebDavDirectories(connection, '/')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '读取 WebDAV 目录失败。', 'danger')
      return
    } finally {
      isVerifying.value = false
    }

    // 结果经会话服务带回，onMounted 消费后批量建源
    openBrowseSession({ mode: 'multiple', initialPath: '/', connection })
  },
})

const isSubmitting = webDavForm.useSelector((state) => state.isSubmitting)

const connectLabel = '连接并浏览'

/** 编辑态提交：verify 连接、更新名称/路径/密码（密码留空保留原密码） */
const submitWebDavEdit = async (value: WebDavFormValues): Promise<void> => {
  const source = pendingSource.value
  if (!source) {
    return
  }

  const name = value.name.trim()
  const path = normalizeWebDavPath(value.path.trim())
  const serverUrl = value.serverUrl.trim()
  const username = value.username.trim()
  const password = value.password

  try {
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
      loadSources(),
    )
    if (!result.updated) {
      throw new Error('找不到要编辑的音源。')
    }
    showToast('音源修改已保存。', 'success')
    scheduleLeave()
  } catch {
    showToast('保存音源修改失败，请稍后重试。', 'danger')
  }
}

/** 编辑态打开目录浏览：解析密码（留空读安全存储原密码），单选确认后回填目录字段 */
const openBrowser = async (): Promise<void> => {
  const source = pendingSource.value
  if (!source) {
    return
  }

  const serverUrl = String(webDavForm.getFieldValue('serverUrl') ?? '').trim()
  const username = String(webDavForm.getFieldValue('username') ?? '').trim()
  let password = String(webDavForm.getFieldValue('password') ?? '')
  if (!password) {
    // 密码留空表示保留原密码，从安全存储读取
    try {
      password = (await getWebDavPassword(source.credentialKey)) ?? ''
    } catch {
      password = ''
    }
  }
  if (!password) {
    showToast('WebDAV 密码不存在，请输入新密码。', 'danger')
    return
  }

  // 从当前目录的上级开始浏览，可直接改选同级或进入子级；结果由 onMounted 消费回填
  const formPath = String(webDavForm.getFieldValue('path') ?? '').trim()
  const initialPath = formPath ? getParentWebDavPath(formPath) ?? normalizeWebDavPath(formPath) : '/'
  openBrowseSession({
    mode: 'single',
    initialPath,
    connection: { serverUrl, username, password },
  })
}

/** add 模式批量建源（自 SourcesPage 平移，基础列表改为即时读取存储） */
const addSelectedWebDavSources = async (paths: string[], connection: WebDavConnectionInput): Promise<void> => {
  // paths 为空或上一批仍在保存中直接忽略（防双击重复建源）
  if (paths.length === 0 || isSavingSources.value) {
    return
  }

  isSavingSources.value = true
  try {
    const createdAt = new Date().toISOString()
    const currentSources = loadSources()
    const newSources: SourceItem[] = []

    for (const path of paths) {
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

    saveSources([...newSources, ...currentSources])
    showToast(`已添加 ${newSources.length} 个 WebDAV 文件夹。`, 'success')
    scheduleLeave()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '保存 WebDAV 音源失败。', 'danger')
  } finally {
    isSavingSources.value = false
  }
}

// ===== 导航 =====

/** 保存/添加成功后稍作停留让 toast 可见，再回音源列表 */
let leaveTimer: number | undefined

const scheduleLeave = (): void => {
  window.clearTimeout(leaveTimer)
  leaveTimer = window.setTimeout(() => {
    void router.replace('/tabs/sources')
  }, 800)
}

const goBack = (): void => {
  router.back()
  // vue-router 的 back() 在无历史时无操作，用超时兜底
  window.setTimeout(() => {
    void router.replace('/tabs/sources')
  }, 100)
}

// ===== 初始化 =====

onMounted(() => {
  // 跳转浏览页会卸载本页；无论从哪个分支返回都先取走草稿（避免残留影响后续进入）
  const draft = takeWebDavFormDraft()
  if (!isEditMode.value) {
    // add 模式：恢复跳转前的表单输入，再消费浏览页带回的多选结果（批量建源）
    webDavForm.reset({ ...emptyWebDavFormValues(), ...draft })
    consumeBrowseResult()
    return
  }

  const id = route.params.id
  const found = loadSources().find(
    (candidate): candidate is WebDavSourceItem =>
      candidate.id === id && candidate.type === 'webdav',
  )
  if (!found) {
    // 先让 toast 可见再返回列表（立即 replace 会卸载本页导致提示不可见）
    showToast('找不到要编辑的音源。', 'danger')
    scheduleLeave()
    return
  }

  pendingSource.value = found
  // 有草稿（从浏览页返回）时恢复用户未保存的输入，否则用存储值初始化
  webDavForm.reset(
    draft ?? {
      name: found.name,
      serverUrl: found.serverUrl,
      username: found.username,
      password: '',
      path: found.path,
    },
  )
  // 返回重新挂载后在此消费浏览结果（回填目录；须在 reset 之后）
  consumeBrowseResult()
})

onUnmounted(() => {
  window.clearTimeout(toastTimer)
  window.clearTimeout(leaveTimer)
})
</script>

<style scoped lang="scss">
.source-webdav-page {
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

  &__form {
    display: flex;
    flex: 0 1 auto;
    min-height: 0;
    flex-direction: column;
    gap: 16px;
    overflow-y: auto;
    padding-bottom: 8px;
  }

  &__form-fields {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__actions {
    display: flex;
    gap: var(--m-spacing-sub);

    :deep(.m-button) {
      flex: 1 1 0;
    }
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

  &__path-row {
    display: flex;
    flex: 1;
    align-items: center;
    gap: 4px;
    min-width: 0;

    .source-webdav-page__input {
      flex: 1;
      width: auto;
      min-width: 0;
    }

    /* MButton 默认 width:100% 会把 flex:1 的目录输入框挤成 0 宽（防坑契约） */
    :deep(.m-button) {
      width: auto;
      flex: 0 0 auto;
    }
  }
}
</style>
