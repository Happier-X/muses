/**
 * 写入有界 Map；命中/覆写时移到末尾，容量超限后淘汰最久未使用条目。
 * Map 的插入顺序作为轻量 LRU 队列，避免额外定时器与依赖。
 */
export const setBoundedCache = <K, V>(cache: Map<K, V>, key: K, value: V, maxSize: number): void => {
  cache.delete(key)
  cache.set(key, value)
  while (cache.size > maxSize) {
    const oldestKey = cache.keys().next().value as K | undefined
    if (oldestKey === undefined) return
    cache.delete(oldestKey)
  }
}

/** 读取并刷新近期使用顺序。 */
export const getBoundedCache = <K, V>(cache: Map<K, V>, key: K): V | undefined => {
  const value = cache.get(key)
  if (value === undefined) return undefined
  cache.delete(key)
  cache.set(key, value)
  return value
}
