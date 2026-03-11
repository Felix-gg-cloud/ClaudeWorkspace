<template>
  <div class="app-shell">
    <router-view />
    <nav class="bottom-nav">
      <router-link to="/dashboard" class="nav-item" :class="{ active: route.path === '/dashboard' }">
        <span class="nav-icon">🏠</span>
        <span>首页</span>
      </router-link>
      <router-link to="/today" class="nav-item" :class="{ active: route.path === '/today' }">
        <span class="nav-icon">📝</span>
        <span>今日学习</span>
      </router-link>
      <router-link to="/calendar" class="nav-item" :class="{ active: route.path === '/calendar' }">
        <span class="nav-icon">📅</span>
        <span>打卡日历</span>
      </router-link>
      <router-link to="/profile" class="nav-item" :class="{ active: route.path === '/profile' }">
        <span class="nav-icon">👤</span>
        <span>我的</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<style scoped>
.app-shell {
  max-width: 480px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f7f8fc;
  position: relative;
}
</style>
