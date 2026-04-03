import { ref } from 'vue'

export interface ToastItem {
  id: number
  message: string
  type: 'error' | 'success' | 'info'
}

const toasts = ref<ToastItem[]>([])
let nextId = 0

export function showToast(message: string, type: ToastItem['type'] = 'error', duration = 3000) {
  const id = nextId++
  toasts.value.push({ id, message, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, duration)
}

export function useToast() {
  return { toasts, showToast }
}
