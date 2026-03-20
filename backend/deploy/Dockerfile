# 基于 Node.js 20 Alpine 镜像
FROM node:20-alpine AS base

# 安装构建依赖
FROM base AS deps
RUN apk add --no-cache \
    python3 \
    make \
    g++ \
    curl

WORKDIR /app

# 复制依赖文件
COPY package*.json ./
COPY prisma ./prisma/

# 安装依赖
RUN npm ci --only=production

# 生成 Prisma 客户端
RUN npx prisma generate

# 安装所有依赖（包括开发依赖，用于构建）
RUN npm install

# 构建应用
FROM base AS builder
WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

RUN npx prisma generate
RUN npm run build

# 生产镜像
FROM base AS runner
WORKDIR /app

ENV NODE_ENV=production

# 安装运行时依赖
RUN apk add --no-cache \
    curl \
    libstdc++ \
    libssl3 \
    zlib

# 创建非 root 用户
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# 设置权限
RUN mkdir -p /app/public/covers && \
    mkdir -p /app/data && \
    chown -R nextjs:nodejs /app

USER nextjs

# 复制构建产物
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public
COPY --from=builder /app/prisma ./prisma

# 暴露端口
EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
