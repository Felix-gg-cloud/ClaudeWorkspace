<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="login-content">
        <div class="login-logo animate__animated animate__bounceIn">📚</div>
        <h1 class="login-title animate__animated animate__fadeInUp">LingoBuddy</h1>
        <p class="login-subtitle animate__animated animate__fadeInUp animate__delay-1s">
          趣味英语，每日进步
        </p>

        <div class="login-form animate__animated animate__fadeInUp animate__delay-1s">
          <div class="form-group">
            <input
              v-model="username"
              type="text"
              placeholder="用户名"
              class="form-input"
              @keyup.enter="handleLogin"
            />
          </div>
          <div class="form-group">
            <input
              v-model="password"
              type="password"
              placeholder="密码"
              class="form-input"
              @keyup.enter="handleLogin"
            />
          </div>
          <button
            class="btn-primary login-btn"
            :disabled="loading"
            @click="handleLogin"
          >
            <span v-if="loading">登录中...</span>
            <span v-else>🚀 开始学习</span>
          </button>
          <p v-if="error" class="login-error animate__animated animate__shakeX">{{ error }}</p>
        </div>

        <p class="login-hint">默认账号: admin / admin123</p>
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
}

.login-bg {
  width: 100%;
  max-width: 480px;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.login-content {
  text-align: center;
  width: 100%;
}

.login-logo {
  font-size: 72px;
  margin-bottom: 12px;
}

.login-title {
  color: white;
  font-size: 36px;
  font-weight: 800;
  margin-bottom: 8px;
  letter-spacing: 2px;
}

.login-subtitle {
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
  margin-bottom: 48px;
}

.login-form {
  width: 100%;
  max-width: 320px;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 16px;
}

.form-input {
  width: 100%;
  padding: 14px 20px;
  border: none;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  font-size: 16px;
  outline: none;
  backdrop-filter: blur(10px);
  transition: background 0.3s;

  &::placeholder {
    color: rgba(255, 255, 255, 0.6);
  }

  &:focus {
    background: rgba(255, 255, 255, 0.3);
  }
}

.login-btn {
  width: 100%;
  padding: 16px;
  font-size: 18px;
  margin-top: 8px;
  background: white;
  color: #667eea;
  border-radius: 14px;

  &:hover {
    background: #f0f0f0;
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
  }
}

.login-error {
  color: #fed7d7;
  margin-top: 12px;
  font-size: 14px;
}

.login-hint {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  margin-top: 32px;
}
</style>
