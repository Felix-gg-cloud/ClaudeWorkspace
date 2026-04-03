<template>
  <div class="login-page">
    <!-- 左侧品牌区域 -->
    <div class="login-brand">
      <div class="brand-bg">
        <div class="brand-orb brand-orb-1" />
        <div class="brand-orb brand-orb-2" />
        <div class="brand-orb brand-orb-3" />
        <div class="brand-grid" />
      </div>
      <div class="brand-content">
        <div class="brand-logo-wrap">
          <div class="brand-logo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" /></svg>
          </div>
        </div>
        <h1 class="brand-title">LinguaLeap</h1>
        <p class="brand-subtitle">AI 驱动的智能英语学习平台</p>
        <div class="brand-features">
          <div class="brand-feature" v-for="f in brandFeatures" :key="f.title">
            <span class="brand-feature-icon" v-html="f.icon" />
            <div>
              <div class="brand-feature-title">{{ f.title }}</div>
              <div class="brand-feature-desc">{{ f.desc }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧表单区域 -->
    <div class="login-form-side">
      <div class="login-form-container">
        <div class="form-header">
          <h2 class="form-title">{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</h2>
          <p class="form-desc">{{ mode === 'login' ? '登录以继续你的学习旅程' : '注册开始你的英语学习之旅' }}</p>
        </div>

        <Transition name="shake">
          <div v-if="error" :key="error" class="error-toast">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            {{ error }}
          </div>
        </Transition>

        <Transition name="form-switch" mode="out-in">
          <form v-if="mode === 'login'" key="login" @submit.prevent="handleLogin" class="login-form">
            <div class="form-field">
              <label>用户名</label>
              <input v-model="loginForm.username" type="text" required autocomplete="username" placeholder="请输入用户名" />
            </div>
            <div class="form-field">
              <label>密码</label>
              <input v-model="loginForm.password" type="password" required autocomplete="current-password" placeholder="请输入密码" />
            </div>
            <button type="submit" class="submit-btn" :disabled="loading">
              <span v-if="loading" class="spinner" />
              <span>{{ loading ? '登录中...' : '登 录' }}</span>
              <svg v-if="!loading" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
            </button>
          </form>

          <form v-else key="register" @submit.prevent="handleRegister" class="login-form">
            <div class="form-row">
              <div class="form-field">
                <label>用户名</label>
                <input v-model="regForm.username" type="text" required minlength="3" maxlength="20" autocomplete="username" placeholder="3-20位字母数字" />
              </div>
              <div class="form-field">
                <label>密码</label>
                <input v-model="regForm.password" type="password" required minlength="6" autocomplete="new-password" placeholder="至少6位" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-field">
                <label>昵称</label>
                <input v-model="regForm.displayName" type="text" autocomplete="nickname" placeholder="可选" />
              </div>
              <div class="form-field">
                <label>年级</label>
                <select v-model="regForm.grade">
                  <option value="">选择年级</option>
                  <option v-for="g in grades" :key="g.value" :value="g.value">{{ g.label }}</option>
                </select>
              </div>
            </div>
            <button type="submit" class="submit-btn" :disabled="loading">
              <span v-if="loading" class="spinner" />
              <span>{{ loading ? '注册中...' : '注 册' }}</span>
              <svg v-if="!loading" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
            </button>
          </form>
        </Transition>

        <div class="switch-mode">
          <span>{{ mode === 'login' ? '还没有账号？' : '已有账号？' }}</span>
          <button @click="mode = mode === 'login' ? 'register' : 'login'" class="switch-btn">
            {{ mode === 'login' ? '立即注册' : '去登录' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const error = ref('')

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', displayName: '', grade: '' })

const grades = [
  { value: '七年级', label: '七年级' },
  { value: '八年级', label: '八年级' },
  { value: '九年级', label: '九年级' },
  { value: '高一', label: '高一' },
  { value: '高二', label: '高二' },
  { value: '高三', label: '高三' },
  { value: '大学', label: '大学' },
  { value: '其他', label: '其他' },
]

const brandFeatures = [
  { icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 3l1.2 3.7a1 1 0 0 0 .6.6L17.5 8.5l-3.7 1.2a1 1 0 0 0-.6.6L12 14l-1.2-3.7a1 1 0 0 0-.6-.6L6.5 8.5l3.7-1.2a1 1 0 0 0 .6-.6z"/></svg>', title: 'AI 智能教学', desc: 'GPT-4o 驱动的个性化学习' },
  { icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 2 7 12 12 22 7"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>', title: '分级知识库', desc: '从小学到高中全覆盖' },
  { icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>', title: '间隔复习', desc: '科学记忆曲线巩固知识' },
]

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await userStore.login(loginForm.username, loginForm.password)
    router.replace('/')
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  error.value = ''
  loading.value = true
  try {
    await userStore.register(regForm.username, regForm.password, regForm.displayName || undefined, regForm.grade || undefined)
    router.replace('/')
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  display: flex;
  min-height: 100vh;
}

/* ====== 左侧品牌区域 ====== */
.login-brand {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0F0B2E;
  overflow: hidden;
}

.brand-bg {
  position: absolute;
  inset: 0;
}

.brand-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);

  &-1 {
    width: 500px; height: 500px;
    background: radial-gradient(circle, rgba(99, 102, 241, 0.4), transparent 70%);
    top: -15%; right: -10%;
    animation: orbFloat 8s ease-in-out infinite alternate;
  }
  &-2 {
    width: 400px; height: 400px;
    background: radial-gradient(circle, rgba(139, 92, 246, 0.3), transparent 70%);
    bottom: -10%; left: -5%;
    animation: orbFloat 10s ease-in-out infinite alternate-reverse;
  }
  &-3 {
    width: 300px; height: 300px;
    background: radial-gradient(circle, rgba(6, 182, 212, 0.25), transparent 70%);
    top: 40%; left: 30%;
    animation: orbFloat 12s ease-in-out infinite alternate;
  }
}

@keyframes orbFloat {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(30px, -25px) scale(1.15); }
}

.brand-grid {
  position: absolute;
  inset: 0;
  opacity: 0.06;
  background-image:
    linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px);
  background-size: 60px 60px;
}

.brand-content {
  position: relative;
  z-index: 1;
  padding: 60px;
  max-width: 480px;
}

.brand-logo-wrap {
  margin-bottom: 32px;
}

.brand-logo {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6, #A78BFA);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 40px rgba(99, 102, 241, 0.4), 0 0 80px rgba(99, 102, 241, 0.15);
  animation: logoGlow 3s ease-in-out infinite alternate;

  svg { width: 36px; height: 36px; color: white; }
}

@keyframes logoGlow {
  from { box-shadow: 0 0 40px rgba(99, 102, 241, 0.4), 0 0 80px rgba(99, 102, 241, 0.15); }
  to { box-shadow: 0 0 60px rgba(99, 102, 241, 0.6), 0 0 120px rgba(99, 102, 241, 0.25); }
}

.brand-title {
  font-size: 42px;
  font-weight: 800;
  color: white;
  letter-spacing: -1px;
  margin-bottom: 10px;
  background: linear-gradient(135deg, #fff 0%, #C4B5FD 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-subtitle {
  font-size: 17px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 48px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.brand-feature {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.brand-feature-icon {
  width: 44px;
  height: 44px;
  min-width: 44px;
  border-radius: 12px;
  background: rgba(99, 102, 241, 0.15);
  border: 1px solid rgba(99, 102, 241, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;

  :deep(svg) { width: 22px; height: 22px; color: #A78BFA; }
}

.brand-feature-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 3px;
}

.brand-feature-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
}

/* ====== 右侧表单区域 ====== */
.login-form-side {
  flex: 0 0 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: linear-gradient(160deg, #F8FAFC 0%, #EEF2FF 50%, #F8FAFC 100%);
}

.login-form-container {
  width: 100%;
  max-width: 400px;
  animation: formSlide 0.5s ease-out;
}

@keyframes formSlide {
  from { opacity: 0; transform: translateX(20px); }
  to { opacity: 1; transform: translateX(0); }
}

.form-header {
  margin-bottom: 32px;
}

.form-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary, #0F172A);
  letter-spacing: -0.5px;
  margin-bottom: 8px;
}

.form-desc {
  font-size: 15px;
  color: var(--text-muted, #94A3B8);
}

.error-toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.15);
  border-radius: 14px;
  font-size: 14px;
  color: #DC2626;
  margin-bottom: 20px;

  svg { width: 18px; height: 18px; flex-shrink: 0; }
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;

  label {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-secondary, #64748B);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  input, select {
    height: 48px;
    padding: 0 16px;
    border: 1.5px solid var(--border, #E2E8F0);
    border-radius: 14px;
    font-size: 15px;
    color: var(--text-primary, #0F172A);
    background: var(--bg-card, #fff);
    outline: none;
    transition: all 0.2s ease;
    font-family: inherit;

    &:focus {
      border-color: #6366F1;
      box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
    }

    &::placeholder {
      color: var(--text-muted, #94A3B8);
    }
  }

  select {
    appearance: none;
    background-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3E%3Cpath fill='none' stroke='%2394A3B8' stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='m2 5 6 6 6-6'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 14px center;
    background-size: 14px;
    padding-right: 40px;
    cursor: pointer;
  }
}

.submit-btn {
  margin-top: 8px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #6366F1 0%, #8B5CF6 50%, #7C3AED 100%);
  color: white;
  font-size: 16px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  border: none;
  transition: all 0.25s ease;
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.35);

  svg { width: 18px; height: 18px; transition: transform 0.2s; }

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 30px rgba(99, 102, 241, 0.4);
    svg { transform: translateX(4px); }
  }

  &:active:not(:disabled) { transform: translateY(0); }
  &:disabled { opacity: 0.6; cursor: not-allowed; }
}

.spinner {
  width: 18px; height: 18px;
  border: 2.5px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.switch-mode {
  margin-top: 28px;
  text-align: center;
  font-size: 14px;
  color: var(--text-muted, #94A3B8);
}

.switch-btn {
  color: #6366F1;
  font-weight: 600;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  margin-left: 4px;

  &:hover { text-decoration: underline; }
}

/* 过渡动画 */
.shake-enter-active { animation: shake 0.4s ease; }
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}

.form-switch-enter-active { animation: formIn 0.3s ease-out; }
.form-switch-leave-active { animation: formOut 0.15s ease-in; }
@keyframes formIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
@keyframes formOut { from { opacity: 1; } to { opacity: 0; transform: translateY(-10px); } }

/* 移动端：隐藏左侧品牌区 */
@media (max-width: 900px) {
  .login-brand { display: none; }
  .login-form-side { flex: none; width: 100%; }
}
</style>
