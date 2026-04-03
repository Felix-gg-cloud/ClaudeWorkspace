<template>
  <div class="settings">
    <h1>设置</h1>

    <div class="settings-card">
      <h2>个人信息</h2>

      <div class="form-group">
        <label>用户名</label>
        <input type="text" :value="userStore.user?.username" disabled class="input disabled" />
      </div>

      <div class="form-group">
        <label>昵称</label>
        <input
          v-model="form.displayName"
          type="text"
          class="input"
          placeholder="输入你的昵称"
          maxlength="20"
        />
      </div>

      <div class="form-group">
        <label>年级</label>
        <div class="grade-select">
          <button
            v-for="g in grades"
            :key="g.value"
            class="grade-option"
            :class="{ active: form.grade === g.value }"
            @click="form.grade = g.value"
          >
            {{ g.label }}
          </button>
        </div>
        <p class="hint">年级决定题目难度和知识库推荐</p>
      </div>

      <button class="btn-save" :disabled="saving || !hasChanges" @click="save">
        <template v-if="saving">保存中...</template>
        <template v-else>保存修改</template>
      </button>

      <p v-if="saveMsg" class="save-msg" :class="{ error: saveError }">{{ saveMsg }}</p>
    </div>

    <div class="settings-card danger-zone">
      <h2>账号</h2>
      <button class="btn-logout" @click="handleLogout">退出登录</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const grades = [
  { value: 'elementary', label: '小学' },
  { value: 'junior', label: '初中' },
  { value: 'senior', label: '高中' },
]

const form = reactive({
  displayName: '',
  grade: 'junior',
})

const saving = ref(false)
const saveMsg = ref('')
const saveError = ref(false)

const hasChanges = computed(() => {
  return form.displayName !== (userStore.user?.displayName || '')
    || form.grade !== (userStore.user?.grade || 'junior')
})

onMounted(() => {
  form.displayName = userStore.user?.displayName || ''
  form.grade = userStore.user?.grade || 'junior'
})

async function save() {
  if (!hasChanges.value || saving.value) return
  saving.value = true
  saveMsg.value = ''
  try {
    await userStore.updateProfile({
      displayName: form.displayName,
      grade: form.grade,
    })
    saveMsg.value = '保存成功'
    saveError.value = false
  } catch {
    saveMsg.value = '保存失败，请重试'
    saveError.value = true
  } finally {
    saving.value = false
    setTimeout(() => { saveMsg.value = '' }, 3000)
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.settings {
  max-width: 600px;
  margin: 0 auto;
  animation: fadeUp 0.4s ease-out;

  h1 {
    font-size: 24px;
    font-weight: 800;
    margin-bottom: 24px;
    color: var(--text-primary);
  }
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.settings-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 28px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  }

  h2 {
    font-size: 17px;
    font-weight: 700;
    margin-bottom: 20px;
    color: var(--text-primary);
  }
}

.form-group {
  margin-bottom: 20px;

  label {
    display: block;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-secondary);
    margin-bottom: 8px;
  }
}

.input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  font-size: 15px;
  background: var(--bg-main);
  color: var(--text-primary);
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;

  &:focus {
    outline: none;
    border-color: var(--primary);
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
  }

  &.disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.grade-select {
  display: flex;
  gap: 10px;
}

.grade-option {
  flex: 1;
  padding: 12px;
  border: 2px solid var(--border);
  border-radius: 12px;
  background: var(--bg-main);
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: rgba(99, 102, 241, 0.3);
    color: var(--text-primary);
  }

  &.active {
    border-color: var(--primary);
    background: color-mix(in srgb, var(--primary) 8%, transparent);
    color: var(--primary);
  }
}

.hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 6px;
}

.btn-save {
  width: 100%;
  padding: 13px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366F1, #4F46E5);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.25);
  transition: all 0.2s;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(99, 102, 241, 0.35);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
    box-shadow: none;
  }
}

.save-msg {
  text-align: center;
  margin-top: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #10b981;

  &.error {
    color: #ef4444;
  }
}

.danger-zone {
  .btn-logout {
    width: 100%;
    padding: 13px;
    border: 2px solid rgba(239, 68, 68, 0.25);
    border-radius: 12px;
    background: rgba(239, 68, 68, 0.04);
    color: #ef4444;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #ef4444;
      border-color: #ef4444;
      color: #fff;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(239, 68, 68, 0.25);
    }
  }
}
</style>
