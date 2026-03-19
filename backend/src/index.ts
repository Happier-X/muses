import 'dotenv/config'
import { PrismaClient } from '@prisma/client'
import bcrypt from 'bcryptjs'
import { Elysia } from 'elysia'
import { node } from '@elysiajs/node'
import { cors } from '@elysiajs/cors'
import { openapi } from '@elysiajs/openapi'
import { auth } from './modules/auth'
import { music } from './modules/music'

const prisma = new PrismaClient()

async function initDefaultUser() {
  const count = await prisma.user.count()
  if (count === 0) {
    const hash = await bcrypt.hash('admin@123', 10)
    await prisma.user.create({
      data: { username: 'admin', password: hash }
    })
    console.log('Default admin user created (username: admin, password: admin@123)')
  }
}

initDefaultUser().then(() => {
  const app = new Elysia({ adapter: node() })
      .use(openapi({ provider: 'scalar' }))
      .use(cors())
      .use(auth)
      .use(music)
      .get('/', () => 'Muses Music Streaming API')
      .get('/health', () => ({ status: 'ok' }))
      .listen(3000)

    console.log('Server is running at http://localhost:3000')
    console.log('OpenAPI docs at http://localhost:3000/openapi')
  })
