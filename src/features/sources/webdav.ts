import { createClient, type FileStat } from 'webdav'
import type { WebDavConnectionInput, WebDavDirectoryItem } from './types'

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

export const getWebDavDisplayName = (path: string): string => {
  const normalized = normalizeWebDavPath(path)
  if (normalized === '/') {
    return '/'
  }

  return normalized.split('/').filter(Boolean).at(-1) ?? normalized
}

const isDirectory = (item: FileStat): boolean => {
  return item.type === 'directory'
}

export const listWebDavDirectories = async (
  connection: WebDavConnectionInput,
  path: string,
): Promise<WebDavDirectoryItem[]> => {
  const client = createClient(connection.serverUrl, {
    username: connection.username,
    password: connection.password,
  })

  const normalizedPath = normalizeWebDavPath(path)
  const contents = await client.getDirectoryContents(normalizedPath)

  return contents
    .filter(isDirectory)
    .map((item) => {
      const itemPath = normalizeWebDavPath(item.filename)
      return {
        basename: item.basename || getWebDavDisplayName(itemPath),
        filename: item.filename,
        path: itemPath,
      }
    })
    .filter((item) => item.path !== normalizedPath)
    .sort((left, right) => left.basename.localeCompare(right.basename, 'zh-Hans-CN'))
}
