import request from '@/utils/request'

const authApi = {
  login: (body: LoginBody) => request('/auth/login', { method: 'POST', body }),
}

export default authApi
