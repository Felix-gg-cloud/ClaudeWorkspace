<template>
  <div class="setup-page">
    <div class="magic-bg">
      <div class="magic-circle circle-1"></div>
      <div class="magic-circle circle-2"></div>
    </div>

    <div class="setup-container">
      <h1 class="setup-title">
        <span class="text-gold">⚔️ 创建你的冒险者</span>
      </h1>
      <p class="setup-subtitle">设定你的角色信息，开始英语冒险之旅！</p>

      <!-- Step indicator -->
      <div class="step-dots">
        <span v-for="s in 3" :key="s" class="dot" :class="{ active: step >= s, current: step === s }"></span>
      </div>

      <!-- Step 1: Name -->
      <div v-if="step === 1" class="step-card dark-panel">
        <h2 class="step-label">📛 你的冒险者名字</h2>
        <input
          v-model="form.displayName"
          class="input-dark"
          placeholder="给自己取个霸气的名字吧"
          maxlength="20"
          @keyup.enter="nextStep"
        />
        <p class="hint">2~20个字符</p>
      </div>

      <!-- Step 2: Avatar -->
      <div v-if="step === 2" class="step-card dark-panel">
        <h2 class="step-label">🎭 选择你的头像</h2>
        <div class="avatar-grid">
          <button
            v-for="a in avatarOptions"
            :key="a"
            class="avatar-option"
            :class="{ selected: form.avatar === a }"
            @click="form.avatar = a"
          >
            {{ a }}
          </button>
        </div>
      </div>

      <!-- Step 3: Level -->
      <div v-if="step === 3" class="step-card dark-panel">
        <h2 class="step-label">📚 你的英语水平</h2>
        <div class="level-options">
          <button
            v-for="opt in levelOptions"
            :key="opt.value"
            class="level-option"
            :class="{ selected: form.cefrLevel === opt.value }"
            @click="form.cefrLevel = opt.value"
          >
            <span class="level-badge">{{ opt.badge }}</span>
            <div class="level-info">
              <div class="level-name">{{ opt.label }}</div>
              <div class="level-desc">{{ opt.desc }}</div>
            </div>
          </button>
        </div>
      </div>

      <!-- Navigation -->
      <div class="step-actions">
        <button v-if="step > 1" class="btn-outline" @click="step--">← 上一步</button>
        <div class="spacer"></div>
        <button v-if="step < 3" class="btn-gold" :disabled="!canNext" @click="nextStep">下一步 →</button>
        <button v-else class="btn-gold" :disabled="saving" @click="saveProfile">
          <span v-if="saving">保存中...</span>
          <span v-else>🚀 开始冒险！</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const step = ref(1)
const saving = ref(false)

const form = ref({
  displayName: userStore.user?.displayName || '',
  avatar: userStore.user?.avatar || '⚔️',
  cefrLevel: userStore.user?.cefrLevel || 'PRE_A1',
})

const avatarOptions = [
  '⚔️', '🛡️', '🏹', '🧙', '🐉', '🦁',
  '🐺', '🦅', '🌟', '🔥', '💎', '🎯',
  '🤖', '👑', '🧝', '🐻', '🦊', '🐱',
  '🎮', '🏰', '🌙', '⭐', '🎪', '🎨',
]

const levelOptions = [
  { value: 'PRE_A1', badge: '🌱', label: 'Pre-A1 零基础', desc: '刚开始学英语，从字母和简单单词起步' },
  { value: 'A1', badge: '📗', label: 'A1 入门级', desc: '认识一些基础单词和简单句子' },
  { value: 'A2', badge: '📘', label: 'A2 初级', desc: '能进行简单日常对话' },
  { value: 'B1', badge: '📙', label: 'B1 中级', desc: '能理解主要内容和表达观点' },
]

const canNext = computed(() => {
  if (step.value === 1) return form.value.displayName.trim().length >= 2
  return true
})

function nextStep() {
  if (canNext.value && step.value < 3) step.value++
}

async function saveProfile() {
  saving.value = true
  const ok = await userStore.updateProfile({
    displayName: form.value.displayName.trim(),
    avatar: form.value.avatar,
    cefrLevel: form.value.cefrLevel,
  })
  saving.value = false
  if (ok) {
    router.replace('/dashboard')
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.setup-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-darker;
  position: relative;
  overflow: hidden;
}

.magic-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.magic-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(245, 158, 11, 0.08);
  animation: spin 30s linear infinite;
}
.circle-1 {
  width: 600px; height: 600px;
  top: -200px; right: -200px;
}
.circle-2 {
  width: 400px; height: 400px;
  bottom: -100px; left: -100px;
  animation-direction: reverse;
}
@keyframes spin { to { transform: rotate(360deg); } }

.setup-container {
  position: relative;
  z-index: 1;
  max-width: 480px;
  width: 100%;
  padding: 20px;
}

.setup-title {
  text-align: center;
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 8px;
}

.setup-subtitle {
  text-align: center;
  color: $text-muted;
  font-size: 14px;
  margin-bottom: 24px;
}

.step-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 24px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  transition: all 0.3s;
  &.active { background: rgba(245, 158, 11, 0.4); }
  &.current { background: $gold; box-shadow: 0 0 8px rgba(245, 158, 11, 0.5); transform: scale(1.2); }
}

.step-card {
  padding: 28px 24px;
  border-radius: $radius-lg;
  margin-bottom: 20px;
}

.step-label {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 16px;
}

.input-dark {
  width: 100%;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid $border-dim;
  border-radius: $radius-md;
  color: $text-primary;
  font-size: 16px;
  outline: none;
  &:focus {
    border-color: $gold;
    box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.15);
  }
}

.hint {
  margin-top: 8px;
  font-size: 12px;
  color: $text-muted;
}

// Avatar grid
.avatar-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}
.avatar-option {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: rgba(255, 255, 255, 0.04);
  border: 2px solid transparent;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(245, 158, 11, 0.2);
  }
  &.selected {
    border-color: $gold;
    background: rgba(245, 158, 11, 0.1);
    box-shadow: 0 0 12px rgba(245, 158, 11, 0.2);
  }
}

// Level options
.level-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.level-option {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 2px solid transparent;
  border-radius: $radius-md;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
  &:hover {
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(245, 158, 11, 0.15);
  }
  &.selected {
    border-color: $gold;
    background: rgba(245, 158, 11, 0.08);
  }
}
.level-badge {
  font-size: 28px;
  flex-shrink: 0;
}
.level-name {
  font-size: 15px;
  font-weight: 700;
  color: $text-primary;
}
.level-desc {
  font-size: 12px;
  color: $text-muted;
  margin-top: 2px;
}

// Actions
.step-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.spacer { flex: 1; }

.btn-gold {
  padding: 12px 28px;
  background: linear-gradient(135deg, $gold-dark, $gold);
  border: none;
  border-radius: $radius-md;
  color: #1a1a2e;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  &:hover:not(:disabled) { box-shadow: 0 4px 16px rgba(245, 158, 11, 0.4); transform: translateY(-1px); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.btn-outline {
  padding: 12px 20px;
  background: transparent;
  border: 1px solid $border-dim;
  border-radius: $radius-md;
  color: $text-secondary;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { border-color: $gold; color: $gold-light; }
}
</style>
