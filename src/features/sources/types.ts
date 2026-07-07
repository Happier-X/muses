export type SourceType = 'local' | 'webdav'

export interface BaseSourceItem {
  id: string
  type: SourceType
  name: string
  createdAt: string
}

export interface LocalSourceItem extends BaseSourceItem {
  type: 'local'
  path: string
}

export interface WebDavSourceItem extends BaseSourceItem {
  type: 'webdav'
  serverUrl: string
  username: string
  path: string
  credentialKey: string
}

export type SourceItem = LocalSourceItem | WebDavSourceItem

export interface WebDavEntryItem {
  basename: string
  filename: string
  path: string
  isDirectory: boolean
}

export type WebDavDirectoryItem = Omit<WebDavEntryItem, 'isDirectory'>

export interface WebDavConnectionInput {
  serverUrl: string
  username: string
  password: string
}
