<template>
  <div class="login-page">
    <!-- Animated magic circle background -->
    <div class="magic-bg">
      <div class="magic-circle circle-1"></div>
      <div class="magic-circle circle-2"></div>
      <div class="magic-circle circle-3"></div>
      <div class="rune-grid">
        <div v-for="i in 30" :key="i" class="rune" :style="runeStyle(i)">✦</div>
      </div>
    </div>

    <div class="login-container">
      <!-- Logo -->
      <div class="login-logo">
        <div class="logo-shield">⚔️</div>
        <h1 class="logo-title">
          <span class="text-gold">English</span><span class="text-fire">Quest</span>
        </h1>
        <p class="logo-subtitle">零基础英语 RPG 养成</p>
      </div>

      <!-- Login Card -->
      <div class="login-card">
        <h2 class="card-title">冒险者登录</h2>

        <div v-if="error" class="error-msg">
          <span>⚠️</span> {{ error }}
        </div>

        <div class="field">
          <label class="field-label">用户名</label>
          <input
            v-model="username"
            class="input-dark"
            placeholder="输入你的冒险者名称"
            @keyup.enter="handleLogin"
          />
        </div>

        <div class="field">
          <label class="field-label">密码</label>
          <input
            v-model="password"
            type="password"
            class="input-dark"
            placeholder="输入秘钥"
            @keyup.enter="handleLogin"
          />
        </div>

        <button class="btn-gold login-btn" :disabled="loading" @click="handleLogin">
          <span v-if="loading" class="loading-spinner"></span>
          <span v-else>⚔️ 进入冒险</span>
        </button>

        <p class="hint-text">默认账号：admin / admin123</p>
      </div>

      <!-- Feature tags -->
      <div class="feature-tags">
        <span class="tag">📖 主线任务</span>
        <span class="tag">🏟️ SRS副本</span>
        <span class="tag">💀 Boss战</span>
        <span class="tag">🌟 技能树</span>
        <span class="tag">🔥 连击系统</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSound } from '@/composables/useSound'

const router = useRouter()
const userStore = useUserStore()
const sound = useSound()

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

  // Simulate network delay
  await new Promise(r => setTimeout(r, 600))

  const ok = userStore.login(username.value, password.value)
  if (ok) {
    sound.coin()
    router.replace('/dashboard')
  } else {
    error.value = '用户名或密码错误'
    sound.wrong()
  }
  loading.value = false
}

function runeStyle(i: number) {
  return {
    left: Math.random() * 100 + '%',
    top: Math.random() * 100 + '%',
    animationDelay: Math.random() * 5 + 's',
    fontSize: (8 + Math.random() * 14) + 'px',
    opacity: 0.05 + Math.random() * 0.15
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: radial-gradient(ellipse at center, #0f1629 0%, $bg-abyss 70%);
}

// Magic background
.magic-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.magic-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(245, 158, 11, 0.08);

  &.circle-1 {
    width: 500px; height: 500px;
    top: 50%; left: 50%;
    transform: translate(-50%, -50%);
    animation: spin-slow 30s linear infinite;
  }

  &.circle-2 {
    width: 700px; height: 700px;
    top: 50%; left: 50%;
    transform: translate(-50%, -50%);
    border-color: rgba(124, 58, 237, 0.06);
    animation: spin-slow 45s linear infinite reverse;
  }

  &.circle-3 {
    width: 900px; height: 900px;
    top: 50%; left: 50%;
    transform: translate(-50%, -50%);
    border-color: rgba(220, 38, 38, 0.04);
    animation: spin-slow 60s linear infinite;
  }
}

.rune-grid {
  position: absolute;
  inset: 0;
}

.rune {
  position: absolute;
  color: $gold;
  animation: pulse-glow 4s infinite ease-in-out;
}

// Container
.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
  width: 100%;
  max-width: 420px;
  padding: 0 20px;
}

// Logo
.login-logo {
  text-align: center;
}

.logo-shield {
  font-size: 56px;
  margin-bottom: 12px;
  animation: float 3s ease-in-out infinite;
  filter: drop-shadow(0 0 20px rgba(245, 158, 11, 0.4));
}

.logo-title {
  font-size: 36px;
  font-weight: 900;
  letter-spacing: -1px;
}

.logo-subtitle {
  color: $text-muted;
  font-size: 14px;
  margin-top: 6px;
}

// Card
.login-card {
  width: 100%;
  background: $glass-bg;
  border: 1px solid $glass-border;
  border-radius: $radius-xl;
  backdrop-filter: blur(20px);
  padding: 32px;
  animation: scale-in 0.4s ease-out;
}

.card-title {
  text-align: center;
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 24px;
  color: $text-primary;
}

.error-msg {
  padding: 10px 14px;
  background: rgba(220, 38, 38, 0.1);
  border: 1px solid rgba(220, 38, 38, 0.2);
  border-radius: $radius-md;
  color: $hp-red-glow;
  font-size: 13px;
  margin-bottom: 16px;
  animation: shake 0.4s;
}

.field {
  margin-bottom: 16px;
}

.field-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: $text-secondary;
  margin-bottom: 6px;
}

.login-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  margin-top: 8px;
}

.hint-text {
  text-align: center;
  font-size: 12px;
  color: $text-muted;
  margin-top: 16px;
}

// Loading spinner
.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(10, 14, 26, 0.3);
  border-top-color: #0a0e1a;
  border-radius: 50%;
  animation: spin-slow 0.6s linear infinite;
}

// Feature tags
.feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.tag {
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid $border-dim;
  border-radius: 20px;
  font-size: 12px;
  color: $text-muted;
}
</style>
