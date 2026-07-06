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
