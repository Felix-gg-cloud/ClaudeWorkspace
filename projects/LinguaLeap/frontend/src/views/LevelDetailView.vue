<template>
  <div class="level-detail">
    <button class="back-btn" @click="router.push('/levels')">
      <AppIcon name="arrow-left" :size="16" />
      返回知识库
    </button>

    <div v-if="loading" class="loading-state">
      <div class="spinner" />
      <p>加载中...</p>
    </div>

    <template v-else-if="level">
      <div class="detail-header">
        <div class="level-badge-lg" :class="level.gradeGroup">{{ level.code }}</div>
        <div>
          <h1>{{ level.name }}</h1>
          <p class="detail-desc">{{ level.description }}</p>
        </div>
      </div>

      <!-- 单元列表 -->
      <div v-if="level.units && level.units.length > 0" class="unit-list">
        <div
          v-for="(unit, idx) in level.units"
          :key="unit.id"
          class="unit-card"
          @click="goToLearn(unit.id)"
        >
          <div class="unit-index">{{ idx + 1 }}</div>
          <div class="unit-info">
            <h3>{{ unit.name }}</h3>
            <p v-if="unit.description" class="unit-desc">{{ unit.description }}</p>
            <div class="unit-meta">
              <span>{{ unit.kpCount }} 个知识点</span>
              <span v-if="unit.progress > 0" class="unit-progress-text">
                已掌握 {{ unit.progress }}%
              </span>
            </div>
          </div>
          <div v-if="unit.kpCount > 0" class="unit-status">
            <button
              class="unit-practice-btn"
              @click.stop="goToPractice(unit)"
              title="去练习"
            >
              <AppIcon name="pen-line" :size="16" />
            </button>
            <div v-if="unit.progress >= 100" class="status-done">
              <AppIcon name="check-circle" :size="20" />
            </div>
            <div v-else-if="unit.progress > 0" class="status-progress">
              <div class="circle-progress">
                <svg viewBox="0 0 36 36">
                  <path class="circle-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                  <path class="circle-fill" :stroke-dasharray="`${unit.progress}, 100`" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                </svg>
              </div>
            </div>
            <AppIcon v-else name="chevron-right" :size="18" class="unit-arrow" />
          </div>
        </div>
      </div>

      <!-- 无内容提示 -->
      <div v-else-if="!level.units?.length" class="empty-state">
        <AppIcon name="layers" :size="48" />
        <h3>该级别暂无学习单元</h3>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { levelApi, type LevelDetail } from '@/api/level'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const level = ref<LevelDetail | null>(null)
const levelId = Number(route.params.id)

function goToLearn(unitId: number) {
  router.push(`/learn/${unitId}`)
}

function goToPractice(unit: { id: number; name: string }) {
  router.push({
    path: '/practice',
    query: {
      unitId: String(unit.id),
      unitName: unit.name,
      grade: level.value?.gradeGroup || '',
      levelName: level.value?.name || '',
    },
  })
}

async function loadLevel() {
  loading.value = true
  try {
    const { data } = await levelApi.getDetail(levelId)
    level.value = data.data
  } catch (e) {
    console.error('加载级别详情失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadLevel)
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.level-detail {
  max-width: 680px;
  margin: 0 auto;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 20px;
  cursor: pointer;
  background: none;
  border: none;

  &:hover { color: var(--primary); }
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 32px;

  h1 {
    font-size: 24px;
    font-weight: 800;
    color: var(--text-primary);
    margin-bottom: 4px;
  }

  .detail-desc {
    font-size: 14px;
    color: var(--text-tertiary);
  }
}

.level-badge-lg {
  width: 56px;
  height: 56px;
  border-radius: $radius-xl;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 18px;
  flex-shrink: 0;

  &.primary { background: #e8f5e9; color: #2e7d32; }
  &.junior { background: #e3f2fd; color: #1565c0; }
  &.senior { background: #fce4ec; color: #c62828; }
}

.unit-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.unit-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-xl;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--primary);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.unit-index {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.unit-info {
  flex: 1;
  min-width: 0;

  h3 {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 2px;
  }

  .unit-desc {
    font-size: 13px;
    color: var(--text-tertiary);
    margin-bottom: 4px;
  }

  .unit-meta {
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: var(--text-secondary);
  }

  .unit-progress-text {
    color: var(--primary);
    font-weight: 600;
  }
}

.unit-status {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.unit-practice-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--border);
  background: var(--bg-page);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--primary);
    color: white;
    border-color: var(--primary);
  }
}

.status-done {
  color: var(--success, #4caf50);
}

.circle-progress {
  width: 28px;
  height: 28px;

  svg {
    width: 100%;
    height: 100%;
    transform: rotate(-90deg);
  }

  .circle-bg {
    fill: none;
    stroke: var(--bg-secondary);
    stroke-width: 3;
  }

  .circle-fill {
    fill: none;
    stroke: var(--primary);
    stroke-width: 3;
    stroke-linecap: round;
  }
}

.unit-arrow {
  color: var(--text-tertiary);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  color: var(--text-tertiary);

  h3 {
    margin-top: 12px;
    font-size: 16px;
    color: var(--text-secondary);
  }

  p {
    margin-top: 4px;
    font-size: 14px;
  }
}



.spinner-sm {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  flex-shrink: 0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  color: var(--text-tertiary);
}
</style>
