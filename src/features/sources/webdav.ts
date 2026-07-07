import { registerPlugin } from '@capacitor/core'
import type { AudioFileEntry, AudioTags } from '@/features/library/types'
import { getFileNameFromPath, isSupportedAudioFile } from '@/features/library/audio'
import type { WebDavConnectionInput, WebDavDirectoryItem, WebDavEntryItem } from './types'

interface WebDavNativePlugin {
  propfind(options: {
    url: string
    username: string
    password: string
  }): Promise<{
    status: number
    data: string
  }>
  readMetadata(options: {
    url: string
    username: string
    password: string
    songId?: string
  }): Promise<AudioTags>
}

export const WebDavNative = registerPlugin<WebDavNativePlugin>('WebDav')

const ensureLeadingSlash = (path: string): string => {
  if (!path) {
    return '/'
  }

  return path.startsWith('/') ? path : `/${path}`
}

export const normalizeWebDavPath = (path: string): string => {
  const normalized = ensureLeadingSlash(path.trim()).replace(/\/+/g, '/')
  if (normalized.length > 1 && normalized.endsWith('/')) {
    return normalized.slice(0, -1)
  }

  return normalized
}

export const getParentWebDavPath = (path: string): string | null => {
  const normalized = normalizeWebDavPath(path)
  if (normalized === '/') {
    return null
  }

  const parent = normalized.slice(0, normalized.lastIndexOf('/'))
  return parent || '/'
}

const safeDecodeURIComponent = (value: string): string => {
  try {
    return decodeURIComponent(value)
  } catch {
    return value
  }
}

export const getWebDavDisplayName = (path: string): string => {
  const normalized = normalizeWebDavPath(path)
  if (normalized === '/') {
    return '/'
  }

  return safeDecodeURIComponent(normalized.split('/').filter(Boolean).at(-1) ?? normalized)
}

const encodePath = (path: string): string => {
  const normalized = normalizeWebDavPath(path)
  if (normalized === '/') {
    return '/'
  }

  return `/${normalized
    .split('/')
    .filter(Boolean)
    .map((segment) => encodeURIComponent(segment))
    .join('/')}`
}

export const buildWebDavUrl = (serverUrl: string, path: string): string => {
  const trimmedServerUrl = serverUrl.trim().replace(/\/+$/, '')
  const encodedPath = encodePath(path)
  return `${trimmedServerUrl}${encodedPath}`
}

const getFirstChildText = (element: Element, localName: string): string => {
  const child = Array.from(element.getElementsByTagName('*')).find((item) => item.localName === localName)
  return child?.textContent?.trim() ?? ''
}

const hasCollectionResourceType = (element: Element): boolean => {
  return Array.from(element.getElementsByTagName('*')).some((item) => item.localName === 'collection')
}

const stripServerBasePath = (serverUrl: string, href: string): string => {
  const serverPath = normalizeWebDavPath(safeDecodeURIComponent(new URL(serverUrl).pathname))
  const hrefPath = normalizeWebDavPath(safeDecodeURIComponent(new URL(href, serverUrl).pathname))

  if (serverPath === '/') {
    return hrefPath
  }

  if (hrefPath === serverPath) {
    return '/'
  }

  if (hrefPath.startsWith(`${serverPath}/`)) {
    return normalizeWebDavPath(hrefPath.slice(serverPath.length))
  }

  return hrefPath
}

export const parseWebDavEntries = (xmlText: string, serverUrl: string, currentPath: string): WebDavEntryItem[] => {
  const document = new DOMParser().parseFromString(xmlText, 'application/xml')
  const parserError = document.getElementsByTagName('parsererror')[0]
  if (parserError) {
    throw new Error('WebDAV 返回内容无法解析。')
  }

  const normalizedCurrentPath = normalizeWebDavPath(currentPath)
  const responseElements = Array.from(document.getElementsByTagName('*')).filter((item) => item.localName === 'response')

  return responseElements
    .map((responseElement) => {
      const href = getFirstChildText(responseElement, 'href')
      const path = stripServerBasePath(serverUrl, href)
      const basename = safeDecodeURIComponent(getFirstChildText(responseElement, 'displayname')) || getWebDavDisplayName(path)

      return {
        basename,
        filename: href,
        path,
        isDirectory: hasCollectionResourceType(responseElement),
      }
    })
    .filter((item) => item.path !== normalizedCurrentPath)
    .sort((left, right) => left.basename.localeCompare(right.basename, 'zh-Hans-CN'))
}

const propfindWebDavEntries = async (connection: WebDavConnectionInput, path: string): Promise<WebDavEntryItem[]> => {
  const normalizedPath = normalizeWebDavPath(path)
  const response = await WebDavNative.propfind({
    url: buildWebDavUrl(connection.serverUrl, normalizedPath),
    username: connection.username,
    password: connection.password,
  })

  if (response.status === 401 || response.status === 403) {
    throw new Error('WebDAV 认证失败，请检查用户名和密码。')
  }

  if (response.status < 200 || response.status >= 300) {
    throw new Error(`WebDAV 目录读取失败，状态码：${response.status}。`)
  }

  return parseWebDavEntries(String(response.data), connection.serverUrl, normalizedPath)
}

export const listWebDavDirectories = async (
  connection: WebDavConnectionInput,
  path: string,
): Promise<WebDavDirectoryItem[]> => {
  return (await propfindWebDavEntries(connection, path))
    .filter((item) => item.isDirectory)
    .map(({ basename, filename, path }) => ({ basename, filename, path }))
}

export const listWebDavAudioFiles = async (connection: WebDavConnectionInput, rootPath: string): Promise<AudioFileEntry[]> => {
  const files: AudioFileEntry[] = []
  const pendingDirectories = [normalizeWebDavPath(rootPath)]

  while (pendingDirectories.length > 0) {
    const currentPath = pendingDirectories.shift()
    if (!currentPath) {
      continue
    }

    const entries = await propfindWebDavEntries(connection, currentPath)
    for (const entry of entries) {
      if (entry.isDirectory) {
        pendingDirectories.push(entry.path)
      } else if (isSupportedAudioFile(entry.path)) {
        files.push({
          path: entry.path,
          uri: buildWebDavUrl(connection.serverUrl, entry.path),
          name: entry.basename || getFileNameFromPath(entry.path),
        })
      }
    }
  }

  return files.sort((left, right) => left.path.localeCompare(right.path, 'zh-Hans-CN'))
}
