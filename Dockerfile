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
RUN npx prisma generate
RUN npm run build

# 生产镜像
FROM node:20-alpine

# 安装 FFmpeg 和 nginx
RUN apk add --no-cache ffmpeg nginx

# 创建目录
RUN mkdir -p /app /app/cache /run/nginx

# 复制前端构建文件
COPY --from=builder /app/web/dist /app/web/dist

# 复制后端文件
COPY --from=builder /app/backend /app/backend

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/nginx.conf

WORKDIR /app/backend

EXPOSE 80

# 使用 npx concurrently 运行多个进程
RUN npm install -g concurrently

CMD ["concurrently", "--kill-others", "node dist/index.js", "nginx"]
