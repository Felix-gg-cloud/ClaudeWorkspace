#!/bin/bash
# LinguaLeap 一键启动脚本

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
FRONTEND_DIR="$PROJECT_DIR/frontend"

# 1. PostgreSQL
echo "▶ 启动 PostgreSQL..."
brew services start postgresql@16 2>/dev/null || true

# 2. 编译公共模块
echo "▶ 编译 common 模块..."
cd "$BACKEND_DIR"
mvn install -pl common -am -q

# 3. 后端服务
echo "▶ 启动 Gateway (8080)..."
cd "$BACKEND_DIR/gateway" && mvn spring-boot:run -q 2>&1 &

echo "▶ 启动 User Service (8081)..."
cd "$BACKEND_DIR/service-user" && mvn spring-boot:run -q 2>&1 &

echo "▶ 启动 Content Service (8082)..."
cd "$BACKEND_DIR/service-content" && mvn spring-boot:run -q 2>&1 &

echo "▶ 启动 AI Service (8083)..."
cd "$BACKEND_DIR/service-ai" && mvn spring-boot:run -q 2>&1 &

# 4. 前端
echo "▶ 启动前端 (5173)..."
cd "$FRONTEND_DIR" && npx vite --host 2>&1 &

echo ""
echo "✅ 全部启动完成"
echo "   前端: http://localhost:5173"
echo "   网关: http://localhost:8080"
