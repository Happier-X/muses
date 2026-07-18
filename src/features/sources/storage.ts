import { SecureStorage } from '@aparajita/capacitor-secure-storage'
import type { SourceItem } from './types'

const SOURCES_STORAGE_KEY = 'muses:sources'
const WEBDAV_PASSWORD_KEY_PREFIX = 'muses:webdav-password:'

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const isString = (value: unknown): value is string => {
  return typeof value === 'string' && value.length > 0
}

const isSourceItem = (value: unknown): value is SourceItem => {
  if (!isRecord(value)) {
    return false
  }

  if (!isString(value.id) || !isString(value.name) || !isString(value.createdAt)) {
    return false
  }

  if (value.type === 'local') {
    return isString(value.path)
  }

  if (value.type === 'webdav') {
    return (
      isString(value.serverUrl) &&
      isString(value.username) &&
      isString(value.path) &&
      isString(value.credentialKey)
    )
  }

  return false
}

export const createSourceId = (): string => {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }

  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export const getWebDavPasswordKey = (sourceId: string): string => {
  return `${WEBDAV_PASSWORD_KEY_PREFIX}${sourceId}`
}

export const loadSources = (): SourceItem[] => {
  const rawValue = localStorage.getItem(SOURCES_STORAGE_KEY)
  if (!rawValue) {
    return []
  }

  try {
    const parsedValue: unknown = JSON.parse(rawValue)
    if (!Array.isArray(parsedValue)) {
      return []
    }

    return parsedValue.filter(isSourceItem)
  } catch {
    return []
  }
}

export const saveSources = (sources: SourceItem[]): void => {
  localStorage.setItem(SOURCES_STORAGE_KEY, JSON.stringify(sources))
}

export const saveWebDavPassword = async (credentialKey: string, password: string): Promise<void> => {
  await SecureStorage.set(credentialKey, password)
}

export const getWebDavPassword = async (credentialKey: string): Promise<string | null> => {
  const value = await SecureStorage.get(credentialKey)
  return typeof value === 'string' ? value : null
}

export const removeWebDavPassword = async (credentialKey: string): Promise<void> => {
  await SecureStorage.remove(credentialKey)
}

export interface LocalSourceUpdate {
  name: string
  path: string
}

export interface WebDavSourceUpdate {
  name: string
  serverUrl: string
  username: string
  path: string
  password?: string
}

export type SourceUpdate = LocalSourceUpdate | WebDavSourceUpdate

export interface UpdateSourceResult {
  updated: SourceItem | null
  sources: SourceItem[]
}

const hasOnlyKeys = (value: object, allowedKeys: string[]): boolean => {
  return Object.keys(value).every((key) => allowedKeys.includes(key))
}

const isLocalSourceUpdate = (value: SourceUpdate): value is LocalSourceUpdate => {
  return (
    isString(value.name) &&
    isString(value.path) &&
    !('serverUrl' in value) &&
    !('username' in value) &&
    !('password' in value) &&
    hasOnlyKeys(value, ['name', 'path'])
  )
}

const isWebDavSourceUpdate = (value: SourceUpdate): value is WebDavSourceUpdate => {
  if (!('serverUrl' in value) || !('username' in value)) {
    return false
  }

  return (
    isString(value.name) &&
    isString(value.serverUrl) &&
    isString(value.username) &&
    isString(value.path) &&
    (!('password' in value) || value.password === undefined || typeof value.password === 'string') &&
    hasOnlyKeys(value, ['name', 'serverUrl', 'username', 'path', 'password'])
  )
}

/**
 * 只更新指定音源的可编辑字段，并保留音源身份与列表顺序。
 * WebDAV 新密码先写安全存储；元数据写入失败时尽力恢复原凭据。
 */
export const updateSource = async (
  sourceId: string,
  changes: SourceUpdate,
  existingSources = loadSources(),
): Promise<UpdateSourceResult> => {
  const index = existingSources.findIndex((source) => source.id === sourceId)
  if (index < 0) {
    return { updated: null, sources: existingSources }
  }

  const current = existingSources[index]
  let updated: SourceItem
  if (current.type === 'local') {
    if (!isLocalSourceUpdate(changes)) {
      throw new Error('本地音源编辑信息无效。')
    }
    updated = { ...current, name: changes.name, path: changes.path }
  } else {
    const currentWebDav = current
    if (!isWebDavSourceUpdate(changes)) {
      throw new Error('WebDAV 音源编辑信息无效。')
    }
    updated = {
      ...currentWebDav,
      name: changes.name,
      serverUrl: changes.serverUrl,
      username: changes.username,
      path: changes.path,
    }
  }

  const sources = existingSources.map((source, sourceIndex) => (sourceIndex === index ? updated : source))
  const newPassword = current.type === 'webdav' && isWebDavSourceUpdate(changes) ? changes.password : undefined
  if (!newPassword) {
    saveSources(sources)
    return { updated, sources }
  }

  if (current.type !== 'webdav') {
    throw new Error('音源类型不匹配。')
  }
  const oldPassword = await getWebDavPassword(current.credentialKey)
  await saveWebDavPassword(current.credentialKey, newPassword)
  try {
    saveSources(sources)
  } catch (error) {
    try {
      if (oldPassword === null) {
        await removeWebDavPassword(current.credentialKey)
      } else {
        await saveWebDavPassword(current.credentialKey, oldPassword)
      }
    } catch {
      // 恢复凭据属于尽力而为，仍向调用方抛出原始元数据保存错误。
    }
    throw error
  }

  return { updated, sources }
}

export interface DeleteSourceResult {
  deleted: SourceItem | null
  sources: SourceItem[]
}

/**
 * 删除指定音源：WebDAV 先移除 SecureStorage 凭据，再写 sources。
 * 凭据删除失败时不写库；找不到 id 时 deleted=null 且不改写库。
 */
export const deleteSource = async (
  sourceId: string,
  existingSources = loadSources(),
): Promise<DeleteSourceResult> => {
  const index = existingSources.findIndex((source) => source.id === sourceId)
  if (index < 0) {
    return { deleted: null, sources: existingSources }
  }

  const deleted = existingSources[index]
  if (deleted.type === 'webdav') {
    await removeWebDavPassword(deleted.credentialKey)
  }

  const sources = existingSources.filter((source) => source.id !== sourceId)
  saveSources(sources)
  return { deleted, sources }
}
