import { t } from 'elysia'

// 登录请求
export const LoginRequest = t.Object({
  username: t.String({ minLength: 1 }),
  password: t.String({ minLength: 1 })
})

export type LoginRequest = typeof LoginRequest.static

// 注册请求
export const RegisterRequest = t.Object({
  username: t.String({ minLength: 3, maxLength: 50 }),
  password: t.String({ minLength: 6 })
})

export type RegisterRequest = typeof RegisterRequest.static

// 登录响应
export const LoginResponse = t.Object({
  token: t.String(),
  user: t.Object({
    id: t.String(),
    username: t.String(),
    email: t.Optional(t.String())
  })
})

export type LoginResponse = typeof LoginResponse.static

// 用户信息响应
export const UserResponse = t.Object({
  id: t.String(),
  username: t.String(),
  email: t.Optional(t.String()),
  createdAt: t.String(),
  updatedAt: t.String()
})

export type UserResponse = typeof UserResponse.static

// 错误响应
export const ErrorResponse = t.Object({
  error: t.String()
})

export type ErrorResponse = typeof ErrorResponse.static
