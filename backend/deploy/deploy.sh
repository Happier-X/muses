#!/bin/bash
# Muses NAS 部署脚本

set -e

echo "=========================================="
echo "  Muses 音乐服务器 - NAS 部署脚本"
echo "=========================================="

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "错误: 未安装 Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "错误: 未安装 Docker Compose"
    exit 1
fi

# 创建必要的目录
echo "创建数据目录..."
mkdir -p data covers

# 复制环境变量文件
if [ ! -f .env ]; then
    echo "创建 .env 文件..."
    cat > .env << EOF
# JWT 密钥 - 请修改为随机字符串
JWT_SECRET=$(openssl rand -base64 32)
JWT_EXPIRES_IN=7d
DATABASE_URL=file:./data/dev.db
EOF
    echo "已创建 .env 文件，请根据需要修改"
fi

# 启动服务
echo "启动 Docker 容器..."
docker-compose up -d --build

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查状态
if curl -f http://localhost:3000/api/config/music > /dev/null 2>&1; then
    echo ""
    echo "=========================================="
    echo "  部署成功！"
    echo "=========================================="
    echo ""
    echo "访问地址: http://你的NAS IP:3000"
    echo ""
    echo "配置音乐文件夹:"
    echo "1. 编辑 docker-compose.yml 中的音乐挂载路径"
    echo "   - /volume1/music:/music:ro"
    echo "2. 调用扫描 API:"
    echo "   curl -X POST http://localhost:3000/api/music/scan \\"
    echo "     -H 'Content-Type: application/json' \\"
    echo "     -d '{\"folderPath\": \"/music\"}'"
    echo ""
else
    echo "启动失败，请检查日志:"
    echo "docker-compose logs -f"
fi
