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
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'today', name: 'Today', component: () => import('@/views/TodayLessonView.vue') },
        { path: 'stages', name: 'Stages', component: () => import('@/views/StageMapView.vue') },
        { path: 'calendar', name: 'Calendar', component: () => import('@/views/CalendarView.vue') },
        { path: 'profile', name: 'Profile', component: () => import('@/views/ProfileView.vue') }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  
  if (to.meta.guest) return true

  if (!userStore.isLoggedIn) {
    const user = await userStore.fetchMe()
    if (!user) return '/login'
  }

  return true
})

export default router
