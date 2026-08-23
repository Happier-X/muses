/**
 * 刮削写回失败文案映射（08-23-scrape-writeback-failure-ux）。
 *
 * 原生层（WebDavPlugin.kt writeMetadata）已返回细粒度 code：
 * missingUrl / missingCredentials / download_failed / empty_file / put_failed /
 * AudioMetadataException.diagnosticCode / write_failed
 * 前端 writeFile 层另有：no_password / not_implemented
 *
 * 本模块把 code 映射为人话文案与原因分组，供 ScrapePage 结果态展示/汇总归类。
 * 未知 code 兜底显示原始 message，保证新增错误码不被吞掉。
 */
import type { WriteMetadataResult } from '@/features/library/native'
import type { WritebackResult } from './writeback'

/** 失败原因分组：网络 / 认证（凭据、配置）/ 上传 / 其他 */
export type WritebackFailureCategory = 'network' | 'auth' | 'upload' | 'other'

/** describeWritebackFailure 的入参形态 */
export interface WritebackFailureInput {
  status: WritebackResult['status']
  fileResult: Pick<WriteMetadataResult, 'code' | 'message'>
  error?: string
}

/** code → 原因分组映射表 */
const CODE_CATEGORY: Record<string, WritebackFailureCategory> = {
  no_password: 'auth',
  missingCredentials: 'auth',
  download_failed: 'network',
  put_failed: 'upload',
}

/** 按已知 code 返回固定人话文案；未知 code 返回 null 走兜底 */
const FIXED_COPY: Record<string, string> = {
  no_password: 'WebDAV 密码缺失，请到音源设置补全后重试',
  missingCredentials: 'WebDAV 密码缺失，请到音源设置补全后重试',
  missingUrl: 'WebDAV 地址缺失，请到音源设置补全后重试',
  download_failed: '下载 WebDAV 音频失败，请检查网络后重试',
}

/**
 * 失败码 → 原因分组。
 * 网络问题=download_failed；认证问题=no_password/missingCredentials；
 * 上传失败=put_failed；其余（empty_file/write_failed/not_implemented/
 * 原生诊断码/unknown）归「其他」。
 */
export const classifyWritebackFailure = (
  code?: string,
): WritebackFailureCategory => {
  if (!code) {
    return 'other'
  }
  return CODE_CATEGORY[code] ?? 'other'
}

/**
 * 写回结果 → 行详情人话文案。
 *
 * - 已知 code 映射固定文案；put_failed 透传含 HTTP 码的 message
 * - 未知 code 兜底 message || error || 「写回失败」
 * - file-failed（值仍入库）由调用方在前面补充「值已入库：」语义，
 *   本函数只负责具体原因，不感知 status
 */
export const describeWritebackFailure = (result: WritebackFailureInput): string => {
  const { fileResult, error } = result
  const fixed = fileResult.code ? FIXED_COPY[fileResult.code] : undefined
  if (fixed) {
    return fixed
  }
  return fileResult.message || error || '写回失败'
}
