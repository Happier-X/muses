import { describe, expect, it } from 'vitest'
import {
  classifyWritebackFailure,
  describeWritebackFailure,
} from '@/features/scrape/failure-copy'

const makeResult = (
  code: string | undefined,
  message?: string,
  error?: string,
): { status: 'file-failed' | 'failed'; fileResult: { code?: string; message?: string }; error?: string } => ({
  status: code === 'unknown' || error !== undefined ? 'failed' : 'file-failed',
  fileResult: { code, message },
  ...(error !== undefined ? { error } : {}),
})

describe('describeWritebackFailure（失败码 → 人话文案）', () => {
  it('no_password → 密码缺失提示', () => {
    expect(describeWritebackFailure(makeResult('no_password', 'WebDAV 密码未配置。')))
      .toBe('WebDAV 密码缺失，请到音源设置补全后重试')
  })

  it('missingCredentials → 密码缺失提示', () => {
    expect(describeWritebackFailure(makeResult('missingCredentials', '凭据无效。')))
      .toBe('WebDAV 密码缺失，请到音源设置补全后重试')
  })

  it('missingUrl → 地址缺失提示', () => {
    expect(describeWritebackFailure(makeResult('missingUrl', '缺少 url。')))
      .toBe('WebDAV 地址缺失，请到音源设置补全后重试')
  })

  it('download_failed → 网络下载失败提示', () => {
    expect(describeWritebackFailure(makeResult('download_failed', '下载缓存失败。')))
      .toBe('下载 WebDAV 音频失败，请检查网络后重试')
  })

  it('put_failed → 透传含 HTTP 码的 message', () => {
    const msg = '上传失败：HTTP 401 Unauthorized'
    expect(describeWritebackFailure(makeResult('put_failed', msg))).toBe(msg)
  })

  it('empty_file → 兜底显示原始 message', () => {
    expect(describeWritebackFailure(makeResult('empty_file', '下载的音频为空文件。')))
      .toBe('下载的音频为空文件。')
  })

  it('write_failed → 兜底显示原始 message', () => {
    expect(describeWritebackFailure(makeResult('write_failed', '标签解析失败。')))
      .toBe('标签解析失败。')
  })

  it('not_implemented → 兜底显示原始 message', () => {
    expect(describeWritebackFailure(makeResult('not_implemented', '当前环境不支持写入本地标签。')))
      .toBe('当前环境不支持写入本地标签。')
  })

  it('原生诊断码等未知 code → 兜底显示原始 message', () => {
    expect(describeWritebackFailure(makeResult('unsupported_format', '不支持的格式。')))
      .toBe('不支持的格式。')
  })

  it('未知 code 且无 message → 回落 error', () => {
    expect(describeWritebackFailure(makeResult('unknown', undefined, '网络中断')))
      .toBe('网络中断')
  })

  it('无 code 无 message 无 error → 兜底「写回失败」', () => {
    expect(describeWritebackFailure(makeResult(undefined)))
      .toBe('写回失败')
  })
})

describe('classifyWritebackFailure（失败码 → 原因分组）', () => {
  it('认证类：no_password / missingCredentials → auth', () => {
    expect(classifyWritebackFailure('no_password')).toBe('auth')
    expect(classifyWritebackFailure('missingCredentials')).toBe('auth')
  })

  it('网络类：download_failed → network', () => {
    expect(classifyWritebackFailure('download_failed')).toBe('network')
  })

  it('上传类：put_failed → upload', () => {
    expect(classifyWritebackFailure('put_failed')).toBe('upload')
  })

  it('其余已知/未知/缺省 code → other', () => {
    expect(classifyWritebackFailure('empty_file')).toBe('other')
    expect(classifyWritebackFailure('write_failed')).toBe('other')
    expect(classifyWritebackFailure('not_implemented')).toBe('other')
    expect(classifyWritebackFailure('missingUrl')).toBe('other')
    expect(classifyWritebackFailure('unknown')).toBe('other')
    expect(classifyWritebackFailure('some_native_diagnostic')).toBe('other')
    expect(classifyWritebackFailure(undefined)).toBe('other')
  })
})
