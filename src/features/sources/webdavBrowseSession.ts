/**
 * WebDAV 目录浏览跨页会话（模块级单例，仅内存，不持久化）：
 * 表单页「连接并浏览」前 set，浏览页 onMounted 时 take（取走即清空）。
 * 凭据、选择结果与表单草稿仅在内存流转，禁止出现在 URL / storage；
 * 浏览页刷新或深链直达时拿不到会话，应兜底回表单页。
 *
 * 注意：跳转浏览页会卸载表单页组件（vue-router 默认非 keep-alive），
 * 选择结果不能依赖跨卸载的 Promise 回传——浏览页确认后写入 result，
 * 表单页重新挂载时 take 消费（回填目录 / 批量建源）。
 */
import type { WebDavConnectionInput } from './types'

export interface WebDavBrowseSession {
  /** 已验证可用的 WebDAV 连接信息 */
  connection: WebDavConnectionInput
  /** multiple：add 流程批量勾选；single：edit 流程单选回填 */
  mode: 'single' | 'multiple'
  /** 浏览起始路径 */
  initialPath: string
}

/** 浏览结果：连接信息随结果一并带回，供表单页批量建源使用 */
export interface WebDavBrowseResult {
  connection: WebDavConnectionInput
  mode: 'single' | 'multiple'
  paths: string[]
}

/** 表单页跳转浏览页前的输入快照（跳转会卸载表单页，未保存的编辑需带回恢复） */
export interface WebDavFormDraft {
  name: string
  serverUrl: string
  username: string
  password: string
  path: string
}

let session: WebDavBrowseSession | null = null
let result: WebDavBrowseResult | null = null
let formDraft: WebDavFormDraft | null = null

/** 设置浏览会话；同时清空旧结果与旧草稿，杜绝上一次流程的残留串入本次 */
export const setWebDavBrowseSession = (next: WebDavBrowseSession): void => {
  session = next
  result = null
}

/** 取走会话并清空；无会话返回 null（刷新 / 深链直达场景） */
export const takeWebDavBrowseSession = (): WebDavBrowseSession | null => {
  const current = session
  session = null
  return current
}

/** 浏览页确认后写入结果（覆盖旧结果） */
export const setWebDavBrowseResult = (next: WebDavBrowseResult): void => {
  result = next
}

/** 取走结果并清空；未确认返回时为 null，表单页据此做到零副作用 */
export const takeWebDavBrowseResult = (): WebDavBrowseResult | null => {
  const current = result
  result = null
  return current
}

/** 跳转浏览页前快照表单输入 */
export const setWebDavFormDraft = (next: WebDavFormDraft): void => {
  formDraft = next
}

/** 取走表单草稿并清空；无草稿返回 null（未经浏览跳转的正常进入） */
export const takeWebDavFormDraft = (): WebDavFormDraft | null => {
  const current = formDraft
  formDraft = null
  return current
}
