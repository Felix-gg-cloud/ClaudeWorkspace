<template>
  <div class="app-shell" @click="userMenuOpen = false">
    <!-- Sidebar (desktop) -->
    <Transition name="sidebar-fade">
      <div v-if="isMobile && sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false" />
    </Transition>

    <aside
      v-show="!isMobile || sidebarOpen"
      class="sidebar"
      :class="{
        'sidebar--mobile': isMobile,
        'sidebar--closed': isMobile && !sidebarOpen,
        'sidebar--open': isMobile && sidebarOpen,
      }"
    >
      <!-- Logo -->
      <div class="sidebar-logo" @click="isMobile && (sidebarOpen = false)">
        <div class="sidebar-logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" /></svg>
        </div>
        <span class="sidebar-logo-text">LinguaLeap</span>
      </div>

      <div class="sidebar-section-label">学习</div>
      <nav class="sidebar-nav">
        <router-link
          v-for="item in mainNav" :key="item.path" :to="item.path"
          class="nav-item"
          :class="{ 'nav-item--active': isActive(item.path) }"
          :style="{ '--item-color': item.color }"
          @click="sidebarOpen = false"
        >
          <span class="nav-item-indicator" />
          <span class="nav-item-icon" :style="isActive(item.path) ? { color: item.color } : {}">
            <component :is="navIcons[item.icon]" />
          </span>
          <span class="nav-item-label">{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-section-label">工具</div>
      <nav class="sidebar-nav">
        <router-link
          v-for="item in toolNav" :key="item.path" :to="item.path"
          class="nav-item"
          :class="{ 'nav-item--active': isActive(item.path) }"
          :style="{ '--item-color': item.color }"
          @click="sidebarOpen = false"
        >
          <span class="nav-item-indicator" />
          <span class="nav-item-icon" :style="isActive(item.path) ? { color: item.color } : {}">
            <component :is="navIcons[item.icon]" />
          </span>
          <span class="nav-item-label">{{ item.label }}</span>
        </router-link>
      </nav>

      <!-- Sidebar footer: user card -->
      <div class="sidebar-footer">
        <div class="sidebar-user" @click.stop="userMenuOpen = !userMenuOpen">
          <span class="sidebar-user-avatar">
            {{ userStore.displayName?.charAt(0)?.toUpperCase() || '?' }}
          </span>
          <div class="sidebar-user-info">
            <span class="sidebar-user-name">{{ userStore.displayName }}</span>
            <span class="sidebar-user-role">{{ userStore.user?.grade || '学习者' }}</span>
          </div>
          <svg class="sidebar-user-chevron" :class="{ 'rotate': userMenuOpen }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
        </div>

        <!-- User dropdown (pops up from sidebar footer) -->
        <Transition name="dropdown">
          <div v-if="userMenuOpen" class="user-dropdown" @click.stop>
            <button class="user-dropdown-item" @click="goTo('/settings')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
              个人设置
            </button>
            <button class="user-dropdown-item" @click="goTo('/stats')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
              学习统计
            </button>
            <button @click="theme.toggle()" class="user-dropdown-item">
              <svg v-if="theme.current.value === 'light'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
              {{ theme.current.value === 'light' ? '深色模式' : '浅色模式' }}
            </button>
            <div class="user-dropdown-divider" />
            <button class="user-dropdown-item user-dropdown-item--danger" @click="handleLogout">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
              退出登录
            </button>
          </div>
        </Transition>
      </div>
    </aside>

    <!-- Top bar (mobile only) -->
    <header v-if="isMobile" class="topbar-mobile">
      <button class="topbar-hamburger" @click="sidebarOpen = !sidebarOpen">
        <span class="hamburger-lines" :class="{ 'menu-open': sidebarOpen }">
          <span /><span /><span />
        </span>
      </button>
      <span class="topbar-mobile-title">LinguaLeap</span>
      <span class="topbar-mobile-avatar" @click.stop="userMenuOpen = !userMenuOpen">
        {{ userStore.displayName?.charAt(0)?.toUpperCase() || '?' }}
      </span>
    </header>

    <!-- Main content -->
    <main class="app-main" :class="{ 'app-main--mobile': isMobile }">
      <router-view v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </router-view>
    </main>

    <!-- Bottom tab bar (mobile) -->
    <nav v-if="isMobile" class="bottom-tabs">
      <router-link
        v-for="tab in bottomTabs" :key="tab.path" :to="tab.path"
        class="bottom-tab"
        :class="{ 'bottom-tab--active': isActive(tab.path) }"
      >
        <span class="bottom-tab-dot" />
        <component :is="navIcons[tab.icon]" class="bottom-tab-icon" />
        <span class="bottom-tab-label">{{ tab.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, h, type FunctionalComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTheme } from '@/stores/theme'

const userStore = useUserStore()
const theme = useTheme()
const route = useRoute()
const router = useRouter()

const windowWidth = ref(window.innerWidth)
const sidebarOpen = ref(false)
const userMenuOpen = ref(false)
const isMobile = computed(() => windowWidth.value < 768)

function onResize() { windowWidth.value = window.innerWidth }
onMounted(() => {
  window.addEventListener('resize', onResize)
  userStore.loadFromCache()
  if (userStore.isLoggedIn) userStore.fetchUser()
})
onUnmounted(() => window.removeEventListener('resize', onResize))

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function goTo(path: string) {
  userMenuOpen.value = false
  router.push(path)
}

function handleLogout() {
  userMenuOpen.value = false
  userStore.logout()
  router.replace('/login')
}

// SVG icon components (inline, no external dependency)
function svgIcon(paths: string, viewBox = '0 0 24 24'): FunctionalComponent {
  return (_props, { attrs }) => h('svg', {
    viewBox, fill: 'none', stroke: 'currentColor',
    'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round',
    ...attrs,
  }, paths.split('|').map(d => {
    if (d.startsWith('C')) return h('circle', { cx: d.split(',')[1], cy: d.split(',')[2], r: d.split(',')[3] })
    if (d.startsWith('L')) { const p = d.split(','); return h('line', { x1: p[1], y1: p[2], x2: p[3], y2: p[4] }) }
    if (d.startsWith('R')) { const p = d.split(','); return h('rect', { x: p[1], y: p[2], width: p[3], height: p[4], rx: p[5] || '0' }) }
    if (d.startsWith('PL')) return h('polyline', { points: d.slice(2) })
    if (d.startsWith('PG')) return h('polygon', { points: d.slice(2) })
    return h('path', { d })
  }))
}

const navIcons: Record<string, FunctionalComponent> = {
  'home': svgIcon('M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z|PL9,22 9,12 15,12 15,22'),
  'upload': svgIcon('PL16,16 12,12 8,16|L,12,12,12,21|M20.39 18.39A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.3'),
  'message-circle': svgIcon('M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z'),
  'layers': svgIcon('PG12,2 2,7 12,12 22,7|PL2,17 12,22 22,17|PL2,12 12,17 22,12'),
  'refresh-cw': svgIcon('PL23,4 23,10 17,10|PL1,20 1,14 7,14|M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15'),
  'x-circle': svgIcon('C,12,12,10|L,15,9,9,15|L,9,9,15,15'),
  'sparkles': svgIcon('M12 3l1.2 3.7a1 1 0 0 0 .6.6L17.5 8.5l-3.7 1.2a1 1 0 0 0-.6.6L12 14l-1.2-3.7a1 1 0 0 0-.6-.6L6.5 8.5l3.7-1.2a1 1 0 0 0 .6-.6z|M5 17l.6 1.8a.5.5 0 0 0 .3.3L7.7 19.7l-1.8.6a.5.5 0 0 0-.3.3L5 22.4l-.6-1.8a.5.5 0 0 0-.3-.3L2.3 19.7l1.8-.6a.5.5 0 0 0 .3-.3z'),
  'bar-chart-2': svgIcon('L,18,20,18,10|L,12,20,12,4|L,6,20,6,14'),
  'settings': svgIcon('C,12,12,3|M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z'),
  'user': svgIcon('M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2|C,12,7,4'),
}

const mainNav = [
  { path: '/', icon: 'home', label: '首页', color: '#6366F1' },
  { path: '/upload', icon: 'upload', label: '上传材料', color: '#10B981' },
  { path: '/chat', icon: 'message-circle', label: 'AI 老师', color: '#EC4899' },
  { path: '/levels', icon: 'layers', label: '知识库', color: '#8B5CF6' },
  { path: '/review', icon: 'refresh-cw', label: '间隔复习', color: '#F59E0B' },
  { path: '/mistakes', icon: 'x-circle', label: '错题本', color: '#EF4444' },
]

const toolNav = [
  { path: '/ai-analyze', icon: 'sparkles', label: 'AI 分析', color: '#06B6D4' },
  { path: '/stats', icon: 'bar-chart-2', label: '学习统计', color: '#6366F1' },
  { path: '/settings', icon: 'settings', label: '设置', color: '#64748B' },
]

const bottomTabs = [
  { path: '/', icon: 'home', label: '首页' },
  { path: '/chat', icon: 'message-circle', label: 'AI 老师' },
  { path: '/levels', icon: 'layers', label: '知识库' },
  { path: '/stats', icon: 'bar-chart-2', label: '统计' },
  { path: '/settings', icon: 'user', label: '我的' },
]
</script>

<style scoped lang="scss">
/* ====== Shell layout ====== */
.app-shell {
  display: flex;
  min-height: 100vh;
  background: var(--bg-page, #F8FAFC);
}

/* ====== Sidebar ====== */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 256px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #0F172A 0%, #1E293B 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  z-index: 90;
  overflow-y: auto;
  padding: 20px 16px;

  &--mobile {
    position: fixed;
    top: 0;
    z-index: 200;
    width: 280px;
    border-radius: 0 24px 24px 0;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
  &--closed { transform: translateX(-100%); }
  &--open { transform: translateX(0); }
}

.sidebar-overlay {
  position: fixed;
  inset: 0;
  z-index: 199;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(4px);
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 8px;
  margin-bottom: 24px;
}

.sidebar-logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 12px rgba(99, 102, 241, 0.3);

  svg { width: 18px; height: 18px; color: white; }
}

.sidebar-logo-text {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: rgba(255, 255, 255, 0.9);
}

.sidebar-section-label {
  padding: 0 12px;
  margin-bottom: 6px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.3);
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 20px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 12px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.55);
  text-decoration: none;
  transition: all 0.15s ease;
  cursor: pointer;

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    color: rgba(255, 255, 255, 0.9);
  }

  &--active {
    background: color-mix(in srgb, var(--item-color, #6366F1) 15%, transparent);
    color: #fff;
    font-weight: 600;

    .nav-item-indicator {
      opacity: 1;
      transform: translateY(-50%) scaleY(1);
    }
  }
}

.nav-item-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%) scaleY(0);
  width: 3px;
  height: 20px;
  border-radius: 0 3px 3px 0;
  background: var(--item-color, #6366F1);
  opacity: 0;
  transition: all 0.2s ease;
}

.nav-item-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.4);
  transition: color 0.15s;

  :deep(svg) { width: 18px; height: 18px; }

  .nav-item:hover & { color: rgba(255, 255, 255, 0.7); }
  .nav-item--active & { color: inherit; }
}

.nav-item-label { flex: 1; }

.nav-item-badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: #EF4444;
  color: white;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ====== Sidebar footer ====== */
.sidebar-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  position: relative;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover { background: rgba(255, 255, 255, 0.06); }
}

.sidebar-user-avatar {
  width: 36px;
  height: 36px;
  min-width: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6, #A78BFA);
  color: white;
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-user-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.sidebar-user-name {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-user-role {
  font-size: 11px;
  color: var(--text-muted, #94A3B8);
}

.sidebar-user-chevron {
  width: 16px;
  height: 16px;
  color: var(--text-muted, #94A3B8);
  transition: transform 0.2s;

  &.rotate { transform: rotate(180deg); }
}

/* ====== User dropdown ====== */
.user-dropdown {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 8px;
  right: 8px;
  background: #1E293B;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 6px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  z-index: 300;
}

.user-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.6);
  background: none;
  border: none;
  cursor: pointer;
  transition: all 0.15s;

  svg { width: 16px; height: 16px; }

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    color: rgba(255, 255, 255, 0.9);
  }

  &--danger {
    color: #EF4444;
    &:hover { background: rgba(239, 68, 68, 0.1); color: #F87171; }
  }
}

.user-dropdown-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.06);
  margin: 4px 8px;
}

/* ====== Mobile topbar ====== */
.topbar-mobile {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: var(--bg-card, #fff);
  border-bottom: 1px solid var(--border, #E2E8F0);
}

.topbar-hamburger {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
}

.hamburger-lines {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 20px;

  span {
    display: block;
    height: 2px;
    background: var(--text-primary, #0F172A);
    border-radius: 2px;
    transition: all 0.3s ease;

    &:nth-child(1) { width: 20px; }
    &:nth-child(2) { width: 14px; }
    &:nth-child(3) { width: 20px; }
  }

  &.menu-open span {
    &:nth-child(1) { transform: rotate(45deg) translate(4px, 4px); }
    &:nth-child(2) { opacity: 0; width: 0; }
    &:nth-child(3) { transform: rotate(-45deg) translate(4px, -4px); }
  }
}

.topbar-mobile-title {
  font-size: 17px;
  font-weight: 800;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.topbar-mobile-avatar {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  color: white;
  font-weight: 700;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

/* ====== Main content ====== */
.app-main {
  margin-left: 256px;
  min-height: 100vh;
  padding: 32px 48px;
  width: calc(100% - 256px);

  &--mobile {
    margin-left: 0;
    width: 100%;
    padding: 72px 16px 88px;
  }
}

/* ====== Bottom tabs (mobile) ====== */
.bottom-tabs {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: var(--bg-card, #fff);
  border-top: 1px solid var(--border, #E2E8F0);
  padding-bottom: env(safe-area-inset-bottom, 0);
}

.bottom-tab {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 16px;
  text-decoration: none;
  color: var(--text-muted, #94A3B8);
  transition: color 0.2s;

  &--active {
    color: #6366F1;

    .bottom-tab-dot {
      opacity: 1;
      transform: scaleX(1);
    }
  }
}

.bottom-tab-dot {
  position: absolute;
  top: 0;
  width: 16px;
  height: 2px;
  border-radius: 1px;
  background: #6366F1;
  opacity: 0;
  transform: scaleX(0);
  transition: all 0.2s;
}

.bottom-tab-icon {
  width: 22px;
  height: 22px;
}

.bottom-tab-label {
  font-size: 11px;
  font-weight: 600;
}

/* ====== Transitions ====== */
.page-enter-active { animation: pageIn 0.3s ease-out; }
.page-leave-active { animation: pageOut 0.15s ease-in; }
@keyframes pageIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
@keyframes pageOut { from { opacity: 1; } to { opacity: 0; } }

.sidebar-fade-enter-active, .sidebar-fade-leave-active { transition: opacity 0.25s ease; }
.sidebar-fade-enter-from, .sidebar-fade-leave-to { opacity: 0; }

.dropdown-enter-active { animation: dropUp 0.2s ease-out; }
.dropdown-leave-active { animation: dropDown 0.15s ease-in; }
@keyframes dropUp { from { opacity: 0; transform: translateY(8px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes dropDown { from { opacity: 1; } to { opacity: 0; transform: translateY(4px) scale(0.97); } }
</style>
