<template>
  <div class="login-page">
    <!-- Animated Background -->
    <div class="login-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
      <div class="bg-orb bg-orb-3"></div>
      <div class="hex-grid"></div>
    </div>

    <div class="login-container">
      <!-- Logo with glow ring -->
      <div class="logo-section">
        <div class="logo-ring">
          <div class="logo-inner">📚</div>
        </div>
        <h1 class="brand-name">
          <span class="brand-lingo">Lingo</span><span class="brand-buddy">Buddy</span>
        </h1>
        <p class="brand-tagline">AI 驱动的趣味英语学习</p>
      </div>

      <!-- Login Card -->
      <div class="login-card">
        <div class="card-glow"></div>
        <h2 class="card-title">欢迎回来</h2>
        <p class="card-sub">登录开始今天的学习旅程</p>

        <div class="form-field">
          <label class="field-label">
            <span class="label-icon">👤</span> 用户名
          </label>
          <input
            v-model="username"
            type="text"
            placeholder="输入用户名"
            class="input-dark"
            @keyup.enter="handleLogin"
          />
        </div>

        <div class="form-field">
          <label class="field-label">
            <span class="label-icon">🔑</span> 密码
          </label>
          <input
            v-model="password"
            type="password"
            placeholder="输入密码"
            class="input-dark"
            @keyup.enter="handleLogin"
          />
        </div>

        <button
          class="btn-primary login-btn"
          :disabled="loading"
          @click="handleLogin"
        >
          <span v-if="loading" class="loading-dots">
            <i></i><i></i><i></i>
          </span>
          <span v-else>🚀 开始学习</span>
        </button>

        <p v-if="error" class="error-msg">{{ error }}</p>
      </div>

      <p class="login-hint">默认账号: admin / admin123</p>

      <!-- Feature pills -->
      <div class="features">
        <span class="feature-pill">📖 75+ 真实内容</span>
        <span class="feature-pill">🏆 成就系统</span>
        <span class="feature-pill">🔥 连续打卡</span>
        <span class="feature-pill">📈 等级进阶</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  if (!username.value || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await userStore.login(username.value, password.value)
    router.push('/dashboard')
  } catch (e: any) {
    error.value = e.response?.data?.error || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: #080b1a;
}

// Animated Background
.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
}

.bg-orb-1 {
  width: 400px;
  height: 400px;
  background: rgba(0, 212, 255, 0.25);
  top: -100px;
  right: -100px;
  animation: float 20s infinite ease-in-out;
}

.bg-orb-2 {
  width: 350px;
  height: 350px;
  background: rgba(168, 85, 247, 0.2);
  bottom: -80px;
  left: -80px;
  animation: float 25s infinite ease-in-out reverse;
}

.bg-orb-3 {
  width: 250px;
  height: 250px;
  background: rgba(236, 72, 153, 0.15);
  top: 40%;
  left: 60%;
  animation: float 18s infinite ease-in-out 3s;
}

.hex-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px);
  background-size: 50px 50px;
}

.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: 24px;
  animation: slide-up 0.6s ease-out;
}

// Logo Section
.logo-section {
  text-align: center;
  margin-bottom: 32px;
}

.logo-ring {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: linear-gradient(135deg, #00d4ff, #a855f7);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  animation: glow-ring 3s infinite ease-in-out;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: -3px;
    border-radius: 50%;
    background: linear-gradient(135deg, #00d4ff, #a855f7, #ec4899);
    z-index: -1;
    animation: counter-spin 6s linear infinite;
    opacity: 0.6;
    filter: blur(4px);
  }
}

.logo-inner {
  width: 78px;
  height: 78px;
  border-radius: 50%;
  background: #0c1029;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38px;
}

.brand-name {
  font-size: 36px;
  font-weight: 800;
  letter-spacing: 1px;
}

.brand-lingo {
  background: linear-gradient(135deg, #00d4ff, #06b6d4);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-buddy {
  background: linear-gradient(135deg, #a855f7, #ec4899);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-tagline {
  color: #5a6480;
  font-size: 14px;
  margin-top: 6px;
  letter-spacing: 2px;
}

// Login Card
.login-card {
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 32px 24px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.3), transparent);
  }
}

.card-glow {
  position: absolute;
  top: -100px;
  left: 50%;
  transform: translateX(-50%);
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.08), transparent 70%);
  pointer-events: none;
}

.card-title {
  font-size: 22px;
  font-weight: 700;
  color: #e8ecf4;
  text-align: center;
}

.card-sub {
  font-size: 13px;
  color: #5a6480;
  text-align: center;
  margin: 4px 0 24px;
}

.form-field {
  margin-bottom: 16px;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #8b95b0;
  margin-bottom: 6px;
}

.label-icon {
  font-size: 14px;
}

.login-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  margin-top: 8px;
  border-radius: 14px;
}

.loading-dots {
  display: flex;
  gap: 6px;
  justify-content: center;

  i {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: white;
    animation: typing-dots 1.2s infinite;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

.error-msg {
  text-align: center;
  color: #ef4444;
  font-size: 13px;
  margin-top: 12px;
  padding: 8px;
  background: rgba(239, 68, 68, 0.1);
  border-radius: 8px;
  border: 1px solid rgba(239, 68, 68, 0.2);
  animation: shake 0.4s;
}

.login-hint {
  text-align: center;
  color: #5a6480;
  font-size: 12px;
  margin-top: 16px;
}

// Feature pills
.features {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
}

.feature-pill {
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 20px;
  font-size: 12px;
  color: #8b95b0;
  white-space: nowrap;
}
</style>
