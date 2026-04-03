#!/bin/bash
# LinguaLeap 停止所有服务

echo "▶ 停止后端服务..."
pkill -f 'spring-boot:run' 2>/dev/null || true

echo "▶ 停止前端..."
pkill -f 'vite' 2>/dev/null || true

sleep 2

# 检查残留
REMAINING=0
for port in 8080 8081 8082 8083 5173; do
  pid=$(lsof -ti:$port 2>/dev/null)
  if [ -n "$pid" ]; then
    echo "⚠ 端口 $port 仍被占用 (PID $pid)，强制终止..."
    kill -9 $pid 2>/dev/null
    REMAINING=1
  fi
done

if [ "$REMAINING" -eq 1 ]; then
  sleep 1
fi

echo "✅ 所有服务已停止"
