import { createAlova } from 'alova'
import VueHook from 'alova/vue'
import adapterFetch from 'alova/fetch'
import { createServerTokenAuthentication } from 'alova/client'
import { useRouter } from 'vue-router'
import { refreshToken } from './methods/auth'

const router = useRouter()
const { onResponseRefreshToken } = createServerTokenAuthentication({
  refreshTokenOnSuccess: {
    isExpired: (response) => {
      return response.status === 401
    },
    handler: async () => {
      try {
        const { access_token, refresh_token } = await refreshToken()
        localStorage.setItem('access_token', access_token)
        localStorage.setItem('refresh_token', refresh_token)
      } catch (error) {
        router.push('/auth')
        throw error
      }
    }
  },
  login(response: any) {
    localStorage.setItem('access_token', response.access_token)
    localStorage.setItem('refresh_token', response.refresh_token)
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
