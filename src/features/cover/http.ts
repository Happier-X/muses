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
    if (response.status < 200 || response.status >= 300) {
      throw new Error(`http ${response.status}`)
    }
    if (typeof response.data === 'string') {
      return response.data
    }
    return JSON.stringify(response.data ?? '')
  } catch (error) {
    // CapacitorHttp 在纯 web/vitest 可能不可用
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
