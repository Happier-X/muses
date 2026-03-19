import urlcat from 'urlcat'
import 'expo-sqlite/localStorage/install'

/**
 * 基础请求函数
 * @param {string} url - API 请求路径（如 '/articles'）
 * @param {object} [options] - 请求配置项
 * @param {string} [options.method='GET'] - HTTP 方法
 * @param {object} [options.params] - URL 查询参数（如 { page: 1, limit: 10 }）
 * @param {object} [options.body] - 请求体数据
 * @returns {Promise<object>} 返回解析后的JSON数据
 *
 * @example
 * // 基础调用示例
 * request('/articles').then(data => console.log(data))
 *
 * @example
 * // 带查询参数的 GET 请求
 * request('/articles', {
 *   params: { page: 1, limit: 10 }
 * })
 *
 * @example
 * // POST 请求
 * // 提交表单数据
 * request('/auth/sign_in', {
 *   method: 'POST',
 *   body: { login: 'user', password: '123123' }
 * })
 */
type HTTPMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
type RequestOptions = {
  method?: HTTPMethod
  params?: Record<string, unknown>
  body?: Record<string, unknown>
}

const request = async (url: string, { method = 'GET', params, body }: RequestOptions = {}) => {
  // 完整的接口地址
  const apiUrl = globalThis.localStorage.getItem('serverAddress') ?? ''
  const requestUrl = urlcat(apiUrl, url, params)
  let token = globalThis.localStorage.getItem('accessToken') ?? ''
  // 请求头
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  }

  const config = {
    method,
    headers,
    ...(body && { body: JSON.stringify(body) }),
  }
  try {
    const response = await fetch(requestUrl, config)
    return await response.json()
  } catch (error) {
    console.error('网络请求失败', error)
    throw error
  }
}

export default request
