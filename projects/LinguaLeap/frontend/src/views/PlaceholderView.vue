<template>
  <div class="placeholder">
    <div class="placeholder__card">
      <span class="placeholder__icon">
        <AppIcon name="construction" :size="52" />
      </span>
      <h2 class="placeholder__title">{{ title }}</h2>
      <p class="placeholder__desc">此功能将在后续版本中上线，敬请期待！</p>
      <router-link to="/" class="placeholder__back">
        <AppIcon name="arrow-left" :size="16" />
        返回首页
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'

const route = useRoute()

const titles: Record<string, string> = {
  '/banks': '题库中心',
  '/practice': '学习练习',
  '/review': '间隔复习',
  '/mistakes': '错题本',
  '/stats': '学习统计',
  '/settings': '设置',
}

const title = computed(() => titles[route.path] || '功能开发中')
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 55vh;
}

.placeholder__card {
  text-align: center;
  padding: 48px 32px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-2xl;
  max-width: 380px;
  animation: cardIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(16px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.placeholder__icon {
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.placeholder__title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.placeholder__desc {
  font-size: 15px;
  color: var(--text-muted);
  margin-bottom: 28px;
  line-height: 1.6;
}

.placeholder__back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--primary);
  text-decoration: none;
  padding: 10px 20px;
  border-radius: $radius-lg;
  background: var(--primary-light);
  transition: all $transition;

  &:hover {
    background: var(--primary-glow);
    transform: translateX(-2px);
  }
}
</style>
