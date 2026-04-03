<template>
  <button
    v-if="supported"
    class="speak-btn"
    :class="{ speaking }"
    :title="speaking ? '停止朗读' : '点击朗读'"
    @click="toggle"
  >
    <AppIcon :name="speaking ? 'volume-x' : 'volume-2'" :size="size" />
  </button>
</template>

<script setup lang="ts">
import { useTts } from '@/composables/useTts'
import AppIcon from '@/components/AppIcon.vue'

const props = withDefaults(defineProps<{
  text: string
  lang?: string
  size?: number
}>(), {
  lang: 'en-US',
  size: 18,
})

const { speak, stop, speaking, supported } = useTts()

function toggle() {
  if (speaking.value) {
    stop()
  } else {
    speak(props.text, props.lang)
  }
}
</script>

<style scoped lang="scss">
.speak-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--primary);
    color: var(--primary);
  }

  &.speaking {
    border-color: var(--primary);
    color: var(--primary);
    animation: pulse 1s ease-in-out infinite;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
