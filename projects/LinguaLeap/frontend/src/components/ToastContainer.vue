<script setup lang="ts">
import { useToast } from '@/composables/useToast'

const { toasts } = useToast()
</script>

<template>
  <div class="toast-container">
    <TransitionGroup name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast-item"
        :class="'toast-' + toast.type"
      >
        <span class="toast-icon">
          {{ toast.type === 'error' ? '✕' : toast.type === 'success' ? '✓' : 'ℹ' }}
        </span>
        <span class="toast-msg">{{ toast.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 1rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  pointer-events: none;
}

.toast-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  pointer-events: auto;
  min-width: 200px;
  max-width: 400px;
}

.toast-error {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.toast-success {
  background: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.toast-info {
  background: #eff6ff;
  color: #1e40af;
  border: 1px solid #bfdbfe;
}

.toast-icon {
  flex-shrink: 0;
  width: 1.25rem;
  height: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 0.75rem;
  font-weight: bold;
}

.toast-error .toast-icon { background: #fecaca; }
.toast-success .toast-icon { background: #bbf7d0; }
.toast-info .toast-icon { background: #bfdbfe; }

.toast-enter-active { transition: all 0.3s ease; }
.toast-leave-active { transition: all 0.2s ease; }
.toast-enter-from { opacity: 0; transform: translateY(-1rem); }
.toast-leave-to { opacity: 0; transform: translateY(-0.5rem); }
</style>
