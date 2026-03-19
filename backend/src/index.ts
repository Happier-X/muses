import 'dotenv/config'
import { Elysia } from 'elysia'
import { node } from '@elysiajs/node'
import { cors } from '@elysiajs/cors'
import { openapi } from '@elysiajs/openapi'
import { auth } from './modules/auth'
import { music } from './modules/music'

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

export type App = typeof app
