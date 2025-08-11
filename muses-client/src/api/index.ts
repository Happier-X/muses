import { createAlova } from 'alova';
import VueHook from 'alova/vue';
import adapterFetch from 'alova/fetch';

export const alova = createAlova({
  baseURL: `${import.meta.env.VITE_API_BASE_URL}`,
  statesHook: VueHook,
  requestAdapter: adapterFetch(),
  responded:response=>response.json(),
  async beforeRequest(method) {
    method.config.headers.token = getToken()
  },
});

const getToken = ()=>{
  return localStorage.getItem('access_token') || ''
}

export const delayLoadingMiddleware =
  (delayTimer = 1000) =>
  async (ctx, next) => {
    const { loading } = ctx.proxyStates;
    ctx.controlLoading();
    const timer = setTimeout(() => {
      loading.v = true;
    }, delayTimer);
    await next();
    loading.v = false;
    clearTimeout(timer);
  }; 