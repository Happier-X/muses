FROM node:20-alpine AS builder

# 构建前端
WORKDIR /app/web
COPY web/package*.json web/
RUN npm ci

COPY web/ .
RUN npm run build

# 构建后端
WORKDIR /app/backend
COPY backend/package*.json backend/
RUN npm ci --only=production

COPY backend/ .
COPY backend/prisma ./prisma/
RUN npx prisma generate
RUN npm run build

# 生产镜像
FROM node:20-alpine

# 安装 FFmpeg
RUN apk add --no-cache ffmpeg

# 创建目录
RUN mkdir -p /app

# 复制前端构建文件
COPY --from=builder /app/web/dist /app/web/dist

# 复制后端文件
COPY --from=builder /app/backend /app/backend

WORKDIR /app/backend

EXPOSE 3000

CMD ["node", "dist/index.js"]
