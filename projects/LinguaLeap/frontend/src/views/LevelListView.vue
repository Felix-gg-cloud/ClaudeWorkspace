<template>
  <div class="level-list">
    <div class="page-header">
      <h1>知识库</h1>
      <p class="page-desc">AI 提取的知识 + 系统分级课程，全面掌握英语</p>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner" />
      <p>加载中...</p>
    </div>

    <template v-else>
      <!-- 我的学习集区域 -->
      <div class="section my-study-sets">
        <div class="section-header">
          <h2>
            <AppIcon name="sparkles" :size="18" />
            我的学习集
          </h2>
          <router-link to="/upload" class="btn-upload">
            <AppIcon name="plus" :size="14" />
            上传材料
          </router-link>
        </div>

        <div v-if="studySets.length === 0" class="empty-upload-hint" @click="router.push('/upload')">
          <div class="hint-icon">
            <AppIcon name="upload" :size="32" />
          </div>
          <div class="hint-text">
            <h3>还没有学习集</h3>
            <p>上传课文、文章或词表，AI 自动提取知识点并生成练习</p>
          </div>
          <AppIcon name="chevron-right" :size="18" class="hint-arrow" />
        </div>

        <div v-else class="study-set-grid">
          <div
            v-for="s in studySets"
            :key="s.id"
            class="study-set-card"
            @click="router.push(`/study-sets/${s.id}`)"
          >
            <div class="ss-card-top">
              <span class="ss-status" :class="s.status">
                {{ ssStatusLabel[s.status] || s.status }}
              </span>
              <span class="ss-date">{{ formatDate(s.createdAt) }}</span>
            </div>
            <h3 class="ss-title">{{ s.title }}</h3>
            <p v-if="s.aiSummary" class="ss-summary">{{ s.aiSummary }}</p>
            <div class="ss-card-bottom">
              <span class="ss-stat">
                <AppIcon name="layers" :size="13" />
                {{ s.itemCount }} 个知识点
              </span>
              <span v-if="s.grade" class="ss-grade">{{ gradeLabel(s.grade) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分级知识库区域 -->
      <div class="section system-levels">
        <div class="section-header">
          <h2>
            <AppIcon name="book-open" :size="18" />
            系统课程
          </h2>
        </div>

        <div
          v-for="(group, groupName) in groupedLevels"
          :key="groupName"
          class="level-group"
        >
          <h3 class="group-title">{{ groupLabel(groupName) }}</h3>
          <div class="level-grid">
            <div
              v-for="level in group"
              :key="level.id"
              class="level-card"
              :class="{ recommended: level.code === recommendedCode }"
              @click="goToLevel(level)"
            >
              <div class="lv-card-top">
                <span class="lv-badge" :class="groupName">{{ level.code }}</span>
                <span v-if="level.code === recommendedCode" class="lv-recommended">⭐ 推荐</span>
                <span v-if="level.progress > 0" class="lv-progress">{{ level.progress }}%</span>
              </div>
              <h3 class="lv-title">{{ level.name }}</h3>
              <p v-if="level.description" class="lv-desc">{{ level.description }}</p>
              <div class="lv-card-bottom">
                <span class="lv-stat">
                  <AppIcon name="layers" :size="13" />
                  {{ level.unitCount > 0 ? level.unitCount + ' 个单元' : '暂无内容' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { levelApi, type KnowledgeLevel } from '@/api/level'
import { studySetApi, type StudySet } from '@/api/studySet'
import { orchestratorApi } from '@/api/teacher'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const loading = ref(false)
const levels = ref<KnowledgeLevel[]>([])
const studySets = ref<StudySet[]>([])
const recommendedCode = ref<string | null>(null)

const ssStatusLabel: Record<string, string> = {
  ready: '可用',
  processing: '处理中',
  failed: '失败',
}

const groupedLevels = computed(() => {
  const groups: Record<string, KnowledgeLevel[]> = {}
  for (const level of levels.value) {
    const g = level.gradeGroup || 'other'
    if (!groups[g]) groups[g] = []
    groups[g].push(level)
  }
  return groups
})

function gradeLabel(grade: string) {
  const labels: Record<string, string> = { primary: '小学', junior: '初中', senior: '高中' }
  return labels[grade] || grade
}

function groupLabel(group: string) {
  return gradeLabel(group)
}

function goToLevel(level: KnowledgeLevel) {
  router.push(`/levels/${level.id}`)
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

onMounted(async () => {
  loading.value = true
  try {
    const [levelsRes, setsRes, planRes] = await Promise.allSettled([
      levelApi.list(),
      studySetApi.list(),
      orchestratorApi.getPlan(),
    ])
    if (levelsRes.status === 'fulfilled') levels.value = levelsRes.value.data.data
    if (setsRes.status === 'fulfilled') studySets.value = setsRes.value.data.data || []
    if (planRes.status === 'fulfilled') recommendedCode.value = planRes.value.data.data?.levelCode || null
  } catch (e) {
    console.error('加载知识库失败', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.level-list {
  max-width: 780px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 28px;

  h1 {
    font-size: 28px;
    font-weight: 800;
    color: var(--text-primary);
    margin-bottom: 4px;
  }

  .page-desc {
    color: var(--text-tertiary);
    font-size: 14px;
  }
}

// Section layout
.section {
  margin-bottom: 36px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h2 {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 17px;
    font-weight: 700;
    color: var(--text-primary);
  }
}

.btn-upload {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  background: var(--primary);
  color: white;
  text-decoration: none;
  transition: all 0.2s;

  &:hover { opacity: 0.9; transform: translateY(-1px); }
}

// Empty upload hint
.empty-upload-hint {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border: 2px dashed var(--border);
  border-radius: $radius-xl;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--primary);
    background: rgba(99, 102, 241, 0.03);
  }

  .hint-icon {
    width: 56px;
    height: 56px;
    border-radius: $radius-lg;
    background: rgba(99, 102, 241, 0.08);
    color: var(--primary);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .hint-text {
    flex: 1;
    h3 {
      font-size: 15px;
      font-weight: 600;
      color: var(--text-primary);
      margin-bottom: 4px;
    }
    p {
      font-size: 13px;
      color: var(--text-tertiary);
    }
  }

  .hint-arrow {
    color: var(--text-tertiary);
    flex-shrink: 0;
  }
}

// Study set cards
.study-set-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.study-set-card {
  padding: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-xl;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--primary);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    transform: translateY(-2px);
  }

  .ss-card-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }

  .ss-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .ss-summary {
    font-size: 12px;
    color: var(--text-tertiary);
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin-bottom: 10px;
  }

  .ss-card-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.ss-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;

  &.ready { background: rgba(16, 185, 129, 0.12); color: #10B981; }
  &.processing { background: rgba(245, 158, 11, 0.12); color: #F59E0B; }
  &.failed { background: rgba(239, 68, 68, 0.12); color: #EF4444; }
}

.ss-date {
  font-size: 11px;
  color: var(--text-tertiary);
}

.ss-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.ss-grade {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
}

// System levels section — same grid as study sets
.level-group {
  margin-bottom: 24px;
}

.group-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-secondary);
  margin-bottom: 10px;
  padding-left: 4px;
}

.level-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.level-card {
  padding: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: $radius-xl;
  cursor: pointer;
  transition: all 0.2s;

  &.recommended {
    border-color: var(--primary);
    box-shadow: 0 0 0 1px var(--primary), 0 2px 12px rgba(99, 102, 241, 0.15);
  }

  &:hover {
    border-color: var(--primary);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    transform: translateY(-2px);
  }

  .lv-card-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    gap: 6px;
  }

  .lv-recommended {
    font-size: 11px;
    color: var(--primary);
    font-weight: 600;
    margin-right: auto;
  }

  .lv-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .lv-desc {
    font-size: 12px;
    color: var(--text-tertiary);
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin-bottom: 10px;
  }

  .lv-card-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.lv-badge {
  font-size: 12px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 6px;

  &.primary { background: #e8f5e9; color: #2e7d32; }
  &.junior { background: #e3f2fd; color: #1565c0; }
  &.senior { background: #fce4ec; color: #c62828; }
}

.lv-progress {
  font-size: 12px;
  font-weight: 600;
  color: var(--primary);
}

.lv-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  color: var(--text-tertiary);
}

@media (max-width: 640px) {
  .study-set-grid,
  .level-grid {
    grid-template-columns: 1fr;
  }
}
</style>
