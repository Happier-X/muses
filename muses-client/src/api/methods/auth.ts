import { alova } from '..'

export const login = (data: any) => {
  const method = alova.Post('/auth/login', {
    username: data.username,
    password: data.password
  })
  method.meta = {
    authRole: 'login'
  }
  return method
}

export const refreshToken = () => {
  const method = alova.Get('/auth/refreshToken', {
    headers: {
      refreshToken: localStorage.getItem('refresh_token')
    }
  })
  method.meta = {
    authRole: 'refreshToken'
  }
  return method
}
