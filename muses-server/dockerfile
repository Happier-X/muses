# 使用 Node.js 的 Alpine Linux 版本作为基础镜像
FROM node:alpine

# 设置工作目录
WORKDIR /app

# 复制项目文件
COPY . .

# 设置环境变量
ENV DATABASE_URL="file:./prisma/dev.db"

# 构建应用
RUN npm install && \
    npx prisma generate && \
    npx prisma migrate deploy && \
    npm run build && \
    npm prune --production && \
    npm cache clean --force

# 暴露端口
EXPOSE 3000

# 启动应用
CMD ["npm", "run", "start:prod"]