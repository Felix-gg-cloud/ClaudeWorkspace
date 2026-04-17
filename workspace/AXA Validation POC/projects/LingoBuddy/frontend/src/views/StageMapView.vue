<template>
  <div class="page-container">
    <div class="map-header">
      <h1 class="map-title">
        <span class="title-icon">🗺️</span>
        <span class="text-gradient">学习旅程</span>
      </h1>
      <p class="map-sub">完成阶段解锁新旅程</p>
    </div>

    <!-- Level Bar -->
    <div class="glass-card level-bar">
      <div class="level-row">
        <span class="lv-badge">Lv.{{ userLevel }}</span>
        <span class="lv-title">{{ userLevelTitle }}</span>
      </div>
      <div class="progress-bar" style="margin-top: 8px;">
        <div class="progress-fill" :style="{ width: xpPercent + '%' }"></div>
      </div>
      <div class="lv-xp-text">{{ userXp }} / {{ nextLevelXp }} XP</div>
    </div>

    <!-- Stage Path -->
    <div class="stage-path">
      <div
        v-for="(stage, idx) in stages"
        :key="idx"
        class="stage-item"
      >
        <!-- Connector line -->
        <div v-if="idx > 0" class="connector" :class="{ active: stage.status !== 'locked' }"></div>

        <div class="stage-node" :class="stage.status">
          <div class="node-ring">
            <div class="node-inner">
              <span class="node-icon">{{ stage.icon }}</span>
            </div>
          </div>
          <div class="node-info">
            <div class="node-name">{{ stage.name }}</div>
            <div class="node-level">等级 {{ stage.levelId }} · 阶段 {{ stage.sortOrder }}</div>
            <div class="node-status-tag" :class="stage.status">
              {{ statusText(stage.status) }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Level Milestones -->
    <div class="section-header">
      <h3>🎖️ 等级里程碑</h3>
    </div>
    <div class="milestones">
      <div
        v-for="lv in levels"
        :key="lv.level"
        class="glass-card milestone"
        :class="{ reached: userLevel >= lv.level }"
      >
        <div class="ms-icon">{{ lv.title.split(' ')[0] }}</div>
        <div class="ms-body">
          <div class="ms-name">{{ lv.title.split(' ').slice(1).join(' ') }}</div>
          <div class="ms-desc">{{ lv.description }}</div>
        </div>
        <div class="ms-xp">{{ lv.requiredXp }} XP</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userLevel = computed(() => userStore.user?.currentLevel || 1)
const userXp = computed(() => userStore.user?.totalXp || 0)
const userLevelTitle = computed(() => userStore.user?.levelTitle || '🌱 萌芽学徒')

const levels = [
  { level: 1, requiredXp: 0, title: '🌱 萌芽学徒', description: '英语学习之旅刚刚开始' },
  { level: 2, requiredXp: 200, title: '📖 初级读者', description: '掌握了基础词汇和句型' },
  { level: 3, requiredXp: 500, title: '🗣️ 对话新手', description: '能进行简单的日常对话' },
  { level: 4, requiredXp: 1000, title: '✍️ 写作入门', description: '能写出简短的英文段落' },
  { level: 5, requiredXp: 1800, title: '🎯 中级达人', description: '英语应用能力显著提升' },
  { level: 6, requiredXp: 3000, title: '🌟 高阶学者', description: '能流利阅读英文文章' },
  { level: 7, requiredXp: 5000, title: '🏆 英语大师', description: '已经是英语达人了！' },
]

const stageData = [
  { name: '日常基础', levelId: 1, sortOrder: 1, icon: '☀️' },
  { name: '问候与自我介绍', levelId: 1, sortOrder: 2, icon: '👋' },
  { name: '购物与餐饮', levelId: 2, sortOrder: 3, icon: '🛍️' },
  { name: '交通与出行', levelId: 2, sortOrder: 4, icon: '🚌' },
  { name: '职场英语', levelId: 3, sortOrder: 5, icon: '💼' },
]

const stages = computed(() => {
  return stageData.map(s => {
    let status: 'completed' | 'current' | 'locked'
    if (s.levelId < userLevel.value) {
      status = 'completed'
    } else if (s.levelId === userLevel.value) {
      status = 'current'
    } else {
      status = 'locked'
    }
    return { ...s, status }
  })
})

const nextLevelXp = computed(() => {
  const next = levels.find(l => l.level === userLevel.value + 1)
  return next ? next.requiredXp : levels[levels.length - 1].requiredXp
})

const xpPercent = computed(() => {
  const currentLv = levels.find(l => l.level === userLevel.value)
  const currentReq = currentLv ? currentLv.requiredXp : 0
  const range = nextLevelXp.value - currentReq
  if (range <= 0) return 100
  return Math.min(100, ((userXp.value - currentReq) / range) * 100)
})

function statusText(status: string) {
  if (status === 'completed') return '已完成'
  if (status === 'current') return '进行中'
  return '已锁定'
}
</script>

<style scoped lang="scss">
.map-header {
  padding: 28px 0 8px;
}

.map-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  font-weight: 800;
}

.title-icon { font-size: 26px; }

.map-sub {
  font-size: 13px;
  color: #5a6480;
  margin-top: 4px;
}

// Level bar
.level-bar { margin-bottom: 20px; }

.level-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.lv-badge {
  background: linear-gradient(135deg, #00d4ff, #a855f7);
  color: #080b1a;
  font-size: 12px;
  font-weight: 800;
  padding: 3px 10px;
  border-radius: 8px;
}

.lv-title {
  font-size: 15px;
  font-weight: 700;
  color: #e8ecf4;
}

.lv-xp-text {
  font-size: 11px;
  color: #5a6480;
  margin-top: 4px;
  text-align: right;
}

// Stage path
.stage-path {
  display: flex;
  flex-direction: column;
  padding-left: 20px;
  margin-bottom: 28px;
}

.stage-item {
  position: relative;
}

.connector {
  width: 3px;
  height: 28px;
  margin-left: 24px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 2px;

  &.active {
    background: linear-gradient(180deg, #00d4ff, #a855f7);
    box-shadow: 0 0 8px rgba(0, 212, 255, 0.3);
  }
}

.stage-node {
  display: flex;
  align-items: center;
  gap: 16px;
}

.node-ring {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  padding: 3px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.06);
  transition: all 0.3s;
}

.completed .node-ring {
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow: 0 0 16px rgba(16, 185, 129, 0.3);
}

.current .node-ring {
  background: conic-gradient(#00d4ff, #a855f7, #ec4899, #00d4ff);
  animation: glow-ring 4s linear infinite;
  box-shadow: 0 0 20px rgba(0, 212, 255, 0.3);
}

.locked .node-ring {
  opacity: 0.4;
}

.node-inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #0c1029;
  display: flex;
  align-items: center;
  justify-content: center;
}

.node-icon {
  font-size: 22px;
}

.locked .node-icon {
  filter: grayscale(0.8);
  opacity: 0.5;
}

.node-info { flex: 1; }

.node-name {
  font-size: 15px;
  font-weight: 700;
  color: #e8ecf4;
}

.locked .node-name { color: #3d4663; }

.node-level {
  font-size: 12px;
  color: #5a6480;
  margin-top: 2px;
}

.node-status-tag {
  display: inline-block;
  margin-top: 4px;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 8px;

  &.completed {
    background: rgba(16, 185, 129, 0.12);
    color: #10b981;
    border: 1px solid rgba(16, 185, 129, 0.2);
  }

  &.current {
    background: rgba(0, 212, 255, 0.1);
    color: #00d4ff;
    border: 1px solid rgba(0, 212, 255, 0.2);
    animation: pulse-glow 2s infinite;
  }

  &.locked {
    background: rgba(255, 255, 255, 0.03);
    color: #3d4663;
    border: 1px solid rgba(255, 255, 255, 0.05);
  }
}

// Milestones
.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;

  h3 {
    font-size: 18px;
    font-weight: 700;
    color: #e8ecf4;
  }
}

.milestones {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-bottom: 24px;
}

.milestone {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  transition: all 0.3s;

  &:not(.reached) {
    opacity: 0.4;
  }

  &.reached {
    border-color: rgba(0, 212, 255, 0.15);
  }
}

.ms-icon {
  font-size: 26px;
  flex-shrink: 0;
}

.ms-body { flex: 1; }

.ms-name {
  font-size: 14px;
  font-weight: 700;
  color: #e8ecf4;
}

.ms-desc {
  font-size: 12px;
  color: #5a6480;
  margin-top: 1px;
}

.ms-xp {
  font-size: 12px;
  font-weight: 700;
  color: #5a6480;
  white-space: nowrap;
}

.milestone.reached .ms-xp {
  color: #00d4ff;
}
</style>
