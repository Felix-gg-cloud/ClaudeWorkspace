<template>
  <div class="app-shell">
    <!-- Animated Background Orbs -->
    <div class="bg-canvas">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
      <div class="grid-overlay"></div>
    </div>

    <!-- Page Content with Transition -->
    <router-view v-slot="{ Component, route: viewRoute }">
      <Transition name="page-slide" mode="out-in">
        <component :is="Component" :key="viewRoute.path" />
      </Transition>
    </router-view>

    <!-- Glass Bottom Navigation -->
    <nav class="bottom-nav">
      <router-link
        v-for="tab in tabs"
        :key="tab.path"
        :to="tab.path"
        class="nav-item"
        :class="{ active: route.path === tab.path }"
      >
        <span class="nav-icon">{{ tab.icon }}</span>
        <span class="nav-label">{{ tab.label }}</span>
        <span v-if="route.path === tab.path" class="nav-indicator"></span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const tabs = [
  { path: '/dashboard', icon: '🏠', label: '首页' },
  { path: '/today', icon: '📝', label: '学习' },
  { path: '/stages', icon: '🗺️', label: '旅程' },
  { path: '/calendar', icon: '📅', label: '打卡' },
  { path: '/profile', icon: '👤', label: '我的' },
]
</script>

<style scoped lang="scss">
.app-shell {
  max-width: 480px;
  margin: 0 auto;
  min-height: 100vh;
  background: #080b1a;
  position: relative;
  overflow-x: hidden;
}

// Animated Background
.bg-canvas {
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 480px;
  height: 100vh;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
}

.orb-1 {
  width: 300px;
  height: 300px;
  background: #00d4ff;
  top: -80px;
  left: -80px;
  animation: float 20s infinite ease-in-out;
}

.orb-2 {
  width: 250px;
  height: 250px;
  background: #a855f7;
  bottom: 20%;
  right: -60px;
  animation: float 25s infinite ease-in-out reverse;
}

.orb-3 {
  width: 200px;
  height: 200px;
  background: #ec4899;
  top: 50%;
  left: 30%;
  opacity: 0.2;
  animation: float 18s infinite ease-in-out 5s;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.015) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.015) 1px, transparent 1px);
  background-size: 60px 60px;
}

// Bottom Navigation
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  background: rgba(12, 16, 41, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  justify-content: space-around;
  padding: 6px 0 2px;
  padding-bottom: max(env(safe-area-inset-bottom, 6px), 6px);
  z-index: 100;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  padding: 6px 10px 4px;
  color: #5a6480;
  text-decoration: none;
  font-size: 10px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  letter-spacing: 0.3px;

  .nav-icon {
    font-size: 22px;
    transition: transform 0.3s;
  }

  .nav-label {
    transition: color 0.3s;
  }

  .nav-indicator {
    position: absolute;
    top: -1px;
    left: 50%;
    transform: translateX(-50%);
    width: 20px;
    height: 2px;
    background: linear-gradient(90deg, #00d4ff, #a855f7);
    border-radius: 1px;
    box-shadow: 0 0 8px rgba(0, 212, 255, 0.5);
  }

  &.active {
    color: #e8ecf4;

    .nav-icon {
      transform: scale(1.1) translateY(-1px);
    }
  }
}
</style>
