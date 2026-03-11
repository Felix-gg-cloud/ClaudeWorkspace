import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'today', name: 'Today', component: () => import('@/views/TodayLessonView.vue') },
        { path: 'calendar', name: 'Calendar', component: () => import('@/views/CalendarView.vue') },
        { path: 'profile', name: 'Profile', component: () => import('@/views/ProfileView.vue') }
      ]
    }
  ]
})

export default router
