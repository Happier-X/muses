import { CapacitorHttp } from '@capacitor/core'

/**
 * 跨端文本 GET：优先 CapacitorHttp（绕过 WebView CORS），测试/浏览器回退 fetch。
 */
export const httpGetText = async (url: string, headers?: Record<string, string>): Promise<string> => {
  try {
    const response = await CapacitorHttp.get({
      url,
      headers,
      responseType: 'text',
    })
    // 业务 HTTP 错误（含 404）直接抛出，勿再回退 fetch，避免双请求与测试实网污染
    if (response.status < 200 || response.status >= 300) {
      throw new Error(`http ${response.status}`)
    }
    if (typeof response.data === 'string') {
      return response.data
    }
    return JSON.stringify(response.data ?? '')
  } catch (error) {
    // 仅当 CapacitorHttp 调用本身失败（插件不可用/网络层）时回退 fetch
    if (error instanceof Error && /^http \d+/.test(error.message)) {
      throw error
    }
    if (typeof fetch === 'function') {
      const response = await fetch(url, { headers })
      if (!response.ok) {
        throw new Error(`http ${response.status}`)
      }
      return response.text()
    }
    throw error
  }
}

export const httpGetJson = async <T>(url: string, headers?: Record<string, string>): Promise<T> => {
  const text = await httpGetText(url, headers)
  return JSON.parse(text) as T
}
