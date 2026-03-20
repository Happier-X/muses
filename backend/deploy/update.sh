#!/bin/bash
# Muses 更新脚本

git pull

docker-compose down
docker-compose up -d --build

echo "更新完成"
