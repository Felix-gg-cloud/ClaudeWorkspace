<template>
  <div class="app-layout" :class="{ 'sidebar-collapsed': collapsed }">
    <!-- Ambient particles -->
    <div class="ambient-particles">
      <div v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)"></div>
    </div>

    <!-- Left Sidebar -->
    <aside class="sidebar">
      <!-- Logo -->
      <div class="sidebar__logo">
        <router-link to="/dashboard" class="logo">
          <span class="logo-icon">⚔️</span>
          <span v-show="!collapsed" class="logo-text text-gold">EnglishQuest</span>
        </router-link>
      </div>

      <!-- Nav links -->
      <nav class="sidebar__nav">
        <router-link to="/dashboard" class="nav-link" active-class="nav-link--active">
          <span class="nav-icon">🏰</span>
          <span v-show="!collapsed" class="nav-label">大厅</span>
        </router-link>
        <router-link to="/camp" class="nav-link" active-class="nav-link--active">
          <span class="nav-icon">🏕️</span>
          <span v-show="!collapsed" class="nav-label">营地</span>
        </router-link>
        <router-link to="/quest" class="nav-link" active-class="nav-link--active">
          <span class="nav-icon">⚔️</span>
          <span v-show="!collapsed" class="nav-label">主线</span>
        </router-link>
        <router-link to="/arena" class="nav-link" active-class="nav-link--active">
          <span class="nav-icon">🏟️</span>
          <span v-show="!collapsed" class="nav-label">副本</span>
        </router-link>
        <router-link to="/skill-tree" class="nav-link" active-class="nav-link--active">
          <span class="nav-icon">🌟</span>
          <span v-show="!collapsed" class="nav-label">技能树</span>
        </router-link>
        <router-link to="/boss" class="nav-link" active-class="nav-link--active">
          <span class="nav-icon">💀</span>
          <span v-show="!collapsed" class="nav-label">Boss</span>
        </router-link>
        <router-link to="/cefr-exam" class="nav-link" active-class="nav-link--active">
          <span class="nav-icon">�</span>
          <span v-show="!collapsed" class="nav-label">试炼之门</span>
        </router-link>
      </nav>

      <!-- Collapse toggle -->
      <button class="sidebar__toggle" @click="collapsed = !collapsed" :title="collapsed ? '展开菜单' : '收起菜单'">
        <span class="toggle-icon">{{ collapsed ? '»' : '«' }}</span>
      </button>
    </aside>

    <!-- Top Bar (stats only) -->
    <header class="top-bar">
      <div class="top-bar__right">
        <div class="stat-chip" :class="{ 'stat-bump': xpBump }" data-tip="经验值">
          <span class="stat-icon">✨</span>
          <span class="stat-value">{{ userStore.totalXp }}</span>
        </div>
        <div class="stat-chip" :class="{ 'stat-bump': coinsBump }" data-tip="金币">
          <span class="stat-icon">💰</span>
          <span class="stat-value">{{ userStore.coins }}</span>
        </div>
        <div class="stat-chip" data-tip="连续打卡">
          <span class="stat-icon">📅</span>
          <span class="stat-value">{{ userStore.streak }}</span>
        </div>
        <!-- Avatar dropdown -->
        <div class="avatar-dropdown-wrap" ref="dropdownRef">
          <div class="avatar tooltip" data-tip="个人设置" @click="showDropdown = !showDropdown">
            {{ userStore.avatar }}
          </div>
          <Transition name="dropdown">
            <div v-if="showDropdown" class="avatar-dropdown dark-panel">
              <div class="dropdown-header">
                <div class="dropdown-avatar">{{ userStore.avatar }}</div>
                <div class="dropdown-info">
                  <div class="dropdown-name">{{ userStore.displayName }}</div>
                  <div class="dropdown-username">@{{ userStore.username }}</div>
                </div>
              </div>
              <div class="dropdown-divider"></div>
              <div class="dropdown-stats">
                <span>⭐ Lv.{{ userStore.currentLevel }}</span>
                <span>✨ {{ userStore.totalXp }} XP</span>
                <span>💰 {{ userStore.coins }}</span>
              </div>
              <div class="dropdown-divider"></div>
              <router-link to="/profile" class="dropdown-item" @click="showDropdown = false">
                <span>👤</span> 个人资料
              </router-link>
              <router-link to="/stats" class="dropdown-item" @click="showDropdown = false">
                <span>📊</span> 学习统计
              </router-link>
              <div class="dropdown-divider"></div>
              <button class="dropdown-item dropdown-item--danger" @click="handleLogout">
                <span>🚪</span> 退出登录
              </button>
            </div>
          </Transition>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-content">
      <slot />
    </main>

    <!-- Debug Panel (Ctrl+D 切换) -->
    <div v-if="showDebug && debugLogs.length" class="debug-panel">
      <div class="debug-title">🐛 Debug (Ctrl+D 隐藏) <button class="debug-clear" @click="debugLogs.length = 0">✕ 清除</button></div>
      <div v-for="(log, i) in debugLogs" :key="i" class="debug-line">{{ log }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { debugLogs } from '@/utils/debugLog'

const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)
const showDropdown = ref(false)
const showDebug = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)

// 数值变化时触发弹跳动画
const xpBump = ref(false)
const coinsBump = ref(false)

watch(() => userStore.totalXp, (newVal, oldVal) => {
  debugLogs.push(`XP: ${oldVal} → ${newVal}`)
  xpBump.value = true
  setTimeout(() => { xpBump.value = false }, 400)
})
watch(() => userStore.coins, (newVal, oldVal) => {
  debugLogs.push(`💰: ${oldVal} → ${newVal}`)
  coinsBump.value = true
  setTimeout(() => { coinsBump.value = false }, 400)
})

function handleClickOutside(e: MouseEvent) {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target as Node)) {
    showDropdown.value = false
  }
}

function handleDebugToggle(e: KeyboardEvent) {
  if (e.ctrlKey && e.key === 'd') { e.preventDefault(); showDebug.value = !showDebug.value }
}

onMounted(() => { document.addEventListener('click', handleClickOutside); document.addEventListener('keydown', handleDebugToggle) })
onUnmounted(() => { document.removeEventListener('click', handleClickOutside); document.removeEventListener('keydown', handleDebugToggle) })

async function handleLogout() {
  showDropdown.value = false
  await userStore.logout()
  router.replace('/login')
}

function particleStyle(i: number) {
  const size = 2 + Math.random() * 3
  const x = Math.random() * 100
  const y = Math.random() * 100
  const delay = Math.random() * 8
  const duration = 6 + Math.random() * 8
  const opacity = 0.1 + Math.random() * 0.3
  return {
    width: size + 'px',
    height: size + 'px',
    left: x + '%',
    top: y + '%',
    animationDelay: delay + 's',
    animationDuration: duration + 's',
    opacity
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.debug-panel {
  position: fixed;
  bottom: 8px;
  right: 8px;
  z-index: 9999;
  background: rgba(0,0,0,0.85);
  color: #0f0;
  font-family: monospace;
  font-size: 11px;
  padding: 6px 10px;
  border-radius: 6px;
  max-width: 420px;
  max-height: 200px;
  overflow-y: auto;
  cursor: pointer;
  .debug-title { color: #ff0; margin-bottom: 2px; display: flex; justify-content: space-between; align-items: center; }
  .debug-clear { background: #c00; color: #fff; border: none; border-radius: 3px; padding: 1px 6px; cursor: pointer; font-size: 10px; }
  .debug-line { white-space: pre-wrap; line-height: 1.4; border-bottom: 1px solid #333; padding: 1px 0; user-select: text; }
}

$sidebar-width: 200px;
$sidebar-collapsed: 60px;
$topbar-height: 48px;

.app-layout {
  min-height: 100vh;
  position: relative;
}

// Ambient particles
.ambient-particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: $gold;
  animation: particle-float infinite ease-in-out;
}

@keyframes particle-float {
  0%, 100% { transform: translateY(0) translateX(0); }
  25% { transform: translateY(-20px) translateX(10px); }
  50% { transform: translateY(-10px) translateX(-10px); }
  75% { transform: translateY(-30px) translateX(5px); }
}

// ─── Sidebar ───
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: $sidebar-width;
  display: flex;
  flex-direction: column;
  background: rgba(10, 14, 26, 0.95);
  backdrop-filter: blur(16px);
  border-right: 1px solid $border-dim;
  z-index: 110;
  transition: width 0.25s $ease-smooth;

  .sidebar-collapsed & {
    width: $sidebar-collapsed;
  }
}

.sidebar__logo {
  height: $topbar-height;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid $border-dim;
  flex-shrink: 0;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 800;
  white-space: nowrap;
}
.logo-icon { font-size: 22px; flex-shrink: 0; }

.sidebar__nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 8px;
  overflow-y: auto;
  overflow-x: hidden;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: $radius-md;
  font-size: 14px;
  font-weight: 600;
  color: $text-muted;
  transition: all 0.2s;
  white-space: nowrap;
  overflow: hidden;

  &:hover {
    color: $text-secondary;
    background: rgba(255, 255, 255, 0.05);
  }

  &--active {
    color: $gold-light !important;
    background: rgba(245, 158, 11, 0.1) !important;
    border: 1px solid rgba(245, 158, 11, 0.15);
  }
}

.nav-icon {
  font-size: 24px;
  flex-shrink: 0;
  width: 24px;
  text-align: center;
}
.nav-label { font-size: 15px; }

.sidebar__toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  margin: 8px;
  border: 1px solid $border-dim;
  border-radius: $radius-md;
  background: rgba(255, 255, 255, 0.03);
  color: $text-muted;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;

  &:hover {
    color: $gold-light;
    background: rgba(245, 158, 11, 0.06);
    border-color: rgba(245, 158, 11, 0.2);
  }
}
.toggle-icon {
  font-size: 16px;
  font-weight: 800;
}

// ─── Top Bar ───
.top-bar {
  position: fixed;
  top: 0;
  left: $sidebar-width;
  right: 0;
  height: $topbar-height;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
  background: rgba(10, 14, 26, 0.85);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid $border-dim;
  z-index: 100;
  transition: left 0.25s $ease-smooth;

  .sidebar-collapsed & {
    left: $sidebar-collapsed;
  }
}

.top-bar__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid $border-dim;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: $text-secondary;
  cursor: default;
  transition: transform 0.15s, color 0.15s;

  &.stat-bump {
    animation: statBump 0.4s ease;
    color: $gold-light;
  }
}

@keyframes statBump {
  0% { transform: scale(1); }
  30% { transform: scale(1.25); }
  60% { transform: scale(0.95); }
  100% { transform: scale(1); }
}

.stat-icon { font-size: 14px; }

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, $gold-dark, $magic-purple);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid rgba(245, 158, 11, 0.3);

  &:hover {
    border-color: $gold;
    box-shadow: 0 0 12px rgba(245, 158, 11, 0.3);
  }
}

// Avatar dropdown
.avatar-dropdown-wrap {
  position: relative;
}

.avatar-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 260px;
  padding: 12px 0;
  border-radius: $radius-lg;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  z-index: 200;
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px 12px;
}
.dropdown-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(139, 92, 246, 0.2));
  border: 2px solid rgba(245, 158, 11, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}
.dropdown-name {
  font-size: 15px;
  font-weight: 700;
  color: $text-primary;
}
.dropdown-username {
  font-size: 12px;
  color: $text-muted;
}

.dropdown-stats {
  display: flex;
  justify-content: space-around;
  padding: 8px 16px;
  font-size: 12px;
  font-weight: 600;
  color: $text-secondary;
}

.dropdown-divider {
  height: 1px;
  background: $border-dim;
  margin: 4px 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 600;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.15s;
  background: none;
  border: none;
  width: 100%;
  text-align: left;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
    color: $text-primary;
  }

  &--danger {
    color: #ef4444;
    &:hover { background: rgba(239, 68, 68, 0.08); color: #f87171; }
  }
}

// Dropdown transition
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}

// ─── Main content ───
.main-content {
  margin-left: $sidebar-width;
  padding-top: $topbar-height;
  min-height: 100vh;
  position: relative;
  z-index: 1;
  transition: margin-left 0.25s $ease-smooth;

  .sidebar-collapsed & {
    margin-left: $sidebar-collapsed;
  }
}
</style>
