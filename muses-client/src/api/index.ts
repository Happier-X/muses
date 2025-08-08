import { createAlova } from 'alova';
import VueHook from 'alova/vue';
import adapterAxios from 'alova/axios';

export const authAlova = createAlova({
  baseURL: '',
  statesHook: VueHook,
  requestAdapter: adapterAxios(),
  async beforeRequest(method) {
    method.config.headers.token = 'user token';
  }
});