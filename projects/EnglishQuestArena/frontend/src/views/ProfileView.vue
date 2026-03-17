<template>
  <MainLayout>
    <div class="profile-page">
      <h2 class="page-title">👤 个人资料</h2>

      <div class="profile-card dark-panel">
        <!-- Avatar + Name -->
        <div class="profile-header">
          <div class="profile-avatar-wrap">
            <div class="profile-avatar-display">{{ form.avatar }}</div>
          </div>
          <div class="profile-meta">
            <div class="profile-name">{{ user?.displayName }}</div>
            <div class="profile-username">@{{ user?.username }}</div>
            <div class="profile-level-badge">{{ levelLabel }}</div>
          </div>
        </div>

        <!-- Stats strip -->
        <div class="stats-strip">
          <div class="strip-item"><span class="strip-icon">⭐</span><span class="strip-val">Lv.{{ user?.currentLevel }}</span></div>
          <div class="strip-item"><span class="strip-icon">✨</span><span class="strip-val">{{ user?.totalXp }} XP</span></div>
          <div class="strip-item"><span class="strip-icon">🪙</span><span class="strip-val">{{ user?.coins }}</span></div>
          <div class="strip-item"><span class="strip-icon">🔥</span><span class="strip-val">{{ user?.streak }} 天</span></div>
          <div class="strip-item"><span class="strip-icon">📅</span><span class="strip-val">{{ user?.totalCheckins }} 签</span></div>
        </div>
      </div>

      <!-- Edit form -->
      <div class="edit-section dark-panel">
        <h3 class="section-title">✏️ 编辑资料</h3>

        <div class="field">
          <label class="field-label">冒险者名字</label>
          <input v-model="form.displayName" class="input-dark" maxlength="20" />
        </div>

        <div class="field">
          <label class="field-label">头像</label>
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

        <div class="field">
          <label class="field-label">英语水平</label>
          <div class="level-select">
            <button
              v-for="opt in levelOptions"
              :key="opt.value"
              class="level-chip"
              :class="{ selected: form.cefrLevel === opt.value }"
              @click="form.cefrLevel = opt.value"
            >
              {{ opt.badge }} {{ opt.label }}
            </button>
          </div>
        </div>

        <div class="field">
          <label class="field-label">TTS 发音</label>
          <div class="level-select">
            <button class="level-chip" :class="{ selected: form.ttsVoice === 'en-US' }" @click="form.ttsVoice = 'en-US'">🇺🇸 美式</button>
            <button class="level-chip" :class="{ selected: form.ttsVoice === 'en-GB' }" @click="form.ttsVoice = 'en-GB'">🇬🇧 英式</button>
          </div>
        </div>

        <div class="form-actions">
          <button class="btn-gold" :disabled="saving || !hasChanges" @click="save">
            {{ saving ? '保存中...' : '💾 保存修改' }}
          </button>
          <span v-if="saved" class="save-ok">✅ 已保存</span>
        </div>
      </div>

      <button class="back-btn" @click="$router.push('/dashboard')">← 返回主页</button>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const user = computed(() => userStore.user)

const saving = ref(false)
const saved = ref(false)

const form = ref({
  displayName: user.value?.displayName || '',
  avatar: user.value?.avatar || '⚔️',
  cefrLevel: user.value?.cefrLevel || 'PRE_A1',
  ttsVoice: user.value?.ttsVoice || 'en-US',
})

const avatarOptions = [
  '⚔️', '🛡️', '🏹', '🧙', '🐉', '🦁',
  '🐺', '🦅', '🌟', '🔥', '💎', '🎯',
  '🤖', '👑', '🧝', '🐻', '🦊', '🐱',
  '🎮', '🏰', '🌙', '⭐', '🎪', '🎨',
]

const levelOptions = [
  { value: 'PRE_A1' as const, badge: '🌱', label: 'Pre-A1 零基础' },
  { value: 'A1' as const, badge: '📗', label: 'A1 入门级' },
  { value: 'A2' as const, badge: '📘', label: 'A2 初级' },
  { value: 'B1' as const, badge: '📙', label: 'B1 中级' },
]

const levelLabel = computed(() => {
  const opt = levelOptions.find(o => o.value === user.value?.cefrLevel)
  return opt ? `${opt.badge} ${opt.label}` : 'PRE_A1'
})

const hasChanges = computed(() =>
  form.value.displayName !== user.value?.displayName ||
  form.value.avatar !== user.value?.avatar ||
  form.value.cefrLevel !== user.value?.cefrLevel ||
  form.value.ttsVoice !== user.value?.ttsVoice
)

async function save() {
  saving.value = true
  saved.value = false
  await userStore.updateProfile({
    displayName: form.value.displayName.trim(),
    avatar: form.value.avatar,
    cefrLevel: form.value.cefrLevel,
    ttsVoice: form.value.ttsVoice,
  })
  saving.value = false
  saved.value = true
  setTimeout(() => { saved.value = false }, 2000)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.profile-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: $text-primary;
  margin-bottom: 20px;
}

// Profile card
.profile-card {
  padding: 24px;
  border-radius: $radius-lg;
  margin-bottom: 20px;
}
.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}
.profile-avatar-display {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(139, 92, 246, 0.2));
  border: 3px solid rgba(245, 158, 11, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
}
.profile-name {
  font-size: 20px;
  font-weight: 800;
  color: $text-primary;
}
.profile-username {
  font-size: 13px;
  color: $text-muted;
  margin-top: 2px;
}
.profile-level-badge {
  margin-top: 6px;
  display: inline-block;
  padding: 2px 10px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: $gold-light;
}

.stats-strip {
  display: flex;
  justify-content: space-around;
  padding-top: 16px;
  border-top: 1px solid $border-dim;
}
.strip-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: $text-secondary;
}
.strip-icon { font-size: 14px; }

// Edit section
.edit-section {
  padding: 24px;
  border-radius: $radius-lg;
  margin-bottom: 20px;
}
.section-title {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 20px;
}
.field {
  margin-bottom: 20px;
}
.field-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: $text-secondary;
  margin-bottom: 8px;
}
.input-dark {
  width: 100%;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid $border-dim;
  border-radius: $radius-md;
  color: $text-primary;
  font-size: 15px;
  outline: none;
  &:focus {
    border-color: $gold;
    box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.15);
  }
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;
}
.avatar-option {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  background: rgba(255, 255, 255, 0.04);
  border: 2px solid transparent;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.15s;
  &:hover { background: rgba(255, 255, 255, 0.08); border-color: rgba(245, 158, 11, 0.2); }
  &.selected { border-color: $gold; background: rgba(245, 158, 11, 0.1); }
}

.level-select {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.level-chip {
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid $border-dim;
  border-radius: 20px;
  color: $text-secondary;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  &:hover { border-color: rgba(245, 158, 11, 0.3); }
  &.selected { border-color: $gold; color: $gold-light; background: rgba(245, 158, 11, 0.1); }
}

.form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.btn-gold {
  padding: 10px 24px;
  background: linear-gradient(135deg, $gold-dark, $gold);
  border: none;
  border-radius: $radius-md;
  color: #1a1a2e;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.save-ok {
  font-size: 13px;
  color: #22c55e;
  font-weight: 600;
}

.back-btn {
  display: inline-block;
  padding: 10px 20px;
  background: transparent;
  border: 1px solid $border-dim;
  border-radius: $radius-md;
  color: $text-secondary;
  font-size: 14px;
  cursor: pointer;
  &:hover { border-color: $gold; color: $gold-light; }
}
</style>
