import { PrismaClient } from '@prisma/client'
import bcrypt from 'bcryptjs'
import type { User } from '@prisma/client'

const prisma = new PrismaClient()

export class AuthService {
  // 根据用户名查找用户
  async findUserByUsername(username: string): Promise<User | null> {
    return prisma.user.findUnique({
      where: { username }
    })
  }

  // 根据 ID 查找用户
  async findUserById(id: string): Promise<User | null> {
    return prisma.user.findUnique({
      where: { id }
    })
  }

  // 验证密码
  async verifyPassword(plainPassword: string, hashedPassword: string): Promise<boolean> {
    return bcrypt.compare(plainPassword, hashedPassword)
  }

  // 创建用户
  async createUser(username: string, password: string, email?: string): Promise<User> {
    const hashedPassword = await bcrypt.hash(password, 10)
    return prisma.user.create({
      data: {
        username,
        password: hashedPassword,
        email
      }
    })
  }

  // 检查用户名是否存在
  async isUsernameExists(username: string): Promise<boolean> {
    const user = await prisma.user.findUnique({
      where: { username }
    })
    return user !== null
  }

  // 检查邮箱是否存在
  async isEmailExists(email: string): Promise<boolean> {
    const user = await prisma.user.findUnique({
      where: { email }
    })
    return user !== null
  }
}

export const authService = new AuthService()
