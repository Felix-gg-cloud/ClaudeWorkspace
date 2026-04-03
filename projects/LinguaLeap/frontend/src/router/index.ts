import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      children: [
        { path: '', name: 'Dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'chat', name: 'Chat', component: () => import('@/views/ChatView.vue'), meta: { title: 'AI 老师' } },
        { path: 'assessment', name: 'Assessment', component: () => import('@/views/AssessmentView.vue'), meta: { title: '入学评估' } },
        { path: 'levels', name: 'Levels', component: () => import('@/views/LevelListView.vue'), meta: { title: '知识库' } },
        { path: 'levels/:id', name: 'LevelDetail', component: () => import('@/views/LevelDetailView.vue'), meta: { title: '级别详情' } },
        { path: 'learn/:unitId', name: 'Learn', component: () => import('@/views/LearnView.vue'), meta: { title: '学习' } },
        { path: 'banks', name: 'Banks', component: () => import('@/views/BankListView.vue'), meta: { title: '题库' } },
        { path: 'banks/:id', name: 'BankDetail', component: () => import('@/views/BankDetailView.vue'), meta: { title: '题库详情' } },
        { path: 'practice', name: 'Practice', component: () => import('@/views/PracticeView.vue'), meta: { title: '练习' } },
        { path: 'review', name: 'Review', component: () => import('@/views/ReviewView.vue'), meta: { title: '复习' } },
        { path: 'mistakes', name: 'Mistakes', component: () => import('@/views/MistakesView.vue'), meta: { title: '错题本' } },
        { path: 'stats', name: 'Stats', component: () => import('@/views/StatsView.vue'), meta: { title: '统计' } },
        { path: 'ai-analyze', name: 'AiAnalyze', component: () => import('@/views/AiAnalyzeView.vue'), meta: { title: 'AI 分析' } },
        { path: 'upload', name: 'Upload', component: () => import('@/views/UploadView.vue'), meta: { title: '上传材料' } },
        { path: 'study-sets/:id', name: 'StudySetDetail', component: () => import('@/views/StudySetDetailView.vue'), meta: { title: '学习集' } },
        { path: 'study-sets/:id/reader', name: 'StudySetReader', component: () => import('@/views/StudySetReaderView.vue'), meta: { title: '原文阅读' } },
        { path: 'settings', name: 'Settings', component: () => import('@/views/SettingsView.vue'), meta: { title: '设置' } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  const token = localStorage.getItem('ll_token')
  if (!token) return { name: 'Login' }
  return true
})

export default router
