import { Elysia, t } from 'elysia'
import { jwt } from '@elysiajs/jwt'
import { authService } from './service'
import { LoginRequest, LoginResponse, RegisterRequest, UserResponse, ErrorResponse } from './model'

const JWT_SECRET = 'muses-secret-key-change-in-production'
const JWT_EXPIRY = '7d'

export const auth = new Elysia({ prefix: '/auth' })
  .use(
    jwt({
      name: 'jwt',
      secret: JWT_SECRET,
      exp: JWT_EXPIRY
    })
  )
  // 登录
  .post('/login', async ({ body, jwt, set }) => {
    const { username, password } = body as { username: string; password: string }

    // 查找用户
    const user = await authService.findUserByUsername(username)
    if (!user) {
      set.status = 401
      return { error: '用户名或密码错误' }
    }

    // 验证密码
    const isValid = await authService.verifyPassword(password, user.password)
    if (!isValid) {
      set.status = 401
      return { error: '用户名或密码错误' }
    }

    // 生成 JWT token
    const token = await jwt.sign({
      sub: user.id,
      username: user.username
    })

    return {
      token,
      user: {
        id: user.id,
        username: user.username,
        email: user.email
      }
    }
  }, {
    body: LoginRequest,
    response: {
      200: LoginResponse,
      401: ErrorResponse
    }
  })
  // 注册
  .post('/register', async ({ body, set }) => {
    const { username, password } = body as { username: string; password: string }

    // 检查用户名是否已存在
    if (await authService.isUsernameExists(username)) {
      set.status = 400
      return { error: '用户名已存在' }
    }

    // 创建用户
    const user = await authService.createUser(username, password)

    return {
      id: user.id,
      username: user.username,
      createdAt: user.createdAt.toISOString(),
      updatedAt: user.updatedAt.toISOString()
    }
  }, {
    body: RegisterRequest,
    response: {
      200: UserResponse,
      400: ErrorResponse
    }
  })
  // 获取当前用户信息
  .get('/me', async ({ jwt, headers, set }) => {
    const authHeader = headers.authorization
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      set.status = 401
      return { error: '未登录' }
    }

    const token = authHeader.substring(7)
    const payload = await jwt.verify(token)
    if (!payload) {
      set.status = 401
      return { error: '未登录' }
    }

    const user = await authService.findUserById(payload.sub as string)
    if (!user) {
      set.status = 404
      return { error: '用户不存在' }
    }

    return {
      id: user.id,
      username: user.username,
      email: user.email,
      createdAt: user.createdAt.toISOString(),
      updatedAt: user.updatedAt.toISOString()
    }
  }, {
    response: {
      200: UserResponse,
      401: ErrorResponse,
      404: ErrorResponse
    }
  })

export type Auth = typeof auth
