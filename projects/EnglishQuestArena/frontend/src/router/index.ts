import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true }
    },
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', name: 'Dashboard', component: () => import('@/views/DashboardView.vue') },
    { path: '/quest', name: 'QuestMap', component: () => import('@/views/quest/QuestMapView.vue') },
    { path: '/quest/today', name: 'QuestToday', component: () => import('@/views/quest/QuestTodayView.vue') },
    { path: '/camp', name: 'Camp', component: () => import('@/views/CampExploreView.vue') },
    { path: '/arena', name: 'Arena', component: () => import('@/views/ArenaView.vue') },
    { path: '/skill-tree', name: 'SkillTree', component: () => import('@/views/SkillTreeView.vue') },
    { path: '/boss', name: 'Boss', component: () => import('@/views/boss/BossView.vue') },
    { path: '/cefr-exam', name: 'CefrExam', component: () => import('@/views/CefrExamView.vue') },
    { path: '/stats', name: 'Stats', component: () => import('@/views/StatsView.vue') },
  ]
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (!to.meta.guest && !userStore.isLoggedIn) {
    return '/login'
  }
})

export default router
