import { useState, useEffect } from 'react'
import { get } from '@/utils/request'

/**
 * 自定义 Hooks 获取数据
 * @param {string} url - API 请求路径（如 '/articles'）
 * @param params - 查询参数（如 { page: 1, limit: 10 }）
 * @returns {{
 *   data: object,
 *   loading: boolean,
 *   error: boolean,
 *   onReload: (function(*=): Promise<void>),
 * }}
 */
const useFetchData = (url, params = {}) => {
  const [data, setData] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [refreshing, setRefreshing] = useState(false)

  /**
   * 请求接口
   * @returns {Promise<void>}
   */
  const fetchData = async () => {
    try {
      const { data } = await get(url, params)
      setData(data)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  /**
   * 重新加载
   * @returns {Promise<void>}
   */
  const onReload = async () => {
    setLoading(true)
    setError(false)
    await fetchData()
  }

  /**
   * 刷新数据（带 refreshing 状态）
   * @returns {Promise<void>}
   */
  const onRefresh = async () => {
    setRefreshing(true)
    await fetchData()
    setRefreshing(false)
  }

  // 当依赖参数是一个对象或引用类型，例如 params，
  // 即使它的内容没有变化，每次组件重新渲染时它的引用都会不同。
  // 从而导致 useEffect 不断触发，会造成无限循环请求。
  // 可以使用 JSON.stringify(params) 转换为字符串，来解决这个问题。
  useEffect(() => {
    fetchData()
  }, [url, JSON.stringify(params)])

  return {
    data,
    loading,
    error,
    refreshing,
    setData,
    onReload,
    onRefresh,
  }
}

export default useFetchData
