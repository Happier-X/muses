import { createAlova } from 'alova'
import VueHook from 'alova/vue'
import adapterFetch from 'alova/fetch'
import { createServerTokenAuthentication } from 'alova/client'
import { refreshToken } from './methods/auth'

const { onResponseRefreshToken } = createServerTokenAuthentication({
  refreshTokenOnSuccess: {
    isExpired: (response) => {
      return response.status === 401
    },
    handler: async () => {
      try {
        const res = await refreshToken()
        localStorage.setItem('access_token', res.data.access_token)
        localStorage.setItem('refresh_token', res.data.refresh_token)
      } catch (error) {
        location.href = '/auth'
        throw error
      }
    }
  },
  async login(response: any) {
    const res = await response.clone().json()
    if (res.statusCode === 201) {
      localStorage.setItem('access_token', res.access_token)
      localStorage.setItem('refresh_token', res.refresh_token)
      location.href = '/app'
    } else {
      window.$message.error(res.message)
    }
  },
  assignToken: (method) => {
    method.config.headers.Authorization = localStorage.getItem('access_token')
  }
})

export const alova = createAlova({
  baseURL: `${import.meta.env.VITE_API_BASE_URL}`,
  statesHook: VueHook,
  requestAdapter: adapterFetch(),
  responded: onResponseRefreshToken((response) => {
    return response.json()
  })
})

export const delayLoadingMiddleware =
  (delayTimer = 1000) =>
  async (ctx, next) => {
    const { loading } = ctx.proxyStates
    ctx.controlLoading()
    const timer = setTimeout(() => {
      loading.v = true
    }, delayTimer)
    await next()
    loading.v = false
    clearTimeout(timer)
  }
